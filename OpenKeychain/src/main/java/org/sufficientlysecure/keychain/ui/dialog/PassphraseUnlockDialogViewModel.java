/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
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

public class PassphraseUnlockDialogViewModel implements BaseViewModel,
        UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";

    private boolean mOperationCompleted = false;
    private StringBuilder mInputKeyword;
    private Activity mActivity;
    private CanonicalizedSecretKeyRing mSecretRing = null;
    private long mSubKeyId;
    private Intent mServiceIntent;
    private UnlockAsyncTask mUnlockAsyncTask;
    private OnViewModelEventBind mViewModelEventBindListener;
    private Passphrase mPassphrase;
    private DialogUnlockOperationState mOperationState;

    public enum DialogUnlockOperationState {
        DIALOG_UNLOCK_OPERATION_STATE_FINISHED,
        DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS,
        DIALOG_UNLOCK_OPERATION_STATE_INITIAL,
        DIALOG_UNLOCK_OPERATION_STATE_FAILED
    }

    /**
     * Internal View Model communication
     */
    public interface OnViewModelEventBind {
        void onUpdateDialogTitle(CharSequence text);

        void onTipTextUpdate(CharSequence text);

        void onNoRetryAllowed();

        void onOperationStateError(int errorId, boolean showToast);

        void onShowProgressBar(boolean show);

        void onUnlockOperationSuccess(Intent serviceIntent);

        void onOperationStarted();

        void onUpdateDialogButtonText(CharSequence text);
    }

    public PassphraseUnlockDialogViewModel(OnViewModelEventBind viewModelEventBind) {
        mViewModelEventBindListener = viewModelEventBind;

        if (mViewModelEventBindListener == null) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
        if (savedInstanceState == null) {
            initializeUnlockOperation(arguments);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    /**
     * Initializes the operation
     */
    public void initializeUnlockOperation(Bundle arguments) {
        if (mInputKeyword == null) {
            mInputKeyword = new StringBuilder();
        } else {
            mInputKeyword.setLength(0);
        }

        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_INITIAL;
        mSubKeyId = arguments.getLong(EXTRA_SUBKEY_ID);
        mServiceIntent = arguments.getParcelable(EXTRA_SERVICE_INTENT);
        setupUnlock();
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
            mViewModelEventBindListener.onUpdateDialogTitle(mActivity.getString(
                    R.string.passphrase_for_symmetric_encryption));
        } else {
            try {
                //attempt at getting the user
                ProviderHelper helper = new ProviderHelper(mActivity);
                mSecretRing = helper.getCanonicalizedSecretKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));

                String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                if (mainUserIdSplit.name != null) {
                    setDialogTitleMessageForUserID(mainUserIdSplit.name);
                } else {
                    setDialogTitleMessageForUserID(mActivity.getString(R.string.user_id_no_name));
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
        mViewModelEventBindListener.onNoRetryAllowed();
        mViewModelEventBindListener.onShowProgressBar(false);
        mViewModelEventBindListener.onOperationStateError(errorId, showErrorAsToast);
    }

    /**
     * Helper method to set the dialog message with its associated userId.
     *
     * @param userId
     */
    public void setDialogTitleMessageForUserID(CharSequence userId) {
        mViewModelEventBindListener.onTipTextUpdate(mActivity.getString(R.string.passphrase_for, userId));
    }

    public void onOperationRequest() {
        if (mOperationCompleted) {
            return;
        }

        switch (mOperationState) {
            /**
             * Unlock operation
             */
            case DIALOG_UNLOCK_OPERATION_STATE_INITIAL: {
                mViewModelEventBindListener.onOperationStarted();
                mViewModelEventBindListener.onTipTextUpdate(mActivity.getString(R.string.enter_passphrase));
                mViewModelEventBindListener.onShowProgressBar(true);
                mPassphrase = new Passphrase(mInputKeyword.toString());
                mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.PASSPHRASE);

                if (mSecretRing == null) {
                    PassphraseCacheService.addCachedPassphrase(mActivity,
                            Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                            mActivity.getString(R.string.passp_cache_notif_pwd));

                    finishCaching(mPassphrase);
                    mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
                    mOperationCompleted = true;
                    return;
                }

                mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS;
                startAsyncUnlockOperation(mPassphrase);
            }
        }
    }

    /**
     * Cancels the current operation.
     */
    public void onOperationCancel() {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }
        mOperationCompleted = true;
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }

    /**
     * Method that starts the async task operation for the current passphrase.
     */
    public void startAsyncUnlockOperation(Passphrase passphrase) {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }

        mUnlockAsyncTask = new UnlockAsyncTask(mActivity, passphrase, mSubKeyId, this);
        mUnlockAsyncTask.execute();
    }

    /**
     * Caches the passphrase on the main thread.
     *
     * @param passphrase
     */
    private void finishCaching(Passphrase passphrase) {
        CryptoInputParcel inputParcel = new CryptoInputParcel(null, passphrase);
        if (mServiceIntent != null) {
            CryptoInputParcelCacheService.addCryptoInputParcel(mActivity, mServiceIntent, inputParcel);
            mViewModelEventBindListener.onUnlockOperationSuccess(mServiceIntent);
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
            mViewModelEventBindListener.onUnlockOperationSuccess(returnIntent);
        }
    }

    /**
     * Concatenates the password number to the current passphrase.
     *
     * @param password
     */
    public void appendPassword(CharSequence password) {
        mInputKeyword.append(password);
    }

    @Override
    public void onErrorCouldNotExtractKey() {
        onFatalError(R.string.error_could_not_extract_private_key, true);
        mViewModelEventBindListener.onUpdateDialogButtonText(mActivity.getString(android.R.string.ok));
    }

    @Override
    public void onErrorWrongPassphrase() {
        resetOperationToInitialState();
        mViewModelEventBindListener.onShowProgressBar(false);
        mViewModelEventBindListener.onOperationStateError(R.string.wrong_passphrase, false);
        mViewModelEventBindListener.onUpdateDialogButtonText(mActivity.getString(R.string.unlock_caps));
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
        mOperationCompleted = true;
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }

    public void onDetachFromActivity() {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
            mUnlockAsyncTask = null;
        }
    }
}
