/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.dialog.AdvancedAppSettingsDialogFragment;
import timber.log.Timber;


public class AppSettingsActivity extends BaseActivity {
    private Uri mAppUri;

    private TextView mAppNameView;
    private ImageView mAppIconView;


    // model
    AppSettings mAppSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppNameView = findViewById(R.id.api_app_settings_app_name);
        mAppIconView = findViewById(R.id.api_app_settings_app_icon);

        findViewById(R.id.fab).setOnClickListener(v -> startApp());

        setFullScreenDialogClose(v -> cancel());
        setTitle(null);

        Intent intent = getIntent();
        mAppUri = intent.getData();
        if (mAppUri == null) {
            Timber.e("Intent data missing. Should be Uri of app!");
            finish();
            return;
        }

        Timber.d("uri: %s", mAppUri);
        loadData(savedInstanceState, mAppUri);
    }

    private void save() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        if (supportFragmentManager == null) {
            Timber.e("Could not retrieve fragmentmanager for saving!");
            return;
        }
        AppSettingsAllowedKeysListFragment allowedKeysFragment = (AppSettingsAllowedKeysListFragment)
                supportFragmentManager.findFragmentById(R.id.api_allowed_keys_list_fragment);
        if (allowedKeysFragment == null) {
            Timber.e("Could not retrieve fragment for saving!");
            return;
        }

        allowedKeysFragment.saveAllowedKeys();
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void cancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_app_settings_activity);
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
            case R.id.menu_api_save: {
                save();
                return true;
            }
            case R.id.menu_api_settings_revoke: {
                revokeAccess();
                return true;
            }
            case R.id.menu_api_settings_advanced: {
                showAdvancedInfo();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAdvancedInfo() {
        String certificate = null;
        // advanced info: package certificate SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(mAppSettings.getPackageCertificate());
            byte[] digest = md.digest();
            certificate = new String(Hex.encode(digest));
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Should not happen!");
        }

        AdvancedAppSettingsDialogFragment dialogFragment =
                AdvancedAppSettingsDialogFragment.newInstance(mAppSettings.getPackageName(), certificate);

        dialogFragment.show(getSupportFragmentManager(), "advancedDialog");
    }

    private void startApp() {
        Intent i;
        PackageManager manager = getPackageManager();
        try {
            i = manager.getLaunchIntentForPackage(mAppSettings.getPackageName());
            if (i == null)
                throw new PackageManager.NameNotFoundException();
            // start like the Android launcher would do
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "startApp");
        }
    }

    private void loadData(Bundle savedInstanceState, Uri appUri) {
        mAppSettings = new ApiDataAccessObject(this).getApiAppSettings(appUri);

        // get application name and icon from package manager
        String appName;
        Drawable appIcon = null;
        PackageManager pm = getApplicationContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(mAppSettings.getPackageName(), 0);

            appName = (String) pm.getApplicationLabel(ai);
            appIcon = pm.getApplicationIcon(ai);
        } catch (PackageManager.NameNotFoundException e) {
            // fallback
            appName = mAppSettings.getPackageName();
        }
        mAppNameView.setText(appName);
        mAppIconView.setImageDrawable(appIcon);

        Uri allowedKeysUri = appUri.buildUpon().appendPath(KeychainContract.PATH_ALLOWED_KEYS).build();
        Timber.d("allowedKeysUri: " + allowedKeysUri);
        startListFragments(savedInstanceState, allowedKeysUri);
    }

    private void startListFragments(Bundle savedInstanceState, Uri allowedKeysUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        AppSettingsAllowedKeysListFragment allowedKeysFragment = AppSettingsAllowedKeysListFragment.newInstance(allowedKeysUri);
        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.api_allowed_keys_list_fragment, allowedKeysFragment)
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
