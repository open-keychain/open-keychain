/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
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
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.SlidingTabLayout;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class ViewKeyActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    public static final String EXTRA_SELECTED_TAB = "selectedTab";
    public static final int TAB_MAIN = 0;
    public static final int TAB_SHARE = 1;
    public static final int TAB_KEYS = 2;
    public static final int TAB_CERTS = 3;

    // view
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private PagerTabStripAdapter mTabsAdapter;
    private View mStatusDivider;
    private View mStatusRevoked;
    private View mStatusExpired;

    public static final int REQUEST_CODE_LOOKUP_KEY = 0x00007006;

    // NFC
    private NfcAdapter mNfcAdapter;
    private NfcAdapter.CreateNdefMessageCallback mNdefCallback;
    private NfcAdapter.OnNdefPushCompleteCallback mNdefCompleteCallback;
    private byte[] mNfcKeyringBytes;
    private static final int NFC_SENT = 1;

    private static final int LOADER_ID_UNIFIED = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(this);
        mProviderHelper = new ProviderHelper(this);

        // let the actionbar look like Android's contact app
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.view_key_activity);

        mStatusRevoked = findViewById(R.id.view_key_revoked);
        mStatusExpired = findViewById(R.id.view_key_expired);

        mViewPager = (ViewPager) findViewById(R.id.view_key_pager);
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.view_key_sliding_tab_layout);

        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);

        int switchToTab = TAB_MAIN;
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SELECTED_TAB)) {
            switchToTab = intent.getExtras().getInt(EXTRA_SELECTED_TAB);
        }

        Uri dataUri = getIntent().getData();
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            finish();
            return;
        }

        loadData(dataUri);

        initNfc(dataUri);

        Bundle mainBundle = new Bundle();
        mainBundle.putParcelable(ViewKeyMainFragment.ARG_DATA_URI, dataUri);
        mTabsAdapter.addTab(ViewKeyMainFragment.class,
                mainBundle, getString(R.string.key_view_tab_main));

        Bundle shareBundle = new Bundle();
        shareBundle.putParcelable(ViewKeyMainFragment.ARG_DATA_URI, dataUri);
        mTabsAdapter.addTab(ViewKeyShareFragment.class,
                mainBundle, getString(R.string.key_view_tab_share));

        Bundle keyDetailsBundle = new Bundle();
        keyDetailsBundle.putParcelable(ViewKeyKeysFragment.ARG_DATA_URI, dataUri);
        mTabsAdapter.addTab(ViewKeyKeysFragment.class,
                keyDetailsBundle, getString(R.string.key_view_tab_keys_details));

        Bundle certBundle = new Bundle();
        certBundle.putParcelable(ViewKeyCertsFragment.ARG_DATA_URI, dataUri);
        mTabsAdapter.addTab(ViewKeyCertsFragment.class,
                certBundle, getString(R.string.key_view_tab_certs));

        // NOTE: must be after adding the tabs!
        mSlidingTabLayout.setViewPager(mViewPager);

        // switch to tab selected by extra
        mViewPager.setCurrentItem(switchToTab);
    }

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getSupportLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    Intent homeIntent = new Intent(this, KeyListActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    return true;
                case R.id.menu_key_view_update:
                    updateFromKeyserver(mDataUri, mProviderHelper);
                    return true;
                case R.id.menu_key_view_export_keyserver:
                    uploadToKeyserver(mDataUri);
                    return true;
                case R.id.menu_key_view_export_file:
                    exportToFile(mDataUri, mExportHelper, mProviderHelper);
                    return true;
                case R.id.menu_key_view_delete: {
                    deleteKey(mDataUri, mExportHelper);
                    return true;
                }
            }
        } catch (ProviderHelper.NotFoundException e) {
            AppMsg.makeText(this, R.string.error_key_not_found, AppMsg.STYLE_ALERT).show();
            Log.e(Constants.TAG, "Key not found", e);
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToFile(Uri dataUri, ExportHelper exportHelper, ProviderHelper providerHelper)
            throws ProviderHelper.NotFoundException {
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri);

        HashMap<String, Object> data = providerHelper.getGenericData(
                baseUri,
                new String[]{KeychainContract.Keys.MASTER_KEY_ID, KeychainContract.KeyRings.HAS_SECRET},
                new int[]{ProviderHelper.FIELD_TYPE_INTEGER, ProviderHelper.FIELD_TYPE_INTEGER});

        exportHelper.showExportKeysDialog(
                new long[]{(Long) data.get(KeychainContract.KeyRings.MASTER_KEY_ID)},
                Constants.Path.APP_DIR_FILE,
                ((Long) data.get(KeychainContract.KeyRings.HAS_SECRET) == 1)
        );
    }

    private void uploadToKeyserver(Uri dataUri) throws ProviderHelper.NotFoundException {
        Intent uploadIntent = new Intent(this, UploadKeyActivity.class);
        uploadIntent.setData(dataUri);
        startActivityForResult(uploadIntent, 0);
    }

    private void updateFromKeyserver(Uri dataUri, ProviderHelper providerHelper)
            throws ProviderHelper.NotFoundException {
        byte[] blob = (byte[]) providerHelper.getGenericData(
                KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
        String fingerprint = PgpKeyHelper.convertFingerprintToHex(blob);

        Intent queryIntent = new Intent(this, ImportKeysActivity.class);
        queryIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN);
        queryIntent.putExtra(ImportKeysActivity.EXTRA_FINGERPRINT, fingerprint);

        startActivityForResult(queryIntent, REQUEST_CODE_LOOKUP_KEY);
    }

    private void deleteKey(Uri dataUri, ExportHelper exportHelper) {
        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                setResult(RESULT_CANCELED);
                finish();
            }
        };

        exportHelper.deleteKey(dataUri, returnHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_LOOKUP_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO: reload key??? move this into fragment?
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    /**
     * NFC: Initialize NFC sharing if OS and device supports it
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initNfc(final Uri dataUri) {
        // check if NFC Beam is supported (>= Android 4.1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            // Implementation for the CreateNdefMessageCallback interface
            mNdefCallback = new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    /*
                     * When a device receives a push with an AAR in it, the application specified in the AAR is
                     * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
                     * guarantee that this activity starts when receiving a beamed message. For now, this code
                     * uses the tag dispatch system.
                     */
                    NdefMessage msg = new NdefMessage(NdefRecord.createMime(Constants.NFC_MIME,
                            mNfcKeyringBytes), NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
                    return msg;
                }
            };

            // Implementation for the OnNdefPushCompleteCallback interface
            mNdefCompleteCallback = new NfcAdapter.OnNdefPushCompleteCallback() {
                @Override
                public void onNdefPushComplete(NfcEvent event) {
                    // A handler is needed to send messages to the activity when this
                    // callback occurs, because it happens from a binder thread
                    mNfcHandler.obtainMessage(NFC_SENT).sendToTarget();
                }
            };

            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter != null) {
                /*
                 * Retrieve mNfcKeyringBytes here asynchronously (to not block the UI)
                 * and init nfc adapter afterwards.
                 * mNfcKeyringBytes can not be retrieved in createNdefMessage, because this process
                 * has no permissions to query the Uri.
                 */
                AsyncTask<Void, Void, Void> initTask =
                        new AsyncTask<Void, Void, Void>() {
                            protected Void doInBackground(Void... unused) {
                                try {
                                    Uri blobUri =
                                            KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
                                    mNfcKeyringBytes = mProviderHelper.getPGPKeyRing(
                                            blobUri).getEncoded();
                                } catch (IOException e) {
                                    Log.e(Constants.TAG, "Error parsing keyring", e);
                                } catch (ProviderHelper.NotFoundException e) {
                                    Log.e(Constants.TAG, "key not found!", e);
                                }

                                // no AsyncTask return (Void)
                                return null;
                            }

                            protected void onPostExecute(Void unused) {
                                // Register callback to set NDEF message
                                mNfcAdapter.setNdefPushMessageCallback(mNdefCallback,
                                        ViewKeyActivity.this);
                                // Register callback to listen for message-sent success
                                mNfcAdapter.setOnNdefPushCompleteCallback(mNdefCompleteCallback,
                                        ViewKeyActivity.this);
                            }
                        };

                initTask.execute();
            }
        }
    }

    /**
     * NFC: This handler receives a message from onNdefPushComplete
     */
    private final Handler mNfcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NFC_SENT:
                    AppMsg.makeText(ViewKeyActivity.this, R.string.nfc_successful,
                            AppMsg.STYLE_INFO).show();
                    break;
            }
        }
    };

    static final String[] UNIFIED_PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.EXPIRY,

    };
    static final int INDEX_UNIFIED_MASTER_KEY_ID = 1;
    static final int INDEX_UNIFIED_USER_ID = 2;
    static final int INDEX_UNIFIED_IS_REVOKED = 3;
    static final int INDEX_UNIFIED_EXPIRY = 4;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(this, baseUri, UNIFIED_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        // Avoid NullPointerExceptions...
        if (data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {
                    // get name, email, and comment from USER_ID
                    String[] mainUserId = PgpKeyHelper.splitUserId(data.getString(INDEX_UNIFIED_USER_ID));
                    if (mainUserId[0] != null) {
                        setTitle(mainUserId[0]);
                    } else {
                        setTitle(R.string.user_id_no_name);
                    }

                    // get key id from MASTER_KEY_ID
                    long masterKeyId = data.getLong(INDEX_UNIFIED_MASTER_KEY_ID);
                    String keyIdStr = PgpKeyHelper.convertKeyIdToHex(masterKeyId);
                    getSupportActionBar().setSubtitle(keyIdStr);

                    // If this key is revoked, it cannot be used for anything!
                    if (data.getInt(INDEX_UNIFIED_IS_REVOKED) != 0) {
                        mStatusDivider.setVisibility(View.VISIBLE);
                        mStatusRevoked.setVisibility(View.VISIBLE);
                        mStatusExpired.setVisibility(View.GONE);
                    } else {
                        mStatusRevoked.setVisibility(View.GONE);

                        Date expiryDate = new Date(data.getLong(INDEX_UNIFIED_EXPIRY) * 1000);
                        if (!data.isNull(INDEX_UNIFIED_EXPIRY) && expiryDate.before(new Date())) {
                            mStatusDivider.setVisibility(View.VISIBLE);
                            mStatusExpired.setVisibility(View.VISIBLE);
                        } else {
                            mStatusDivider.setVisibility(View.GONE);
                            mStatusExpired.setVisibility(View.GONE);
                        }
                    }

                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
