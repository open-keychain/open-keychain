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

package org.sufficientlysecure.keychain.ui;

import java.util.ArrayList;
import java.util.List;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.beardedhen.androidbootstrap.BootstrapButton;

public class ImportKeysActivity extends DrawerActivity implements OnNavigationListener {
    public static final String ACTION_IMPORT_KEY = Constants.INTENT_PREFIX + "IMPORT_KEY";
    public static final String ACTION_IMPORT_KEY_FROM_QR_CODE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_QR_CODE";

    // Actions for internal use only:
    public static final String ACTION_IMPORT_KEY_FROM_FILE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_FILE";
    public static final String ACTION_IMPORT_KEY_FROM_NFC = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_NFC";

    // only used by IMPORT
    public static final String EXTRA_KEY_BYTES = "key_bytes";

    // TODO: import keys from server
    // public static final String EXTRA_KEY_ID = "keyId";

    protected boolean mDeleteAfterImport = false;

    FileDialogFragment mFileDialog;
    ImportKeysListFragment mListFragment;
    OnNavigationListener mOnNavigationListener;
    String[] mNavigationStrings;

    Fragment mCurrentFragment;

    BootstrapButton mImportButton;
    BootstrapButton mImportSignUploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_keys_activity);

        mImportButton = (BootstrapButton) findViewById(R.id.import_import);
        mImportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                importKeys();
            }
        });
        mImportSignUploadButton = (BootstrapButton) findViewById(R.id.import_sign_and_upload);
        mImportSignUploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signAndUploadOnClick();
            }
        });

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setupDrawerNavigation(savedInstanceState);

        // set actionbar without home button if called from another app
        // ActionBarHelper.setBackButton(this);

        // set drop down navigation
        mNavigationStrings = getResources().getStringArray(R.array.import_action_list);
        Context context = getSupportActionBar().getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context,
                R.array.import_action_list, R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(list, this);

        handleActions(savedInstanceState, getIntent());
    }

    protected void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * Android Standard Actions
         */
        if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)
            // override action to delegate it to Keychain's ACTION_IMPORT_KEY
            action = ACTION_IMPORT_KEY;
        }

        /**
         * Keychain's own Actions
         */
        if (ACTION_IMPORT_KEY.equals(action)) {
            if ("file".equals(intent.getScheme()) && intent.getDataString() != null) {
                String importFilename = intent.getData().getPath();

                // display selected filename
                getSupportActionBar().setSelectedNavigationItem(0);
                Bundle args = new Bundle();
                args.putString(ImportKeysFileFragment.ARG_PATH, importFilename);
                loadFragment(ImportKeysFileFragment.class, args, mNavigationStrings[0]);

                // directly load data
                startListFragment(savedInstanceState, null, importFilename);
            } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                byte[] importData = intent.getByteArrayExtra(EXTRA_KEY_BYTES);

                // directly load data
                startListFragment(savedInstanceState, importData, null);
            }
        } else {
            // Internal actions
            startListFragment(savedInstanceState, null, null);

            if (ACTION_IMPORT_KEY_FROM_FILE.equals(action)) {
                getSupportActionBar().setSelectedNavigationItem(0);
                loadFragment(ImportKeysFileFragment.class, null, mNavigationStrings[0]);
            } else if (ACTION_IMPORT_KEY_FROM_QR_CODE.equals(action)) {
                getSupportActionBar().setSelectedNavigationItem(2);
                loadFragment(ImportKeysQrCodeFragment.class, null, mNavigationStrings[2]);
            } else if (ACTION_IMPORT_KEY_FROM_NFC.equals(action)) {
                getSupportActionBar().setSelectedNavigationItem(3);
                loadFragment(ImportKeysNFCFragment.class, null, mNavigationStrings[3]);
            }
        }
    }

    private void startListFragment(Bundle savedInstanceState, byte[] bytes, String filename) {
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.import_keys_list_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mListFragment = ImportKeysListFragment.newInstance(bytes, filename);

            // Add the fragment to the 'fragment_container' FrameLayout
            // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.import_keys_list_container, mListFragment)
                    .commitAllowingStateLoss();
            // do it immediately!
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // Create new fragment from our own Fragment class
        switch (itemPosition) {
        case 0:
            loadFragment(ImportKeysFileFragment.class, null, mNavigationStrings[itemPosition]);
            break;
        case 1:
            loadFragment(ImportKeysServerFragment.class, null, mNavigationStrings[itemPosition]);
            break;
        case 2:
            loadFragment(ImportKeysQrCodeFragment.class, null, mNavigationStrings[itemPosition]);
            break;
        case 3:
            loadFragment(ImportKeysClipboardFragment.class, null, mNavigationStrings[itemPosition]);
            break;
        case 4:
            loadFragment(ImportKeysNFCFragment.class, null, mNavigationStrings[itemPosition]);
            break;

        default:
            break;
        }
        return true;
    }

    private void loadFragment(Class<?> clss, Bundle args, String tag) {
        mCurrentFragment = Fragment.instantiate(this, clss.getName(), args);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace whatever is in the fragment container with this fragment
        // and give the fragment a tag name equal to the string at the position selected
        ft.replace(R.id.import_navigation_fragment, mCurrentFragment, tag);
        // Apply changes
        ft.commit();
    }

    public void loadCallback(byte[] importData, String importFilename) {
        mListFragment.loadNew(importData, importFilename);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // required for qr code scanning
        if (mCurrentFragment != null) {
            mCurrentFragment.onActivityResult(requestCode, resultCode, data);
        }
        // super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        if (mListFragment.getKeyBytes() != null || mListFragment.getImportFilename() != null) {
            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // get selected key ids
            List<ImportKeysListEntry> listEntries = mListFragment.getData();
            ArrayList<Long> selectedKeyIds = new ArrayList<Long>();
            for (ImportKeysListEntry entry : listEntries) {
                if (entry.isSelected()) {
                    selectedKeyIds.add(entry.keyId);
                }
            }

            data.putSerializable(KeychainIntentService.IMPORT_KEY_LIST, selectedKeyIds);

            if (mListFragment.getKeyBytes() != null) {
                data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_BYTES);
                data.putByteArray(KeychainIntentService.IMPORT_BYTES, mListFragment.getKeyBytes());
            } else {
                data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_FILE);
                data.putString(KeychainIntentService.IMPORT_FILENAME,
                        mListFragment.getImportFilename());
            }

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Message is received after importing is done in ApgService
            KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                    R.string.progress_importing, ProgressDialog.STYLE_HORIZONTAL) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                        // get returned data bundle
                        Bundle returnData = message.getData();

                        int added = returnData.getInt(KeychainIntentService.RESULT_IMPORT_ADDED);
                        int updated = returnData
                                .getInt(KeychainIntentService.RESULT_IMPORT_UPDATED);
                        int bad = returnData.getInt(KeychainIntentService.RESULT_IMPORT_BAD);
                        String toastMessage;
                        if (added > 0 && updated > 0) {
                            toastMessage = getString(R.string.keys_added_and_updated, added,
                                    updated);
                        } else if (added > 0) {
                            toastMessage = getString(R.string.keys_added, added);
                        } else if (updated > 0) {
                            toastMessage = getString(R.string.keys_updated, updated);
                        } else {
                            toastMessage = getString(R.string.no_keys_added_or_updated);
                        }
                        Toast.makeText(ImportKeysActivity.this, toastMessage, Toast.LENGTH_SHORT)
                                .show();
                        if (bad > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    ImportKeysActivity.this);

                            alert.setIcon(android.R.drawable.ic_dialog_alert);
                            alert.setTitle(R.string.warning);
                            alert.setMessage(ImportKeysActivity.this.getString(
                                    R.string.bad_keys_encountered, bad));

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
                                    .newInstance(mListFragment.getImportFilename());
                            deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
                        }
                    }
                };
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else {
            Toast.makeText(this, R.string.error_nothing_import, Toast.LENGTH_LONG).show();
        }
    }

    public void importOnClick() {
        importKeys();
    }

    public void signAndUploadOnClick() {
        // first, import!
        // importOnClick(view);

        // TODO: implement sign and upload!
        Toast.makeText(ImportKeysActivity.this, "Not implemented right now!", Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * NFC
     */
    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            handleActionNdefDiscovered(getIntent());
        }
    }

    /**
     * NFC
     */
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * NFC: Parses the NDEF Message from the intent and prints to the TextView
     */
    @SuppressLint("NewApi")
    void handleActionNdefDiscovered(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        byte[] receivedKeyringBytes = msg.getRecords()[0].getPayload();

        Intent importIntent = new Intent(this, ImportKeysActivity.class);
        importIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY);
        importIntent.putExtra(ImportKeysActivity.EXTRA_KEY_BYTES, receivedKeyringBytes);

        handleActions(null, importIntent);
    }

}
