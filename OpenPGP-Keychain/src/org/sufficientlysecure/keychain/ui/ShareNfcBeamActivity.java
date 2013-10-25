/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 The Android Open Source Project
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

import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ShareNfcBeamActivity extends SherlockFragmentActivity implements
        CreateNdefMessageCallback, OnNdefPushCompleteCallback {
    public static final String ACTION_SHARE_KEYRING_WITH_NFC = Constants.INTENT_PREFIX
            + "SHARE_KEYRING_WITH_NFC";

    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";

    NfcAdapter mNfcAdapter;

    byte[] mSharedKeyringBytes;

    private static final int MESSAGE_SENT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Toast.makeText(this,
                    getString(R.string.error) + ": " + getString(R.string.error_jelly_bean_needed),
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter == null) {
                Toast.makeText(this,
                        getString(R.string.error) + ": " + getString(R.string.error_nfc_needed),
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                // handle actions after verifying that nfc works...
                handleActions(getIntent());
            }
        }
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        if (ACTION_SHARE_KEYRING_WITH_NFC.equals(action)) {
            long masterKeyId = extras.getLong(EXTRA_MASTER_KEY_ID);

            // get public keyring as byte array
            mSharedKeyringBytes = ProviderHelper.getPublicKeyRingsAsByteArray(this,
                    new long[] { masterKeyId });

            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void handleActionNdefDiscovered(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        byte[] receivedKeyringBytes = msg.getRecords()[0].getPayload();

        Intent importIntent = new Intent(this, ImportKeysActivity.class);
        importIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY);
        importIntent.putExtra(ImportKeysActivity.EXTRA_KEY_BYTES, receivedKeyringBytes);

        finish();

        startActivity(importIntent);
    }

    private void buildView() {
        setContentView(R.layout.share_nfc_beam);

        HtmlTextView aboutTextView = (HtmlTextView) findViewById(R.id.nfc_beam_text);

        // load html from raw resource (Parsing handled by HtmlTextView library)
        aboutTextView.setHtmlFromRawResource(this, R.raw.nfc_beam_share);

        // no flickering when clicking textview for Android < 4
        aboutTextView.setTextColor(getResources().getColor(android.R.color.black));

        // set actionbar without home button if called from another app
        ActionBarHelper.setBackButton(this);
    }

    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        /**
         * When a device receives a push with an AAR in it, the application specified in the AAR is
         * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
         * guarantee that this activity starts when receiving a beamed message. For now, this code
         * uses the tag dispatch system.
         */
        NdefMessage msg = new NdefMessage(NdefRecord.createMime(Constants.NFC_MIME,
                mSharedKeyringBytes), NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
        return msg;
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SENT:
                Toast.makeText(getApplicationContext(), R.string.nfc_successfull, Toast.LENGTH_LONG)
                        .show();
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            handleActionNdefDiscovered(getIntent());
        } else {
            // build view only when sending nfc, not when receiving, as it gets directly into Import
            // activity on receiving
            buildView();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.nfc_beam, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go to KeyListPublicActivity
            Intent intent = new Intent(this, KeyListPublicActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        case R.id.menu_settings:
            Intent intentSettings = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
            startActivity(intentSettings);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
