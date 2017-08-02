/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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


import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.ui.util.Notify;

/**
 * This class contains NFC functionality that can be shared across Fragments or Activities.
 */
public class NfcHelper {
    private static final int NFC_SENT = 1;


    public static NfcHelper getInstance() {
            return new NfcHelper();
    }

    private NfcHelper() { }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initNfcIfSupported(final Activity activity, final KeyRepository keyRepository, final Uri dataUri) {
        // check if NFC Beam is supported (>= Android 4.1)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        // Check for available NFC Adapter
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            return;
        }

        final NfcHandler nfcHandler = new NfcHandler(activity);

        // Implementation for the OnNdefPushCompleteCallback interface
        final NfcAdapter.OnNdefPushCompleteCallback ndefCompleteCallback = new NfcAdapter.OnNdefPushCompleteCallback() {
            @Override
            public void onNdefPushComplete(NfcEvent event) {
                // A handler is needed to send messages to the activity when this
                // callback occurs, because it happens from a binder thread
                nfcHandler.obtainMessage(NFC_SENT).sendToTarget();
            }
        };

        /* Retrieve mNfcKeyringBytes here asynchronously (to not block the UI) and init nfc adapter afterwards.
         * nfcKeyringBytes can not be retrieved in createNdefMessage, because this process has no permissions
         * to query the Uri.
         */
        AsyncTask<Void, Void, byte[]> initTask = new AsyncTask<Void, Void, byte[]>() {
            protected byte[] doInBackground(Void... unused) {
                try {
                    long masterKeyId = keyRepository.getCachedPublicKeyRing(dataUri).extractOrGetMasterKeyId();
                    return keyRepository.loadPublicKeyRingData(masterKeyId);
                } catch (NotFoundException | PgpKeyNotFoundException e) {
                    Log.e(Constants.TAG, "key not found!", e);
                    return null;
                } catch (IllegalStateException e) {
                    // we specifically handle the database error if the blob is too large: "Couldn't read row 0, col 0
                    // from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it."
                    if (!e.getMessage().startsWith("Couldn't read row")) {
                        throw e;
                    }
                    Log.e(Constants.TAG, "key blob too large to retrieve", e);
                    return null;
                }
            }

            protected void onPostExecute(final byte[] keyringBytes) {
                if (keyringBytes == null) {
                    Log.e(Constants.TAG, "Could not obtain keyring bytes, NFC not available!");
                }

                if (activity.isFinishing()) {
                    return;
                }

                // Implementation for the CreateNdefMessageCallback interface
                CreateNdefMessageCallback ndefCallback = new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                    /*
                     * When a device receives a push with an AAR in it, the application specified in the AAR is
                     * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
                     * guarantee that this activity starts when receiving a beamed message. For now, this code
                     * uses the tag dispatch system.
                     */
                        return new NdefMessage(NdefRecord.createMime(Constants.MIME_TYPE_KEYS, keyringBytes),
                                NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
                    }
                };

                // Register callback to set NDEF message
                nfcAdapter.setNdefPushMessageCallback(ndefCallback, activity);
                // Register callback to listen for message-sent success
                nfcAdapter.setOnNdefPushCompleteCallback(ndefCompleteCallback, activity);

                Log.d(Constants.TAG, "NFC NDEF enabled!");
            }
        };

        initTask.execute();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void invokeNfcBeam(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        boolean hasNfc = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        if (!hasNfc) {
            Notify.create(activity, R.string.no_nfc_support, Notify.LENGTH_LONG, Notify.Style.ERROR).show();
            return;
        }
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        boolean isNfcEnabled = nfcAdapter != null && nfcAdapter.isEnabled();
        if (!isNfcEnabled) {
            Notify.create(activity, R.string.error_nfc_needed, Notify.LENGTH_LONG, Notify.Style.ERROR,
                    new Notify.ActionListener() {
                        @Override
                        public void onAction() {
                            Intent intentSettings = new Intent(Settings.ACTION_NFC_SETTINGS);
                            activity.startActivity(intentSettings);
                }
            }, R.string.menu_nfc_preferences).show();

            return;
        }

        if (!nfcAdapter.isNdefPushEnabled()) {
            Notify.create(activity, R.string.error_beam_needed, Notify.LENGTH_LONG, Notify.Style.ERROR, new Notify.ActionListener() {
                @Override
                public void onAction() {
                    Intent intentSettings = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                    activity.startActivity(intentSettings);
                }
            }, R.string.menu_beam_preferences).show();

            return;
        }

        nfcAdapter.invokeBeam(activity);
    }

    private static class NfcHandler extends Handler {
        private final WeakReference<Activity> activityReference;

        NfcHandler(Activity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (activityReference.get() != null) {
                switch (msg.what) {
                    case NFC_SENT:
                        Notify.create(activityReference.get(), R.string.nfc_successful, Notify.Style.OK).show();
                        break;
                }
            }
        }
    }

}