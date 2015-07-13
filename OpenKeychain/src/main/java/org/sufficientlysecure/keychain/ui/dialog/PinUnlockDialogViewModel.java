package org.sufficientlysecure.keychain.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.CryptoInputParcelCacheService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.tasks.UnlockAsyncTask;
import org.sufficientlysecure.keychain.util.Passphrase;

/**
 * Pin unlock dialog view Model.
 */
public class PinUnlockDialogViewModel implements BaseViewModel,
        UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_PARAM_OPERATION_TYPE = "EXTRA_PARAM_OPERATION_TYPE";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";

    private DialogUnlockOperationState mOperationState;
    private boolean mOperationCompleted = false;
    private StringBuilder mInputKeyword;
    private Context mContext;
    private CanonicalizedSecretKeyRing mSecretRing = null;
    private long mSubKeyId;
    private Intent mServiceIntent;
    private UnlockAsyncTask mUnlockAsyncTask;
    private OnViewModelEventBind mOnViewModelEventBind;
    private Passphrase mPassphrase;

    /**
     * Internal View Model communication
     */
    public interface OnViewModelEventBind {
        void onUnlockOperationSuccess(Intent serviceIntent);

        void onOperationStateError(int errorId, boolean showToast);

        void onShowProgressBar(boolean show);

        void onUpdateDialogButtonText(CharSequence text);

        void onOperationStarted();

        void onNoRetryAllowed();

        void onUpdateDialogTitle(CharSequence text);

        void onTipTextUpdate(CharSequence text);
    }

    /**
     * Operations allowed by this dialog
     */
    public enum DialogUnlockOperation {
        DIALOG_UNLOCK_TYPE_UNLOCK_KEY
    }

    /**
     * Operation state
     */
    public enum DialogUnlockOperationState {
        DIALOG_UNLOCK_OPERATION_STATE_FINISHED,
        DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS,
        DIALOG_UNLOCK_OPERATION_STATE_INITIAL,
        DIALOG_UNLOCK_OPERATION_STATE_FAILED
    }

    /**
     * Binds the View to the viewModel for callback communication.
     *
     * @param viewModelEventBind
     */
    public PinUnlockDialogViewModel(OnViewModelEventBind viewModelEventBind)
            throws UnsupportedOperationException {
        mOnViewModelEventBind = viewModelEventBind;

        if (mOnViewModelEventBind == null) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        if (savedInstanceState == null) {
            initializeUnlockOperation((PinUnlockDialogViewModel.DialogUnlockOperation) arguments.
                    getSerializable(EXTRA_PARAM_OPERATION_TYPE), arguments);
        }
    }

    /**
     * Initializes the operation
     *
     * @param dialogUnlockType
     */
    public void initializeUnlockOperation(DialogUnlockOperation dialogUnlockType, Bundle arguments) {
        if (mInputKeyword == null) {
            mInputKeyword = new StringBuilder();
        } else {
            mInputKeyword.setLength(0);
        }

        switch (dialogUnlockType) {
            case DIALOG_UNLOCK_TYPE_UNLOCK_KEY: {
                mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_INITIAL;
                mSubKeyId = arguments.getLong(EXTRA_SUBKEY_ID);
                mServiceIntent = arguments.getParcelable(EXTRA_SERVICE_INTENT);
                setupUnlock();
            }
        }
    }

    /**
     * Resets the operation to its initial state
     */
    public void resetOperationToInitialState() {
        if (mInputKeyword == null) {
            mInputKeyword = new StringBuilder();
        } else {
            mInputKeyword.setLength(0);
        }

        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_INITIAL;
    }

    /**
     * Prepares all the relevant data to display to the user and to be used for the unlock request.
     */
    public void setupUnlock() {
        if (mSubKeyId == Constants.key.symmetric || mSubKeyId == Constants.key.none) {
            mOnViewModelEventBind.onUpdateDialogTitle(mContext.getString(
                    R.string.passphrase_for_symmetric_encryption));
        } else {
            try {
                //attempt at getting the user
                ProviderHelper helper = new ProviderHelper(mContext);
                mSecretRing = helper.getCanonicalizedSecretKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));

                String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                if (mainUserIdSplit.name != null) {
                    setDialogTitleMessageForUserID(mainUserIdSplit.name);
                } else {
                    setDialogTitleMessageForUserID(mContext.getString(R.string.user_id_no_name));
                }
            } catch (ProviderHelper.NotFoundException | PgpKeyNotFoundException e) {
                onFatalError(R.string.title_key_not_found, false);
            }
        }
    }

    /**
     * Notifies the view that a fatal operation error as occurred.
     *
     * @param errorId
     * @param showErrorAsToast
     */
    public void onFatalError(int errorId, boolean showErrorAsToast) {
        mOperationCompleted = true;
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FAILED;
        mOnViewModelEventBind.onNoRetryAllowed();
        mOnViewModelEventBind.onShowProgressBar(false);
        mOnViewModelEventBind.onOperationStateError(errorId, showErrorAsToast);
    }

    /**
     * Method that starts the async task operation for the current pin.
     */
    public void startAsyncUnlockOperationForPin(Passphrase passphrase) {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.cancel(true);
        }

        mUnlockAsyncTask = new UnlockAsyncTask(mContext, passphrase, mSubKeyId, this);
        mUnlockAsyncTask.execute();
    }

    /**
     * Helper method to set the dialog message with its associated userId.
     *
     * @param userId
     */
    public void setDialogTitleMessageForUserID(CharSequence userId) {
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.pin_for, userId));
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    /**
     * Updates the operation state.
     * Failed operations are allowed to be restarted if unlock retries are permitted.
     */
    public void onOperationRequest() {
        if (mOperationCompleted) {
            return;
        }

        switch (mOperationState) {
            /**
             * Unlock operation
             */
            case DIALOG_UNLOCK_OPERATION_STATE_INITIAL: {
                mOnViewModelEventBind.onOperationStarted();
                mOnViewModelEventBind.onShowProgressBar(true);
                mPassphrase = new Passphrase(mInputKeyword.toString());
                mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.PIN);

                if (mSecretRing == null) {
                    PassphraseCacheService.addCachedPassphrase(mContext,
                            Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                            mContext.getString(R.string.passp_cache_notif_pwd));

                    finishCaching(mPassphrase);
                    return;
                }

                mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS;
                startAsyncUnlockOperationForPin(mPassphrase);
            }
        }
    }

    /**
     * Caches the passphrase on the main thread.
     *
     * @param passphrase
     */
    private void finishCaching(Passphrase passphrase) {
        CryptoInputParcel inputParcel = new CryptoInputParcel(null, passphrase);
        if (mServiceIntent != null) {
            CryptoInputParcelCacheService.addCryptoInputParcel(mContext, mServiceIntent, inputParcel);
            mOnViewModelEventBind.onUnlockOperationSuccess(mServiceIntent);
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
            mOnViewModelEventBind.onUnlockOperationSuccess(returnIntent);
        }
    }

    /**
     * Cancels the current operation.
     */
    public void onOperationCancel() {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.cancel(true);
        }
        mOperationCompleted = true;
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }

    /**
     * Concatenates the pin number to the current passphrase.
     *
     * @param number
     */
    public void appendPinNumber(CharSequence number) {
        mInputKeyword.append(number);
    }

    /**
     * UnlockAsyncTask callbacks that will be redirected to the dialog,
     */

    @Override
    public void onErrorCouldNotExtractKey() {
        onFatalError(R.string.error_could_not_extract_private_key, true);
        mOnViewModelEventBind.onUpdateDialogButtonText(mContext.getString(android.R.string.ok));
    }

    @Override
    public void onErrorWrongPassphrase() {
        resetOperationToInitialState();
        mOnViewModelEventBind.onShowProgressBar(false);
        mOnViewModelEventBind.onOperationStateError(R.string.error_wrong_pin, false);
        mOnViewModelEventBind.onUpdateDialogButtonText(mContext.getString(R.string.unlock_caps));
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
        mOperationCompleted = true;
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }
}
