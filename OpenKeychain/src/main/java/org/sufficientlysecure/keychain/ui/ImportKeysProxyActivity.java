/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.tor.OrbotHelper;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Proxy activity (just a transparent content view) to scan QR Codes using the Barcode Scanner app
 */
public class ImportKeysProxyActivity extends FragmentActivity {

    public static final String ACTION_QR_CODE_API = OpenKeychainIntents.IMPORT_KEY_FROM_QR_CODE;
    // implies activity returns scanned fingerprint as extra and does not import
    public static final String ACTION_SCAN_WITH_RESULT = Constants.INTENT_PREFIX + "SCAN_QR_CODE_WITH_RESULT";
    public static final String ACTION_SCAN_IMPORT = Constants.INTENT_PREFIX + "SCAN_QR_CODE_IMPORT";

    public static final String EXTRA_FINGERPRINT = "fingerprint";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        handleActions(getIntent());
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Uri dataUri = intent.getData();
        String scheme = intent.getScheme();

        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH).equals(Constants.FINGERPRINT_SCHEME)) {
            // Scanning a fingerprint directly with Barcode Scanner, thus we already have scanned

            processScannedContent(dataUri);
        } else if (ACTION_SCAN_WITH_RESULT.equals(action)
                || ACTION_SCAN_IMPORT.equals(action) || ACTION_QR_CODE_API.equals(action)) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                    .setPrompt(getString(R.string.import_qr_code_text))
                    .setResultDisplayDuration(0);
            integrator.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            integrator.initiateScan();
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            // Check to see if the Activity started due to an Android Beam
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                handleActionNdefDiscovered(getIntent());
            } else {
                Log.e(Constants.TAG, "Android Beam not supported by Android < 4.1");
                finish();
            }
        } else {
            Log.e(Constants.TAG, "No valid scheme or action given!");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegratorSupportV4.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegratorSupportV4.parseActivityResult(requestCode,
                    resultCode, data);

            if (scanResult == null || scanResult.getFormatName() == null) {
                Log.e(Constants.TAG, "scanResult or formatName null! Should not happen!");
                finish();
                return;
            }

            String scannedContent = scanResult.getContents();
            processScannedContent(scannedContent);

            return;
        }
        // if a result has been returned, return it down to other activity
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            returnResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            finish();
        }
    }

    private void processScannedContent(String content) {
        Uri uri = Uri.parse(content);
        processScannedContent(uri);
    }

    private void processScannedContent(Uri uri) {
        String action = getIntent().getAction();

        Log.d(Constants.TAG, "scanned: " + uri);

        // example: openpgp4fpr:73EE2314F65FA92EC2390D3A718C070100012282
        if (uri == null || uri.getScheme() == null ||
                !uri.getScheme().toLowerCase(Locale.ENGLISH).equals(Constants.FINGERPRINT_SCHEME)) {
            SingletonResult result = new SingletonResult(
                    SingletonResult.RESULT_ERROR, LogType.MSG_WRONG_QR_CODE);
            Intent intent = new Intent();
            intent.putExtra(SingletonResult.EXTRA_RESULT, result);
            returnResult(intent);
            return;
        }
        final String fingerprint = uri.getEncodedSchemeSpecificPart().toLowerCase(Locale.ENGLISH);
        if (!fingerprint.matches("[a-fA-F0-9]{40}")) {
            SingletonResult result = new SingletonResult(
                    SingletonResult.RESULT_ERROR, LogType.MSG_WRONG_QR_CODE_FP);
            Intent intent = new Intent();
            intent.putExtra(SingletonResult.EXTRA_RESULT, result);
            returnResult(intent);
            return;
        }

        if (ACTION_SCAN_WITH_RESULT.equals(action)) {
            Intent result = new Intent();
            result.putExtra(EXTRA_FINGERPRINT, fingerprint);
            setResult(RESULT_OK, result);
            finish();
        } else {
            final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(this).getProxyPrefs();
            Runnable ignoreTor = new Runnable() {
                @Override
                public void run() {
                    importKeys(fingerprint, new ParcelableProxy(null, -1, null));
                }
            };

            if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                    this)) {
                importKeys(fingerprint, proxyPrefs.parcelableProxy);
            }
        }

    }

    public void returnResult(Intent data) {
        String action = getIntent().getAction();

        if (ACTION_QR_CODE_API.equals(action)) {
            // display last log message but as Toast for calls from outside OpenKeychain
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            String str = getString(result.getLog().getLast().mType.getMsgId());
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            finish();
        } else {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    public void importKeys(byte[] keyringData, ParcelableProxy parcelableProxy) {
        ParcelableKeyRing keyEntry = new ParcelableKeyRing(keyringData);
        ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
        selectedEntries.add(keyEntry);

        startImportService(selectedEntries, parcelableProxy);
    }

    public void importKeys(String fingerprint, ParcelableProxy parcelableProxy) {
        ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null, null);
        ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
        selectedEntries.add(keyEntry);

        startImportService(selectedEntries, parcelableProxy);
    }

    private void startImportService(ArrayList<ParcelableKeyRing> keyRings, ParcelableProxy parcelableProxy) {

        // Message is received after importing is done in KeychainService
        ServiceProgressHandler serviceHandler = new ServiceProgressHandler(this) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        finish();
                        return;
                    }
                    final ImportKeyResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);
                    if (result == null) {
                        Log.e(Constants.TAG, "result == null");
                        finish();
                        return;
                    }

                    if (!result.success()) {
                        // only return if no success...
                        Intent data = new Intent();
                        data.putExtras(returnData);
                        returnResult(data);
                        return;
                    }

                    Intent certifyIntent = new Intent(ImportKeysProxyActivity.this,
                            CertifyKeyActivity.class);
                    certifyIntent.putExtra(CertifyKeyActivity.EXTRA_RESULT, result);
                    certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS,
                            result.getImportedMasterKeyIds());
                    startActivityForResult(certifyIntent, 0);
                }
            }
        };

        // fill values for this action
        Bundle data = new Bundle();

        // search config
        {
            Preferences prefs = Preferences.getPreferences(this);
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            data.putString(KeychainService.IMPORT_KEY_SERVER, cloudPrefs.keyserver);
        }

        data.putParcelableArrayList(KeychainService.IMPORT_KEY_LIST, keyRings);

        data.putParcelable(KeychainService.EXTRA_PARCELABLE_PROXY, parcelableProxy);

        // Send all information needed to service to query keys in other thread
        Intent intent = new Intent(this, KeychainService.class);
        intent.setAction(KeychainService.ACTION_IMPORT_KEYRING);
        intent.putExtra(KeychainService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL, true);

        // start service with intent
        startService(intent);
    }

    /**
     * NFC: Parses the NDEF Message from the intent and prints to the TextView
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void handleActionNdefDiscovered(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        final byte[] receivedKeyringBytes = msg.getRecords()[0].getPayload();
        final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(this)
                .getProxyPrefs();
        Runnable ignoreTor = new Runnable() {
            @Override
            public void run() {
                importKeys(receivedKeyringBytes, new ParcelableProxy(null, -1, null));
            }
        };

        if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs, this)) {
            importKeys(receivedKeyringBytes, proxyPrefs.parcelableProxy);
        }
    }

}
