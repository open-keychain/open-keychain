/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Senecaso
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.thialfihar.android.apg.util.Log;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ImportFromQRCodeActivity extends SherlockFragmentActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String IMPORT_FROM_QR_CODE = Constants.INTENT_PREFIX
            + "IMPORT_FROM_QR_CODE";

    // public static final String EXTRA_KEY_ID = "keyId";

    private TextView mContentView;

    private String mScannedContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_from_qr_code);
        mContentView = (TextView) findViewById(R.id.import_from_qr_code_content);

        // set actionbar without home button if called from another app
        OtherHelper.setActionBarBackButton(this);

        // start scanning
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    // private void importAndSignOld(final long keyId, final String expectedFingerprint) {
    // if (expectedFingerprint != null && expectedFingerprint.length() > 0) {
    //
    // Thread t = new Thread() {
    // @Override
    // public void run() {
    // try {
    // // TODO: display some sort of spinner here while the user waits
    //
    // // TODO: there should be only 1
    // HkpKeyServer server = new HkpKeyServer(mPreferences.getKeyServers()[0]);
    // String encodedKey = server.get(keyId);
    //
    // PGPKeyRing keyring = PGPHelper.decodeKeyRing(new ByteArrayInputStream(
    // encodedKey.getBytes()));
    // if (keyring != null && keyring instanceof PGPPublicKeyRing) {
    // PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
    //
    // // make sure the fingerprints match before we cache this thing
    // String actualFingerprint = PGPHelper.convertToHex(publicKeyRing
    // .getPublicKey().getFingerprint());
    // if (expectedFingerprint.equals(actualFingerprint)) {
    // // store the signed key in our local cache
    // int retval = PGPMain.storeKeyRingInCache(publicKeyRing);
    // if (retval != Id.return_value.ok
    // && retval != Id.return_value.updated) {
    // status.putString(EXTRA_ERROR,
    // "Failed to store signed key in local cache");
    // } else {
    // Intent intent = new Intent(ImportFromQRCodeActivity.this,
    // SignKeyActivity.class);
    // intent.putExtra(EXTRA_KEY_ID, keyId);
    // startActivityForResult(intent, Id.request.sign_key);
    // }
    // } else {
    // status.putString(
    // EXTRA_ERROR,
    // "Scanned fingerprint does NOT match the fingerprint of the received key.  You shouldnt trust this key.");
    // }
    // }
    // } catch (QueryException e) {
    // Log.e(TAG, "Failed to query KeyServer", e);
    // status.putString(EXTRA_ERROR, "Failed to query KeyServer");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // } catch (IOException e) {
    // Log.e(TAG, "Failed to query KeyServer", e);
    // status.putString(EXTRA_ERROR, "Failed to query KeyServer");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // }
    // }
    // };
    //
    // t.setName("KeyExchange Download Thread");
    // t.setDaemon(true);
    // t.start();
    // }
    // }

    public void scanAgainOnClick(View view) {
        new IntentIntegrator(this).initiateScan();
    }

    public void finishOnClick(View view) {
        finish();
    }

    public void importOnClick(View view) {
        Log.d(Constants.TAG, "import key started");

        if (mScannedContent != null) {
            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, ApgIntentService.class);

            intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_IMPORT_KEY);

            // fill values for this action
            Bundle data = new Bundle();

            data.putInt(ApgIntentService.IMPORT_KEY_TYPE, Id.type.public_key);

            data.putInt(ApgIntentService.TARGET, ApgIntentService.TARGET_BYTES);
            data.putByteArray(ApgIntentService.IMPORT_BYTES, mScannedContent.getBytes());

            intent.putExtra(ApgIntentService.EXTRA_DATA, data);

            // Message is received after importing is done in ApgService
            ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(this,
                    R.string.progress_importing, ProgressDialog.STYLE_HORIZONTAL) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                        // get returned data bundle
                        Bundle returnData = message.getData();

                        int added = returnData.getInt(ApgIntentService.RESULT_IMPORT_ADDED);
                        int updated = returnData.getInt(ApgIntentService.RESULT_IMPORT_UPDATED);
                        int bad = returnData.getInt(ApgIntentService.RESULT_IMPORT_BAD);
                        String toastMessage;
                        if (added > 0 && updated > 0) {
                            toastMessage = getString(R.string.keysAddedAndUpdated, added, updated);
                        } else if (added > 0) {
                            toastMessage = getString(R.string.keysAdded, added);
                        } else if (updated > 0) {
                            toastMessage = getString(R.string.keysUpdated, updated);
                        } else {
                            toastMessage = getString(R.string.noKeysAddedOrUpdated);
                        }
                        Toast.makeText(ImportFromQRCodeActivity.this, toastMessage,
                                Toast.LENGTH_SHORT).show();
                        if (bad > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    ImportFromQRCodeActivity.this);

                            alert.setIcon(android.R.drawable.ic_dialog_alert);
                            alert.setTitle(R.string.warning);
                            alert.setMessage(ImportFromQRCodeActivity.this.getString(
                                    R.string.badKeysEncountered, bad));

                            alert.setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            alert.setCancelable(true);
                            alert.create().show();
                        }
                    }
                };
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        }
    }

    public void signAndUploadOnClick(View view) {
        // first, import!
        importOnClick(view);

        // TODO: implement sign and upload!
        Toast.makeText(ImportFromQRCodeActivity.this, "Not implemented right now!",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case IntentIntegrator.REQUEST_CODE: {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                    data);
            if (scanResult != null && scanResult.getFormatName() != null) {

                mScannedContent = scanResult.getContents();

                mContentView.setText(mScannedContent);
                // String[] bits = scanResult.getContents().split(",");
                // if (bits.length != 2) {
                // return; // dont know how to handle this. Not a valid code
                // }
                //
                // long keyId = Long.parseLong(bits[0]);
                // String expectedFingerprint = bits[1];

                // importAndSign(keyId, expectedFingerprint);
            }

            break;
        }

        default: {
            super.onActivityResult(requestCode, resultCode, data);
        }
        }
    }
}
