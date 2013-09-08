/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.SelectPublicKeyFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class CryptoServiceActivity extends SherlockFragmentActivity {

    public static final String ACTION_REGISTER = Constants.INTENT_PREFIX + "API_ACTIVITY_REGISTER";
    public static final String ACTION_CACHE_PASSPHRASE = Constants.INTENT_PREFIX
            + "API_ACTIVITY_CACHE_PASSPHRASE";
    public static final String ACTION_SELECT_PUB_KEYS = Constants.INTENT_PREFIX
            + "API_ACTIVITY_SELECT_PUB_KEYS";

    public static final String EXTRA_MESSENGER = "messenger";

    public static final String EXTRA_SECRET_KEY_ID = "secretKeyId";
    public static final String EXTRA_PACKAGE_NAME = "packageName";
    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "masterKeyIds";

    private Messenger mMessenger;

    // register view
    private AppSettingsFragment mSettingsFragment;
    // select pub key view
    private SelectPublicKeyFragment mSelectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent(), savedInstanceState);
    }

    protected void handleActions(Intent intent, Bundle savedInstanceState) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        mMessenger = extras.getParcelable(EXTRA_MESSENGER);

        /**
         * com.android.crypto actions
         */
        if (ACTION_REGISTER.equals(action)) {
            final String packageName = extras.getString(EXTRA_PACKAGE_NAME);

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.api_register_allow,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Allow

                            // user needs to select a key!
                            if (mSettingsFragment.getAppSettings().getKeyId() == Id.key.none) {
                                Toast.makeText(CryptoServiceActivity.this,
                                        R.string.api_register_error_select_key, Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                ProviderHelper.insertApiApp(CryptoServiceActivity.this,
                                        mSettingsFragment.getAppSettings());

                                Message msg = Message.obtain();
                                msg.arg1 = CryptoService.RegisterActivityCallback.ALLOW;
                                Bundle data = new Bundle();
                                data.putString(CryptoService.RegisterActivityCallback.PACKAGE_NAME,
                                        packageName);
                                msg.setData(data);
                                try {
                                    mMessenger.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(Constants.TAG, "CryptoServiceActivity", e);
                                }
                                finish();
                            }
                        }
                    }, R.string.api_register_disallow, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Disallow

                            Message msg = Message.obtain();
                            msg.arg1 = CryptoService.RegisterActivityCallback.DISALLOW;
                            try {
                                mMessenger.send(msg);
                            } catch (RemoteException e) {
                                Log.e(Constants.TAG, "CryptoServiceActivity", e);
                            }
                            finish();
                        }
                    });

            setContentView(R.layout.api_app_register_activity);

            mSettingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                    R.id.api_app_settings_fragment);

            AppSettings settings = new AppSettings(packageName);
            mSettingsFragment.setAppSettings(settings);
        } else if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);

            showPassphraseDialog(secretKeyId);
        } else if (ACTION_SELECT_PUB_KEYS.equals(action)) {
            long[] selectedMasterKeyIds = intent.getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.btn_okay,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // ok

                            Message msg = Message.obtain();
                            msg.arg1 = CryptoService.SelectPubKeysActivityCallback.OKAY;
                            Bundle data = new Bundle();
                            data.putLongArray(
                                    CryptoService.SelectPubKeysActivityCallback.PUB_KEY_IDS,
                                    mSelectFragment.getSelectedMasterKeyIds());
                            msg.setData(data);
                            try {
                                mMessenger.send(msg);
                            } catch (RemoteException e) {
                                Log.e(Constants.TAG, "CryptoServiceActivity", e);
                            }
                            finish();
                        }
                    }, R.string.btn_doNotSave, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // cancel

                            Message msg = Message.obtain();
                            msg.arg1 = CryptoService.SelectPubKeysActivityCallback.CANCEL;
                            ;
                            try {
                                mMessenger.send(msg);
                            } catch (RemoteException e) {
                                Log.e(Constants.TAG, "CryptoServiceActivity", e);
                            }
                            finish();
                        }
                    });

            setContentView(R.layout.api_app_select_pub_keys_activity);

            /* Load select pub keys fragment */
            // Check that the activity is using the layout version with
            // the fragment_container FrameLayout
            if (findViewById(R.id.api_select_pub_keys_fragment_container) != null) {

                // However, if we're being restored from a previous state,
                // then we don't need to do anything and should return or else
                // we could end up with overlapping fragments.
                if (savedInstanceState != null) {
                    return;
                }

                // Create an instance of the fragment
                mSelectFragment = SelectPublicKeyFragment.newInstance(selectedMasterKeyIds);

                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.api_select_pub_keys_fragment_container, mSelectFragment).commit();
            }

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
                    Message msg = Message.obtain();
                    msg.arg1 = CryptoService.PassphraseActivityCallback.SUCCESS;
                    try {
                        mMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoServiceActivity", e);
                    }
                } else {
                    Message msg = Message.obtain();
                    msg.arg1 = CryptoService.PassphraseActivityCallback.NO_SUCCESS;
                    try {
                        mMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoServiceActivity", e);
                    }
                }
                finish();
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
