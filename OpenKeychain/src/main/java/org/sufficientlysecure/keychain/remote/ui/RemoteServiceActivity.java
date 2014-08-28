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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.ui.SelectPublicKeyFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class RemoteServiceActivity extends ActionBarActivity {

    public static final String ACTION_REGISTER = Constants.INTENT_PREFIX + "API_ACTIVITY_REGISTER";
    public static final String ACTION_CREATE_ACCOUNT = Constants.INTENT_PREFIX
            + "API_ACTIVITY_CREATE_ACCOUNT";
    public static final String ACTION_CACHE_PASSPHRASE = Constants.INTENT_PREFIX
            + "API_ACTIVITY_CACHE_PASSPHRASE";
    public static final String ACTION_SELECT_PUB_KEYS = Constants.INTENT_PREFIX
            + "API_ACTIVITY_SELECT_PUB_KEYS";
    public static final String ACTION_ERROR_MESSAGE = Constants.INTENT_PREFIX
            + "API_ACTIVITY_ERROR_MESSAGE";

    public static final String EXTRA_MESSENGER = "messenger";

    public static final String EXTRA_DATA = "data";

    // passphrase action
    public static final String EXTRA_SECRET_KEY_ID = "secret_key_id";
    // register action
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";
    // create acc action
    public static final String EXTRA_ACC_NAME = "acc_name";
    // select pub keys action
    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_MISSING_USER_IDS = "missing_user_ids";
    public static final String EXTRA_DUPLICATE_USER_IDS = "dublicate_user_ids";
    public static final String EXTRA_NO_USER_IDS_CHECK = "no_user_ids";
    // error message
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    // register view
    private AppSettingsFragment mAppSettingsFragment;
    // create acc view
    private AccountSettingsFragment mAccSettingsFragment;
    // select pub keys view
    private SelectPublicKeyFragment mSelectFragment;

    private ProviderHelper mProviderHelper;

    // for ACTION_CREATE_ACCOUNT
    boolean mUpdateExistingAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProviderHelper = new ProviderHelper(this);

        handleActions(getIntent(), savedInstanceState);
    }

    protected void handleActions(Intent intent, Bundle savedInstanceState) {

        String action = intent.getAction();
        final Bundle extras = intent.getExtras();


        if (ACTION_REGISTER.equals(action)) {
            final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
            final byte[] packageSignature = extras.getByteArray(EXTRA_PACKAGE_SIGNATURE);
            Log.d(Constants.TAG, "ACTION_REGISTER packageName: " + packageName);

            setContentView(R.layout.api_remote_register_app);

            mAppSettingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                    R.id.api_app_settings_fragment);

            AppSettings settings = new AppSettings(packageName, packageSignature);
            mAppSettingsFragment.setAppSettings(settings);

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setTwoButtonView(getSupportActionBar(),
                    R.string.api_register_allow, R.drawable.ic_action_done,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Allow

                            mProviderHelper.insertApiApp(mAppSettingsFragment.getAppSettings());

                            // give data through for new service call
                            Intent resultData = extras.getParcelable(EXTRA_DATA);
                            RemoteServiceActivity.this.setResult(RESULT_OK, resultData);
                            RemoteServiceActivity.this.finish();
                        }
                    }, R.string.api_register_disallow, R.drawable.ic_action_cancel,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Disallow
                            RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                            RemoteServiceActivity.this.finish();
                        }
                    }
            );
        } else if (ACTION_CREATE_ACCOUNT.equals(action)) {
            final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
            final String accName = extras.getString(EXTRA_ACC_NAME);

            setContentView(R.layout.api_remote_create_account);

            mAccSettingsFragment = (AccountSettingsFragment) getSupportFragmentManager().findFragmentById(
                    R.id.api_account_settings_fragment);

            TextView text = (TextView) findViewById(R.id.api_remote_create_account_text);

            // update existing?
            Uri uri = KeychainContract.ApiAccounts.buildByPackageAndAccountUri(packageName, accName);
            AccountSettings settings = mProviderHelper.getApiAccountSettings(uri);
            if (settings == null) {
                // create new account
                settings = new AccountSettings(accName);
                mUpdateExistingAccount = false;

                text.setText(R.string.api_create_account_text);
            } else {
                // update existing account
                mUpdateExistingAccount = true;

                text.setText(R.string.api_update_account_text);
            }
            mAccSettingsFragment.setAccSettings(settings);

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setTwoButtonView(getSupportActionBar(),
                    R.string.api_settings_save, R.drawable.ic_action_done,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Save

                            // user needs to select a key!
                            if (mAccSettingsFragment.getAccSettings().getKeyId() == Constants.key.none) {
                                mAccSettingsFragment.setErrorOnSelectKeyFragment(
                                        getString(R.string.api_register_error_select_key));
                            } else {
                                if (mUpdateExistingAccount) {
                                    Uri baseUri = KeychainContract.ApiAccounts.buildBaseUri(packageName);
                                    Uri accountUri = baseUri.buildUpon().appendEncodedPath(accName).build();
                                    mProviderHelper.updateApiAccount(
                                            accountUri,
                                            mAccSettingsFragment.getAccSettings());
                                } else {
                                    mProviderHelper.insertApiAccount(
                                            KeychainContract.ApiAccounts.buildBaseUri(packageName),
                                            mAccSettingsFragment.getAccSettings());
                                }

                                // give data through for new service call
                                Intent resultData = extras.getParcelable(EXTRA_DATA);
                                RemoteServiceActivity.this.setResult(RESULT_OK, resultData);
                                RemoteServiceActivity.this.finish();
                            }
                        }
                    }, R.string.api_settings_cancel, R.drawable.ic_action_cancel,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Cancel
                            RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                            RemoteServiceActivity.this.finish();
                        }
                    }
            );

        } else if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);
            final Intent resultData = extras.getParcelable(EXTRA_DATA);

            PassphraseDialogFragment.show(this, secretKeyId,
                    new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                // return given params again, for calling the service method again
                                RemoteServiceActivity.this.setResult(RESULT_OK, resultData);
                            } else {
                                RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                            }

                            RemoteServiceActivity.this.finish();
                        }
                    }
            );

        } else if (ACTION_SELECT_PUB_KEYS.equals(action)) {
            long[] selectedMasterKeyIds = intent.getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);
            boolean noUserIdsCheck = intent.getBooleanExtra(EXTRA_NO_USER_IDS_CHECK, true);
            ArrayList<String> missingUserIds = intent
                    .getStringArrayListExtra(EXTRA_MISSING_USER_IDS);
            ArrayList<String> dublicateUserIds = intent
                    .getStringArrayListExtra(EXTRA_DUPLICATE_USER_IDS);

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            final SpannableString textIntro = new SpannableString(
                    noUserIdsCheck ? getString(R.string.api_select_pub_keys_text_no_user_ids)
                            : getString(R.string.api_select_pub_keys_text)
            );
            textIntro.setSpan(new StyleSpan(Typeface.BOLD), 0, textIntro.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(textIntro);

            if (missingUserIds != null && missingUserIds.size() > 0) {
                ssb.append("\n\n");
                ssb.append(getString(R.string.api_select_pub_keys_missing_text));
                ssb.append("\n");
                for (String userId : missingUserIds) {
                    SpannableString ss = new SpannableString(userId + "\n");
                    ss.setSpan(new BulletSpan(15, Color.BLACK), 0, ss.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.append(ss);
                }
            }
            if (dublicateUserIds != null && dublicateUserIds.size() > 0) {
                ssb.append("\n\n");
                ssb.append(getString(R.string.api_select_pub_keys_dublicates_text));
                ssb.append("\n");
                for (String userId : dublicateUserIds) {
                    SpannableString ss = new SpannableString(userId + "\n");
                    ss.setSpan(new BulletSpan(15, Color.BLACK), 0, ss.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.append(ss);
                }
            }

            // Inflate a "Done"/"Cancel" custom action bar view
            ActionBarHelper.setTwoButtonView(getSupportActionBar(),
                    R.string.btn_okay, R.drawable.ic_action_done,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // add key ids to params Bundle for new request
                            Intent resultData = extras.getParcelable(EXTRA_DATA);
                            resultData.putExtra(OpenPgpApi.EXTRA_KEY_IDS,
                                    mSelectFragment.getSelectedMasterKeyIds());

                            RemoteServiceActivity.this.setResult(RESULT_OK, resultData);
                            RemoteServiceActivity.this.finish();
                        }
                    }, R.string.btn_do_not_save, R.drawable.ic_action_cancel, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // cancel
                            RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                            RemoteServiceActivity.this.finish();
                        }
                    }
            );

            setContentView(R.layout.api_remote_select_pub_keys);

            // set text on view
            TextView textView = (TextView) findViewById(R.id.api_select_pub_keys_text);
            textView.setText(ssb, TextView.BufferType.SPANNABLE);

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

            String text = "<font color=\"red\">" + errorMessage + "</font>";

            // Inflate a "Done" custom action bar view
            ActionBarHelper.setOneButtonView(getSupportActionBar(),
                    R.string.btn_okay, R.drawable.ic_action_done,
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                            RemoteServiceActivity.this.finish();
                        }
                    }
            );

            setContentView(R.layout.api_remote_error_message);

            // set text on view
            HtmlTextView textView = (HtmlTextView) findViewById(R.id.api_app_error_message_text);
            textView.setHtmlFromString(text, true);
        } else {
            Log.e(Constants.TAG, "Action does not exist!");
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
