/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.SecurityTokenSignOperationsBuilder;
import org.sufficientlysecure.keychain.util.ProgressScaler;


/**
 * This is a high-level operation, which encapsulates one or more sign/encrypt
 * operations, using URIs or byte arrays as input and output.
 * <p/>
 * This operation is fail-fast: If any sign/encrypt sub-operation fails or returns
 * a pending result, it will terminate.
 */
public class SignEncryptOperation extends BaseOperation<SignEncryptParcel> {

    public SignEncryptOperation(Context context, KeyRepository keyRepository,
                                Progressable progressable, AtomicBoolean cancelled) {
        super(context, keyRepository, progressable, cancelled);
    }


    @NonNull
    public SignEncryptResult execute(SignEncryptParcel input, CryptoInputParcel cryptoInput) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_SE, 0);

        ArrayDeque<Uri> inputUris = new ArrayDeque<>(input.getInputUris());
        ArrayDeque<Uri> outputUris = new ArrayDeque<>(input.getOutputUris());
        byte[] inputBytes = input.getBytes();

        int total = inputBytes != null ? 1 : inputUris.size(), count = 0;
        ArrayList<PgpSignEncryptResult> results = new ArrayList<>();

        SecurityTokenSignOperationsBuilder pendingInputBuilder = null;

        do {
            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new SignEncryptResult(SignEncryptResult.RESULT_CANCELLED, log, results);
            }

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(mContext, mKeyRepository,
                    new ProgressScaler(mProgressable, 100 * count / total, 100 * ++count / total, 100), mCancelled);
            PgpSignEncryptInputParcel inputParcel;
            if (inputBytes != null) {
                inputParcel = PgpSignEncryptInputParcel.createForBytes(
                        input.getSignEncryptData(), outputUris.pollFirst(), inputBytes);
            } else {
                inputParcel = PgpSignEncryptInputParcel.createForInputUri(
                        input.getSignEncryptData(), outputUris.pollFirst(), inputUris.removeFirst());
            }

            PgpSignEncryptResult result = op.execute(inputParcel, cryptoInput);
            results.add(result);
            log.add(result, 2);

            if (result.isPending()) {
                RequiredInputParcel requiredInput = result.getRequiredInputParcel();
                // Passphrase returns immediately, nfc are aggregated
                if (requiredInput.mType == RequiredInputType.PASSPHRASE) {
                    return new SignEncryptResult(log, requiredInput, results, cryptoInput);
                }
                if (pendingInputBuilder == null) {
                    pendingInputBuilder = new SecurityTokenSignOperationsBuilder(requiredInput.mSignatureTime,
                            requiredInput.getMasterKeyId(), requiredInput.getSubKeyId());
                }
                pendingInputBuilder.addAll(requiredInput);
            } else if (!result.success()) {
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log, results);
            }
        } while (!inputUris.isEmpty());

        if (pendingInputBuilder != null && !pendingInputBuilder.isEmpty()) {
            return new SignEncryptResult(log, pendingInputBuilder.build(), results, cryptoInput);
        }

        if (!outputUris.isEmpty()) {
            throw new AssertionError("Got outputs left but no inputs. This is a programming error, please report!");
        }

        log.add(LogType.MSG_SE_SUCCESS, 1);
        return new SignEncryptResult(SignEncryptResult.RESULT_OK, log, results,
                results.get(results.size() - 1).getOutputBytes());
    }

}
