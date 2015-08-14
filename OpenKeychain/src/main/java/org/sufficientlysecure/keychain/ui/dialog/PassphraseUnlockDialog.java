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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * Passphrase unlock dialog (used for text based passwords).
 */
public class PassphraseUnlockDialog extends UnlockDialog
        implements UnlockAsyncTask.OnUnlockAsyncTaskListener {

    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";
    public static final String EXTRA_KEY_TYPE = "secret_key_type";

    private Button mPositiveDialogButton;
    private EditText mPassphraseEditText;
    private TextView mPassphraseText;
    private View mProgressLayout;
    private View mInputLayout;
    private StringBuilder mInputKeyword;
    private CanonicalizedSecretKeyRing mSecretRing = null;
    private long mSubKeyId;
    private Intent mServiceIntent;
    private CanonicalizedSecretKey.SecretKeyType mKeyType;
    private UnlockAsyncTask mUnlockAsyncTask;
    private Passphrase mPassphrase;
    private DialogUnlockOperationState mOperationState;

    public enum DialogUnlockOperationState {
        DIALOG_UNLOCK_OPERATION_STATE_FINISHED,
        DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS,
        DIALOG_UNLOCK_OPERATION_STATE_INITIAL
    }

    /**
     * Dialog setup for the unlock operation
     *
     * @param savedInstanceState
     * @return
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        mKeyType = (CanonicalizedSecretKey.SecretKeyType) getArguments().getSerializable(EXTRA_KEY_TYPE);

        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(mActivity);
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

        // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
        //alert.setTitle()
        View view = LayoutInflater.from(theme).inflate(R.layout.passphrase_dialog, null);
        alert.setView(view);

        mPassphraseText = (TextView) view.findViewById(R.id.passphrase_text);
        mPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_passphrase);
        mInputLayout = view.findViewById(R.id.input);
        mProgressLayout = view.findViewById(R.id.progress);

        // Hack to open keyboard.
        // This is the only method that I found to work across all Android versions
        // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
        // Notes: * onCreateView can't be used because we want to add buttons to the dialog
        //        * opening in onActivityCreated does not work on Android 4.4
        mPassphraseEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mPassphraseEditText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity == null || mPassphraseEditText == null) {
                            return;
                        }
                        InputMethodManager imm = (InputMethodManager) mActivity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mPassphraseEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        mPassphraseEditText.requestFocus();
        mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
        mPassphraseEditText.setOnEditorActionListener(this);

        if ((mKeyType == CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD && Preferences.getPreferences(mActivity).useNumKeypadForYubiKeyPin())) {
            mPassphraseEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        alert.setPositiveButton(getString(R.string.unlock_caps), null);
        alert.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alert.show();
        mPositiveDialogButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appendPassword(mPassphraseEditText.getText());
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

        //only call this method after the ui is initialized.
        if (savedInstanceState == null) {
            initializeUnlockOperation(getArguments());
        }

        return mAlertDialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
            mUnlockAsyncTask = null;
        }
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
        mPassphraseText.setText(text);
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
     * Notifies the user of any errors that may have occurred
     */
    public void onOperationStateError(int errorId, boolean showToast) {
        mInputLayout.setVisibility(View.VISIBLE);

        if (showToast) {
            Toast.makeText(mActivity, errorId, Toast.LENGTH_SHORT).show();
        } else {
            mPassphraseText.setText(getString(errorId));
        }
    }

    /**
     * Shows the progress bar.
     *
     * @param show
     */
    public void onShowProgressBar(boolean show) {
        mProgressLayout.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Method that is called when the unlock operation is successful;
     *
     * @param serviceIntent
     */
    public void onUnlockOperationSuccess(Intent serviceIntent) {
        mActivity.setResult(Activity.RESULT_OK, serviceIntent);
        mActivity.finish();
    }

    /**
     * Notifies the dialog that the unlock operation has started.
     */
    public void onOperationStarted() {
        mInputLayout.setVisibility(View.INVISIBLE);
        mPositiveDialogButton.setEnabled(false);
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
        final Activity activity = mActivity;
        if (mSubKeyId == Constants.key.symmetric || mSubKeyId == Constants.key.none) {
            onUpdateDialogTitle(activity.getString(
                    R.string.passphrase_for_symmetric_encryption));
        } else {
            try {
                //attempt at getting the user
                ProviderHelper helper = new ProviderHelper(activity);
                mSecretRing = helper.getCanonicalizedSecretKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));

                String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                if (mainUserIdSplit.name != null) {
                    setDialogTitleMessageForUserID(mainUserIdSplit.name);
                } else {
                    setDialogTitleMessageForUserID(activity.getString(R.string.user_id_no_name));
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
     * Helper method to set the dialog message with its associated userId.
     *
     * @param userId
     */
    public void setDialogTitleMessageForUserID(CharSequence userId) {
        onTipTextUpdate(mActivity.getString(R.string.passphrase_for, userId));
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
            /**
             * Unlock operation
             */
            case DIALOG_UNLOCK_OPERATION_STATE_INITIAL: {
                onUnlockOperationStateInitial();
            }
        }
    }

    /**
     * Handles the initial operation state.
     */
    public void onUnlockOperationStateInitial() {
        onOperationStarted();
        onTipTextUpdate(mActivity.getString(R.string.enter_passphrase));
        onShowProgressBar(true);
        mPassphrase = new Passphrase(mInputKeyword.toString());
        mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.PASSPHRASE);

        if (mSecretRing == null) {
            PassphraseCacheService.addCachedPassphrase(mActivity,
                    Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                    mActivity.getString(R.string.passp_cache_notif_pwd));

            finishCaching(mPassphrase);
            mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
            return;
        }

        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_IN_PROGRESS;
        startAsyncUnlockOperation(mPassphrase);
    }

    /**
     * Cancels the current operation.
     */
    public void onOperationCancel() {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }
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
            onUnlockOperationSuccess(mServiceIntent);
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
            onUnlockOperationSuccess(returnIntent);
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
        onUpdateDialogButtonText(mActivity.getString(android.R.string.ok));
    }

    /**
     * UnlockAsyncTask callbacks that will be redirected to the dialog,
     */

    @Override
    public void onErrorWrongPassphrase() {
        resetOperationToInitialState();
        onShowProgressBar(false);
        onOperationStateError(R.string.wrong_passphrase, false);
        onUpdateDialogButtonText(mActivity.getString(R.string.unlock_caps));
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
        mOperationState = DialogUnlockOperationState.DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
    }
}
