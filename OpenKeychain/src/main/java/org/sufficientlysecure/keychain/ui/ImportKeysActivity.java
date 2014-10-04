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
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.api.OpenKeychainIntents;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.service.results.ImportKeyResult;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.widget.SlidingTabLayout;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class ImportKeysActivity extends ActionBarActivity {
    public static final String ACTION_IMPORT_KEY = OpenKeychainIntents.IMPORT_KEY;
    public static final String ACTION_IMPORT_KEY_FROM_QR_CODE = OpenKeychainIntents.IMPORT_KEY_FROM_QR_CODE;
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
    private View mImportButton;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private PagerTabStripAdapter mTabsAdapter;


    private static final int ALL_TABS = -1;
    private static final int TAB_CLOUD = 0;
    private static final int TAB_QR_CODE = 1;
    private static final int TAB_FILE = 2;

    private int mSwitchToTab = TAB_CLOUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_keys_activity);

        mViewPager = (ViewPager) findViewById(R.id.import_pager);
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.import_sliding_tab_layout);

        mImportButton = findViewById(R.id.import_import);
        mImportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                importKeys();
            }
        });

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
            // delegate action to ACTION_IMPORT_KEY
            action = ACTION_IMPORT_KEY;
        }

        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH).equals(Constants.FINGERPRINT_SCHEME)) {
            /* Scanning a fingerprint directly with Barcode Scanner */
            // delegate action to ACTION_IMPORT_KEY_FROM_KEYSERVER
            String fingerprint = getFingerprintFromUri(dataUri);
            action = ACTION_IMPORT_KEY_FROM_KEYSERVER;
            extras.putString(EXTRA_FINGERPRINT, fingerprint);
        }

        Bundle serverBundle = null;
        int showTabOnly = ALL_TABS;
        if (ACTION_IMPORT_KEY.equals(action)) {
            /* Keychain's own Actions */

            // display file fragment
            mViewPager.setCurrentItem(TAB_FILE);

            if (dataUri != null) {
                // action: directly load data
                startListFragment(savedInstanceState, null, dataUri, null);
            } else if (extras.containsKey(EXTRA_KEY_BYTES)) {
                byte[] importData = extras.getByteArray(EXTRA_KEY_BYTES);

                // action: directly load data
                startListFragment(savedInstanceState, importData, null, null);
            }
        } else if (ACTION_IMPORT_KEY_FROM_KEYSERVER.equals(action)
                || ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE.equals(action)
                || ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT.equals(action)) {

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
                    serverBundle = new Bundle();
                    serverBundle.putString(ImportKeysCloudFragment.ARG_QUERY, query);
                    mSwitchToTab = TAB_CLOUD;

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
                    serverBundle = new Bundle();
                    serverBundle.putString(ImportKeysCloudFragment.ARG_QUERY, query);
                    serverBundle.putBoolean(ImportKeysCloudFragment.ARG_DISABLE_QUERY_EDIT, true);
                    // display server tab only
                    showTabOnly = TAB_CLOUD;
                    mSwitchToTab = TAB_CLOUD;

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
        } else if (ACTION_IMPORT_KEY_FROM_FILE.equals(action)) {
            // NOTE: this only displays the appropriate fragment, no actions are taken
            mSwitchToTab = TAB_FILE;

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else if (ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN.equals(action)) {
            // NOTE: this only displays the appropriate fragment, no actions are taken
            mSwitchToTab = TAB_FILE;
            // display file tab only
            showTabOnly = TAB_FILE;

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else if (ACTION_IMPORT_KEY_FROM_QR_CODE.equals(action)) {
            // also exposed in AndroidManifest

            // NOTE: this only displays the appropriate fragment, no actions are taken
            mSwitchToTab = TAB_QR_CODE;

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else if (ACTION_IMPORT_KEY_FROM_NFC.equals(action)) {

            // NOTE: this only displays the appropriate fragment, no actions are taken
            mSwitchToTab = TAB_QR_CODE;

            // no immediate actions!
            startListFragment(savedInstanceState, null, null, null);
        } else {
            startListFragment(savedInstanceState, null, null, null);
        }

        initTabs(serverBundle, showTabOnly);
    }

    private void initTabs(Bundle serverBundle, int showTabOnly) {
        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // cancel loader and clear list
                if (mListFragment != null) {
                    mListFragment.destroyLoader();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        switch (showTabOnly) {
            case ALL_TABS:
                // show default tabs
                mTabsAdapter.addTab(ImportKeysCloudFragment.class,
                        serverBundle, getString(R.string.import_tab_cloud));
                mTabsAdapter.addTab(ImportKeysQrCodeFragment.class,
                        null, getString(R.string.import_tab_qr_code));
                mTabsAdapter.addTab(ImportKeysFileFragment.class,
                        null, getString(R.string.import_tab_direct));
                break;
            case TAB_CLOUD:
                mTabsAdapter.addTab(ImportKeysCloudFragment.class,
                        serverBundle, getString(R.string.import_tab_cloud));
                break;
            case TAB_QR_CODE:
                mTabsAdapter.addTab(ImportKeysQrCodeFragment.class,
                        null, getString(R.string.import_tab_qr_code));
                break;
            case TAB_FILE:
                mTabsAdapter.addTab(ImportKeysFileFragment.class,
                        null, getString(R.string.import_tab_direct));
                break;
        }

        // update layout after operations
        mSlidingTabLayout.setViewPager(mViewPager);

        mViewPager.setCurrentItem(mSwitchToTab);
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

    private String getFingerprintFromUri(Uri dataUri) {
        String fingerprint = dataUri.toString().split(":")[1].toLowerCase(Locale.ENGLISH);
        Log.d(Constants.TAG, "fingerprint: " + fingerprint);
        return fingerprint;
    }

    public void loadFromFingerprintUri(Uri dataUri) {
        String query = "0x" + getFingerprintFromUri(dataUri);

        // setCurrentItem does not work directly after onResume (from qr code scanner)
        // see http://stackoverflow.com/q/19316729
        // so, reset adapter completely!
        if (mViewPager.getAdapter() != null)
            mViewPager.setAdapter(null);
        mViewPager.setAdapter(mTabsAdapter);
        mViewPager.setCurrentItem(TAB_CLOUD);

        ImportKeysCloudFragment f = (ImportKeysCloudFragment)
                getActiveFragment(mViewPager, TAB_CLOUD);

        // search config
        Preferences prefs = Preferences.getPreferences(ImportKeysActivity.this);
        Preferences.CloudSearchPrefs cloudPrefs = new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());

        // search directly
        loadCallback(new ImportKeysListFragment.CloudLoaderState(query, cloudPrefs));
    }

    // http://stackoverflow.com/a/9293207
    public Fragment getActiveFragment(ViewPager container, int position) {
        String name = makeFragmentName(container.getId(), position);
        return getSupportFragmentManager().findFragmentByTag(name);
    }

    // http://stackoverflow.com/a/9293207
    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    private boolean isFingerprintValid(String fingerprint) {
        if (fingerprint == null || fingerprint.length() < 40) {
            Notify.showNotify(this, R.string.import_qr_code_too_short_fingerprint, Notify.Style.ERROR);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Scroll ViewPager left and right
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        if (!result) {
            try {
                mViewPager.onTouchEvent(event);
            } catch (IllegalArgumentException e) {
                // workaround for Android bug?
                // http://stackoverflow.com/q/16459196
                Log.d(Constants.TAG, "Workaround: Catched IllegalArgumentException");
            }
        }

        return result;
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
                        new ParcelableFileCache<ParcelableKeyRing>(this, "key_import.pcl");
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

            intent.setAction(KeychainIntentService.ACTION_DOWNLOAD_AND_IMPORT_KEYS);

            // fill values for this action
            Bundle data = new Bundle();

            data.putString(KeychainIntentService.DOWNLOAD_KEY_SERVER, sls.mCloudPrefs.keyserver);

            // get selected key entries
            ArrayList<ImportKeysListEntry> selectedEntries = mListFragment.getSelectedEntries();
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
