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
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

public class AccountSettingsActivity extends BaseActivity {
    private Uri mAccountUri;

    private AccountSettingsFragment mAccountSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar
        setFullScreenDialogDoneClose(R.string.api_settings_save,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        save();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
        setTitle(null);

        mAccountSettingsFragment = (AccountSettingsFragment) getSupportFragmentManager().findFragmentById(
                R.id.api_account_settings_fragment);

        Intent intent = getIntent();
        mAccountUri = intent.getData();
        if (mAccountUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mAccountUri);
            loadData(mAccountUri);
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_account_settings_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.api_account_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_account_settings_delete: {
                deleteAccount();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData(Uri accountUri) {
        AccountSettings settings = new ApiDataAccessObject(this).getApiAccountSettings(accountUri);
        mAccountSettingsFragment.setAccSettings(settings);
    }

    private void deleteAccount() {
        if (getContentResolver().delete(mAccountUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

    private void save() {
        new ApiDataAccessObject(this).updateApiAccount(mAccountUri, mAccountSettingsFragment.getAccSettings());
        SingletonResult result = new SingletonResult(
                SingletonResult.RESULT_OK, LogType.MSG_ACC_SAVED);
        Intent intent = new Intent();
        intent.putExtra(SingletonResult.EXTRA_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
