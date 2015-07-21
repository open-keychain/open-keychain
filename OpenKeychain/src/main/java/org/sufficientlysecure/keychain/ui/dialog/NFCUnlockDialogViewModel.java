package org.sufficientlysecure.keychain.ui.dialog;

import android.content.Context;
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
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.tasks.UnlockAsyncTask;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.util.Arrays;

public class NFCUnlockDialogViewModel implements BaseViewModel,
        CreateKeyWizardActivity.NfcListenerFragment,
        UnlockAsyncTask.OnUnlockAsyncTaskListener {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_PARAM_OPERATION_TYPE = "EXTRA_PARAM_OPERATION_TYPE";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_SERVICE_INTENT = "data";
    public static final int NUM_PROGRESS_OPERATIONS = 5; //never zero!!
    public static final int MESSAGE_PROGRESS_UPDATE = 1;

    private CanonicalizedSecretKeyRing mSecretRing = null;
    private Intent mServiceIntent;
    private OnViewModelEventBind mOnViewModelEventBind;
    private NfcTechnology mNfcTechnology;
    private Context mContext;
    private long mSubKeyId;
    private ProgressHandler mProgressHandler;
    private OperationState mOperationState;
    private UnlockAsyncTask mUnlockAsyncTask;
    private Passphrase mPassphrase;


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
        OPERATION_STATE_PERFORM_UNLOCK,
        OPERATION_STATE_READING_NFC_TAG,
        OPERATION_STATE_FINALIZED
    }

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void onOperationStateError(String error);

        void onOperationStateOK(String showText);

        void onTipTextUpdate(CharSequence text);

        void onShowProgressBar(boolean show);

        void onUpdateProgress(int progress);

        void onProgressBarUpdateStyle(boolean indeterminate, int tint);

        void onUpdateDialogTitle(CharSequence text);

        void onUnlockOperationSuccess(Intent serviceIntent);
    }

    /**
     * Use this constructor to initialize the view model.
     *
     * @param onViewModelEventBind
     */
    public NFCUnlockDialogViewModel(OnViewModelEventBind onViewModelEventBind) {
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
            mSubKeyId = arguments.getLong(EXTRA_SUBKEY_ID);
            mServiceIntent = arguments.getParcelable(EXTRA_SERVICE_INTENT);
            initializeUnlockOperation();
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

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

    public void onDetachFromActivity() {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
            mUnlockAsyncTask = null;
        }
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
        mOnViewModelEventBind.onShowProgressBar(true);
        mOnViewModelEventBind.onTipTextUpdate(mContext.getString(R.string.nfc_move_card));
        mOnViewModelEventBind.onProgressBarUpdateStyle(true, mContext.getResources().
                getColor(R.color.android_green_dark));
        mOnViewModelEventBind.onOperationStateOK(null);
        return false;
    }

    public boolean handleOperationPerformUnlock() {
        if (mSecretRing == null) {
            PassphraseCacheService.addCachedPassphrase(mContext,
                    Constants.key.symmetric, Constants.key.symmetric, mPassphrase,
                    mContext.getString(R.string.passp_cache_notif_pwd));

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
            mOnViewModelEventBind.onUpdateDialogTitle(mContext.getString(
                    R.string.passphrase_for_symmetric_encryption));
        } else {
            try {
                //attempt at getting the user
                ProviderHelper helper = new ProviderHelper(mContext);
                mSecretRing = helper.getCanonicalizedSecretKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));

                String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                if (mainUserIdSplit.name != null) {
                    setDialogTitleMessageForUserID(mainUserIdSplit.name);
                } else {
                    setDialogTitleMessageForUserID(mContext.getString(R.string.user_id_no_name));
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
        mOnViewModelEventBind.onUpdateDialogTitle(mContext.getString(R.string.pin_for, userId));
    }

    /**
     * Caches the passphrase on the main thread.
     *
     * @param passphrase
     */
    private void finishCaching(Passphrase passphrase) {
        CryptoInputParcel inputParcel = new CryptoInputParcel(null, passphrase);
        if (mServiceIntent != null) {
            CryptoInputParcelCacheService.addCryptoInputParcel(mContext, mServiceIntent, inputParcel);
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
        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
    }

    /**
     * Notifies the view that a fatal operation error as occurred.
     *
     * @param errorId
     */
    public void onFatalError(int errorId) {
        mOperationState = OperationState.OPERATION_STATE_FINALIZED;
        mOnViewModelEventBind.onShowProgressBar(false);
        mOnViewModelEventBind.onOperationStateError(mContext.getString(errorId));
    }

    /**
     * Method that starts the async task operation for the current pin.
     */
    public void startAsyncUnlockOperationForPin(Passphrase passphrase) {
        if (mUnlockAsyncTask != null) {
            mUnlockAsyncTask.setOnUnlockAsyncTaskListener(null);
            mUnlockAsyncTask.cancel(true);
        }

        mUnlockAsyncTask = new UnlockAsyncTask(mContext, passphrase, mSubKeyId, this);
        mUnlockAsyncTask.execute();
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
        mOperationState = OperationState.OPERATION_STATE_READING_NFC_TAG;
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

        postProgressToMainThread(1);

        //read the pin
        byte[] nfcPin = mNfcTechnology.read();

        //verify step
        mNfcTechnology.close();
        postProgressToMainThread(2);
        String sPin = new String(nfcPin, "ISO-8859-1");
        mPassphrase = new Passphrase(sPin.toCharArray());
        mPassphrase.setSecretKeyType(CanonicalizedSecretKey.SecretKeyType.NFC);

        return null;
    }

    @Override
    public void onNfcPostExecute() throws IOException {
        if (mOperationState == OperationState.OPERATION_STATE_FINALIZED) {
            return;
        }

        //last phase of verifications
        if (mPassphrase.getSecretKeyType() == CanonicalizedSecretKey.SecretKeyType.NFC &&
                mPassphrase.getCharArray().length == 16) {
            mOperationState = OperationState.OPERATION_STATE_PERFORM_UNLOCK;
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
        }

        //get device NDEF records
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            NdefMessage msg = (NdefMessage) rawMsgs[0];
        }
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
        mOnViewModelEventBind.onShowProgressBar(false);
        mOnViewModelEventBind.onOperationStateError(mContext.getString(R.string.error_wrong_pin));
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
}
