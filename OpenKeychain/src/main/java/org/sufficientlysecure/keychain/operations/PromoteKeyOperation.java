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
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.service.PromoteKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** An operation which promotes a public key ring to a secret one.
 *
 * This operation can only be applied to public key rings where no secret key
 * is available. Doing this "promotes" the public key ring to a secret one
 * without secret key material, using a GNU_DUMMY s2k type.
 *
 */
public class PromoteKeyOperation extends BaseOperation<PromoteKeyringParcel> {

    public PromoteKeyOperation(Context context, ProviderHelper providerHelper,
                               Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    @Override
    public PromoteKeyResult execute(PromoteKeyringParcel promoteKeyringParcel,
                                    CryptoInputParcel cryptoInputParcel) {
        // Input
        long masterKeyId = promoteKeyringParcel.mKeyRingId;
        byte[] cardAid = promoteKeyringParcel.mCardAid;
        long[] subKeyIds = promoteKeyringParcel.mSubKeyIds;

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_PR, 0);

        // Perform actual type change
        UncachedKeyRing promotedRing;
        {
            try {

                log.add(LogType.MSG_PR_FETCHING, 1,
                        KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
                CanonicalizedPublicKeyRing pubRing =
                        mProviderHelper.getCanonicalizedPublicKeyRing(masterKeyId);

                if (subKeyIds == null) {
                    log.add(LogType.MSG_PR_ALL, 1);
                } else {
                    // sort for binary search
                    for (CanonicalizedPublicKey key : pubRing.publicKeyIterator()) {
                        long subKeyId = key.getKeyId();
                        if (naiveIndexOf(subKeyIds, subKeyId) != null) {
                            log.add(LogType.MSG_PR_SUBKEY_MATCH, 1,
                                    KeyFormattingUtils.convertKeyIdToHex(subKeyId));
                        } else {
                            log.add(LogType.MSG_PR_SUBKEY_NOMATCH, 1,
                                    KeyFormattingUtils.convertKeyIdToHex(subKeyId));
                        }
                    }
                }

                // create divert-to-card secret key from public key
                promotedRing = pubRing.createDivertSecretRing(cardAid, subKeyIds);

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
        SaveKeyringResult saveResult = mProviderHelper
                .saveSecretKeyRing(promotedRing, new ProgressScaler(mProgressable, 60, 95, 100));
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_PR_SUCCESS, 0);
        return new PromoteKeyResult(PromoteKeyResult.RESULT_OK, log, promotedRing.getMasterKeyId());

    }

    static private Integer naiveIndexOf(long[] haystack, long needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (needle == haystack[i]) {
                return i;
            }
        }
        return null;
    }

}
