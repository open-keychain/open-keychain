/*
 * Copyright (C) 2014-2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.NfcSignOperationsBuilder;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** This is a high-level operation, which encapsulates one or more sign/encrypt
 * operations, using URIs or byte arrays as input and output.
 *
 * This operation is fail-fast: If any sign/encrypt sub-operation fails or returns
 * a pending result, it will terminate.
 *
 */
public class SignEncryptOperation extends BaseOperation {

    public SignEncryptOperation(Context context, ProviderHelper providerHelper,
                                Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public SignEncryptResult execute(SignEncryptParcel input, CryptoInputParcel cryptoInput) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_SE, 0);

        ArrayDeque<Uri> inputUris = new ArrayDeque<>(input.getInputUris());
        ArrayDeque<Uri> outputUris = new ArrayDeque<>(input.getOutputUris());
        byte[] inputBytes = input.getBytes();
        byte[] outputBytes = null;

        int total = inputBytes != null ? 1 : inputUris.size(), count = 0;
        ArrayList<PgpSignEncryptResult> results = new ArrayList<>();

        NfcSignOperationsBuilder pendingInputBuilder = null;

        // if signing subkey has not explicitly been set, get first usable subkey capable of signing
        if (input.getSignatureMasterKeyId() != Constants.key.none
                && input.getSignatureSubKeyId() == null) {
            try {
                long signKeyId = mProviderHelper.getCachedPublicKeyRing(
                        input.getSignatureMasterKeyId()).getSecretSignId();
                input.setSignatureSubKeyId(signKeyId);
            } catch (PgpKeyNotFoundException e) {
                e.printStackTrace();
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
            }
        }

        do {

            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new SignEncryptResult(SignEncryptResult.RESULT_CANCELLED, log, results);
            }

            InputData inputData;
            {
                if (inputBytes != null) {
                    log.add(LogType.MSG_SE_INPUT_BYTES, 1);
                    InputStream is = new ByteArrayInputStream(inputBytes);
                    inputData = new InputData(is, inputBytes.length);
                    inputBytes = null;
                } else {
                    if (inputUris.isEmpty()) {
                        log.add(LogType.MSG_SE_ERROR_NO_INPUT, 1);
                        return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
                    }

                    log.add(LogType.MSG_SE_INPUT_URI, 1);
                    Uri uri = inputUris.removeFirst();
                    try {
                        InputStream is = mContext.getContentResolver().openInputStream(uri);
                        long fileSize = FileHelper.getFileSize(mContext, uri, 0);
                        String filename = FileHelper.getFilename(mContext, uri);
                        inputData = new InputData(is, fileSize, filename);
                    } catch (FileNotFoundException e) {
                        log.add(LogType.MSG_SE_ERROR_INPUT_URI_NOT_FOUND, 1);
                        return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
                    }
                }
            }

            OutputStream outStream;
            {
                if (!outputUris.isEmpty()) {
                    try {
                        Uri outputUri = outputUris.removeFirst();
                        outStream = mContext.getContentResolver().openOutputStream(outputUri);
                    } catch (FileNotFoundException e) {
                        log.add(LogType.MSG_SE_ERROR_OUTPUT_URI_NOT_FOUND, 1);
                        return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
                    }
                } else {
                    if (outputBytes != null) {
                        log.add(LogType.MSG_SE_ERROR_TOO_MANY_INPUTS, 1);
                        return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
                    }
                    outStream = new ByteArrayOutputStream();
                }
            }

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(mContext, mProviderHelper,
                    new ProgressScaler(mProgressable, 100 * count / total, 100 * ++count / total, 100), mCancelled);
            PgpSignEncryptResult result = op.execute(input, cryptoInput, inputData, outStream);
            results.add(result);
            log.add(result, 2);

            if (result.isPending()) {
                RequiredInputParcel requiredInput = result.getRequiredInputParcel();
                // Passphrase returns immediately, nfc are aggregated
                if (requiredInput.mType == RequiredInputType.PASSPHRASE) {
                    return new SignEncryptResult(log, requiredInput, results);
                }
                if (pendingInputBuilder == null) {
                    pendingInputBuilder = new NfcSignOperationsBuilder(requiredInput.mSignatureTime,
                            input.getSignatureMasterKeyId(), input.getSignatureSubKeyId());
                }
                pendingInputBuilder.addAll(requiredInput);
            } else if (!result.success()) {
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
            }

            if (outStream instanceof ByteArrayOutputStream) {
                outputBytes = ((ByteArrayOutputStream) outStream).toByteArray();
            }

        } while (!inputUris.isEmpty());

        if (pendingInputBuilder != null && !pendingInputBuilder.isEmpty()) {
            return new SignEncryptResult(log, pendingInputBuilder.build(), results);
        }

        if (!outputUris.isEmpty()) {
            throw new AssertionError("Got outputs left but no inputs. This is a programming error, please report!");
        }

        log.add(LogType.MSG_SE_SUCCESS, 1);
        return new SignEncryptResult(SignEncryptResult.RESULT_OK, log, results, outputBytes);

    }

}
