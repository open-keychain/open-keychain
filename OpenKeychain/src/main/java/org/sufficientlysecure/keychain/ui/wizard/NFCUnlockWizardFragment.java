package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

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
    private NfcTechnology mNfcTechnology;
    private ProgressHandler mProgressHandler;
    private boolean mPinMovedToCard = false;
    private WizardFragmentListener mWizardFragmentListener;

    /**
     * NFC Technology interface
     */
    public interface NfcTechnology {
        void connect() throws IOException;

        void upload(byte[] data) throws IOException;

        byte[] read() throws IOException;

        boolean verify(byte[] original, byte[] fromNFC) throws IOException;

        void close() throws IOException;
    }

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

    public void onUpdateNavigationState(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
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
        onProgressBarUpdateStyle(false, getResources().
                getColor(R.color.android_green_dark));
        onUpdateProgress(0);
    }

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
            mNfcPin.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.NFC);

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
        if (mNfcPin.getSecretKeyType() == CanonicalizedSecretKey.SecretKeyType.NFC &&
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
            mNfcTechnology = new MifareUltralightTechnology(mifareUltralight);
            postProgressToMainThread(1);
        } else {
            NfcA nfcA = NfcA.get(detectedTag);
            if (nfcA != null) {
                if (Constants.DEBUG) {
                    Log.v(Constants.TAG, "Using NfcA tech");
                }
                mNfcTechnology = new NfcATechnology(nfcA);
                postProgressToMainThread(1);
            }
        }

        //get device NDEF records
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            NdefMessage msg = (NdefMessage) rawMsgs[0];
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
            case OPERATION_STATE_FINALIZED: {
                return true;
            }
            default:
                return false;
        }
    }

    public boolean handleOperationStateWaitForNFCTag() {
        onShowProgressBar(true);
        onTipTextUpdate(getActivity().getString(R.string.nfc_move_card));
        onProgressBarUpdateStyle(true, getActivity().getResources().getColor(R.color.android_green_dark));
        onOperationStateOK(null);
        return false;
    }

    public boolean handleOperationStatePinUpload() {
        return false;
    }

    public boolean handleOperationStateCardReady() {
        onTipTextUpdate(getString(R.string.nfc_pin_moved_to_card));
        onOperationStateCompleted(null);
        onUpdateNavigationState(false, false);

        //update the results back to the activity holding the data
        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.setPassphrase(mNfcPin);
        }

        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
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

    /**
     * Mifare UltraLight NFC communication
     * Specification: http://www.nxp.com/documents/data_sheet/MF0ICU1.pdf
     */
    public static class MifareUltralightTechnology implements NfcTechnology {
        public static final byte COMMAND_WRITE = (byte) 0xA2;
        public static final byte MEMORY_START_BLOCK = 0x04;
        public static final byte MEMORY_END_BLOCK = 0x15;

        private static final int sTimeout = 100000;
        protected MifareUltralight mMifareUltralight;

        public MifareUltralightTechnology(MifareUltralight mifareUltralight) {
            mMifareUltralight = mifareUltralight;
        }

        @Override
        public void connect() throws IOException {
            //timeout is set to 100 seconds to avoid cancellation during calculation
            mMifareUltralight.setTimeout(sTimeout);
            if (!mMifareUltralight.isConnected()) {
                mMifareUltralight.connect();
            }
        }

        @Override
        public void upload(byte[] data) throws IOException {
            int totalBytes = data.length;
            int page = MEMORY_START_BLOCK;
            byte[] dataToSend;
            try {
                while (totalBytes > 0) {
                    dataToSend = Arrays.copyOfRange(data, (page - MEMORY_START_BLOCK) * 4,
                            (page - MEMORY_START_BLOCK) * 4 + 4);
                    mMifareUltralight.writePage(page, dataToSend);
                    totalBytes -= 4;
                    page = 1;
                }
            } catch (ArrayIndexOutOfBoundsException | IllegalStateException e) {
                close();
                throw new IOException(e.getCause());
            }
        }

        @Override
        public byte[] read() throws IOException {
            byte[] payload = new byte[16];

            int totalBytes = 16;
            int page = MEMORY_START_BLOCK;

            try {
                while (totalBytes > 0) {
                    System.arraycopy(mMifareUltralight.readPages(page), 0, payload,
                            (page - MEMORY_START_BLOCK) * 4, 4);
                    totalBytes -= 4;
                    page = 1;
                }
            } catch (ArrayIndexOutOfBoundsException | IllegalStateException e) {
                close();
                throw new IOException(e.getCause());
            }

            return payload;
        }

        @Override
        public boolean verify(byte[] original, byte[] fromNFC) throws IOException {
            return Arrays.equals(original, fromNFC);
        }


        @Override
        public void close() throws IOException {
            mMifareUltralight.close();
        }
    }

    /**
     * NfcA technology communication
     * Work in progress
     * Specification: http://apps4android.org/nfc-specifications/NFCForum-TS-Type-1-Tag_1.1.pdf
     */
    public static class NfcATechnology implements NfcTechnology {
        public static final byte HR0_STATIC_MEMORY = 0x11;
        public static final byte HR0_DYNAMIC_MEMORY = 0x10;

        //static commands
        public static final byte COMMAND_RALL = 0x00;
        public static final byte COMMAND_READ = 0x01;
        public static final byte COMMAND_WRITE_E = 0x53;
        public static final byte COMMAND_WRITE_NE = 0x1A;

        public static final byte COMMAND_RSEG = 0x10;
        public static final byte COMMAND_READ8 = 0x02;
        public static final byte COMMAND_WRITE_E8 = 0x54;
        public static final byte COMMAND_WRITE_NE8 = 0x1B;

        //Memory blocks 8 bytes each total 96 bytes
        //Byte 0 (LSB) corresponds to byte address
        //(byte address, LSB)00 00 00 00 00 00 00 00(MSB)
        //Static memory
        public static final byte STATIC_MEMORY_BLOCK_START = 0x01;
        public static final byte STATIC_MEMORY_BLOCK_END = 0x0C;
        public static final byte STATIC_MEMORY_LOCK_BLOCK = 0x0E;

        //Dynamic memory
        public static final byte DYNAMIC_MEMORY_BLOCK_START = 0x0F;
        public static final byte DYNAMIC_MEMORY_BLOCK_END = 0x15; //k

        //TLV
        public static final byte TLV_NULL = 0x00;
        public static final byte TLV_LOCK_CONTROL = 0x01;
        public static final byte TLV_MEMORY_CONTROL = 0x02;
        public static final byte TLV_NDEF_MESSAGE = 0x03;
        public static final short TLV_PROPRIETARY = 0xFD;
        public static final short TLV_TERMINATOR = 0xFE;

        //
        public static final byte OPERAND_NULL = 0x00;
        public static final byte DUMMY_FRAME = 0x00;


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
            StringBuilder dataToSend = new StringBuilder(16);
            dataToSend.append(COMMAND_RALL);
            dataToSend.append(OPERAND_NULL);
            dataToSend.append(DUMMY_FRAME);
            //UID-0-3
            //mNfcA.transceive(data);
        }

        @Override
        public byte[] read() throws IOException {
            return new byte[0];
        }

        @Override
        public boolean verify(byte[] original, byte[] fromNFC) throws IOException {
            return Arrays.equals(original, fromNFC);
        }

        public void close() throws IOException {
            mNfcA.close();
        }

        /**
         * Using NDEF to write to the tag.
         *
         * @param payload
         */
        private void writeNDEF(byte[] payload) {
            String URI = "http://nfc.openkeychain.com/unlock";
            NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, URI.getBytes(Charset.forName("US-ASCII")), new byte[0], new byte[0]);
        }
    }
}
