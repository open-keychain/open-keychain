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
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;


public class ChangeUnlockOperation extends BaseOperation<ChangeUnlockParcel> {

    public ChangeUnlockOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    public OperationResult execute(ChangeUnlockParcel unlockParcel, CryptoInputParcel cryptoInput) {
        OperationResult.OperationLog log = new OperationResult.OperationLog();
        log.add(LogType.MSG_ED, 0);

        if (unlockParcel == null || unlockParcel.mMasterKeyId == null) {
            log.add(LogType.MSG_ED_ERROR_NO_PARCEL, 1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        if (!cryptoInput.hasPassphrase()) {
            log.add(LogType.MSG_MF_REQUIRE_PASSPHRASE, 2);
            return new PgpEditKeyResult(log,
                    RequiredInputParcel.createRequiredKeyringPassphrase(unlockParcel.mMasterKeyId),
                    cryptoInput);
        }

        // Cannot cancel from here on out!
        mProgressable.setPreventCancel();

        // retrieve ring
        CanonicalizedSecretKeyRing retrievedRing;
        {
            try {
                log.add(LogType.MSG_ED_FETCHING, 1,
                        KeyFormattingUtils.convertKeyIdToHex(unlockParcel.mMasterKeyId));
                retrievedRing = mProviderHelper.getCanonicalizedSecretKeyRing(
                        unlockParcel.mMasterKeyId,
                        cryptoInput.getPassphrase()
                );
            } catch (ProviderHelper.NotFoundException e) {
                log.add(LogType.MSG_ED_ERROR_KEY_NOT_FOUND, 2);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            } catch (ByteArrayEncryptor.EncryptDecryptException e) {
                log.add(LogType.MSG_ED_ERROR_ENCRYPT_DECRYPT, 2);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            } catch (ByteArrayEncryptor.IncorrectPassphraseException e) {
                log.add(LogType.MSG_ED_ERROR_INCORRECT_PASSPHRASE, 2);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }
        }

        SaveKeyringResult saveResult = mProviderHelper
                .saveSecretKeyRing(
                        retrievedRing.getUncachedKeyRing(),
                        new KeyringPassphrases(retrievedRing.getMasterKeyId(), unlockParcel.mNewPassphrase),
                        new ProgressScaler(mProgressable, 0, 95, 100)
                );
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // clear cache of old passphrase
        PassphraseCacheService.clearCachedPassphrase(mContext, retrievedRing.getMasterKeyId());

        updateProgress(R.string.progress_done, 100, 100);
        log.add(LogType.MSG_ED_SUCCESS, 0);
        return new EditKeyResult(EditKeyResult.RESULT_OK, log, retrievedRing.getMasterKeyId());

    }

}
