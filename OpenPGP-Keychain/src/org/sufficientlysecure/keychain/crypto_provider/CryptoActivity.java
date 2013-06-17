/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.crypto_provider;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CryptoActivity extends SherlockFragmentActivity {

    public static final String ACTION_REGISTER = "org.sufficientlysecure.keychain.REGISTER";
    public static final String ACTION_CACHE_PASSPHRASE = "org.sufficientlysecure.keychain.CRYPTO_CACHE_PASSPHRASE";

    public static final String EXTRA_SECRET_KEY_ID = "secretKeyId";
    public static final String EXTRA_PACKAGE_NAME = "packageName";

    private ICryptoServiceActivity mService;
    private boolean mServiceBound;

    private ServiceConnection mServiceActivityConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ICryptoServiceActivity.Stub.asInterface(service);
            Log.d(Constants.TAG, "connected to ICryptoServiceActivity");
            mServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(Constants.TAG, "disconnected from ICryptoServiceActivity");
            mServiceBound = false;
        }
    };

    /**
     * If not already bound, bind!
     * 
     * @return
     */
    public boolean bindToService() {
        if (mService == null && !mServiceBound) { // if not already connected
            try {
                Log.d(Constants.TAG, "not bound yet");

                Intent serviceIntent = new Intent();
                serviceIntent.setAction("org.openintents.crypto.ICryptoService");
                bindService(serviceIntent, mServiceActivityConnection, Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(Constants.TAG, "Exception", e);
                return false;
            }
        } else { // already connected
            Log.d(Constants.TAG, "already bound... ");
            return true;
        }
    }

    public void unbindFromService() {
        unbindService(mServiceActivityConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Constants.TAG, "onCreate…");

        // bind to our own crypto service
        bindToService();

        handleActions(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unbind from our crypto service
        if (mServiceActivityConnection != null) {
            unbindFromService();
        }
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * com.android.crypto actions
         */
        if (ACTION_REGISTER.equals(action)) {
            final String packageName = extras.getString(EXTRA_PACKAGE_NAME);

            setContentView(R.layout.register_crypto_consumer_activity);

            Button allowButton = (Button) findViewById(R.id.register_crypto_consumer_allow);
            Button disallowButton = (Button) findViewById(R.id.register_crypto_consumer_disallow);

            allowButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // ProviderHelper.addCryptoConsumer(RegisterActivity.this, callingPackageName);
                    // Intent data = new Intent();

                    setResult(RESULT_OK);
                    finish();
                }
            });

            disallowButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        } else if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);

            showPassphraseDialog(secretKeyId);
        } else {
            Log.e(Constants.TAG, "Wrong action!");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
     * for a symmetric passphrase
     */
    private void showPassphraseDialog(long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(this,
                    messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpMain.PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }
}
