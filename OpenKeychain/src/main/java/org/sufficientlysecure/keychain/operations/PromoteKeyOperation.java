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


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.PromoteKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

/** An operation which promotes a public key ring to a secret one.
 *
 * This operation can only be applied to public key rings where no secret key
 * is available. Doing this "promotes" the public key ring to a secret one
 * without secret key material, using a GNU_DUMMY s2k type.
 *
 */
public class PromoteKeyOperation extends BaseReadWriteOperation<PromoteKeyringParcel> {
    public PromoteKeyOperation(Context context, KeyWritableRepository databaseInteractor,
                               Progressable progressable, AtomicBoolean cancelled) {
        super(context, databaseInteractor, progressable, cancelled);
    }

    @NonNull
    @Override
    public PromoteKeyResult execute(PromoteKeyringParcel promoteKeyringParcel,
                                    CryptoInputParcel cryptoInputParcel) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_PR, 0);

        // Perform actual type change
        UncachedKeyRing promotedRing;
        {
            try {

                log.add(LogType.MSG_PR_FETCHING, 1,
                        KeyFormattingUtils.convertKeyIdToHex(promoteKeyringParcel.getMasterKeyId()));
                CanonicalizedPublicKeyRing pubRing =
                        mKeyRepository.getCanonicalizedPublicKeyRing(promoteKeyringParcel.getMasterKeyId());

                List<byte[]> fingerprints = promoteKeyringParcel.getFingerprints();
                if (fingerprints == null) {
                    log.add(LogType.MSG_PR_ALL, 1);
                } else {
                    // sort for binary search
                    for (CanonicalizedPublicKey key : pubRing.publicKeyIterator()) {
                        long subKeyId = key.getKeyId();

                        if (naiveArraySearch(fingerprints, key.getFingerprint())) {
                            log.add(LogType.MSG_PR_SUBKEY_MATCH, 1, KeyFormattingUtils.convertKeyIdToHex(subKeyId));
                        } else {
                            log.add(LogType.MSG_PR_SUBKEY_NOMATCH, 1, KeyFormattingUtils.convertKeyIdToHex(subKeyId));
                        }
                    }
                }

                // create divert-to-card secret key from public key
                promotedRing = pubRing.createDivertSecretRing(promoteKeyringParcel.getCardAid(), fingerprints);

            } catch (NotFoundException e) {
                log.add(LogType.MSG_PR_ERROR_KEY_NOT_FOUND, 2);
                return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
            }
        }

        // If the edit operation didn't succeed, exit here
        if (promotedRing == null) {
            // error is already logged by modification
            return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
        }

        // Check if the action was cancelled
        if (checkCancelled()) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 0);
            return new PromoteKeyResult(PgpEditKeyResult.RESULT_CANCELLED, log, null);
        }

        // Cannot cancel from here on out!
        setPreventCancel();

        // Save the new keyring.
        updateProgress(R.string.progress_saving, 80, 100);
        SaveKeyringResult saveResult = mKeyWritableRepository.saveSecretKeyRing(promotedRing);
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_PR_SUCCESS, 0);
        return new PromoteKeyResult(PromoteKeyResult.RESULT_OK, log, promotedRing.getMasterKeyId());

    }

    static private boolean naiveArraySearch(List<byte[]> searchElements, byte[] needle) {
        for (byte[] searchElement : searchElements) {
            if (Arrays.equals(needle, searchElement)) {
                return true;
            }
        }
        return false;
    }

}
