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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;

import java.io.IOException;
import java.util.ArrayList;

public class ImportKeysActivity extends BaseNfcActivity
        implements CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

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
    public static final String ACTION_SEARCH_KEYSERVER_FROM_URL = Constants.INTENT_PREFIX
            + "SEARCH_KEYSERVER_FROM_URL";
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

    // for CryptoOperationHelper.Callback
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;

    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mOperationHelper;

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
            startCloudFragment(savedInstanceState, null, false, null);
            startListFragment(savedInstanceState, null, null, null, null);
            return;
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            if (scheme.equals("http") || scheme.equals("https")) {
                action = ACTION_SEARCH_KEYSERVER_FROM_URL;
            } else {
                // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)
                // delegate action to ACTION_IMPORT_KEY
                action = ACTION_IMPORT_KEY;
            }
        }

        switch (action) {
            case ACTION_IMPORT_KEY: {
                /* Keychain's own Actions */
                startFileFragment(savedInstanceState);

                if (dataUri != null) {
                    // action: directly load data
                    startListFragment(savedInstanceState, null, dataUri, null, null);
                } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                    byte[] importData = extras.getByteArray(EXTRA_KEY_BYTES);

                    // action: directly load data
                    startListFragment(savedInstanceState, importData, null, null, null);
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
                        startCloudFragment(savedInstanceState, query, false, null);

                        // action: search immediately
                        startListFragment(savedInstanceState, null, null, query, null);
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
                        startCloudFragment(savedInstanceState, query, true, null);

                        // action: search immediately
                        startListFragment(savedInstanceState, null, null, query, null);
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
                startListFragment(savedInstanceState, null, null, null, null);
                break;
            }
            case ACTION_SEARCH_KEYSERVER_FROM_URL: {
                // need to process URL to get search query and keyserver authority
                String query = dataUri.getQueryParameter("search");
                String keyserver = dataUri.getAuthority();
                // if query not specified, we still allow users to search the keyserver in the link
                if (query == null) {
                    Notify.create(this, R.string.import_url_warn_no_search_parameter, Notify.LENGTH_INDEFINITE,
                            Notify.Style.WARN).show(mTopFragment);
                    // we just set the keyserver
                    startCloudFragment(savedInstanceState, null, false, keyserver);
                    // it's not necessary to set the keyserver for ImportKeysListFragment since
                    // it'll be taken care of by ImportKeysCloudFragment when the user clicks
                    // the search button
                    startListFragment(savedInstanceState, null, null, null, null);
                } else {
                    // we allow our users to edit the query if they wish
                    startCloudFragment(savedInstanceState, query, false, keyserver);
                    // search immediately
                    startListFragment(savedInstanceState, null, null, query, keyserver);
                }
                break;
            }
            case ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN: {
                // NOTE: this only displays the appropriate fragment, no actions are taken
                startFileFragment(savedInstanceState);

                // no immediate actions!
                startListFragment(savedInstanceState, null, null, null, null);
                break;
            }
            default: {
                startCloudFragment(savedInstanceState, null, false, null);
                startListFragment(savedInstanceState, null, null, null, null);
                break;
            }
        }
    }


    /**
     * if the fragment is started with non-null bytes/dataUri/serverQuery, it will immediately
     * load content
     *
     * @param savedInstanceState
     * @param bytes              bytes containing list of keyrings to import
     * @param dataUri            uri to file to import keyrings from
     * @param serverQuery        query to search for on the keyserver
     * @param keyserver          keyserver authority to search on. If null will use keyserver from
     *                           user preferences
     */
    private void startListFragment(Bundle savedInstanceState, byte[] bytes, Uri dataUri,
                                   String serverQuery, String keyserver) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (mListFragment != null) {
            return;
        }

        mListFragment = ImportKeysListFragment.newInstance(bytes, dataUri, serverQuery, false,
                keyserver);

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
        if (mTopFragment != null) {
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

    /**
     * loads the CloudFragment, which consists of the search bar, search button and settings icon
     * visually.
     *
     * @param savedInstanceState
     * @param query              search query
     * @param disableQueryEdit   if true, user will not be able to edit the search query
     * @param keyserver          keyserver authority to use for search, if null will use keyserver
     *                           specified in user preferences
     */

    private void startCloudFragment(Bundle savedInstanceState, String query, boolean disableQueryEdit, String keyserver) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (mTopFragment != null) {
            return;
        }

        // Create an instance of the fragment
        mTopFragment = ImportKeysCloudFragment.newInstance(query, disableQueryEdit, keyserver);

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
            Notify.create(this, R.string.import_qr_code_too_short_fingerprint, Notify.Style.ERROR)
                    .show((ViewGroup) findViewById(R.id.import_snackbar));
            return false;
        } else {
            return true;
        }
    }

    public void loadCallback(ImportKeysListFragment.LoaderState loaderState) {
        mListFragment.loadNew(loaderState);
    }

    private void handleMessage(Message message) {
        if (message.arg1 == ServiceProgressHandler.MessageStatus.OKAY.ordinal()) {
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

            result.createNotify(ImportKeysActivity.this)
                    .show((ViewGroup) findViewById(R.id.import_snackbar));
        }
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {

        if (mListFragment.getSelectedEntries().size() == 0) {
            Notify.create(this, R.string.error_nothing_import_selected, Notify.Style.ERROR)
                    .show((ViewGroup) findViewById(R.id.import_snackbar));
            return;
        }

        mOperationHelper = new CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult>(
                this, this, R.string.progress_importing
        );

        ImportKeysListFragment.LoaderState ls = mListFragment.getLoaderState();
        if (ls instanceof ImportKeysListFragment.BytesLoaderState) {
            Log.d(Constants.TAG, "importKeys started");

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

                mKeyList = null;
                mKeyserver = null;
                mOperationHelper.cryptoOperation();

            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.create(this, "Problem writing cache file!", Notify.Style.ERROR)
                        .show((ViewGroup) findViewById(R.id.import_snackbar));
            }
        } else if (ls instanceof ImportKeysListFragment.CloudLoaderState) {
            ImportKeysListFragment.CloudLoaderState sls =
                    (ImportKeysListFragment.CloudLoaderState) ls;

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

            mKeyList = keys;
            mKeyserver = sls.mCloudPrefs.keyserver;
            mOperationHelper.cryptoOperation();

        }
    }

    @Override
    protected void onNfcPerform() throws IOException {
        // this displays the key or moves to the yubikey import dialogue.
        super.onNfcPerform();
        // either way, finish afterwards
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mOperationHelper == null ||
                !mOperationHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void handleResult (ImportKeyResult result) {
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

        result.createNotify(ImportKeysActivity.this)
                .show((ViewGroup) findViewById(R.id.import_snackbar));
    }
    // methods from CryptoOperationHelper.Callback

    @Override
    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        handleResult(result);
    }

    @Override
    public void onCryptoOperationCancelled() {
        // do nothing
    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        handleResult(result);
    }
}
