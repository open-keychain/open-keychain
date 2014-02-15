/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import org.openintents.openpgp.util.OpenPgpConstants;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.SelectPublicKeyFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class RemoteServiceActivity extends ActionBarActivity {

    public static final String ACTION_REGISTER = Constants.INTENT_PREFIX + "API_ACTIVITY_REGISTER";
    public static final String ACTION_CACHE_PASSPHRASE = Constants.INTENT_PREFIX
            + "API_ACTIVITY_CACHE_PASSPHRASE";
    public static final String ACTION_SELECT_PUB_KEYS = Constants.INTENT_PREFIX
            + "API_ACTIVITY_SELECT_PUB_KEYS";
    public static final String ACTION_ERROR_MESSAGE = Constants.INTENT_PREFIX
            + "API_ACTIVITY_ERROR_MESSAGE";

    public static final String EXTRA_MESSENGER = "messenger";

    // passphrase action
    public static final String EXTRA_SECRET_KEY_ID = "secret_key_id";
    // register action
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";
    // select pub keys action
    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_MISSING_USER_IDS = "missing_user_ids";
    public static final String EXTRA_DUBLICATE_USER_IDS = "dublicate_user_ids";
    // error message
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    private Messenger mMessenger;

    // register view
    private AppSettingsFragment mSettingsFragment;
    // select pub keys view
    private SelectPublicKeyFragment mSelectFragment;

    // has the user clicked one of the buttons
    // or do we need to handle the callback in onStop()
    private boolean finishHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent(), savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!finishHandled && mMessenger != null) {
            Message msg = Message.obtain();
            msg.arg1 = RemoteService.RegisterActivityCallback.CANCEL;
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "CryptoServiceActivity", e);
            }
        }
    }

    protected void handleActions(Intent intent, Bundle savedInstanceState) {
        finishHandled = false;

        String action = intent.getAction();
        final Bundle extras = intent.getExtras();

        mMessenger = extras.getParcelable(EXTRA_MESSENGER);

        /**
         * com.android.crypto actions
         */
        if (ACTION_REGISTER.equals(action)) {
            final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
            final byte[] packageSignature = extras.getByteArray(EXTRA_PACKAGE_SIGNATURE);

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.api_register_allow,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Allow

                            // user needs to select a key!
                            if (mSettingsFragment.getAppSettings().getKeyId() == Id.key.none) {
                                mSettingsFragment.setErrorOnSelectKeyFragment(
                                        getString(R.string.api_register_error_select_key));
                            } else {
                                ProviderHelper.insertApiApp(RemoteServiceActivity.this,
                                        mSettingsFragment.getAppSettings());

                                Message msg = Message.obtain();
                                msg.arg1 = RemoteService.RegisterActivityCallback.OKAY;
                                Bundle data = new Bundle();
                                data.putString(RemoteService.RegisterActivityCallback.PACKAGE_NAME,
                                        packageName);
                                msg.setData(data);
                                try {
                                    mMessenger.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(Constants.TAG, "CryptoServiceActivity", e);
                                }

                                finishHandled = true;
                                finish();
                            }
                        }
                    }, R.string.api_register_disallow, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Disallow

                            Message msg = Message.obtain();
                            msg.arg1 = RemoteService.RegisterActivityCallback.CANCEL;
                            try {
                                mMessenger.send(msg);
                            } catch (RemoteException e) {
                                Log.e(Constants.TAG, "CryptoServiceActivity", e);
                            }

                            finishHandled = true;
                            finish();
                        }
                    }
            );

            setContentView(R.layout.api_app_register_activity);

            mSettingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                    R.id.api_app_settings_fragment);

            AppSettings settings = new AppSettings(packageName, packageSignature);
            mSettingsFragment.setAppSettings(settings);
        } else if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);
            Bundle oldParams = extras.getBundle(OpenPgpConstants.PI_RESULT_PARAMS);

            showPassphraseDialog(oldParams, secretKeyId);
        } else if (ACTION_SELECT_PUB_KEYS.equals(action)) {
            long[] selectedMasterKeyIds = intent.getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);
            ArrayList<String> missingUserIds = intent
                    .getStringArrayListExtra(EXTRA_MISSING_USER_IDS);
            ArrayList<String> dublicateUserIds = intent
                    .getStringArrayListExtra(EXTRA_DUBLICATE_USER_IDS);

            String text = new String();
            text += "<b>" + getString(R.string.api_select_pub_keys_text) + "</b>";
            text += "<br/><br/>";
            if (missingUserIds != null && missingUserIds.size() > 0) {
                text += getString(R.string.api_select_pub_keys_missing_text);
                text += "<br/>";
                text += "<ul>";
                for (String userId : missingUserIds) {
                    text += "<li>" + userId + "</li>";
                }
                text += "</ul>";
                text += "<br/>";
            }
            if (dublicateUserIds != null && dublicateUserIds.size() > 0) {
                text += getString(R.string.api_select_pub_keys_dublicates_text);
                text += "<br/>";
                text += "<ul>";
                for (String userId : dublicateUserIds) {
                    text += "<li>" + userId + "</li>";
                }
                text += "</ul>";
            }

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.btn_okay,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // sdd key ids to params Bundle for new request
                            Bundle params = extras.getBundle(OpenPgpConstants.PI_RESULT_PARAMS);
                            params.putLongArray(OpenPgpConstants.PARAMS_KEY_IDS,
                                    mSelectFragment.getSelectedMasterKeyIds());

                            Intent finishIntent = new Intent();
                            finishIntent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);
                            setResult(RESULT_OK, finishIntent);
                            finish();
                        }
                    }, R.string.btn_do_not_save, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // cancel
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }
            );

            setContentView(R.layout.api_app_select_pub_keys_activity);

            // set text on view
            HtmlTextView textView = (HtmlTextView) findViewById(R.id.api_select_pub_keys_text);
            textView.setHtmlFromString(text);

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
        } else if (ACTION_ERROR_MESSAGE.equals(action)) {
            String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);

            String text = new String();
            text += "<font color=\"red\">" + errorMessage + "</font>";

            // Inflate a "Done" custom action bar view
            ActionBarHelper.setDoneView(getSupportActionBar(), R.string.btn_okay,
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });

            setContentView(R.layout.api_app_error_message);

            // set text on view
            HtmlTextView textView = (HtmlTextView) findViewById(R.id.api_app_error_message_text);
            textView.setHtmlFromString(text);
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
    private void showPassphraseDialog(final Bundle params, long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    // return given params again, for calling the service method again
                    Intent finishIntent = new Intent();
                    finishIntent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);
                    RemoteServiceActivity.this.setResult(RESULT_OK, finishIntent);
                } else {
                    RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                }

                RemoteServiceActivity.this.finish();
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(this,
                    messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // return given params again, for calling the service method again
            Intent finishIntent = new Intent();
            finishIntent.putExtras(params);
            setResult(RESULT_OK, finishIntent);
            finish();
        }
    }
}
