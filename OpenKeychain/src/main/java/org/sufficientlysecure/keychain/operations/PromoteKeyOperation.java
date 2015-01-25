package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.concurrent.atomic.AtomicBoolean;

/** An operation which promotes a public key ring to a secret one.
 *
 * This operation can only be applied to public key rings where no secret key
 * is available. Doing this "promotes" the public key ring to a secret one
 * without secret key material, using a GNU_DUMMY s2k type.
 *
 */
public class PromoteKeyOperation extends BaseOperation {

    public PromoteKeyOperation(Context context, ProviderHelper providerHelper,
                               Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public PromoteKeyResult execute(long masterKeyId) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_PR, 0);

        // Perform actual type change
        UncachedKeyRing promotedRing;
        {

            try {

                // This operation is only allowed for pure public keys
                // TODO delete secret keys if they are stripped, or have been moved to the card?
                if (mProviderHelper.getCachedPublicKeyRing(masterKeyId).hasAnySecret()) {
                    log.add(LogType.MSG_PR_ERROR_ALREADY_SECRET, 2);
                    return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
                }

                log.add(LogType.MSG_PR_FETCHING, 1,
                        KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
                CanonicalizedPublicKeyRing pubRing =
                        mProviderHelper.getCanonicalizedPublicKeyRing(masterKeyId);

                // create divert-to-card secret key from public key
                promotedRing = pubRing.createDummySecretRing();

            } catch (PgpKeyNotFoundException e) {
                log.add(LogType.MSG_PR_ERROR_KEY_NOT_FOUND, 2);
                return new PromoteKeyResult(PromoteKeyResult.RESULT_ERROR, log, null);
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

}
