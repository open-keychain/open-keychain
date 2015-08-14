package org.sufficientlysecure.keychain.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * NFC Dispatcher class
 */
public final class NfcDispatcher {
    public static final String EXTRA_TAG_HANDLING_ENABLED = "tag_handling_enabled";
    public static final String TAG = "NfcDispatcher";
    public static final short EXCEPTION_STATUS_GENERIC = -1;

    private Activity mActivity;
    private NfcAdapter mNfcAdapter;
    private boolean mTagHandlingEnabled;
    private NfcDispatcherCallback mNfcDispatcherCallback;
    private NfcDispatchTask mNfcDispatchTask;
    private RegisteredTechHandler mRegisteredTechHandler;
    private BaseNfcTagTechnology mBaseNfcTagTechnology;

    /**
     * NFC Exception
     */
    public static class CardException extends IOException {
        private short mResponseCode;

        public CardException(String detailMessage, short responseCode) {
            super(detailMessage);
            mResponseCode = responseCode;
        }

        public short getResponseCode() {
            return mResponseCode;
        }
    }

    /**
     * Dispatch interface
     */
    public interface NfcDispatcherCallback {
        void onNfcPreExecute() throws CardException;

        void doNfcInBackground() throws CardException;

        void onNfcPostExecute() throws CardException;

        void onNfcError(CardException exception);

        void handleTagDiscoveredIntent(Intent intent) throws CardException;

        void onNfcTechnologyInitialized(BaseNfcTagTechnology baseNfcTagTechnology);
    }

    /**
     * Generic dispatch implementation
     */

    public NfcDispatcher(NfcDispatcherCallback callback, Activity activity,
                         RegisteredTechHandler registeredTechHandler) {
        mRegisteredTechHandler = registeredTechHandler;
        mActivity = activity;
        mNfcDispatcherCallback = callback;
        if (callback == null) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Initializes the dispatcher.
     *
     * @param savedInstanceState
     */
    public void initialize(Bundle savedInstanceState) {
        // Check whether we're recreating a previously destroyed instance
        // Restore value of members from saved state
        mTagHandlingEnabled = savedInstanceState == null ||
                savedInstanceState.getBoolean(EXTRA_TAG_HANDLING_ENABLED);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_TAG_HANDLING_ENABLED, mTagHandlingEnabled);
    }

    /**
     * Handles the pause lifecycle.
     */
    public void onPause() {
        try {
            disableNfcForegroundDispatch();
        } catch (CardException e) {

        }
    }

    /**
     * Handles the resume lifecycle.
     */
    public void onResume() {
        enableNfcForegroundDispatch();
    }

    /**
     * Handles the destroy lifecycle.
     */
    public void onDestroy() {
        try {
            cancelDispatchTask();
        } catch (CardException e) {

        }
    }

    /**
     * Disconnects the nfc tag.
     */
    private void disconnectFromCard() throws CardException {
        if (mBaseNfcTagTechnology != null && mBaseNfcTagTechnology.isConnected()) {
            mBaseNfcTagTechnology.close();
        }
    }

    /**
     * Cancels the dispatch async task.
     */
    private void cancelDispatchTask() throws CardException {
        disconnectFromCard();
        if (mNfcDispatchTask != null) {
            mNfcDispatchTask.cancel(true);
            mNfcDispatchTask = null;
        }
    }

    /**
     * Method that receives all nfc errors that may occur.
     * Besides receiving nfc errors, any pending async operation will be canceled.
     * A try is used on this method to avoid disconnect exceptions when there is a operation failure.
     * Otherwise the previous exception message may be lost.
     *
     * @param e
     */
    protected void handleNfcError(CardException e) {
        try {
            cancelDispatchTask();
        } catch (CardException e1) {
            mNfcDispatcherCallback.onNfcError(e1);
            return;
        }
        mNfcDispatcherCallback.onNfcError(e);
    }

