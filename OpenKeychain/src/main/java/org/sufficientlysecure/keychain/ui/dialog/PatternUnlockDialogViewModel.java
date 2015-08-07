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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PatternUnlockDialogViewModel implements BaseViewModel,
        UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";

    private DialogUnlockOperationState mOperationState;
    private StringBuilder mInputKeyword;
    private Activity mActivity;
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

        void onUpdateDialogTitle(CharSequence text);

        void onTipTextUpdate(CharSequence text);

        void onNoRetryAllowed();
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

    public PatternUnlockDialogViewModel(OnViewModelEventBind viewModelEventBind) {
        mOnViewModelEventBind = viewModelEventBind;

        if (mOnViewModelEventBind == null) {
            throw new NullPointerException();
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
     *
     * @param arguments
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
            mOnViewModelEventBind.onUpdateDialogTitle(mActivity.getString(
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
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }

        mUnlockAsyncTask = new UnlockAsyncTask(mActivity, passphrase, mSubKeyId, this);
        mUnlockAsyncTask.execute();
    }

    /**
     * Helper method to set the dialog message with its associated userId.
     *
     * @param userId
     */
    public void setDialogTitleMessageForUserID(CharSequence userId) {
        mOnViewModelEventBind.onTipTextUpdate(mActivity.getString(R.string.pattern_for, userId));
    }

    /**
     * Updates the operation state.
     * Failed operations are allowed to be restarted if unlock retries are permitted.
     */
    public void onOperationRequest() {
        if (mOperationState == DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED) {
            return;
        }

        switch (mOperationState) {
            case DIALOG_UNLOCK_OPERATION_STATE_INITIAL: {
                onOperationStateInitial();
            }
        }
    }

    /**
     * Handles the initial operation.
     */
    public void onOperationStateInitial() {
        mOnViewModelEventBind.onOperationStarted();
        mOnViewModelEventBind.onShowProgressBar(true);

        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-256");
            md.update(mInputKeyword.toString().getBytes());
            byte[] digest = md.digest();

            mPassphrase = new Passphrase(new String(digest, "ISO-8859-1").toCharArray());
            mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.PATTERN);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            onFatalError(R.string.msg_dc_error_extract_key, true);
            return;
        }

        if (mSecretRing == null) {
            PassphraseCacheService.addCachedPassphrase(mActivity,
                    Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                    mActivity.getString(R.string.passp_cache_notif_pwd));

            finishCaching(mPassphrase);
            return;
        }

        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS;
        startAsyncUnlockOperationForPin(mPassphrase);
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
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
            mUnlockAsyncTask = null;
        }
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }

    /**
     * Appends the current input pattern.
     *
     * @param pattern
     */
    public void appendPattern(CharSequence pattern) {
        resetCurrentKeyword();
        mInputKeyword.append(pattern);
    }

    /**
     * Resets the current input keyword.
     */
    public void resetCurrentKeyword() {
        mInputKeyword.setLength(0);
    }

    /**
     * UnlockAsyncTask callbacks that will be redirected to the dialog,
     */

    @Override
    public void onErrorCouldNotExtractKey() {
        onFatalError(R.string.msg_dc_error_extract_key, true);
        mOnViewModelEventBind.onUpdateDialogButtonText(mActivity.getString(android.R.string.ok));
    }

    @Override
    public void onErrorWrongPassphrase() {
        resetOperationToInitialState();
        mOnViewModelEventBind.onShowProgressBar(false);
        mOnViewModelEventBind.onOperationStateError(R.string.error_wrong_pattern, false);
        mOnViewModelEventBind.onUpdateDialogButtonText(mActivity.getString(R.string.unlock_caps));
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
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
