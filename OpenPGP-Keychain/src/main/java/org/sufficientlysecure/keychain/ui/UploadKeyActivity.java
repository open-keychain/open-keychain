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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.Log;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.beardedhen.androidbootstrap.BootstrapButton;

/**
 * gpg --send-key activity
 * 
 * Sends the selected public key to a key server
 */
public class UploadKeyActivity extends SherlockFragmentActivity {
    private BootstrapButton mUploadButton;
    private Spinner mKeyServerSpinner;

    private Uri mDataUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_server_export);

        mUploadButton = (BootstrapButton) findViewById(R.id.btn_export_to_server);
        mKeyServerSpinner = (Spinner) findViewById(R.id.sign_key_keyserver);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                        .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeyServerSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mKeyServerSpinner.setSelection(0);
        } else {
            mUploadButton.setEnabled(false);
        }

        mUploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadKey();
            }
        });

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        intent.setData(mDataUri);

        // fill values for this action
        Bundle data = new Bundle();

        String server = (String) mKeyServerSpinner.getSelectedItem();
        data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                R.string.progress_exporting, ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(UploadKeyActivity.this, R.string.key_send_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
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
    }
}
