package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.concurrent.atomic.AtomicBoolean;

/** An operation which implements a high level key edit operation.
 *
 * This operation provides a higher level interface to the edit and
 * create key operations in PgpKeyOperation. It takes care of fetching
 * and saving the key before and after the operation.
 *
 * @see SaveKeyringParcel
 *
 */
public class EditKeyOperation extends BaseOperation {

    public EditKeyOperation(Context context, ProviderHelper providerHelper,
                            Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public EditKeyResult execute(SaveKeyringParcel saveParcel, String passphrase) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_ED, 0);

        if (saveParcel == null) {
            log.add(LogType.MSG_ED_ERROR_NO_PARCEL, 1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // Perform actual modification (or creation)
        PgpEditKeyResult modifyResult;
        {
            PgpKeyOperation keyOperations =
                    new PgpKeyOperation(new ProgressScaler(mProgressable, 10, 60, 100), mCancelled);

            // If a key id is specified, fetch and edit
            if (saveParcel.mMasterKeyId != null) {
                try {

                    log.add(LogType.MSG_ED_FETCHING, 1,
                            KeyFormattingUtils.convertKeyIdToHex(saveParcel.mMasterKeyId));
                    CanonicalizedSecretKeyRing secRing =
                            mProviderHelper.getCanonicalizedSecretKeyRing(saveParcel.mMasterKeyId);

                    modifyResult = keyOperations.modifySecretKeyRing(secRing, saveParcel, passphrase);

                } catch (NotFoundException e) {
                    log.add(LogType.MSG_ED_ERROR_KEY_NOT_FOUND, 2);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }
            } else {
                // otherwise, create new one
                modifyResult = keyOperations.createSecretKeyRing(saveParcel);
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

        // Save the new keyring.
        SaveKeyringResult saveResult = mProviderHelper
                .saveSecretKeyRing(ring, new ProgressScaler(mProgressable, 60, 95, 100));
        log.add(saveResult, 1);

        // If the save operation didn't succeed, exit here
        if (!saveResult.success()) {
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // There is a new passphrase - cache it
        if (saveParcel.mNewUnlock != null) {
            log.add(LogType.MSG_ED_CACHING_NEW, 1);
            PassphraseCacheService.addCachedPassphrase(mContext,
                    ring.getMasterKeyId(),
                    ring.getMasterKeyId(),
                    saveParcel.mNewUnlock.mNewPassphrase != null
                            ? saveParcel.mNewUnlock.mNewPassphrase
                            : saveParcel.mNewUnlock.mNewPin,
                    ring.getPublicKey().getPrimaryUserIdWithFallback());
        }

        updateProgress(R.string.progress_done, 100, 100);

        // make sure new data is synced into contacts
        ContactSyncAdapterService.requestSync();

        log.add(LogType.MSG_ED_SUCCESS, 0);
        return new EditKeyResult(EditKeyResult.RESULT_OK, log, ring.getMasterKeyId());

    }

}
