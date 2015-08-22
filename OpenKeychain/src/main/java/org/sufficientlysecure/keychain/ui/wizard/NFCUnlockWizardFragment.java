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
package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.nfc.BaseNfcTagTechnology;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Wizard fragment that handles the NFC configuration.
 */
public class NFCUnlockWizardFragment extends WizardFragment
        implements CreateKeyWizardActivity.NfcListenerFragment {
    public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";
    public static final String STATE_SAVE_NFC_PIN = "STATE_SAVE_NFC_PIN";
    public static final int NUM_PROGRESS_OPERATIONS = 5; //never zero!!
    public static final int MESSAGE_PROGRESS_UPDATE = 1;
    private TextView mUnlockTip;
    private ProgressBar mProgressBar;
    private OperationState mOperationState;
    private Passphrase mNfcPin;
    private BaseNfcTagTechnology mNfcTechnology;
    private ProgressHandler mProgressHandler;
    private boolean mPinMovedToCard = false;

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_WAITING_FOR_NFC_TAG,
        OPERATION_STATE_NFC_PIN_UPLOAD,
        OPERATION_STATE_CARD_READY,
        OPERATION_STATE_FINALIZED
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mNfcPin = savedInstanceState.getParcelable(STATE_SAVE_NFC_PIN);
            mOperationState = (OperationState) savedInstanceState.getSerializable(STATE_SAVE_OPERATION_STATE);
        }

        mProgressHandler = new ProgressHandler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_nfc_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false, true);
        }

        if (savedInstanceState == null) {
            initializeUnlockOperation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SAVE_OPERATION_STATE, mOperationState);
        outState.putParcelable(STATE_SAVE_NFC_PIN, mNfcPin);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mProgressHandler.removeMessages(MESSAGE_PROGRESS_UPDATE);
        mProgressHandler = null;
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
        updateOperationState();
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    public void onOperationStateError(String error) {
        Notify.create(getActivity(), error, Notify.Style.WARN).show();
    }

    /**
     * Allows the user to advance to the next wizard step.
     *
     * @return
     */
    @Override
    public boolean onNextClicked() {
        return updateOperationState();
    }

    /**
     * Updates the layout top text.
     *
     * @param text
     */
    public void onTipTextUpdate(CharSequence text) {
        mUnlockTip.setText(text);
    }

    /**
     * Shows or hides the progress bar.
     *
     * @param show
     */
    public void onShowProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Updates the progress bar.
     *
     * @param progress
     */
    public void onUpdateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    /**
     * Updates the progress bar style that is displayed to the user.
     *
     * @param indeterminate
     * @param tint
     */
    public void onProgressBarUpdateStyle(boolean indeterminate, int tint) {
        mProgressBar.setIndeterminate(indeterminate);
        DrawableCompat.setTint(mProgressBar.getIndeterminateDrawable(), tint);
        DrawableCompat.setTint(mProgressBar.getProgressDrawable(), tint);
    }

    /**
     * Returns the current nfc technology being used.
     *
     * @return
     */
    public BaseNfcTagTechnology getNfcTechnology() {
        return mNfcTechnology;
    }

    /**
     * NFC handling
     *
     * @param exception
     */
    public void onNfcError(Exception exception) {
        mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
        updateOperationState();
    }

    public void onNfcPreExecute() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }
        mOperationState = OperationState.OPERATION_STATE_NFC_PIN_UPLOAD;
        onTipTextUpdate(getActivity().getString(R.string.nfc_configuring_card));
        onShowProgressBar(true);
        onProgressBarUpdateStyle(false, getResources().getColor(R.color.android_green_dark));
        onUpdateProgress(0);
    }

    /**
     * Connects to the NFC card and uploads the pin.
     * There is a small verification just to be sure that the uploaded pin matches the
     * generated pin.
     *
     * @return
     * @throws IOException
     */
    public void doNfcInBackground() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }

        if (mNfcTechnology != null) {
            mNfcTechnology.connect();
        } else {
            throw new IOException(getString(R.string.error_nfc_dispatcher_no_registered_tech));
        }

        postProgressToMainThread(2);
        //generates the pin
        byte[] pin;

        try {
            pin = generateSecureRoomPin();
            mNfcPin = new Passphrase(pin);
            mNfcPin.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.NFC_TAG);

            if (Constants.DEBUG) {
                Log.v(Constants.TAG, "Generated Pin: " + Hex.toHexString(pin));
            }
        } catch (NoSuchAlgorithmException e) {
            mNfcTechnology.close();
            throw new IOException(getString(R.string.error_nfc_failed_to_generate_pin));
        }

        postProgressToMainThread(3);

        //upload the key
        mNfcTechnology.upload(pin);

        //read the pin
        byte[] nfcPin = mNfcTechnology.read();

        //verify step
        mNfcTechnology.close();
        postProgressToMainThread(4);

        if (mNfcTechnology.verify(pin, nfcPin)) {
            mPinMovedToCard = true;
            return;
        }
        throw new IOException(getString(R.string.error_nfc_encoded_pin_mismatch));
    }

    @Override
    public void onNfcPostExecute() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }
        //last phase of verifications
        if (mNfcPin.getSecretKeyType() == CanonicalizedSecretKey.SecretKeyType.NFC_TAG &&
                mNfcPin.getCharArray().length == 16 && mPinMovedToCard) {
            mOperationState = OperationState.OPERATION_STATE_CARD_READY;
            onUpdateProgress(calculateProgress(NUM_PROGRESS_OPERATIONS));
        } else {
            mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
            onOperationStateError(getString(R.string.nfc_configuration_error));
        }
        updateOperationState();
    }

    /**
     * Method that handles NFC Tag discovery.
     * Done in background. Don't do UI manipulations here!
     *
     * @param intent
     * @throws IOException
     */
    public void onNfcTagDiscovery(Intent intent) throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }

        //extract the supported technologies
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareUltralight mifareUltralight = MifareUltralight.get(detectedTag);
        if (mifareUltralight != null) {
            if (Constants.DEBUG) {
                Log.v(Constants.TAG, "Using Mifare Ultra Light tech");
            }
            mNfcTechnology = new org.sufficientlysecure.keychain.nfc.
                    MifareUltralight(mifareUltralight, getActivity());
            postProgressToMainThread(1);
        }
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
     * Updates the operation state.
     *
     * @return
     */
    public boolean updateOperationState() {
        switch (mOperationState) {
            case OPERATION_STATE_WAITING_FOR_NFC_TAG:
                return handleOperationStateWaitForNFCTag();
            case OPERATION_STATE_CARD_READY:
                return handleOperationStateCardReady();
            case OPERATION_STATE_FINALIZED: {
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Handles the wait for nfc tag state.
     * The wizard will wait for the user to move the nfc tag to the back of the device before
     * proceeding.
     *
     * @return
     */
    public boolean handleOperationStateWaitForNFCTag() {
        onUpdateProgress(0);
        onShowProgressBar(true);
        onTipTextUpdate(getActivity().getString(R.string.nfc_move_card));
        onProgressBarUpdateStyle(false, getActivity().getResources().getColor(R.color.android_green_dark));
        return false;
    }

    /**
     * Handles the card ready state.
     *
     * @return
     */
    public boolean handleOperationStateCardReady() {
        onTipTextUpdate(getString(R.string.nfc_pin_moved_to_card));
        mWizardFragmentListener.onHideNavigationButtons(false, false);

        //update the results back to the activity holding the data
        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.setPassphrase(mNfcPin);
        }

        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
        return false;
    }

    /**
     * Generates a random 128 bit key.
     *
     * @throws NoSuchAlgorithmException
     */
    public byte[] generateSecureRoomPin() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte buffer[] = new byte[16];
        sr.nextBytes(buffer);
        return buffer;
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
