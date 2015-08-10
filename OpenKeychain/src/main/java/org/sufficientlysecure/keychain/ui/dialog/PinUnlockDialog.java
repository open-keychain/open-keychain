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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import org.sufficientlysecure.keychain.ui.tasks.UnlockAsyncTask;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
import org.sufficientlysecure.keychain.util.Passphrase;

/**
 * Pin Unlocking dialog
 */
public class PinUnlockDialog extends UnlockDialog
        implements UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_PARAM_OPERATION_TYPE = "EXTRA_PARAM_OPERATION_TYPE";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";

    private DialogUnlockOperationState mOperationState;
    private StringBuilder mInputKeyword;
    private CanonicalizedSecretKeyRing mSecretRing = null;
    private long mSubKeyId;
    private Intent mServiceIntent;
    private UnlockAsyncTask mUnlockAsyncTask;
    private Passphrase mPassphrase;
    private TextView mUnlockTip;
    private ProgressBar mProgressBar;
    private Button mPositiveDialogButton;
    private FeedbackIndicatorView mFeedbackIndicatorView;

    /**
     * Operation state
     */
    public enum DialogUnlockOperationState {
        DIALOG_UNLOCK_OPERATION_STATE_FINISHED,
        DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS,
        DIALOG_UNLOCK_OPERATION_STATE_INITIAL
    }

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            appendPinNumber(((TextView) v).getText());
        }
    };

    /**
     * Dialog setup for the unlock operation
     *
     * @param alertDialogBuilder
     * @return
     */
    public Dialog prepareUnlockDialog(CustomAlertDialogBuilder alertDialogBuilder) {
        alertDialogBuilder.setTitle(getString(R.string.title_unlock));
        alertDialogBuilder.setPositiveButton(getString(R.string.unlock_caps), null);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alertDialogBuilder.show();
        mPositiveDialogButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOperationRequest();
            }
        });

        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperationCancel();
                mAlertDialog.cancel();
            }
        });

        mAlertDialog.setCanceledOnTouchOutside(false);

        return mAlertDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(mActivity);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_pin_fragment, null);
        alert.setView(view);

        Button pinUnlockKey = (Button) view.findViewById(R.id.unlockKey0);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey9);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey8);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey7);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey6);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey5);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey4);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey3);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey2);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey1);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);

        //only call this method after the ui is initialized.
        if (savedInstanceState == null) {
            initializeUnlockOperation(getArguments());
        }

        return prepareUnlockDialog(alert);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
            mUnlockAsyncTask = null;
        }
    }

    public void onUnlockOperationSuccess(Intent serviceIntent) {
        getActivity().setResult(Activity.RESULT_OK, serviceIntent);
        getActivity().finish();
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    public void onOperationStateError(int errorId, boolean showToast) {
        if (showToast) {
            Toast.makeText(getActivity(), errorId, Toast.LENGTH_SHORT).show();
        } else {
            mFeedbackIndicatorView.showWrongTextMessage(getString(errorId), true);
        }
    }

    /**
     * Shows the progress bar.
     *
     * @param show
     */
    public void onShowProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Updates the dialog button.
     *
     * @param text
     */
    public void onUpdateDialogButtonText(CharSequence text) {
        mPositiveDialogButton.setText(text);
    }

    /**
     * Notifies the dialog that the unlock operation has started.
     */
    public void onOperationStarted() {
        mFeedbackIndicatorView.hideMessageAndIcon();
    }

    /**
     * Notifies the view that no retries are allowed. The next positive action will be the dialog
     * dismiss.
     */
    public void onNoRetryAllowed() {
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * updates the dialog title.
     *
     * @param text
     */
    public void onUpdateDialogTitle(CharSequence text) {
        mAlertDialog.setTitle(text);
    }

    /**
     * updates the dialog tip text.
     *
     * @param text
     */
    public void onTipTextUpdate(CharSequence text) {
        mUnlockTip.setText(text);
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
            onUpdateDialogTitle(mActivity.getString(R.string.passphrase_for_symmetric_encryption));
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
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
        onNoRetryAllowed();
        onShowProgressBar(false);
        onOperationStateError(errorId, showErrorAsToast);
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
        onTipTextUpdate(mActivity.getString(R.string.pin_for, userId));
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

    public void onOperationStateInitial() {
        onOperationStarted();
        onShowProgressBar(true);
        mPassphrase = new Passphrase(mInputKeyword.toString());
        mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.PIN);

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
            onUnlockOperationSuccess(mServiceIntent);
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
            onUnlockOperationSuccess(returnIntent);
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
        onUpdateDialogButtonText(mActivity.getString(android.R.string.ok));
    }

    @Override
    public void onErrorWrongPassphrase() {
        resetOperationToInitialState();
        onShowProgressBar(false);
        onOperationStateError(R.string.error_wrong_pin, false);
        onUpdateDialogButtonText(mActivity.getString(R.string.unlock_caps));
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }
}
