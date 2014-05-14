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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.util.Log;

public class AppSettingsActivity extends ActionBarActivity {
    private Uri mAppUri;

    private AppSettingsFragment mSettingsFragment;
    private AccountsListFragment mAccountsListFragment;

    // model
    AppSettings mAppSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // let the actionbar look like Android's contact app
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.api_app_settings_activity);

        mSettingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                R.id.api_app_settings_fragment);

        Intent intent = getIntent();
        mAppUri = intent.getData();
        if (mAppUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mAppUri);
            loadData(savedInstanceState, mAppUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.api_app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_api_settings_revoke:
                revokeAccess();
                return true;
            case R.id.menu_api_settings_start:
                startApp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startApp() {
        Intent i;
        PackageManager manager = getPackageManager();
        try {
            i = manager.getLaunchIntentForPackage(mAppSettings.getPackageName());
            if (i == null)
                throw new PackageManager.NameNotFoundException();
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Constants.TAG, "startApp", e);
        }
    }

    private void loadData(Bundle savedInstanceState, Uri appUri) {
        mAppSettings = new ProviderHelper(this).getApiAppSettings(appUri);
        mSettingsFragment.setAppSettings(mAppSettings);

        String appName;
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(mAppSettings.getPackageName(), 0);
            appName = (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            // fallback
            appName = mAppSettings.getPackageName();
        }
        setTitle(appName);

        Uri accountsUri = appUri.buildUpon().appendPath(KeychainContract.PATH_ACCOUNTS).build();
        Log.d(Constants.TAG, "accountsUri: " + accountsUri);
        startListFragment(savedInstanceState, accountsUri);
    }

    private void startListFragment(Bundle savedInstanceState, Uri dataUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mAccountsListFragment = AccountsListFragment.newInstance(dataUri);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.api_accounts_list_fragment, mAccountsListFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    private void revokeAccess() {
        if (getContentResolver().delete(mAppUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

}
