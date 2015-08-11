package org.sufficientlysecure.keychain.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;

import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;

/**
 * NFC Dispatcher class
 */
public final class NfcDispatcher {
    public static final String EXTRA_TAG_HANDLING_ENABLED = "tag_handling_enabled";
    public static final String TAG = "NfcDispatcher";

    private Activity mActivity;
    private NfcAdapter mNfcAdapter;
    private boolean mTagHandlingEnabled;
    private NfcDispatcherCallback mNfcDispatcherCallback;
    private NfcDispatchTask mNfcDispatchTask;

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
    }

    /**
     * Generic dispatch implementation
     */

    public NfcDispatcher(NfcDispatcherCallback callback, Activity activity) {
        mActivity = activity;
        mNfcDispatcherCallback = callback;
        if (callback == null) {
            throw new UnsupportedOperationException();
        }
    }

    public void initialize(Bundle savedInstanceState) {
        // Check whether we're recreating a previously destroyed instance
        // Restore value of members from saved state
        mTagHandlingEnabled = savedInstanceState == null ||
                savedInstanceState.getBoolean(EXTRA_TAG_HANDLING_ENABLED);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_TAG_HANDLING_ENABLED, mTagHandlingEnabled);
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        disableNfcForegroundDispatch();
    }

    public void onResume() {
        Log.d(TAG, "BaseNfcActivity.onResume");
        enableNfcForegroundDispatch();
    }

    public void onDestroy() {
        cancelDispatchTask();
    }

    private void cancelDispatchTask() {
        if (mNfcDispatchTask != null) {
            mNfcDispatchTask.cancel(true);
            mNfcDispatchTask = null;
        }
    }

    protected void handleNfcError(CardException e) {
        Log.e(TAG, "nfc error", e);
        cancelDispatchTask();
        mNfcDispatcherCallback.onNfcError(e);
    }

    /**
     * Handle the NFC Intent
     *
     * @param intent
     */
    public void onNewIntent(final Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && mTagHandlingEnabled) {
            try {
                handleIntentInBackground(intent);
            } catch (CardException e) {
                e.printStackTrace();
                mNfcDispatcherCallback.onNfcError(e);
            }
        }
    }

    void handleIntentInBackground(final Intent intent) throws CardException {
        if (mNfcDispatchTask != null) {
            mNfcDispatchTask.cancel(true);
            mNfcDispatchTask = null;
        }

        mNfcDispatchTask = new NfcDispatchTask(intent);
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
    public void disableNfcForegroundDispatch() {
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
        Intent nfcI = new Intent(mActivity, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(mActivity, 0, nfcI,
                PendingIntent.FLAG_CANCEL_CURRENT);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
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

    // Actual NFC operations are executed in doInBackground to not block the UI thread
    private class NfcDispatchTask extends AsyncTask<Void, Void, CardException> {
        private Intent mIntent;

        public NfcDispatchTask(Intent intent) {
            mIntent = intent;
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
                mNfcDispatcherCallback.handleTagDiscoveredIntent(mIntent);
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
            } catch (CardException e) {
                handleNfcError(e);
            }
        }
    }
}

