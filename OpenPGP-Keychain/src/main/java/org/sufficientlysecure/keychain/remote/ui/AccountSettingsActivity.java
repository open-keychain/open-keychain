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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.util.Log;

public class AccountSettingsActivity extends ActionBarActivity {
    private Uri mAccountUri;

    private AccountSettingsFragment mAccountSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar
        ActionBarHelper.setOneButtonView(getSupportActionBar(),
                R.string.api_settings_save, R.drawable.ic_action_done,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        save();
                    }
                });

        setContentView(R.layout.api_account_settings_activity);

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
            loadData(savedInstanceState, mAccountUri);
        }
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
            case R.id.menu_account_settings_delete:
                deleteAccount();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData(Bundle savedInstanceState, Uri accountUri) {
        // TODO: load this also like other fragment with newInstance arguments?
        AccountSettings settings = ProviderHelper.getApiAccountSettings(this, accountUri);
        mAccountSettingsFragment.setAccSettings(settings);
    }

    private void deleteAccount() {
        if (getContentResolver().delete(mAccountUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

    private void save() {
        ProviderHelper.updateApiAccount(this, mAccountSettingsFragment.getAccSettings(), mAccountUri);

        finish();
    }

}
