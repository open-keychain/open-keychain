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


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.provider.ProviderReader.NotFoundException;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

/**
 * An operation which implements a high level key edit operation.
 * <p/>
 * This operation provides a higher level interface to the edit and
 * create key operations in PgpKeyOperation. It takes care of fetching
 * and saving the key before and after the operation.
 *
 * @see SaveKeyringParcel
 *
 */
public class EditKeyOperation extends BaseOperation<SaveKeyringParcel> {

    public EditKeyOperation(Context context, ProviderHelper providerHelper,
                            Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    /**
     * Saves an edited key, and uploads it to a server atomically or otherwise as
     * specified in saveParcel
     *
     * @param saveParcel  primary input to the operation
     * @param cryptoInput input that changes if user interaction is required
     * @return the result of the operation
     */
    @NonNull
    public EditKeyResult execute(SaveKeyringParcel saveParcel, CryptoInputParcel cryptoInput) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_ED, 0);

        if (saveParcel == null) {
            log.add(LogType.MSG_ED_ERROR_NO_PARCEL, 1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        Long masterKeyId = saveParcel.mMasterKeyId;
        boolean isNewKey = masterKeyId == null;
        Passphrase keyringPassphrase;

        // get the keyring's passphrase
        if (isNewKey) {
            keyringPassphrase = saveParcel.mPassphrase;
        } else {
            CachedPublicKeyRing cachedPublicKeyRing = mProviderHelper.mReader.getCachedPublicKeyRing(masterKeyId);

            try {
                switch (cachedPublicKeyRing.getSecretKeyringType()) {
                    case PASSPHRASE_EMPTY: {
                        keyringPassphrase = new Passphrase();
                        break;
                    }
                    case PASSPHRASE: {
                        keyringPassphrase = cryptoInput.getPassphrase();
                        if (keyringPassphrase == null) {
                            log.add(LogType.MSG_ED_REQUIRE_KEYRING_PASSPHRASE, 2);
                            return new EditKeyResult(log,
                                    RequiredInputParcel.createRequiredKeyringPassphrase(masterKeyId),
                                    cryptoInput);
                        }
                        break;
                    }
                    default: {
                        throw new AssertionError("Unsupported keyring type");
                    }
                }
            } catch (NotFoundException e) {
                log.add(LogType.MSG_ED_ERROR_KEYRING_NOT_FOUND, 2);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }
        }

        // Perform actual modification (or creation)
        PgpEditKeyResult modifyResult;
        {
            PgpKeyOperation keyOperations =
                    new PgpKeyOperation(new ProgressScaler(mProgressable, 10, 60, 100), mCancelled);

            if (isNewKey) {
                modifyResult = keyOperations.createSecretKeyRing(saveParcel);
            } else {
                try {
                    log.add(LogType.MSG_ED_FETCHING, 1,
                            KeyFormattingUtils.convertKeyIdToHex(saveParcel.mMasterKeyId));

                    // update the keyring along the way
                    CanonicalizedSecretKeyRing secRing =
                            mProviderHelper.mReader.getCanonicalizedSecretKeyRingWithMerge(
                                    saveParcel.mMasterKeyId, keyringPassphrase);

                    modifyResult = keyOperations.modifySecretKeyRing(secRing, cryptoInput, saveParcel);
                    if (modifyResult.isPending()) {
                        log.add(modifyResult, 1);
                        return new EditKeyResult(log, modifyResult);
                    }
                } catch (NotFoundException e) {
                    log.add(LogType.MSG_ED_ERROR_KEYRING_NOT_FOUND, 2);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                } catch (ByteArrayEncryptor.EncryptDecryptException e) {
                    log.add(LogType.MSG_ED_ERROR_DECRYPT_KEYRING, 2);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                } catch (ByteArrayEncryptor.IncorrectPassphraseException e) {
                    log.add(LogType.MSG_ED_ERROR_INCORRECT_PASSPHRASE, 2);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                } catch (ProviderReader.FailedMergeException e) {
                    log.add(LogType.MSG_ED_ERROR_MERGE_KEYRING, 2);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }
            }
        }

        // Add the result to the log
        log.add(modifyResult, 1);

        // Check if the action was cancelled
        if (checkCancelled()) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 0);
            return new EditKeyResult(PgpEditKeyResult.RESULT_CANCELLED, log, null);
        }

        // If the edit operation didn't succeed, exit here
        if (!modifyResult.success()) {
            // error is already logged by modification
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // Cannot cancel from here on out!
        mProgressable.setPreventCancel();

        // It's a success, so this must be non-null now
        UncachedKeyRing ring = modifyResult.getRing();

        if (saveParcel.isUpload()) {
            byte[] keyringBytes;
            try {
                UncachedKeyRing publicKeyRing = ring.extractPublicKeyRing();
                keyringBytes = publicKeyRing.getEncoded();
            } catch (IOException e) {
                log.add(LogType.MSG_ED_ERROR_EXTRACTING_PUBLIC_UPLOAD, 1);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            UploadKeyringParcel exportKeyringParcel =
                    new UploadKeyringParcel(saveParcel.getUploadKeyserver(), keyringBytes);

            UploadResult uploadResult =
                    new UploadOperation(mContext, mProviderHelper, mProgressable, mCancelled)
                            .execute(exportKeyringParcel, cryptoInput);

            log.add(uploadResult, 2);

            if (uploadResult.isPending()) {
                return new EditKeyResult(log, uploadResult);
            } else if (!uploadResult.success() && saveParcel.isUploadAtomic()) {
                // if atomic, update fail implies edit operation should also fail and not save
                return new EditKeyResult(log, RequiredInputParcel.createRetryUploadOperation(), cryptoInput);
            }
        }

        KeyringPassphrases passphrases = new KeyringPassphrases(ring.getMasterKeyId(), keyringPassphrase);

        // Save the new keyring.
        SaveKeyringResult saveResult = mProviderHelper
                .mWriter.saveSecretKeyRing(ring, passphrases, new ProgressScaler(mProgressable, 60, 95, 100));
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        updateProgress(R.string.progress_done, 100, 100);

        // make sure new data is synced into contacts
        ContactSyncAdapterService.requestContactsSync();

        log.add(LogType.MSG_ED_SUCCESS, 0);
        return new EditKeyResult(EditKeyResult.RESULT_OK, log, ring.getMasterKeyId());

    }
}
