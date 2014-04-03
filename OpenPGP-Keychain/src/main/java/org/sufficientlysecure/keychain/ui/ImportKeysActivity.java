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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.ui.dialog.BadImportKeyDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class ImportKeysActivity extends DrawerActivity implements ActionBar.OnNavigationListener {
    public static final String ACTION_IMPORT_KEY = Constants.INTENT_PREFIX + "IMPORT_KEY";
    public static final String ACTION_IMPORT_KEY_FROM_QR_CODE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_QR_CODE";
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_KEYSERVER";
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_KEY_SERVER_AND_RETURN";

    // Actions for internal use only:
    public static final String ACTION_IMPORT_KEY_FROM_FILE = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_FILE";
    public static final String ACTION_IMPORT_KEY_FROM_NFC = Constants.INTENT_PREFIX
            + "IMPORT_KEY_FROM_NFC";

    // only used by ACTION_IMPORT_KEY
    public static final String EXTRA_KEY_BYTES = "key_bytes";

    // only used by ACTION_IMPORT_KEY_FROM_KEYSERVER
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String EXTRA_FINGERPRINT = "fingerprint";

    // only used by ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN when used from OpenPgpService
    public static final String EXTRA_PENDING_INTENT_DATA = "data";
    private Intent mPendingIntentData;

    // view
    private ImportKeysListFragment mListFragment;
    private String[] mNavigationStrings;
    private Fragment mCurrentFragment;
    private BootstrapButton mImportButton;

    private static final Class[] NAVIGATION_CLASSES = new Class[]{
            ImportKeysServerFragment.class,
            ImportKeysFileFragment.class,
            ImportKeysQrCodeFragment.class,
            ImportKeysClipboardFragment.class,
            ImportKeysNFCFragment.class
    };

    private int mCurrentNavPosition = -1;

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

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mNavigationStrings = getResources().getStringArray(R.array.import_action_list);

        if (ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN.equals(getIntent().getAction())) {
            setTitle(R.string.nav_import);
        } else {
            setupDrawerNavigation(savedInstanceState);

            // set drop down navigation
            Context context = getSupportActionBar().getThemedContext();
            ArrayAdapter<CharSequence> navigationAdapter = ArrayAdapter.createFromResource(context,
                    R.array.import_action_list, android.R.layout.simple_spinner_dropdown_item);
            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getSupportActionBar().setListNavigationCallbacks(navigationAdapter, this);
        }

        handleActions(savedInstanceState, getIntent());
    }


    protected void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        Uri dataUri = intent.getData();
        String scheme = intent.getScheme();

        if (extras == null) {
            extras = new Bundle();
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)
            // override action to delegate it to Keychain's ACTION_IMPORT_KEY
            action = ACTION_IMPORT_KEY;
        }

        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH).equals(Constants.FINGERPRINT_SCHEME)) {
            /* Scanning a fingerprint directly with Barcode Scanner */
            loadFromFingerprintUri(savedInstanceState, dataUri);
        } else if (ACTION_IMPORT_KEY.equals(action)) {
            /* Keychain's own Actions */

            // display file fragment
            loadNavFragment(1, null);

            if (dataUri != null) {
                // action: directly load data
                startListFragment(savedInstanceState, null, dataUri, null);
            } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                byte[] importData = intent.getByteArrayExtra(EXTRA_KEY_BYTES);

                // action: directly load data
                startListFragment(savedInstanceState, importData, null, null);
            }
        } else if (ACTION_IMPORT_KEY_FROM_KEYSERVER.equals(action)
                || ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN.equals(action)) {

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
                    long keyId = intent.getLongExtra(EXTRA_KEY_ID, 0);
                    if (keyId != 0) {
                        query = PgpKeyHelper.convertKeyIdToHex(keyId);
                    }
                }

                if (query != null && query.length() > 0) {
                    // display keyserver fragment with query
                    Bundle args = new Bundle();
                    args.putString(ImportKeysServerFragment.ARG_QUERY, query);
                    loadNavFragment(0, args);

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

                String fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT);
                loadFromFingerprint(savedInstanceState, fingerprint);
            } else {
                Log.e(Constants.TAG,
                        "IMPORT_KEY_FROM_KEYSERVER action needs to contain the 'query', 'key_id', or " +
                                "'fingerprint' extra!");
                return;
            }
        } else if (ACTION_IMPORT_KEY_FROM_FILE.equals(action)) {

            // NOTE: this only displays the appropriate fragment, no actions are taken
            loadNavFragment(1, null);

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else if (ACTION_IMPORT_KEY_FROM_QR_CODE.equals(action)) {
            // also exposed in AndroidManifest

            // NOTE: this only displays the appropriate fragment, no actions are taken
            loadNavFragment(2, null);

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else if (ACTION_IMPORT_KEY_FROM_NFC.equals(action)) {

            // NOTE: this only displays the appropriate fragment, no actions are taken
            loadNavFragment(3, null);

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else {
            startListFragment(savedInstanceState, null, null, null);
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

    /**
     * "Basically, when using a list navigation, onNavigationItemSelected() is automatically
     * called when your activity is created/re-created, whether you like it or not. To prevent
     * your Fragment's onCreateView() from being called twice, this initial automatic call to
     * onNavigationItemSelected() should check whether the Fragment is already in existence
     * inside your Activity."
     * <p/>
     * from http://stackoverflow.com/a/14295474
     * <p/>
     * In our case, if we start ImportKeysActivity with parameters to directly search using a fingerprint,
     * the fragment would be loaded twice resulting in the query being empty after the second load.
     * <p/>
     * Our solution:
     * To prevent that a fragment will be loaded again even if it was already loaded loadNavFragment
     * checks against mCurrentNavPosition.
     *
     * @param itemPosition
     * @param itemId
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.d(Constants.TAG, "onNavigationItemSelected");

        loadNavFragment(itemPosition, null);

        return true;
    }

    private void loadNavFragment(int itemPosition, Bundle args) {
        if (mCurrentNavPosition != itemPosition) {
            if (ActionBar.NAVIGATION_MODE_LIST == getSupportActionBar().getNavigationMode()) {
                getSupportActionBar().setSelectedNavigationItem(itemPosition);
            }
            loadFragment(NAVIGATION_CLASSES[itemPosition], args, mNavigationStrings[itemPosition]);
            mCurrentNavPosition = itemPosition;
        }
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

    public void loadFromFingerprintUri(Bundle savedInstanceState, Uri dataUri) {
        String fingerprint = dataUri.toString().split(":")[1].toLowerCase(Locale.ENGLISH);

        Log.d(Constants.TAG, "fingerprint: " + fingerprint);

        loadFromFingerprint(savedInstanceState, fingerprint);
    }

    public void loadFromFingerprint(Bundle savedInstanceState, String fingerprint) {
        if (fingerprint == null || fingerprint.length() < 40) {
            AppMsg.makeText(this, R.string.import_qr_code_too_short_fingerprint,
                    AppMsg.STYLE_ALERT).show();
            return;
        }

        String query = "0x" + fingerprint;

        // display keyserver fragment with query
        Bundle args = new Bundle();
        args.putString(ImportKeysServerFragment.ARG_QUERY, query);
        args.putBoolean(ImportKeysServerFragment.ARG_DISABLE_QUERY_EDIT, true);
        loadNavFragment(0, args);

        // action: search directly
        startListFragment(savedInstanceState, null, null, query);
    }

    public void loadCallback(byte[] importData, Uri dataUri, String serverQuery, String keyServer) {
        mListFragment.loadNew(importData, dataUri, serverQuery, keyServer);
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
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
                        String addedStr = getResources().getQuantityString(
                                R.plurals.keys_added_and_updated_1, added, added);
                        String updatedStr = getResources().getQuantityString(
                                R.plurals.keys_added_and_updated_2, updated, updated);
                        toastMessage = addedStr + updatedStr;
                    } else if (added > 0) {
                        toastMessage = getResources().getQuantityString(R.plurals.keys_added,
                                added, added);
                    } else if (updated > 0) {
                        toastMessage = getResources().getQuantityString(R.plurals.keys_updated,
                                updated, updated);
                    } else {
                        toastMessage = getString(R.string.no_keys_added_or_updated);
                    }
                    AppMsg.makeText(ImportKeysActivity.this, toastMessage, AppMsg.STYLE_INFO)
                            .show();
                    if (bad > 0) {
                        BadImportKeyDialogFragment badImportKeyDialogFragment =
                                BadImportKeyDialogFragment.newInstance(bad);
                        badImportKeyDialogFragment.show(getSupportFragmentManager(), "badKeyDialog");
                    }

                    if (ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN.equals(getIntent().getAction())) {
                        ImportKeysActivity.this.setResult(Activity.RESULT_OK, mPendingIntentData);
                        finish();
                    }
                }
            }
        };

        if (mListFragment.getKeyBytes() != null || mListFragment.getDataUri() != null) {
            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // get selected key entries
            ArrayList<ImportKeysListEntry> selectedEntries = mListFragment.getSelectedData();
            data.putParcelableArrayList(KeychainIntentService.IMPORT_KEY_LIST, selectedEntries);

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else if (mListFragment.getServerQuery() != null) {
            // Send all information needed to service to query keys in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_DOWNLOAD_AND_IMPORT_KEYS);

            // fill values for this action
            Bundle data = new Bundle();

            data.putString(KeychainIntentService.DOWNLOAD_KEY_SERVER, mListFragment.getKeyServer());

            // get selected key entries
            ArrayList<ImportKeysListEntry> selectedEntries = mListFragment.getSelectedData();
            data.putParcelableArrayList(KeychainIntentService.DOWNLOAD_KEY_LIST, selectedEntries);

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else {
            AppMsg.makeText(this, R.string.error_nothing_import, AppMsg.STYLE_ALERT).show();
        }
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
