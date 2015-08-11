package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class NFCUnlockWizardFragment extends WizardFragment
        implements CreateKeyWizardActivity.NfcListenerFragment {
    public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";
    public static final String STATE_SAVE_NFC_PIN = "STATE_SAVE_NFC_PIN";
    public static final int NUM_PROGRESS_OPERATIONS = 2; //never zero!!
    public static final int MESSAGE_PROGRESS_UPDATE = 1;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;
    private OperationState mOperationState;
    private Passphrase mNfcPin;
    private NfcTechnology mNfcTechnology;
    private ProgressHandler mProgressHandler;

    /**
     * NFC Technology interface
     */
    public interface NfcTechnology {
        void connect() throws IOException;
    }

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_WAITING_FOR_NFC_TAG,
        OPERATION_STATE_NFC_PIN_UPLOAD,
        OPERATION_STATE_CARD_READY
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

    public void onOperationStateOK(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, false);
    }

    @Override
    public boolean onNextClicked() {
        return updateOperationState();
    }

    @Override
    public boolean onBackClicked() {
        return super.onBackClicked();
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    public void onOperationStateCompleted(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, true);
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

    /**
     * NFC handling
     *
     * @param exception
     */
    public void onNfcError(Exception exception) {

    }

    public void onNfcPreExecute() throws IOException {
        onTipTextUpdate(getActivity().getString(R.string.nfc_configuring_card));
        onShowProgressBar(true);
        onProgressBarUpdateStyle(false, getResources().
                getColor(R.color.android_green_dark));
        onUpdateProgress(0);
    }

    @Override
    public void doNfcInBackground() throws IOException {
        if (mNfcTechnology != null) {
            mNfcTechnology.connect();
        }
        postProgressToMainThread(2);
    }

    @Override
    public void onNfcPostExecute() throws IOException {

    }

    /**
     * Method that handles NFC Tag discovery.
     * Done in background. Don't do UI manipulations here!
     *
     * @param intent
     * @throws IOException
     */
    public void onNfcTagDiscovery(Intent intent) throws IOException {
        //extract the supported technologies
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NfcA nfcA = NfcA.get(detectedTag);
        if (nfcA != null) {
            if (Constants.DEBUG) {
                Log.v(Constants.TAG, "Using NfcA tech");
            }
            mNfcTechnology = new NfcATechnology(nfcA);
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
            case OPERATION_STATE_NFC_PIN_UPLOAD:
                return handleOperationStatePinUpload();
            case OPERATION_STATE_CARD_READY:
                return handleOperationStateCardReady();
            default:
                return false;
        }
    }

    public boolean handleOperationStateWaitForNFCTag() {
        onShowProgressBar(true);
        onTipTextUpdate(getActivity().getString(R.string.nfc_move_card));
        onProgressBarUpdateStyle(true, getActivity().getResources().getColor(R.color.android_green_dark));
        return false;
    }

    public boolean handleOperationStatePinUpload() {
        return false;
    }

    public boolean handleOperationStateCardReady() {
        return false;
    }

    /**
     * Reads the tag data from the nfc tag
     *
     * @param tag
     */
    public void onNfcTagDiscovery(Tag tag) {

    }

    /**
     * Generates a random 128 bit key.
     *
     * @throws NoSuchAlgorithmException
     */
    public void generateSecureRoomPin() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte buffer[] = new byte[16];
        sr.nextBytes(buffer);
        mNfcPin = new Passphrase(Arrays.toString(buffer));
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

    /**
     * NfcA technology communication
     */
    public static class NfcATechnology implements NfcTechnology {
        private static final int sTimeout = 100000;
        protected NfcA mNfcA;

        public NfcATechnology(NfcA nfcA) {
            mNfcA = nfcA;
        }

        public void connect() throws IOException {
            mNfcA.setTimeout(sTimeout); // timeout is set to 100 seconds to avoid cancellation during calculation
            if (!mNfcA.isConnected()) {
                mNfcA.connect();
            }
        }
    }
}
