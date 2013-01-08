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
import org.thialfihar.android.apg.compatibility.DialogFragmentWorkaround;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import org.thialfihar.android.apg.ui.dialog.DeleteFileDialogFragment;
import org.thialfihar.android.apg.ui.dialog.FileDialogFragment;
import org.thialfihar.android.apg.util.Log;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ImportKeysActivity extends SherlockFragmentActivity {
    public static final String ACTION_IMPORT = Constants.INTENT_PREFIX + "IMPORT";
    public static final String ACTION_IMPORT_FROM_FILE = Constants.INTENT_PREFIX
            + "IMPORT_FROM_FILE";
    public static final String ACTION_IMPORT_FROM_QR_CODE = Constants.INTENT_PREFIX
            + "IMPORT_FROM_QR_CODE";
    public static final String ACTION_IMPORT_FROM_NFC = Constants.INTENT_PREFIX + "IMPORT_FROM_NFC";

    // only used by IMPORT
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_KEYRING_BYTES = "keyringBytes";

    // public static final String EXTRA_KEY_ID = "keyId";

    protected String mImportFilename;
    protected byte[] mImportData;

    protected boolean mDeleteAfterImport = false;

    FileDialogFragment mFileDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_keys);

        // set actionbar without home button if called from another app
        OtherHelper.setActionBarBackButton(this);

        handleActions(getIntent());
    }

    /**
     * ActionBar menu is created based on class variables to change it at runtime
     * 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, Id.menu.option.import_from_file, 0, R.string.menu_importFromFile)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.import_from_qr_code, 1, R.string.menu_importFromQrCode)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.import_from_nfc, 2, R.string.menu_importFromNfc)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
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

        case Id.menu.option.import_from_file:
            showImportFromFileDialog();
            return true;

        case Id.menu.option.import_from_qr_code:
            importFromQrCode();
            return true;

        case Id.menu.option.import_from_nfc:
            importFromNfc();
            return true;

        default:
            return super.onOptionsItemSelected(item);

        }
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * Android Standard Actions
         */
        if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to APG (see AndroidManifest.xml)
            // override action to delegate it to APGs ACTION_IMPORT
            action = ACTION_IMPORT;
        }

        /**
         * APG's own Actions
         */
        if (ACTION_IMPORT.equals(action)) {
            if ("file".equals(intent.getScheme()) && intent.getDataString() != null) {
                mImportFilename = intent.getData().getPath();
                mImportData = null;
            } else if (extras.containsKey(EXTRA_TEXT)) {
                mImportData = intent.getStringExtra(EXTRA_TEXT).getBytes();
                mImportFilename = null;
            } else if (extras.containsKey(EXTRA_KEYRING_BYTES)) {
                mImportData = intent.getByteArrayExtra(EXTRA_KEYRING_BYTES);
                mImportFilename = null;
            }
            loadKeyListFragment();
        } else if (ACTION_IMPORT_FROM_FILE.equals(action)) {
            if ("file".equals(intent.getScheme()) && intent.getDataString() != null) {
                mImportFilename = intent.getData().getPath();
                mImportData = null;
            }
            showImportFromFileDialog();
        } else if (ACTION_IMPORT_FROM_QR_CODE.equals(action)) {
            importFromQrCode();
        } else if (ACTION_IMPORT_FROM_NFC.equals(action)) {
            importFromNfc();
        }
    }

    public void loadKeyListFragment() {
        if (mImportData != null || mImportFilename != null) {
            // generate list of keyrings
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            ImportKeysListFragment listFragment = new ImportKeysListFragment();
            Bundle args = new Bundle();
            args.putByteArray(ImportKeysListFragment.ARG_KEYRING_BYTES, mImportData);
            args.putString(ImportKeysListFragment.ARG_IMPORT_FILENAME, mImportFilename);
            listFragment.setArguments(args);
            // replace container in view with fragment
            fragmentTransaction.replace(R.id.import_keys_list_container, listFragment);
            fragmentTransaction.commit();
        }
    }

    private void importFromQrCode() {
        new IntentIntegrator(this).initiateScan();
    }

    private void importFromNfc() {
        // show nfc help
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_SELECTED_TAB, 1);
        startActivityForResult(intent, 0);
    }

    /**
     * Show to dialog from where to import keys
     */
    public void showImportFromFileDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mImportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
                    mDeleteAfterImport = data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);
                    
                    Log.d(Constants.TAG, "mImportFilename: " + mImportFilename);
                    Log.d(Constants.TAG, "mDeleteAfterImport: " + mDeleteAfterImport);

                    loadKeyListFragment();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_importKeys),
                        getString(R.string.specifyFileToImportFrom), Constants.path.APP_DIR + "/",
                        null, Id.request.filename);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        });
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
    // String actualFingerprint = PGPHelper.convertFingerprintToHex(publicKeyRing
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
        Log.d(Constants.TAG, "Import key button clicked!");

        importKeys();
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        if (mImportData != null || mImportFilename != null) {
            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, ApgIntentService.class);

            intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // TODO: check for key type?
            // data.putInt(ApgIntentService.IMPORT_KEY_TYPE, Id.type.secret_key);

            if (mImportData != null) {
                data.putInt(ApgIntentService.TARGET, ApgIntentService.TARGET_BYTES);
                data.putByteArray(ApgIntentService.IMPORT_BYTES, mImportData);
            } else {
                data.putInt(ApgIntentService.TARGET, ApgIntentService.TARGET_FILE);
                data.putString(ApgIntentService.IMPORT_FILENAME, mImportFilename);
            }

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
                        Toast.makeText(ImportKeysActivity.this, toastMessage, Toast.LENGTH_SHORT)
                                .show();
                        if (bad > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    ImportKeysActivity.this);

                            alert.setIcon(android.R.drawable.ic_dialog_alert);
                            alert.setTitle(R.string.warning);
                            alert.setMessage(ImportKeysActivity.this.getString(
                                    R.string.badKeysEncountered, bad));

                            alert.setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            alert.setCancelable(true);
                            alert.create().show();
                        } else if (mDeleteAfterImport) {
                            // everything went well, so now delete, if that was turned on
                            DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment
                                    .newInstance(mImportFilename);
                            deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
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
        } else {
            Toast.makeText(this, R.string.error_nothingImport, Toast.LENGTH_LONG).show();
        }
    }

    public void signAndUploadOnClick(View view) {
        // first, import!
        importOnClick(view);

        // TODO: implement sign and upload!
        Toast.makeText(ImportKeysActivity.this, "Not implemented right now!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.filename: {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String path = data.getData().getPath();
                    Log.d(Constants.TAG, "path=" + path);

                    // set filename used in export/import dialogs
                    mFileDialog.setFilename(path);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "Nullpointer while retrieving path!", e);
                }
            }
            return;
        }
        case IntentIntegrator.REQUEST_CODE: {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                    data);
            if (scanResult != null && scanResult.getFormatName() != null) {

                // mScannedContent = scanResult.getContents();

                mImportData = scanResult.getContents().getBytes();
                mImportFilename = null;

                // mContentView.setText(mScannedContent);
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

    @Override
    protected void onResume() {
        super.onResume();

        loadKeyListFragment();
    }

}
