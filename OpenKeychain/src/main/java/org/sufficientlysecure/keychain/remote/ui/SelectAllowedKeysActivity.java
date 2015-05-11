/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

public class SelectAllowedKeysActivity extends BaseActivity {

    public static final String EXTRA_SERVICE_INTENT = "data";

    private Uri mAppUri;

    private AppSettingsAllowedKeysListFragment mAllowedKeysFragment;

    Intent mServiceData;

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
                        cancel();
                    }
                });

        Intent intent = getIntent();
        mServiceData = intent.getParcelableExtra(EXTRA_SERVICE_INTENT);
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
    protected void initLayout() {
        setContentView(R.layout.api_remote_select_allowed_keys);
    }

    private void save() {
        mAllowedKeysFragment.saveAllowedKeys();
        setResult(Activity.RESULT_OK, mServiceData);
        finish();
    }

    private void cancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void loadData(Bundle savedInstanceState, Uri appUri) {
        Uri allowedKeysUri = appUri.buildUpon().appendPath(KeychainContract.PATH_ALLOWED_KEYS).build();
        Log.d(Constants.TAG, "allowedKeysUri: " + allowedKeysUri);
        startListFragments(savedInstanceState, allowedKeysUri);
    }

    private void startListFragments(Bundle savedInstanceState, Uri allowedKeysUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragments
        mAllowedKeysFragment = AppSettingsAllowedKeysListFragment.newInstance(allowedKeysUri);
        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.api_allowed_keys_list_fragment, mAllowedKeysFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
