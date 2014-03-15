/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.beardedhen.androidbootstrap.BootstrapButton;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Iterator;

/**
 * Signs the specified public key with the specified secret master key
 */
public class CertifyKeyActivity extends ActionBarActivity implements
        SelectSecretKeyLayoutFragment.SelectSecretKeyCallback {
    private BootstrapButton mSignButton;
    private CheckBox mUploadKeyCheckbox;
    private Spinner mSelectKeyserverSpinner;

    private SelectSecretKeyLayoutFragment mSelectKeyFragment;

    private Uri mDataUri;
    private long mPubKeyId = 0;
    private long mMasterKeyId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.certify_key_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        mSelectKeyFragment = (SelectSecretKeyLayoutFragment) getSupportFragmentManager()
                .findFragmentById(R.id.sign_key_select_key_fragment);
        mSelectKeyFragment.setCallback(this);
        mSelectKeyFragment.setFilterCertify(true);

        mSelectKeyserverSpinner = (Spinner) findViewById(R.id.sign_key_keyserver);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSelectKeyserverSpinner.setAdapter(adapter);

        mUploadKeyCheckbox = (CheckBox) findViewById(R.id.sign_key_upload_checkbox);
        if (!mUploadKeyCheckbox.isChecked()) {
            mSelectKeyserverSpinner.setEnabled(false);
        } else {
            mSelectKeyserverSpinner.setEnabled(true);
        }

        mUploadKeyCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mSelectKeyserverSpinner.setEnabled(false);
                } else {
                    mSelectKeyserverSpinner.setEnabled(true);
                }
            }
        });

        mSignButton = (BootstrapButton) findViewById(R.id.sign_key_sign_button);
        mSignButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPubKeyId != 0) {
                    if (mMasterKeyId == 0) {
                        mSelectKeyFragment.setError(getString(R.string.select_key_to_sign));
                    } else {
                        initiateSigning();
                    }
                }
            }
        });

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }

        PGPPublicKeyRing signKey = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(this, mDataUri);

        if (signKey != null) {
            mPubKeyId = PgpKeyHelper.getMasterKey(signKey).getKeyID();
        }
        if (mPubKeyId == 0) {
            Log.e(Constants.TAG, "this shouldn't happen. KeyId == 0!");
            finish();
            return;
        }
    }

    private void showPassphraseDialog(final long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    startSigning();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(this,
                    messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key!");
            // send message to handler to start certification directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    /**
     * handles the UI bits of the signing process on the UI thread
     */
    private void initiateSigning() {
        PGPPublicKeyRing pubring = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(this, mPubKeyId);
        if (pubring != null) {
            // if we have already signed this key, dont bother doing it again
            boolean alreadySigned = false;

            @SuppressWarnings("unchecked")
            Iterator<PGPSignature> itr = pubring.getPublicKey(mPubKeyId).getSignatures();
            while (itr.hasNext()) {
                PGPSignature sig = itr.next();
                if (sig.getKeyID() == mMasterKeyId) {
                    alreadySigned = true;
                    break;
                }
            }

            if (!alreadySigned) {
                /*
                 * get the user's passphrase for this key (if required)
                 */
                String passphrase = PassphraseCacheService.getCachedPassphrase(this, mMasterKeyId);
                if (passphrase == null) {
                    showPassphraseDialog(mMasterKeyId);
                    // bail out; need to wait until the user has entered the passphrase before trying again
                    return;
                } else {
                    startSigning();
                }
            } else {
                Toast.makeText(this, R.string.key_has_already_been_signed, Toast.LENGTH_SHORT)
                        .show();

                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    /**
     * kicks off the actual signing process on a background thread
     */
    private void startSigning() {
        // Send all information needed to service to sign key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_CERTIFY_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        data.putLong(KeychainIntentService.CERTIFY_KEY_MASTER_KEY_ID, mMasterKeyId);
        data.putLong(KeychainIntentService.CERTIFY_KEY_PUB_KEY_ID, mPubKeyId);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after signing is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_signing), ProgressDialog.STYLE_SPINNER) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(CertifyKeyActivity.this, R.string.key_sign_success,
                            Toast.LENGTH_SHORT).show();

                    // check if we need to send the key to the server or not
                    if (mUploadKeyCheckbox.isChecked()) {
                        // upload the newly signed key to the keyserver
                        uploadKey();
                    } else {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        intent.setData(mDataUri);

        // fill values for this action
        Bundle data = new Bundle();

        Spinner keyServer = (Spinner) findViewById(R.id.sign_key_keyserver);
        String server = (String) keyServer.getSelectedItem();
        data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_exporting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    Toast.makeText(CertifyKeyActivity.this, R.string.key_send_success,
                            Toast.LENGTH_SHORT).show();

                    setResult(RESULT_OK);
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    /**
     * callback from select key fragment
     */
    @Override
    public void onKeySelected(long secretKeyId) {
        mMasterKeyId = secretKeyId;
    }
}
