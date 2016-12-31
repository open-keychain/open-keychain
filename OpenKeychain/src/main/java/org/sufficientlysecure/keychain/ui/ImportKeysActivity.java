/*
 * Copyright (C) 2012-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.keyimport.FacebookKeyserver;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysOperationCallback;
import org.sufficientlysecure.keychain.keyimport.processing.LoaderState;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportKeysActivity extends BaseActivity implements ImportKeysListener {

    public static final String ACTION_IMPORT_KEY = OpenKeychainIntents.IMPORT_KEY;
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER = OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER;
    public static final String ACTION_IMPORT_KEY_FROM_FACEBOOK
            = Constants.INTENT_PREFIX + "IMPORT_KEY_FROM_FACEBOOK";
    public static final String ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT =
            Constants.INTENT_PREFIX + "IMPORT_KEY_FROM_KEY_SERVER_AND_RETURN_RESULT";
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

    public static final String TAG_FRAG_LIST = "frag_list";
    public static final String TAG_FRAG_TOP = "frag_top";

    private boolean mFreshIntent;
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mOpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we're started with a new Intent that needs to be handled by onResumeFragments
        mFreshIntent = true;

        setFullScreenDialogClose(Activity.RESULT_CANCELED, true);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.import_keys_activity);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mFreshIntent) {
            handleActions(getIntent());
            // we've consumed this Intent, we don't want to repeat the action it represents
            // every time the activity is resumed
            mFreshIntent = false;
        }
    }

    protected void handleActions(@NonNull Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        Uri dataUri = intent.getData();
        String scheme = intent.getScheme();

        if (extras == null) {
            extras = new Bundle();
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            if (FacebookKeyserver.isFacebookHost(dataUri)) {
                action = ACTION_IMPORT_KEY_FROM_FACEBOOK;
            } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                action = ACTION_SEARCH_KEYSERVER_FROM_URL;
            } else if ("openpgp4fpr".equalsIgnoreCase(scheme)) {
                action = ACTION_IMPORT_KEY_FROM_KEYSERVER;
                extras.putString(EXTRA_FINGERPRINT, dataUri.getSchemeSpecificPart());
            } else {
                // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)
                // delegate action to ACTION_IMPORT_KEY
                action = ACTION_IMPORT_KEY;
            }
        }
        if (action == null) {
            // -> switch to default below
            action = "";
        }

        switch (action) {
            case ACTION_IMPORT_KEY: {
                if (dataUri != null) {
                    // action: directly load data
                    startListFragment(null, dataUri, null, null);
                } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                    byte[] importData = extras.getByteArray(EXTRA_KEY_BYTES);

                    // action: directly load data
                    startListFragment(importData, null, null, null);
                } else {
                    startTopFileFragment();
                    startListFragment(null, null, null, null);
                }
                break;
            }
            case ACTION_IMPORT_KEY_FROM_KEYSERVER:
            case ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT: {

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
                        startTopCloudFragment(query, null);

                        // action: search immediately
                        startListFragment(null, null, query, null);
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

                        // action: search immediately
                        startListFragment(null, null, query, null);
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
            case ACTION_IMPORT_KEY_FROM_FACEBOOK: {
                String fbUsername = FacebookKeyserver.getUsernameFromUri(dataUri);

                Preferences.CloudSearchPrefs cloudSearchPrefs =
                        new Preferences.CloudSearchPrefs(false, true, true, null);
                // search immediately
                startListFragment(null, null, fbUsername, cloudSearchPrefs);
                break;
            }
            case ACTION_SEARCH_KEYSERVER_FROM_URL: {
                // get keyserver from URL
                ParcelableHkpKeyserver keyserver = new ParcelableHkpKeyserver(
                        dataUri.getScheme() + "://" + dataUri.getAuthority());
                Preferences.CloudSearchPrefs cloudSearchPrefs = new Preferences.CloudSearchPrefs(
                        true, false, false, keyserver);
                Log.d(Constants.TAG, "Using keyserver: " + keyserver);

                // process URL to get operation and query
                String operation = dataUri.getQueryParameter("op");
                String query = dataUri.getQueryParameter("search");

                // if query or operation not specified, we still allow users to search
                if (query == null || operation == null) {
                    startTopCloudFragment(null, cloudSearchPrefs);
                    startListFragment(null, null, null, cloudSearchPrefs);
                } else {
                    if (operation.equalsIgnoreCase("get")) {
                        // don't allow searching here, only one key!
                        startListFragment(null, null, query, cloudSearchPrefs);
                    } else { // for example: operation: index
                        startTopCloudFragment(query, cloudSearchPrefs);
                        startListFragment(null, null, query, cloudSearchPrefs);
                    }
                }
                break;
            }
            case ACTION_IMPORT_KEY_FROM_FILE:
            case ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN: {
                // NOTE: this only displays the appropriate fragment, no actions are taken
                startTopFileFragment();
                startListFragment(null, null, null, null);
                break;
            }
            default: {
                startTopCloudFragment(null, null);
                startListFragment(null, null, null, null);
                break;
            }
        }
    }

    /**
     * Shows the list of keys to be imported.
     * If the fragment is started with non-null bytes/dataUri/serverQuery, it will immediately
     * load content.
     *
     * @param bytes            bytes containing list of keyrings to import
     * @param dataUri          uri to file to import keyrings from
     * @param serverQuery      query to search for on the keyserver
     * @param cloudSearchPrefs search specifications to use. If null will retrieve from user's
     *                         preferences.
     */
    private void startListFragment(byte[] bytes, Uri dataUri, String serverQuery,
                                   Preferences.CloudSearchPrefs cloudSearchPrefs) {

        Fragment listFragment =
                ImportKeysListFragment.newInstance(bytes, dataUri, serverQuery, false,
                        cloudSearchPrefs);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_keys_list_container, listFragment, TAG_FRAG_LIST)
                .commit();
    }

    private void startTopFileFragment() {
        FragmentManager fM = getSupportFragmentManager();
        if (fM.findFragmentByTag(TAG_FRAG_TOP) == null) {
            Fragment importFileFragment = ImportKeysFileFragment.newInstance();
            fM.beginTransaction().add(importFileFragment, TAG_FRAG_TOP).commit();
        }
    }

    /**
     * loads the CloudFragment, which enables the search bar
     *
     * @param query            search query
     * @param cloudSearchPrefs keyserver authority to use for search, if null will use keyserver
     *                         specified in user preferences
     */
    private void startTopCloudFragment(String query,
                                       Preferences.CloudSearchPrefs cloudSearchPrefs) {

        FragmentManager fM = getSupportFragmentManager();
        if (fM.findFragmentByTag(TAG_FRAG_TOP) == null) {
            Fragment importCloudFragment = ImportKeysSearchFragment.newInstance(query,
                    cloudSearchPrefs);
            fM.beginTransaction().add(importCloudFragment, TAG_FRAG_TOP).commit();
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mOpHelper != null &&
                mOpHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fM = getSupportFragmentManager();
        ImportKeysListFragment listFragment =
                (ImportKeysListFragment) fM.findFragmentByTag(TAG_FRAG_LIST);

        if ((listFragment == null) || listFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void loadKeys(LoaderState loaderState) {
        FragmentManager fM = getSupportFragmentManager();
        ((ImportKeysListFragment) fM.findFragmentByTag(TAG_FRAG_LIST)).loadState(loaderState);
    }

    @Override
    public void importKeys(List<ImportKeysListEntry> entries) {
        List<ParcelableKeyRing> keyRings = new ArrayList<>();
        for (ImportKeysListEntry e : entries) {
            keyRings.add(e.getParcelableKeyRing());
        }
        // instead of giving the entries by Intent extra, cache them into a
        // file to prevent Java Binder problems on heavy imports
        // read FileImportCache for more info.
        try {
            // We parcel this iteratively into a file - anything we can
            // display here, we should be able to import.
            ParcelableFileCache<ParcelableKeyRing> cache =
                    new ParcelableFileCache<>(this, ImportOperation.CACHE_FILE_NAME);
            cache.writeCache(entries.size(), keyRings.iterator());
        } catch (IOException e) {
            Log.e(Constants.TAG, "Problem writing cache file", e);
            Notify.create(this, "Problem writing cache file!", Notify.Style.ERROR).show();
            return;
        }

        ImportKeyringParcel inputParcel = new ImportKeyringParcel(null, null);
        ImportKeysOperationCallback callback = new ImportKeysOperationCallback(this, inputParcel, null);
        mOpHelper = new CryptoOperationHelper<>(1, this, callback, R.string.progress_importing);
        mOpHelper.cryptoOperation();
    }

    @Override
    public void handleResult(ImportKeyResult result, Integer position) {
        String intentAction = getIntent().getAction();

        if (ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT.equals(intentAction)
                || ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN.equals(intentAction)) {
            Intent intent = new Intent();
            intent.putExtra(ImportKeyResult.EXTRA_RESULT, result);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else if (result.isOkNew() || result.isOkUpdated()) {
            // User has successfully imported a key, hide first time dialog
            Preferences.getPreferences(this).setFirstTime(false);

            // Close activities opened for importing keys and go to the list of keys
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(ImportKeyResult.EXTRA_RESULT, result);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            result.createNotify(this).show();
        }
    }

}
