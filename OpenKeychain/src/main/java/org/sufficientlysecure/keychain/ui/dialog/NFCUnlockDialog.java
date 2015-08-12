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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.nfc.BaseNfcTagTechnology;
import org.sufficientlysecure.keychain.nfc.NfcDispatcher;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NFCUnlockDialog extends UnlockDialog
        implements NfcDispatcher.NfcDispatcherCallback, UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";
    public static final int NUM_PROGRESS_OPERATIONS = 3; //never zero!!
    public static final int MESSAGE_PROGRESS_UPDATE = 1;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;
    private CanonicalizedSecretKeyRing mSecretRing = null;
    private Intent mServiceIntent;
    private BaseNfcTagTechnology mNfcTechnology;
    private long mSubKeyId;
    private ProgressHandler mProgressHandler;
    private OperationState mOperationState;
    private UnlockAsyncTask mUnlockAsyncTask;
    private Passphrase mPassphrase;

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_WAITING_FOR_NFC_TAG,
        OPERATION_STATE_PERFORM_UNLOCK,
        OPERATION_STATE_READING_NFC_TAG,
        OPERATION_STATE_FINALIZED
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(mActivity);
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_nfc_fragment, null);
        alert.setView(view);

        mProgressHandler = new ProgressHandler(Looper.getMainLooper());

        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        alert.setPositiveButton(getString(R.string.unlock_caps), null);
        alert.setNegativeButton(android.R.string.cancel, null);
        alert.setTitle(getString(R.string.title_unlock));

        mAlertDialog = alert.show();
        mAlertDialog.setCanceledOnTouchOutside(false);

        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperationCancel();
                mAlertDialog.cancel();
            }
        });

        b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setVisibility(View.GONE);

        if (savedInstanceState == null) {
            mSubKeyId = getArguments().getLong(EXTRA_SUBKEY_ID);
            mServiceIntent = getArguments().getParcelable(EXTRA_SERVICE_INTENT);
            initializeUnlockOperation();
        }

        return mAlertDialog;
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

    /**
     * Returns the progress made so far.
     *
     * @param operationSequence
     * @return 0 to 100;
     */
    public static int calculateProgress(int operationSequence) {
        return (operationSequence * 100 / NUM_PROGRESS_OPERATIONS);
    }

    /**
     * Initializes the operation
     */
    public void initializeUnlockOperation() {
        mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
        setupUnlock();
        updateOperationState();
    }

    /**
     * Updates the operation state.
     *
     * @return
     */
    public boolean updateOperationState() {
        switch (mOperationState) {
            case OPERATION_STATE_WAITING_FOR_NFC_TAG:
                return handleOperationStateWaitForNFCTag();
            case OPERATION_STATE_PERFORM_UNLOCK:
                return handleOperationPerformUnlock();
            case OPERATION_STATE_READING_NFC_TAG:
                return handleOperationStateReadingNfcTag();
            case OPERATION_STATE_FINALIZED: {
                return true;
            }
            default:
                return false;
        }
    }

    public boolean handleOperationStateReadingNfcTag() {
        return false;
    }

    public boolean handleOperationStateWaitForNFCTag() {
        onShowProgressBar(true);
        onTipTextUpdate(getString(R.string.nfc_move_card));
        onProgressBarUpdateStyle(true, getResources().getColor(R.color.android_green_dark));
        onOperationStateOK(null);
        return false;
    }

    public boolean handleOperationPerformUnlock() {
        if (mSecretRing == null) {
            PassphraseCacheService.addCachedPassphrase(getActivity(),
                    Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                    getString(R.string.passp_cache_notif_pwd));

            finishCaching(mPassphrase);
            return true;
        }

        startAsyncUnlockOperationForPin(mPassphrase);
        return false;
    }

    /**
     * Prepares all the relevant data to display to the user and to be used for the unlock request.
     */
    public void setupUnlock() {
        if (mSubKeyId == Constants.key.symmetric || mSubKeyId == Constants.key.none) {
            onUpdateDialogTitle(getString(R.string.passphrase_for_symmetric_encryption));
        } else {
            try {
                //attempt at getting the user
                ProviderHelper helper = new ProviderHelper(getActivity());
                mSecretRing = helper.getCanonicalizedSecretKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));

                String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                if (mainUserIdSplit.name != null) {
                    setDialogTitleMessageForUserID(mainUserIdSplit.name);
                } else {
                    setDialogTitleMessageForUserID(getString(R.string.user_id_no_name));
                }
            } catch (ProviderHelper.NotFoundException | PgpKeyNotFoundException e) {
                onFatalError(R.string.title_key_not_found);
            }
        }
    }

    /**
     * Helper method to set the dialog message with its associated userId.
     *
     * @param userId
     */
    public void setDialogTitleMessageForUserID(CharSequence userId) {
        onUpdateDialogTitle(getString(R.string.nfc_for, userId));
    }

    /**
     * Caches the passphrase on the main thread.
     *
     * @param passphrase
     */
    private void finishCaching(Passphrase passphrase) {
        CryptoInputParcel inputParcel = new CryptoInputParcel(null, passphrase);
        if (mServiceIntent != null) {
            CryptoInputParcelCacheService.addCryptoInputParcel(getActivity(), mServiceIntent, inputParcel);
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
        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
    }

    /**
     * Notifies the view that a fatal operation error as occurred.
     *
     * @param errorId
     */
    public void onFatalError(int errorId) {
        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
        onShowProgressBar(false);
        onOperationStateError(getString(errorId));
    }

    /**
     * Method that starts the async task operation for the current pin.
     */
    public void startAsyncUnlockOperationForPin(Passphrase passphrase) {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }

        mUnlockAsyncTask = new UnlockAsyncTask(getActivity(), passphrase, mSubKeyId, this);
        mUnlockAsyncTask.execute();
    }

    public void onOperationStateError(String error) {
        mUnlockUserFeedback.showWrongTextMessage(error, true);
    }

    public void onOperationStateOK(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, false);
    }

    public void onTipTextUpdate(CharSequence text) {
        mUnlockTip.setText(text);
    }

    public void onShowProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public void onUpdateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    public void onProgressBarUpdateStyle(boolean indeterminate, int tint) {
        mProgressBar.setIndeterminate(indeterminate);
        DrawableCompat.setTint(mProgressBar.getIndeterminateDrawable(), tint);
        DrawableCompat.setTint(mProgressBar.getProgressDrawable(), tint);
    }

    public void onUpdateDialogTitle(CharSequence text) {
        mAlertDialog.setTitle(text);
    }

    public void onUnlockOperationSuccess(Intent serviceIntent) {
        mUnlockUserFeedback.showCorrectTextMessage(null, true);
        getActivity().setResult(Activity.RESULT_OK, serviceIntent);
        getActivity().finish();
    }

    @Override
    public void onNfcError(NfcDispatcher.CardException exception) {
        onOperationStateError(exception.getMessage());
        mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
        updateOperationState();
    }

    @Override
    public void onNfcPreExecute() throws NfcDispatcher.CardException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }
        mOperationState = OperationState.OPERATION_STATE_READING_NFC_TAG;
        onTipTextUpdate(getString(R.string.nfc_configuring_card));
        onShowProgressBar(true);
        onProgressBarUpdateStyle(false, getResources().getColor(R.color.android_green_dark));
        onUpdateProgress(0);
    }

    @Override
    public void doNfcInBackground() throws NfcDispatcher.CardException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }

        if (mNfcTechnology != null) {
            try {
                mNfcTechnology.connect();
            } catch (IOException e) {
                throw new NfcDispatcher.CardException(e.getMessage(), NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }
        } else {
            throw new NfcDispatcher.CardException("No technology was present, forgot to register the nfc technologies?",
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }

        postProgressToMainThread(1);

        //read the pin
        byte[] nfcPin = mNfcTechnology.read();

        //verify step
        mNfcTechnology.close();
        postProgressToMainThread(2);
        String sPin;
        try {
            sPin = new String(nfcPin, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new NfcDispatcher.CardException("Error while attempting to encode the nfc passphrase",
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
        mPassphrase = new Passphrase(sPin.toCharArray());
        mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.NFC_TAG);
    }

    @Override
    public void onNfcPostExecute() throws NfcDispatcher.CardException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }

        //last phase of verifications
        if (mPassphrase.getSecretKeyType() == CanonicalizedSecretKey.SecretKeyType.NFC_TAG &&
                mPassphrase.getCharArray().length == 16) {
            mOperationState = OperationState.OPERATION_STATE_PERFORM_UNLOCK;
            onUpdateProgress(calculateProgress(NUM_PROGRESS_OPERATIONS));
        } else {
            mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
            onOperationStateError(getString(R.string.nfc_configuration_error));
        }
        updateOperationState();
    }

    public void onNfcTechnologyInitialized(BaseNfcTagTechnology baseNfcTagTechnology) {
        mNfcTechnology = baseNfcTagTechnology;
    }

    @Override
    public void handleTagDiscoveredIntent(Intent intent) throws NfcDispatcher.CardException {

    }

    /**
     * UnlockAsyncTask callbacks that will be redirected to the dialog,
     */

    @Override
    public void onErrorCouldNotExtractKey() {
        onFatalError(R.string.error_could_not_extract_private_key);
    }

    @Override
    public void onErrorWrongPassphrase() {
        onShowProgressBar(false);
        onOperationStateError(getString(R.string.error_wrong_pin));
        mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
        updateOperationState();
    }

    @Override
    public void onUnlockOperationSuccess() {
        Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");
        finishCaching(mPassphrase);
        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
    }

    /**
     * Dispatch messages to the progress handler.
     * Use this to update the progress from the background thread.
     *
     * @param operationSequence
     */
    private void postProgressToMainThread(int operationSequence) {
        Message message = mProgressHandler.obtainMessage(MESSAGE_PROGRESS_UPDATE, operationSequence);
        message.sendToTarget();
    }

    /**
     * Progress Handler
     */
    public class ProgressHandler extends Handler {

        public ProgressHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_PROGRESS_UPDATE: {
                    onUpdateProgress(calculateProgress((Integer) msg.obj));
                }
                break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
