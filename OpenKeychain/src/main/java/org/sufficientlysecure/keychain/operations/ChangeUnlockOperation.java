/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ProgressScaler;


public class ChangeUnlockOperation extends BaseOperation<ChangeUnlockParcel> {

    public ChangeUnlockOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    public OperationResult execute(ChangeUnlockParcel unlockParcel, CryptoInputParcel cryptoInput) {
        OperationResult.OperationLog log = new OperationResult.OperationLog();
        log.add(OperationResult.LogType.MSG_ED, 0);

        if (unlockParcel == null || unlockParcel.mMasterKeyId == null) {
            log.add(OperationResult.LogType.MSG_ED_ERROR_NO_PARCEL, 1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // Perform actual modification
        PgpEditKeyResult modifyResult;
        {
            PgpKeyOperation keyOperations =
                    new PgpKeyOperation(new ProgressScaler(mProgressable, 0, 70, 100));

            try {
                log.add(OperationResult.LogType.MSG_ED_FETCHING, 1,
                        KeyFormattingUtils.convertKeyIdToHex(unlockParcel.mMasterKeyId));

                CanonicalizedSecretKeyRing secRing =
                        mProviderHelper.getCanonicalizedSecretKeyRing(unlockParcel.mMasterKeyId);
                modifyResult = keyOperations.modifyKeyRingPassphrase(secRing, cryptoInput, unlockParcel);

                if (modifyResult.isPending()) {
                    // obtain original passphrase from user
                    log.add(modifyResult, 1);
                    return new EditKeyResult(log, modifyResult);
                }
            } catch (ProviderHelper.NotFoundException e) {
                log.add(OperationResult.LogType.MSG_ED_ERROR_KEY_NOT_FOUND, 2);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }
        }

        log.add(modifyResult, 1);

        if (!modifyResult.success()) {
            // error is already logged by modification
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // Cannot cancel from here on out!
        mProgressable.setPreventCancel();

        // It's a success, so this must be non-null now
        UncachedKeyRing ring = modifyResult.getRing();

        SaveKeyringResult saveResult = mProviderHelper
                .saveSecretKeyRing(ring, new ProgressScaler(mProgressable, 70, 95, 100));
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        updateProgress(R.string.progress_done, 100, 100);
        log.add(OperationResult.LogType.MSG_ED_SUCCESS, 0);
        return new EditKeyResult(EditKeyResult.RESULT_OK, log, ring.getMasterKeyId());

    }

}
