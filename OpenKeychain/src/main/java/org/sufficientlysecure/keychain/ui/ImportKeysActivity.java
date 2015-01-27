/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.api.OpenKeychainIntents;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;

import java.io.IOException;
import java.util.ArrayList;

public class ImportKeysActivity extends BaseActivity {
    public static final String ACTION_IMPORT_KEY = OpenKeychainIntents.IMPORT_KEY;
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER = OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER;
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT =
            Constants.INTENT_PREFIX + "IMPORT_KEY_FROM_KEY_SERVER_AND_RETURN_RESULT";
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_KEY_SERVER_AND_RETURN";
    public static final String ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_FILE_AND_RETURN";

    // Actions for internal use only:
    public static final String ACTION_IMPORT_KEY_FROM_FILE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_FILE";
    public static final String ACTION_IMPORT_KEY_FROM_NFC = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_NFC";

    public static final String EXTRA_RESULT = "result";

    // only used by ACTION_IMPORT_KEY
    public static final String EXTRA_KEY_BYTES = OpenKeychainIntents.IMPORT_EXTRA_KEY_EXTRA_KEY_BYTES;

    // only used by ACTION_IMPORT_KEY_FROM_KEYSERVER
    public static final String EXTRA_QUERY = OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER_EXTRA_QUERY;
    public static final String EXTRA_KEY_ID = Constants.EXTRA_PREFIX + "EXTRA_KEY_ID";
    public static final String EXTRA_FINGERPRINT = OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER_EXTRA_FINGERPRINT;

    // only used by ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE when used from OpenPgpService
    public static final String EXTRA_PENDING_INTENT_DATA = "data";
    private Intent mPendingIntentData;

