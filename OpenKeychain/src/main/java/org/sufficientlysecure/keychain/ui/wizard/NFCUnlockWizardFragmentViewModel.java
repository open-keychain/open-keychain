package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Context;
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
import android.util.Log;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
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
    private boolean mPinMovedToCard = false;


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

        void onUpdateNavigationState(boolean hideBack, boolean hideNext);
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
            case OPERATION_STATE_FINALIZED: {
                return true;
            }
            default:
                return false;
        }
    }

    public boolean handleOperationStateWaitForNFCTag() {
        mOnViewModelEventBind.onShowProgressBar(true);
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_move_card));
        mOnViewModelEventBind.onProgressBarUpdateStyle(true, mContext.getResources().
                getColor(R.color.android_green_dark));
        mOnViewModelEventBind.onOperationStateOK(null);
        return false;
    }

    public boolean handleOperationStatePinUpload() {
        return false;
    }

    public boolean handleOperationStateCardReady() {
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_pin_moved_to_card));
        mOnViewModelEventBind.onOperationStateCompleted(null);
        mOnViewModelEventBind.onUpdateNavigationState(false, false);
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


    @Override
    public void onNfcError(Exception exception) {
        mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
        updateOperationState();
    }

    @Override
    public void onNfcPreExecute() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }
        mOperationState = OperationState.OPERATION_STATE_NFC_PIN_UPLOAD;
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_configuring_card));
        mOnViewModelEventBind.onShowProgressBar(true);
        mOnViewModelEventBind.onProgressBarUpdateStyle(false, mContext.getResources().
                getColor(R.color.android_green_dark));
        mOnViewModelEventBind.onUpdateProgress(0);
    }

    @Override
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
            e.printStackTrace();
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
            mOnViewModelEventBind.onUpdateProgress(calculateProgress(NUM_PROGRESS_OPERATIONS));
        } else {
            mOperationState = OperationState.OPERATION_STATE_WAITING_FOR_NFC_TAG;
            mOnViewModelEventBind.onOperationStateError(mContext.
                    getString(R.string.nfc_configuration_error));
        }
        updateOperationState();
    }

    /**
     * Method that handles NFC Tag discovery.
     * Done in background. Don't do UI manipulations here!
     *
     * @param intent
     */
    @Override
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
                    page += 1;
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
                    page += 1;
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

        //dynamic commands
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
