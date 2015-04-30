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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.SelectPublicKeyFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

// TODO: make extensible BaseRemoteServiceActivity and extend these cases from it
public class RemoteServiceActivity extends BaseActivity {

    public static final String ACTION_REGISTER = Constants.INTENT_PREFIX + "API_ACTIVITY_REGISTER";
    public static final String ACTION_CREATE_ACCOUNT = Constants.INTENT_PREFIX
            + "API_ACTIVITY_CREATE_ACCOUNT";
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
    private AppSettingsHeaderFragment mAppSettingsHeaderFragment;
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

    @Override
    protected void initLayout() {
        // done in handleActions()
    }

    protected void handleActions(Intent intent, Bundle savedInstanceState) {

        String action = intent.getAction();
        final Bundle extras = intent.getExtras();


        switch (action) {
            case ACTION_REGISTER: {
                final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
                final byte[] packageSignature = extras.getByteArray(EXTRA_PACKAGE_SIGNATURE);
                Log.d(Constants.TAG, "ACTION_REGISTER packageName: " + packageName);

                setContentView(R.layout.api_remote_register_app);
                initToolbar();

                mAppSettingsHeaderFragment = (AppSettingsHeaderFragment) getSupportFragmentManager().findFragmentById(
                        R.id.api_app_settings_fragment);

                AppSettings settings = new AppSettings(packageName, packageSignature);
                mAppSettingsHeaderFragment.setAppSettings(settings);

                // Inflate a "Done"/"Cancel" custom action bar view
                setFullScreenDialogTwoButtons(
                        R.string.api_register_allow, R.drawable.ic_check_white_24dp,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Allow

                                mProviderHelper.insertApiApp(mAppSettingsHeaderFragment.getAppSettings());

                                // give data through for new service call
                                Intent resultData = extras.getParcelable(EXTRA_DATA);
                                RemoteServiceActivity.this.setResult(RESULT_OK, resultData);
                                RemoteServiceActivity.this.finish();
                            }
                        }, R.string.api_register_disallow, R.drawable.ic_close_white_24dp,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Disallow
                                RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                                RemoteServiceActivity.this.finish();
                            }
                        }
                );
                break;
            }
            case ACTION_CREATE_ACCOUNT: {
                final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
                final String accName = extras.getString(EXTRA_ACC_NAME);

                setContentView(R.layout.api_remote_create_account);
                initToolbar();

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
                setFullScreenDialogDoneClose(R.string.api_settings_save,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Save

                                // user needs to select a key if it has explicitly requested (None is only allowed for new accounts)
                                if (mUpdateExistingAccount && mAccSettingsFragment.getAccSettings().getKeyId() == Constants.key.none) {
                                    Notify.create(RemoteServiceActivity.this, getString(R.string.api_register_error_select_key), Notify.Style.ERROR).show();
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
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Cancel
                                RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                                RemoteServiceActivity.this.finish();
                            }
                        });

                break;
            }
            case ACTION_SELECT_PUB_KEYS: {
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

                setContentView(R.layout.api_remote_select_pub_keys);
                initToolbar();

                // Inflate a "Done"/"Cancel" custom action bar view
                setFullScreenDialogDoneClose(R.string.btn_okay,
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
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // cancel
                                RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                                RemoteServiceActivity.this.finish();
                            }
                        });

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
                break;
            }
            case ACTION_ERROR_MESSAGE: {
                String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);

                Spannable redErrorMessage = new SpannableString(errorMessage);
                redErrorMessage.setSpan(new ForegroundColorSpan(Color.RED), 0, errorMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                setContentView(R.layout.api_remote_error_message);
                initToolbar();

                // Inflate a "Done" custom action bar view
                setFullScreenDialogClose(
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                RemoteServiceActivity.this.setResult(RESULT_CANCELED);
                                RemoteServiceActivity.this.finish();
                            }
                        }
                );

                // set text on view
                TextView textView = (TextView) findViewById(R.id.api_app_error_message_text);
                textView.setText(redErrorMessage);
                break;
            }
            default:
                Log.e(Constants.TAG, "Action does not exist!");
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }
}
