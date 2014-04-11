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
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.TabsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.ShareNfcDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.ShareQrCodeDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.HashMap;

public class ViewKeyActivity extends ActionBarActivity {

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    public static final String EXTRA_SELECTED_TAB = "selectedTab";

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    public static final int REQUEST_CODE_LOOKUP_KEY = 0x00007006;

    // NFC
    private NfcAdapter mNfcAdapter;
    private NfcAdapter.CreateNdefMessageCallback mNdefCallback;
    private NfcAdapter.OnNdefPushCompleteCallback mNdefCompleteCallback;
    private byte[] mNfcKeyringBytes;
    private static final int NFC_SENT = 1;

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
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        setContentView(R.layout.view_key_activity);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mViewPager);

        int selectedTab = 0;
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SELECTED_TAB)) {
            selectedTab = intent.getExtras().getInt(EXTRA_SELECTED_TAB);
        }

        mDataUri = getIntent().getData();

        initNfc(mDataUri);

        Bundle mainBundle = new Bundle();
        mainBundle.putParcelable(ViewKeyMainFragment.ARG_DATA_URI, mDataUri);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.key_view_tab_main)),
                ViewKeyMainFragment.class, mainBundle, (selectedTab == 0));

        Bundle certBundle = new Bundle();
        certBundle.putParcelable(ViewKeyCertsFragment.ARG_DATA_URI, mDataUri);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.key_view_tab_certs)),
                ViewKeyCertsFragment.class, certBundle, (selectedTab == 1));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
            case R.id.menu_key_view_share_default_fingerprint:
                shareKey(mDataUri, true, mProviderHelper);
                return true;
            case R.id.menu_key_view_share_default:
                shareKey(mDataUri, false, mProviderHelper);
                return true;
            case R.id.menu_key_view_share_qr_code_fingerprint:
                shareKeyQrCode(mDataUri, true);
                return true;
            case R.id.menu_key_view_share_qr_code:
                shareKeyQrCode(mDataUri, false);
                return true;
            case R.id.menu_key_view_share_nfc:
                shareNfc();
                return true;
            case R.id.menu_key_view_share_clipboard:
                copyToClipboard(mDataUri, mProviderHelper);
                return true;
            case R.id.menu_key_view_delete: {
                deleteKey(mDataUri, mExportHelper);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToFile(Uri dataUri, ExportHelper exportHelper, ProviderHelper providerHelper) {
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

    private void uploadToKeyserver(Uri dataUri) {
        Intent uploadIntent = new Intent(this, UploadKeyActivity.class);
        uploadIntent.setData(dataUri);
        startActivityForResult(uploadIntent, 0);
    }

    private void updateFromKeyserver(Uri dataUri, ProviderHelper providerHelper) {
        byte[] blob = (byte[]) providerHelper.getGenericData(
                KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
        String fingerprint = PgpKeyHelper.convertFingerprintToHex(blob);

        Intent queryIntent = new Intent(this, ImportKeysActivity.class);
        queryIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN);
        queryIntent.putExtra(ImportKeysActivity.EXTRA_FINGERPRINT, fingerprint);

        startActivityForResult(queryIntent, REQUEST_CODE_LOOKUP_KEY);
    }

    private void shareKey(Uri dataUri, boolean fingerprintOnly, ProviderHelper providerHelper) {
        String content = null;
        if (fingerprintOnly) {
            byte[] data = (byte[]) providerHelper.getGenericData(
                    KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                    KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
            if (data != null) {
                String fingerprint = PgpKeyHelper.convertFingerprintToHex(data);
                content = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;
            } else {
                AppMsg.makeText(this, "Bad key selected!",
                        AppMsg.STYLE_ALERT).show();
                return;
            }
        } else {
            // get public keyring as ascii armored string
            try {
                Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
                content = providerHelper.getKeyRingAsArmoredString(uri);

                // Android will fail with android.os.TransactionTooLargeException if key is too big
                // see http://www.lonestarprod.com/?p=34
                if (content.length() >= 86389) {
                    AppMsg.makeText(this, R.string.key_too_big_for_sharing,
                            AppMsg.STYLE_ALERT).show();
                    return;
                }
            } catch (IOException e) {
                Log.e(Constants.TAG, "error processing key!", e);
                AppMsg.makeText(this, R.string.error_invalid_data, AppMsg.STYLE_ALERT).show();
            } catch (ProviderHelper.NotFoundException e) {
                Log.e(Constants.TAG, "key not found!", e);
                AppMsg.makeText(this, R.string.error_key_not_found, AppMsg.STYLE_ALERT).show();
            }
        }

        if (content != null) {
            // let user choose application
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, content);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent,
                    getResources().getText(R.string.action_share_key_with)));
        } else {
            Log.e(Constants.TAG, "content is null!");
        }
    }

    private void shareKeyQrCode(Uri dataUri, boolean fingerprintOnly) {
        ShareQrCodeDialogFragment dialog = ShareQrCodeDialogFragment.newInstance(dataUri,
                fingerprintOnly);
        dialog.show(getSupportFragmentManager(), "shareQrCodeDialog");
    }

    private void copyToClipboard(Uri dataUri, ProviderHelper providerHelper) {
        // get public keyring as ascii armored string
        try {
            Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
            String keyringArmored = providerHelper.getKeyRingAsArmoredString(uri);

            ClipboardReflection.copyToClipboard(this, keyringArmored);
            AppMsg.makeText(this, R.string.key_copied_to_clipboard, AppMsg.STYLE_INFO)
                    .show();
        } catch (IOException e) {
            Log.e(Constants.TAG, "error processing key!", e);
            AppMsg.makeText(this, R.string.error_key_processing, AppMsg.STYLE_ALERT).show();
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            AppMsg.makeText(this, R.string.error_key_not_found, AppMsg.STYLE_ALERT).show();
        }
    }

    private void shareNfc() {
        ShareNfcDialogFragment dialog = ShareNfcDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "shareNfcDialog");
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
                    AppMsg.makeText(ViewKeyActivity.this, R.string.nfc_successfull,
                            AppMsg.STYLE_INFO).show();
                    break;
            }
        }
    };

}