    /**
     * Handle the NFC Intent
     *
     * @param intent
     */
    public void onNewIntent(final Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && mTagHandlingEnabled ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) && mTagHandlingEnabled) {
            try {
                handleIntentInBackground(intent);
            } catch (CardException e) {
                handleNfcError(e);
            }
        }
    }

    /**
     * Handle the intent in the background. Only one task may be active per tag detection.
     *
     * @param intent
     * @throws CardException
     */
    void handleIntentInBackground(final Intent intent) throws CardException {
        //card still connected, keep the current task running
        if (mBaseNfcTagTechnology != null && mBaseNfcTagTechnology.isConnected()) {
            return;
        }
        if (mNfcDispatchTask == null) {
            mNfcDispatchTask = new NfcDispatchTask(intent);
            mNfcDispatchTask.execute();
        }
    }

    public void pauseTagHandling() {
        mTagHandlingEnabled = false;
    }

    public void resumeTagHandling() {
        mTagHandlingEnabled = true;
    }

    /**
     * Disable foreground dispatch in onPause!
     */
    public void disableNfcForegroundDispatch() throws CardException {
        if (mNfcAdapter == null) {
            return;
        }
        cancelDispatchTask();
        mNfcAdapter.disableForegroundDispatch(mActivity);
        Log.d(TAG, "NfcForegroundDispatch has been disabled!");
    }

    /**
     * Receive new NFC Intents to this activity only by enabling foreground dispatch.
     * This can only be done in onResume!
     */
    public void enableNfcForegroundDispatch() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
        if (mNfcAdapter == null) {
            return;
        }
        Intent nfcI = new Intent(mActivity, mActivity.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(mActivity, 0, nfcI,
                PendingIntent.FLAG_CANCEL_CURRENT);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        // https://code.google.com/p/android/issues/detail?id=62918
        // maybe mNfcAdapter.enableReaderMode(); ?
        try {
            mNfcAdapter.enableForegroundDispatch(mActivity, nfcPendingIntent, writeTagFilters, null);
        } catch (IllegalStateException e) {
            Log.i(TAG, "NfcForegroundDispatch Error!", e);
        }
        Log.d(TAG, "NfcForegroundDispatch has been enabled!");
    }

    /**
     * Handles the intent. This method will attempt to instantiate any registered nfc tag technologies.
     * When registering a nfc technology, it will be given priority to the first element of the array.
     *
     * @param intent
     * @throws CardException
     */
    void handleTagDiscoveredIntent(Intent intent) throws CardException {
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (mRegisteredTechHandler == null || mRegisteredTechHandler.getNumNfcTechnologies() == 0) {
            throw new CardException(mActivity.getString(R.string.error_nfc_dispatcher_no_registered_tech),
                    EXCEPTION_STATUS_GENERIC);
        }

        for (Class nfcTechnologyClass : mRegisteredTechHandler) {
            if (nfcTechnologyClass.equals(MifareUltralight.class)) {
                android.nfc.tech.MifareUltralight mifareUltralight = android.nfc.tech.
                        MifareUltralight.get(detectedTag);
                if (mifareUltralight != null) {
                    Log.v(TAG, "Using Mifare UltraLight nfc technology");
                    mBaseNfcTagTechnology = new MifareUltralight(mifareUltralight, mActivity);
                    mNfcDispatcherCallback.onNfcTechnologyInitialized(mBaseNfcTagTechnology);
                }
                break;
            } else if (nfcTechnologyClass.equals(Ndef.class)) {
                android.nfc.tech.Ndef ndef = android.nfc.tech.Ndef.get(detectedTag);
                if (ndef != null) {
                    Log.v(TAG, "Using Ndef nfc technology");
                    mBaseNfcTagTechnology = new Ndef(ndef, mActivity);
                    mNfcDispatcherCallback.onNfcTechnologyInitialized(mBaseNfcTagTechnology);
                    break;
                }
            } else if (nfcTechnologyClass.equals(NdefFormatable.class)) {
                android.nfc.tech.NdefFormatable ndefFormatable = android.nfc.tech.
                        NdefFormatable.get(detectedTag);
                if (ndefFormatable != null) {
                    Log.v(TAG, "Using NdefFormatable nfc technology, only write operations are available");
                    mBaseNfcTagTechnology = new NdefFormatable(ndefFormatable, mActivity);
                    mNfcDispatcherCallback.onNfcTechnologyInitialized(mBaseNfcTagTechnology);
                    break;
                }
            }
        }
    }

    /**
     * Actual NFC operations are executed in doInBackground to not block the UI thread
     */
    private class NfcDispatchTask extends AsyncTask<Void, Void, CardException> {
        private Intent mIntent;

        public NfcDispatchTask(Intent intent) {
            mIntent = intent;
        }

        @Override
        protected void onCancelled(CardException e) {
            super.onCancelled(e);
            mNfcDispatchTask = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                mNfcDispatcherCallback.onNfcPreExecute();
            } catch (CardException e) {
                handleNfcError(e);
            }
        }

        @Override
        protected CardException doInBackground(Void... params) {
            try {
                handleTagDiscoveredIntent(mIntent);
                mNfcDispatcherCallback.doNfcInBackground();
            } catch (CardException e) {
                return e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(CardException exception) {
            super.onPostExecute(exception);

            if (exception != null) {
                handleNfcError(exception);
                return;
            }

            try {
                mNfcDispatcherCallback.onNfcPostExecute();
                mNfcDispatchTask = null;
            } catch (CardException e) {
                handleNfcError(e);
            }
        }
    }

    /**
     * Registers all the handled NFC technologies by the host.
     */
    public static class RegisteredTechHandler implements Parcelable, Iterable<Class> {
        private ArrayList<Class> mRegisterTechArray;

        public RegisteredTechHandler() {
            mRegisterTechArray = new ArrayList<>();
        }

        public int getNumNfcTechnologies() {
            return mRegisterTechArray.size();
        }

        public void put(Class NfcTech) {
            mRegisterTechArray.add(NfcTech);
        }


        public boolean has(Class nfcTech) {
            for (Class nfcTechItem : mRegisterTechArray) {
                if (nfcTechItem.equals(nfcTech)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(this.mRegisterTechArray);
        }

        protected RegisteredTechHandler(Parcel in) {
            this.mRegisterTechArray = new ArrayList<>();
            in.readList(this.mRegisterTechArray, List.class.getClassLoader());
        }

        public final Parcelable.Creator<RegisteredTechHandler> CREATOR = new Parcelable.Creator<RegisteredTechHandler>() {
            public RegisteredTechHandler createFromParcel(Parcel source) {
                return new RegisteredTechHandler(source);
            }

            public RegisteredTechHandler[] newArray(int size) {
                return new RegisteredTechHandler[size];
            }
        };

        @Override
        public Iterator<Class> iterator() {
            return mRegisterTechArray.listIterator();
        }
    }
}

