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

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

/**
 * Sends the selected public key to a keyserver
 */
public class UploadKeyActivity extends BaseActivity {
    private View mUploadButton;
    private Spinner mKeyServerSpinner;

    private Uri mDataUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUploadButton = findViewById(R.id.upload_key_action_upload);
        mKeyServerSpinner = (Spinner) findViewById(R.id.upload_key_keyserver);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                .getKeyServers()
        );
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
                final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(UploadKeyActivity.this)
                        .getProxyPrefs();
                Runnable ignoreTor = new Runnable() {
                    @Override
                    public void run() {
                        uploadKey(proxyPrefs.parcelableProxy);
                    }
                };

                if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                        UploadKeyActivity.this)) {
                    uploadKey(proxyPrefs.parcelableProxy);
                }
            }
        });

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.upload_key_activity);
    }

    private void uploadKey(ParcelableProxy parcelableProxy) {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, KeychainService.class);

        intent.setAction(KeychainService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        Uri blobUri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
        intent.setData(blobUri);

        // fill values for this action
        Bundle data = new Bundle();

        String server = (String) mKeyServerSpinner.getSelectedItem();
        data.putString(KeychainService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeychainService.EXTRA_DATA, data);
        intent.putExtra(KeychainService.EXTRA_PARCELABLE_PROXY, parcelableProxy);

        // Message is received after uploading is done in KeychainService
        ServiceProgressHandler saveHandler = new ServiceProgressHandler(this) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {

                    Toast.makeText(UploadKeyActivity.this, R.string.msg_crt_upload_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(
                getString(R.string.progress_uploading),
                ProgressDialog.STYLE_HORIZONTAL, false);

        // start service with intent
        startService(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent viewIntent = NavUtils.getParentActivityIntent(this);
                viewIntent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(mDataUri));
                NavUtils.navigateUpTo(this, viewIntent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
