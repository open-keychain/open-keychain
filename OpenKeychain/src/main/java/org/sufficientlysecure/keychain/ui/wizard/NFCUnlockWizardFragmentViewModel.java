package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class NFCUnlockWizardFragmentViewModel implements BaseViewModel,
        CreateKeyWizardActivity.NfcListenerFragment {
    public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";
    public static final String STATE_SAVE_NFC_PIN = "STATE_SAVE_NFC_PIN";
    public static final int NUM_PROGRESS_OPERATIONS = 5; //never zero!!
    public static final int MESSAGE_PROGRESS_UPDATE = 1;
    private Context mContext;
    private OnViewModelEventBind mOnViewModelEventBind;
    private OperationState mOperationState;
    private Passphrase mNfcPin;
    private NfcTechnology mNfcTechnology;
    private ProgressHandler mProgressHandler;


    /**
     * NFC Technology interface
     */
    public interface NfcTechnology {
        void connect() throws IOException;

        void upload(byte[] data) throws IOException;

        void verify() throws IOException;

        void close() throws IOException;
    }

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_WAITING_FOR_NFC_TAG,
        OPERATION_STATE_NFC_PIN_UPLOAD,
        OPERATION_STATE_CARD_READY
    }

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void onOperationStateError(String error);

        void onOperationStateOK(String showText);

        void onOperationStateCompleted(String showText);

        void onTipTextUpdate(CharSequence text);

        void onShowProgressBar(boolean show);

        void onUpdateProgress(int progress);

        void onProgressBarUpdateStyle(boolean indeterminate, int tint);
    }

    public NFCUnlockWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind) {
        mOnViewModelEventBind = onViewModelEventBind;
        mProgressHandler = new ProgressHandler(Looper.getMainLooper());

        if (mOnViewModelEventBind == null) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        if (savedInstanceState != null) {
            restoreViewModelState(savedInstanceState);
        } else {
            initializeUnlockOperation();
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_OPERATION_STATE, mOperationState);
        outState.putParcelable(STATE_SAVE_NFC_PIN, mNfcPin);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        mNfcPin = savedInstanceState.getParcelable(STATE_SAVE_NFC_PIN);
        mOperationState = (OperationState) savedInstanceState.
                getSerializable(STATE_SAVE_OPERATION_STATE);
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
        mOnViewModelEventBind.onShowProgressBar(true);
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_move_card));
        mOnViewModelEventBind.onProgressBarUpdateStyle(true, mContext.getResources().
                getColor(R.color.android_green_dark));
        return false;
    }

    public boolean handleOperationStatePinUpload() {
        return false;
    }

    public boolean handleOperationStateCardReady() {
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


    @Override
    public void onNfcError(Exception exception) {

    }

    @Override
    public void onNfcPreExecute() throws IOException {
        mOperationState = OperationState.OPERATION_STATE_NFC_PIN_UPLOAD;
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_configuring_card));
        mOnViewModelEventBind.onShowProgressBar(true);
        mOnViewModelEventBind.onProgressBarUpdateStyle(false, mContext.getResources().
                getColor(R.color.android_green_dark));
        mOnViewModelEventBind.onUpdateProgress(0);
    }

    @Override
    public Throwable doNfcInBackground() throws IOException {
        if (mNfcTechnology != null) {
            mNfcTechnology.connect();
        }
        postProgressToMainThread(2);
        //generates the pin
        try {
            byte[] pin = generateSecureRoomPin();
            mNfcPin = new Passphrase(Arrays.toString(pin));

            if (Constants.DEBUG) {
                Log.v(Constants.TAG, "Generated Pin: " + Hex.toHexString(pin));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return e.getCause();
        }

        postProgressToMainThread(3);

        return null;
    }

    @Override
    public void onNfcPostExecute() throws IOException {

    }

    /**
     * Method that handles NFC Tag discovery.
     * Done in background. Don't do UI manipulations here!
     *
     * @param intent
     */
    @Override
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
     * Clears all viewModel data.
     */
    public void onDetachFromView() {
        mProgressHandler.removeMessages(MESSAGE_PROGRESS_UPDATE);
        mProgressHandler = null;
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
                    mOnViewModelEventBind.onUpdateProgress(calculateProgress((Integer) msg.obj));
                }
                break;
                default:
                    super.handleMessage(msg);

            }
        }
    }

    /**
     * NfcA technology communication
     * Specification: http://apps4android.org/nfc-specifications/NFCForum-TS-Type-1-Tag_1.1.pdf
     */
    public static class NfcATechnology implements NfcTechnology {
        public static final byte COMMAND_RALL = 0x00;
        public static final byte COMMAND_READ = 0x01;
        public static final byte COMMAND_WRITE_E = 0x53;
        public static final byte COMMAND_WRITE_NE = 0x1A;

        public static final byte COMMAND_RSEG = 0x10;
        public static final byte COMMAND_READ8 = 0x02;
        public static final byte COMMAND_WRITE_E8 = 0x54;
        public static final byte COMMAND_WRITE_NE8 = 0x1B;

        //Memory blocks 8 bytes each total 96 bytes
        public static final byte STATIC_MEMORY_BLOCK_START = 0x01;
        public static final byte STATIC_MEMORY_BLOCK_END = 0x0C;

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

        public void upload(byte[] data) throws IOException {
            mNfcA.transceive(data);

        }

        public void verify() throws IOException {

        }

        public void close() throws IOException {
            mNfcA.close();
        }
    }
}
