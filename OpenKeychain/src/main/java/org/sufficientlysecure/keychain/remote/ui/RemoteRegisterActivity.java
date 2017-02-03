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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.RemoteRegisterPresenter.RemoteRegisterView;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class RemoteRegisterActivity extends BaseActivity implements RemoteRegisterView {

    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";

    public static final String EXTRA_DATA = "data";

    private AppSettingsHeaderFragment mAppSettingsHeaderFragment;
    private RemoteRegisterPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.presenter = new RemoteRegisterPresenter(getBaseContext());

        Intent intent = getIntent();
        Intent resultData = intent.getParcelableExtra(EXTRA_DATA);
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        byte[] packageSignature = intent.getByteArrayExtra(EXTRA_PACKAGE_SIGNATURE);

        presenter.setupFromIntent(resultData, packageName, packageSignature);
        presenter.setView(this);

        mAppSettingsHeaderFragment = (AppSettingsHeaderFragment) getSupportFragmentManager().findFragmentById(
                R.id.api_app_settings_fragment);
        // TODO unclean, fix later
        mAppSettingsHeaderFragment.setAppSettings(presenter.appSettings);

        setFullScreenDialogTwoButtons(
                R.string.api_register_allow, R.drawable.ic_check_white_24dp,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.onClickAllow();
                    }
                }, R.string.api_register_disallow, R.drawable.ic_close_white_24dp,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.onClickCancel();
                    }
                }
        );
    }

    @Override
    public void finishWithResult(Intent resultIntent) {
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void finishAsCancelled() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_register_app);
    }

}
