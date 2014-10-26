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

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.api.OpenKeychainIntents;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Proxy activity (just a transparent content view) to scan QR Codes using the Barcode Scanner app
 */
public class QrCodeScanActivity extends FragmentActivity {

    public static final String ACTION_QR_CODE_API = OpenKeychainIntents.IMPORT_KEY_FROM_QR_CODE;
    public static final String ACTION_SCAN_WITH_RESULT = Constants.INTENT_PREFIX + "SCAN_QR_CODE_WITH_RESULT";

    boolean returnResult;

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

            returnResult = false;
            startCertify(dataUri);
        } else if (ACTION_SCAN_WITH_RESULT.equals(action)) {
            // scan using xzing's Barcode Scanner and return result parcel in OpenKeychain

            returnResult = true;
            new IntentIntegrator(this).initiateScan();
        } else if (ACTION_QR_CODE_API.equals(action)) {
            // scan using xzing's Barcode Scanner from outside OpenKeychain

            returnResult = false;
            new IntentIntegrator(this).initiateScan();
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
            if (scanResult != null && scanResult.getFormatName() != null) {
                String scannedContent = scanResult.getContents();
                Log.d(Constants.TAG, "scannedContent: " + scannedContent);

                startCertify(Uri.parse(scanResult.getContents()));
            } else {
                Log.e(Constants.TAG, "scanResult or formatName null! Should not happen!");
                finish();
            }

            return;
        }
        // if a result has been returned, return it down to other activity
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            returnResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void returnResult(Intent data) {
        if (returnResult) {
            setResult(RESULT_OK, data);
            finish();
        } else {
            // display last log message but as Toast for calls from outside OpenKeychain
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            String str = getString(result.getLog().getLast().mType.getMsgId());
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void startCertify(Uri dataUri) {
        // example: openpgp4fpr:73EE2314F65FA92EC2390D3A718C070100012282
        if (dataUri.getScheme().equals(Constants.FINGERPRINT_SCHEME)) {
            String fingerprint = dataUri.getEncodedSchemeSpecificPart().toLowerCase(Locale.ENGLISH);
            importKeys(fingerprint);
        } else {
            SingletonResult result = new SingletonResult(
                    SingletonResult.RESULT_ERROR, OperationResult.LogType.MSG_WRONG_QR_CODE);
            Intent intent = new Intent();
            intent.putExtra(SingletonResult.EXTRA_RESULT, result);
            returnResult(intent);
        }
    }

    public void importKeys(String fingerprint) {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler serviceHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL,
                true) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
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

                    if ( ! result.success()) {
                        // only return if no success...
                        Intent data = new Intent();
                        data.putExtras(returnData);
                        returnResult(data);
                        return;
                    }

                    Intent certifyIntent = new Intent(QrCodeScanActivity.this, CertifyKeyActivity.class);
                    certifyIntent.putExtra(CertifyKeyActivity.EXTRA_RESULT, result);
                    certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, result.getImportedMasterKeyIds());
                    startActivityForResult(certifyIntent, 0);
                }
            }
        };

        // search config
        Preferences prefs = Preferences.getPreferences(this);
        Preferences.CloudSearchPrefs cloudPrefs = new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());

        // Send all information needed to service to query keys in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        data.putString(KeychainIntentService.IMPORT_KEY_SERVER, cloudPrefs.keyserver);

        ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null, null);
        ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<ParcelableKeyRing>();
        selectedEntries.add(keyEntry);

        data.putParcelableArrayList(KeychainIntentService.IMPORT_KEY_LIST, selectedEntries);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

}
