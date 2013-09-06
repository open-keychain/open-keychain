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

package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ServiceActivity extends SherlockFragmentActivity {

    public static final String ACTION_REGISTER = "org.sufficientlysecure.keychain.remote_api.REGISTER";
    public static final String ACTION_CACHE_PASSPHRASE = "org.sufficientlysecure.keychain.remote_api.CRYPTO_CACHE_PASSPHRASE";

    public static final String EXTRA_SECRET_KEY_ID = "secretKeyId";
    public static final String EXTRA_PACKAGE_NAME = "packageName";

    private IServiceActivityCallback mServiceCallback;
    private boolean mServiceBound;

    // view
    AppSettingsFragment settingsFragment;

    private ServiceConnection mServiceActivityConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceCallback = IServiceActivityCallback.Stub.asInterface(service);
            Log.d(Constants.TAG, "connected to ICryptoServiceActivity");
            mServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mServiceCallback = null;
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
        if (mServiceCallback == null && !mServiceBound) { // if not already connected
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

            // Inflate a "Done"/"Cancel" custom action bar view
            final LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
                    .getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            final View customActionBarView = inflater.inflate(
                    R.layout.actionbar_custom_view_done_cancel, null);

            ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text))
                    .setText(R.string.api_register_allow);
            customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Allow

                            if (settingsFragment.getSecretKeyId() == Id.key.none) {
                                Toast.makeText(ServiceActivity.this,
                                        R.string.api_register_error_select_key, Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                ProviderHelper.addCryptoConsumer(ServiceActivity.this, packageName);
                                // Intent data = new Intent();

                                try {
                                    mServiceCallback.onRegistered(true, packageName);
                                } catch (RemoteException e) {
                                    Log.e(Constants.TAG, "ServiceActivity");
                                }
                                finish();
                            }
                        }
                    });
            ((TextView) customActionBarView.findViewById(R.id.actionbar_cancel_text))
                    .setText(R.string.api_register_disallow);
            customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Disallow
                            try {
                                mServiceCallback.onRegistered(false, packageName);
                            } catch (RemoteException e) {
                                Log.e(Constants.TAG, "ServiceActivity");
                            }
                            finish();
                        }
                    });

            // Show the custom action bar view and hide the normal Home icon and title.
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            setContentView(R.layout.api_app_register_activity);

            settingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                    R.id.api_app_settings_fragment);

            settingsFragment.setPackage(packageName);

            // TODO: handle if app is already registered
            // LinearLayout layoutRegister = (LinearLayout)
            // findViewById(R.id.register_crypto_consumer_register_layout);
            // LinearLayout layoutEdit = (LinearLayout)
            // findViewById(R.id.register_crypto_consumer_edit_layout);
            //
            // // if already registered show edit buttons
            // ArrayList<String> allowedPkgs = ProviderHelper.getCryptoConsumers(this);
            // if (allowedPkgs.contains(packageName)) {
            // Log.d(Constants.TAG, "Package is allowed! packageName: " + packageName);
            // layoutRegister.setVisibility(View.GONE);
            // layoutEdit.setVisibility(View.VISIBLE);
            // } else {
            // layoutRegister.setVisibility(View.VISIBLE);
            // layoutEdit.setVisibility(View.GONE);
            // }

        } else if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);

            showPassphraseDialog(secretKeyId);
        } else {
            Log.e(Constants.TAG, "Wrong action!");
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
                    try {
                        mServiceCallback.onCachedPassphrase(true);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "ServiceActivity");
                    }
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