    // view
    private ImportKeysListFragment mListFragment;
    private Fragment mTopFragment;
    private View mImportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImportButton = findViewById(R.id.import_import);
        mImportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                importKeys();
            }
        });

        handleActions(savedInstanceState, getIntent());
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.import_keys_activity);
    }

    protected void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        Uri dataUri = intent.getData();
        String scheme = intent.getScheme();

        if (extras == null) {
            extras = new Bundle();
        }

        if (action == null) {
            startCloudFragment(savedInstanceState, null, false);
            startListFragment(savedInstanceState, null, null, null);
            return;
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)
            // delegate action to ACTION_IMPORT_KEY
            action = ACTION_IMPORT_KEY;
        }

        switch (action) {
            case ACTION_IMPORT_KEY: {
                /* Keychain's own Actions */
                startFileFragment(savedInstanceState);

                if (dataUri != null) {
                    // action: directly load data
                    startListFragment(savedInstanceState, null, dataUri, null);
                } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                    byte[] importData = extras.getByteArray(EXTRA_KEY_BYTES);

                    // action: directly load data
                    startListFragment(savedInstanceState, importData, null, null);
                }
                break;
            }
            case ACTION_IMPORT_KEY_FROM_KEYSERVER:
            case ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE:
            case ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT: {

                // only used for OpenPgpService
                if (extras.containsKey(EXTRA_PENDING_INTENT_DATA)) {
                    mPendingIntentData = extras.getParcelable(EXTRA_PENDING_INTENT_DATA);
                }
                if (extras.containsKey(EXTRA_QUERY) || extras.containsKey(EXTRA_KEY_ID)) {
                    /* simple search based on query or key id */

                    String query = null;
                    if (extras.containsKey(EXTRA_QUERY)) {
                        query = extras.getString(EXTRA_QUERY);
                    } else if (extras.containsKey(EXTRA_KEY_ID)) {
                        long keyId = extras.getLong(EXTRA_KEY_ID, 0);
                        if (keyId != 0) {
                            query = KeyFormattingUtils.convertKeyIdToHex(keyId);
                        }
                    }

                    if (query != null && query.length() > 0) {
                        // display keyserver fragment with query
                        startCloudFragment(savedInstanceState, query, false);

                        // action: search immediately
                        startListFragment(savedInstanceState, null, null, query);
                    } else {
                        Log.e(Constants.TAG, "Query is empty!");
                        return;
                    }
                } else if (extras.containsKey(EXTRA_FINGERPRINT)) {
                    /*
                     * search based on fingerprint, here we can enforce a check in the end
                     * if the right key has been downloaded
                     */

                    String fingerprint = extras.getString(EXTRA_FINGERPRINT);
                    if (isFingerprintValid(fingerprint)) {
                        String query = "0x" + fingerprint;

                        // display keyserver fragment with query
                        startCloudFragment(savedInstanceState, query, true);

                        // action: search immediately
                        startListFragment(savedInstanceState, null, null, query);
                    }
                } else {
                    Log.e(Constants.TAG,
                            "IMPORT_KEY_FROM_KEYSERVER action needs to contain the 'query', 'key_id', or " +
                                    "'fingerprint' extra!"
                    );
                    return;
                }
                break;
            }
            case ACTION_IMPORT_KEY_FROM_FILE: {
                // NOTE: this only displays the appropriate fragment, no actions are taken
                startFileFragment(savedInstanceState);

                // no immediate actions!
                startListFragment(savedInstanceState, null, null, null);
                break;
            }
            case ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN: {
                // NOTE: this only displays the appropriate fragment, no actions are taken
                startFileFragment(savedInstanceState);

                // no immediate actions!
                startListFragment(savedInstanceState, null, null, null);
                break;
            }
            case ACTION_IMPORT_KEY_FROM_NFC: {
                // NOTE: this only displays the appropriate fragment, no actions are taken
                startFileFragment(savedInstanceState);
                // TODO!!!!!

                // no immediate actions!
                startListFragment(savedInstanceState, null, null, null);
                break;
            }
            default: {
                startCloudFragment(savedInstanceState, null, false);
                startListFragment(savedInstanceState, null, null, null);
                break;
            }
        }
    }

    private void startListFragment(Bundle savedInstanceState, byte[] bytes, Uri dataUri, String serverQuery) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mListFragment = ImportKeysListFragment.newInstance(bytes, dataUri, serverQuery);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_keys_list_container, mListFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    private void startFileFragment(Bundle savedInstanceState) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mTopFragment = ImportKeysFileFragment.newInstance();

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_keys_top_container, mTopFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    private void startCloudFragment(Bundle savedInstanceState, String query, boolean disableQueryEdit) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mTopFragment = ImportKeysCloudFragment.newInstance(query, disableQueryEdit);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_keys_top_container, mTopFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    private boolean isFingerprintValid(String fingerprint) {
        if (fingerprint == null || fingerprint.length() < 40) {
            Notify.showNotify(this, R.string.import_qr_code_too_short_fingerprint, Notify.Style.ERROR);
            return false;
        } else {
            return true;
        }
    }

    public void loadCallback(ImportKeysListFragment.LoaderState loaderState) {
        mListFragment.loadNew(loaderState);
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
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
                        return;
                    }
                    final ImportKeyResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);
                    if (result == null) {
                        Log.e(Constants.TAG, "result == null");
                        return;
                    }

                    if (ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT.equals(getIntent().getAction())
                            || ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN.equals(getIntent().getAction())) {
                        Intent intent = new Intent();
                        intent.putExtra(ImportKeyResult.EXTRA_RESULT, result);
                        ImportKeysActivity.this.setResult(RESULT_OK, intent);
                        ImportKeysActivity.this.finish();
                        return;
                    }
                    if (ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE.equals(getIntent().getAction())) {
                        ImportKeysActivity.this.setResult(RESULT_OK, mPendingIntentData);
                        ImportKeysActivity.this.finish();
                        return;
                    }

                    result.createNotify(ImportKeysActivity.this).show();
                }
            }
        };

        ImportKeysListFragment.LoaderState ls = mListFragment.getLoaderState();
        if (ls instanceof ImportKeysListFragment.BytesLoaderState) {
            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // get DATA from selected key entries
            IteratorWithSize<ParcelableKeyRing> selectedEntries = mListFragment.getSelectedData();

            // instead of giving the entries by Intent extra, cache them into a
            // file to prevent Java Binder problems on heavy imports
            // read FileImportCache for more info.
            try {
                // We parcel this iteratively into a file - anything we can
                // display here, we should be able to import.
                ParcelableFileCache<ParcelableKeyRing> cache =
                        new ParcelableFileCache<>(this, "key_import.pcl");
                cache.writeCache(selectedEntries);

                intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                saveHandler.showProgressDialog(this);

                // start service with intent
                startService(intent);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.showNotify(this, "Problem writing cache file!", Notify.Style.ERROR);
            }
        } else if (ls instanceof ImportKeysListFragment.CloudLoaderState) {
            ImportKeysListFragment.CloudLoaderState sls = (ImportKeysListFragment.CloudLoaderState) ls;

            // Send all information needed to service to query keys in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            data.putString(KeychainIntentService.IMPORT_KEY_SERVER, sls.mCloudPrefs.keyserver);

            // get selected key entries
            ArrayList<ParcelableKeyRing> keys = new ArrayList<>();
            {
                // change the format into ParcelableKeyRing
                ArrayList<ImportKeysListEntry> entries = mListFragment.getSelectedEntries();
                for (ImportKeysListEntry entry : entries) {
                    keys.add(new ParcelableKeyRing(
                                    entry.getFingerprintHex(), entry.getKeyIdHex(), entry.getExtraData())
                    );
                }
            }
            data.putParcelableArrayList(KeychainIntentService.IMPORT_KEY_LIST, keys);

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else {
            Notify.showNotify(this, R.string.error_nothing_import, Notify.Style.ERROR);
        }
    }

    /**
     * NFC
     */
    @Override
    public void onResume() {
        super.onResume();

        // Check to see if the Activity started due to an Android Beam
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                handleActionNdefDiscovered(getIntent());
            } else {
                Log.d(Constants.TAG, "NFC: No NDEF discovered!");
            }
        } else {
            Log.e(Constants.TAG, "Android Beam not supported by Android < 4.1");
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
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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
