package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
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
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
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
    private FeedbackIndicatorView mUnlockUserFeedback;
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
        return inflater.inflate(R.layout.unlock_nfc_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
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
        mUnlockUserFeedback.showWrongTextMessage(error, true);
    }

    /**
     * Notifies the user if the operation was successful.
     *
     * @param showText
     */
    public void onOperationStateOK(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, false);
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
     * Updates the view state by giving feedback to the user.
     */
    public void onOperationStateCompleted(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, true);
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
    public Throwable doNfcInBackground() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return null;
        }

        if (mNfcTechnology != null) {
            mNfcTechnology.connect();
        } else {
            throw new IOException("Unsupported Technology -> no data was present");
        }

        postProgressToMainThread(2);
        //generates the pin
        byte[] pin;

        try {
            pin = generateSecureRoomPin();
            String sPin = new String(pin, "ISO-8859-1");

            mNfcPin = new Passphrase(sPin.toCharArray());
            mNfcPin.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.NFC_TAG);

            if (Constants.DEBUG) {
                Log.v(Constants.TAG, "Generated Pin: " + Hex.toHexString(pin));
            }
        } catch (NoSuchAlgorithmException e) {
            mNfcTechnology.close();
            return new IOException(e.getCause());
        }

        postProgressToMainThread(3);

        //upload the key
        mNfcTechnology.upload(pin);

        //read the pin
        byte[] nfcPin = mNfcTechnology.read();

        //verify step
        mNfcTechnology.close();
        postProgressToMainThread(4);

        byte[] pinPasspgrase = new String(mNfcPin.getCharArray()).getBytes("ISO-8859-1");
        if (mNfcTechnology.verify(pinPasspgrase, nfcPin)) {
            mPinMovedToCard = true;
            return null;
        }
        return new IOException("Generate 128 bit passphrase did not match - internal error");
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

        //get device NDEF records
        if (Constants.DEBUG) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                Log.v(Constants.TAG, msg.toString());
            }
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
        onShowProgressBar(true);
        onTipTextUpdate(getActivity().getString(R.string.nfc_move_card));
        onProgressBarUpdateStyle(true, getActivity().getResources().getColor(R.color.android_green_dark));
        onOperationStateOK(null);
        return false;
    }

    /**
     * Handles the card ready state.
     *
     * @return
     */
    public boolean handleOperationStateCardReady() {
        onTipTextUpdate(getString(R.string.nfc_pin_moved_to_card));
        onOperationStateCompleted(null);
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
