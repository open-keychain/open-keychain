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
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

public class RemoteRegisterActivity extends BaseActivity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";

    public static final String EXTRA_DATA = "data";

    private AppSettingsHeaderFragment mAppSettingsHeaderFragment;

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_register_app);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();

        final String packageName = extras.getString(EXTRA_PACKAGE_NAME);
        final byte[] packageSignature = extras.getByteArray(EXTRA_PACKAGE_SIGNATURE);
        Log.d(Constants.TAG, "ACTION_REGISTER packageName: " + packageName);

        final ApiDataAccessObject apiDao = new ApiDataAccessObject(this);

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
                        apiDao.insertApiApp(mAppSettingsHeaderFragment.getAppSettings());

                        // give data through for new service call
                        Intent resultData = extras.getParcelable(EXTRA_DATA);
                        RemoteRegisterActivity.this.setResult(RESULT_OK, resultData);
                        RemoteRegisterActivity.this.finish();
                    }
                }, R.string.api_register_disallow, R.drawable.ic_close_white_24dp,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Disallow
                        RemoteRegisterActivity.this.setResult(RESULT_CANCELED);
                        RemoteRegisterActivity.this.finish();
                    }
                }
        );
    }

}
