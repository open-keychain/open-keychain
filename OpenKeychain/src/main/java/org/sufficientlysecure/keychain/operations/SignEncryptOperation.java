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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
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
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.system.OsConstants.S_IFMT;
import static android.system.OsConstants.S_IROTH;

/**
 * This is a high-level operation, which encapsulates one or more sign/encrypt
 * operations, using URIs or byte arrays as input and output.
 *
 * This operation is fail-fast: If any sign/encrypt sub-operation fails or returns
 * a pending result, it will terminate.
 */
public class SignEncryptOperation extends BaseOperation<SignEncryptParcel> {

    public SignEncryptOperation(Context context, ProviderHelper providerHelper,
                                Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }


    /**
     * Tests whether a file is readable by others
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean S_IROTH(int mode) {
        return (mode & S_IROTH) == S_IROTH;
    }

    /**
     * A replacement for ContentResolver.openInputStream() that does not allow the usage of
     * "file" Uris that point to private files owned by the application only.
     *
     * This is not allowed:
     * am start -a android.intent.action.SEND -t text/plain -n
     * "org.sufficientlysecure.keychain.debug/org.sufficientlysecure.keychain.ui.EncryptFilesActivity" --eu
     * android.intent.extra.STREAM
     * file:///data/data/org.sufficientlysecure.keychain.debug/databases/openkeychain.db
     *
     * @throws FileNotFoundException
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private InputStream openInputStreamSafe(ContentResolver resolver, Uri uri)
            throws FileNotFoundException {

        // Not supported on Android < 5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return resolver.openInputStream(uri);
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                    new File(uri.getPath()), ParcelFileDescriptor.parseMode("r"));

            try {
                final StructStat st = Os.fstat(pfd.getFileDescriptor());
                if (!S_IROTH(st.st_mode)) {
                    Log.e(Constants.TAG, "File is not readable by others, aborting!");
                    throw new FileNotFoundException("Unable to create stream");
                }
            } catch (ErrnoException e) {
                Log.e(Constants.TAG, "fstat() failed: " + e);
                throw new FileNotFoundException("fstat() failed");
            }

            AssetFileDescriptor fd = new AssetFileDescriptor(pfd, 0, -1);
            try {
                return fd.createInputStream();
            } catch (IOException e) {
                throw new FileNotFoundException("Unable to create stream");
            }
        } else {
            return resolver.openInputStream(uri);
        }
    }

    @NonNull
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
                Log.e(Constants.TAG, "Key not found", e);
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
                        InputStream is = openInputStreamSafe(mContext.getContentResolver(), uri);
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
                    return new SignEncryptResult(log, requiredInput, results, cryptoInput);
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
            return new SignEncryptResult(log, pendingInputBuilder.build(), results, cryptoInput);
        }

        if (!outputUris.isEmpty()) {
            throw new AssertionError("Got outputs left but no inputs. This is a programming error, please report!");
        }

        log.add(LogType.MSG_SE_SUCCESS, 1);
        return new SignEncryptResult(SignEncryptResult.RESULT_OK, log, results, outputBytes);

    }

}
