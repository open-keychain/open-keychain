/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Kent Nguyen <kentnguyen@moneylover.me>
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

package org.sufficientlysecure.keychain.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.lang.ref.WeakReference;

/**
 * This class contains NFC functionality that can be shared across Fragments or Activities.
 */

public class NfcHelper {

    private Activity mActivity;
    private ProviderHelper mProviderHelper;

    /**
     * NFC: This handler receives a message from onNdefPushComplete
     */
    private static NfcHandler mNfcHandler;

    private NfcAdapter mNfcAdapter;
    private NfcAdapter.CreateNdefMessageCallback mNdefCallback;
    private NfcAdapter.OnNdefPushCompleteCallback mNdefCompleteCallback;
    private byte[] mNfcKeyringBytes;
    private static final int NFC_SENT = 1;

    /**
     * Initializes the NfcHelper.
     */
    public NfcHelper(final Activity activity, final ProviderHelper providerHelper) {
        mActivity = activity;
        mProviderHelper = providerHelper;

        mNfcHandler = new NfcHandler(mActivity);
    }

    /**
     * Return true if the NFC Adapter of this Helper has any features enabled.
     *
     * @return true if this NFC Adapter has any features enabled
     */
    public boolean isEnabled() {
        return mNfcAdapter.isEnabled();
    }

    /**
     * NFC: Initialize NFC sharing if OS and device supports it
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initNfc(final Uri dataUri) {
        // check if NFC Beam is supported (>= Android 4.1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            // Implementation for the CreateNdefMessageCallback interface
            mNdefCallback = new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    /*
                     * When a device receives a push with an AAR in it, the application specified in the AAR is
                     * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
                     * guarantee that this activity starts when receiving a beamed message. For now, this code
                     * uses the tag dispatch system.
                     */
                    return new NdefMessage(NdefRecord.createMime(Constants.MIME_TYPE_KEYS,
                            mNfcKeyringBytes), NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
                }
            };

            // Implementation for the OnNdefPushCompleteCallback interface
            mNdefCompleteCallback = new NfcAdapter.OnNdefPushCompleteCallback() {
                @Override
                public void onNdefPushComplete(NfcEvent event) {
                    // A handler is needed to send messages to the activity when this
                    // callback occurs, because it happens from a binder thread
                    mNfcHandler.obtainMessage(NFC_SENT).sendToTarget();
                }
            };

            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
            if (mNfcAdapter != null) {
                /*
                 * Retrieve mNfcKeyringBytes here asynchronously (to not block the UI)
                 * and init nfc adapter afterwards.
                 * mNfcKeyringBytes can not be retrieved in createNdefMessage, because this process
                 * has no permissions to query the Uri.
                 */
                AsyncTask<Void, Void, Void> initTask =
                        new AsyncTask<Void, Void, Void>() {
                            protected Void doInBackground(Void... unused) {
                                try {
                                    Uri blobUri =
                                            KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
                                    mNfcKeyringBytes = (byte[]) mProviderHelper.read().getGenericData(
                                            blobUri,
                                            KeychainContract.KeyRingData.KEY_RING_DATA,
                                            Cursor.FIELD_TYPE_BLOB);
                                } catch (ProviderReader.NotFoundException e) {
                                    Log.e(Constants.TAG, "key not found!", e);
                                }

                                // no AsyncTask return (Void)
                                return null;
                            }

                            protected void onPostExecute(Void unused) {
                                if (mActivity.isFinishing()) {
                                    return;
                                }

                                // Register callback to set NDEF message
                                mNfcAdapter.setNdefPushMessageCallback(mNdefCallback,
                                        mActivity);
                                // Register callback to listen for message-sent success
                                mNfcAdapter.setOnNdefPushCompleteCallback(mNdefCompleteCallback,
                                        mActivity);
                            }
                        };

                initTask.execute();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void invokeNfcBeam() {
        // Check if device supports NFC
        if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Notify.create(mActivity, R.string.no_nfc_support, Notify.LENGTH_LONG, Notify.Style.ERROR).show();
            return;
        }
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            Notify.create(mActivity, R.string.error_nfc_needed, Notify.LENGTH_LONG, Notify.Style.ERROR, new Notify.ActionListener() {
                @Override
                public void onAction() {
                    Intent intentSettings = new Intent(Settings.ACTION_NFC_SETTINGS);
                    mActivity.startActivity(intentSettings);
                }
            }, R.string.menu_nfc_preferences).show();

            return;
        }

        if (!mNfcAdapter.isNdefPushEnabled()) {
            Notify.create(mActivity, R.string.error_beam_needed, Notify.LENGTH_LONG, Notify.Style.ERROR, new Notify.ActionListener() {
                @Override
                public void onAction() {
                    Intent intentSettings = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                    mActivity.startActivity(intentSettings);
                }
            }, R.string.menu_beam_preferences).show();

            return;
        }

        mNfcAdapter.invokeBeam(mActivity);
    }

    private static class NfcHandler extends Handler {
        private final WeakReference<Activity> mActivityReference;

        public NfcHandler(Activity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivityReference.get() != null) {
                switch (msg.what) {
                    case NFC_SENT:
                        Notify.create(mActivityReference.get(), R.string.nfc_successful, Notify.Style.OK).show();
                        break;
                }
            }
        }
    }

}