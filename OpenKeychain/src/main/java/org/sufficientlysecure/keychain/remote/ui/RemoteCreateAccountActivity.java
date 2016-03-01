/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;

public class RemoteCreateAccountActivity extends BaseActivity {

    public static final String EXTRA_ACC_NAME = "acc_name";
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    public static final String EXTRA_DATA = "data";

    private AccountSettingsFragment mAccSettingsFragment;

    boolean mUpdateExistingAccount;

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_create_account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();

        final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
        final String accName = extras.getString(EXTRA_ACC_NAME);

        final ApiDataAccessObject apiDao = new ApiDataAccessObject(this);

        mAccSettingsFragment = (AccountSettingsFragment) getSupportFragmentManager().findFragmentById(
                R.id.api_account_settings_fragment);

        TextView text = (TextView) findViewById(R.id.api_remote_create_account_text);

        // update existing?
        Uri uri = KeychainContract.ApiAccounts.buildByPackageAndAccountUri(packageName, accName);
        AccountSettings settings = apiDao.getApiAccountSettings(uri);
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
                            Notify.create(RemoteCreateAccountActivity.this, getString(R.string.api_register_error_select_key), Notify.Style.ERROR).show();
                        } else {
                            if (mUpdateExistingAccount) {
                                Uri baseUri = KeychainContract.ApiAccounts.buildBaseUri(packageName);
                                Uri accountUri = baseUri.buildUpon().appendEncodedPath(accName).build();
                                apiDao.updateApiAccount(
                                        accountUri,
                                        mAccSettingsFragment.getAccSettings());
                            } else {
                                apiDao.insertApiAccount(
                                        KeychainContract.ApiAccounts.buildBaseUri(packageName),
                                        mAccSettingsFragment.getAccSettings());
                            }

                            // give data through for new service call
                            Intent resultData = extras.getParcelable(EXTRA_DATA);
                            RemoteCreateAccountActivity.this.setResult(RESULT_OK, resultData);
                            RemoteCreateAccountActivity.this.finish();
                        }
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Cancel
                        RemoteCreateAccountActivity.this.setResult(RESULT_CANCELED);
                        RemoteCreateAccountActivity.this.finish();
                    }
                });

    }

}
