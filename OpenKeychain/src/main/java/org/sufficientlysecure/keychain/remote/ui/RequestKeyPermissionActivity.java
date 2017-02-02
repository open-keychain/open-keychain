/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper.WrongPackageCertificateException;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;


public class RequestKeyPermissionActivity extends BaseActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_REQUESTED_KEY_IDS = "requested_key_ids";

    private ViewAnimator viewAnimator;

    private String packageName;
    private View keyInfoLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);

        checkPackageAllowed(packageName);

        // Inflate a "Done" custom action bar
        setFullScreenDialogClose(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });

        ImageView iconClientApp = (ImageView) findViewById(R.id.icon_client_app);
        Drawable appIcon;
        CharSequence appName;
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            appIcon = packageManager.getApplicationIcon(applicationInfo);
            appName = packageManager.getApplicationLabel(applicationInfo);
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Unable to find info of calling app!");
            finish();
            return;
        }
        iconClientApp.setImageDrawable(appIcon);

        TextView titleText = (TextView) findViewById(R.id.select_identity_key_title);
        titleText.setText(getString(R.string.request_permission_title, appName));

        viewAnimator = (ViewAnimator) findViewById(R.id.status_animator);
        keyInfoLayout = findViewById(R.id.key_info_layout);

        long[] requestedMasterKeyIds = getIntent().getLongArrayExtra(EXTRA_REQUESTED_KEY_IDS);
        long masterKeyId = requestedMasterKeyIds[0];
//        long masterKeyId = 4817915339785265755L;
        try {
            CachedPublicKeyRing cachedPublicKeyRing = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);

            UserId userId = cachedPublicKeyRing.getSplitPrimaryUserIdWithFallback();
            displayKeyInfo(masterKeyId, userId);

            if (cachedPublicKeyRing.hasAnySecret()) {
                displayRequestKeyChoice(masterKeyId);
            } else {
                displayNoSecretKeyError();
            }
        } catch (PgpKeyNotFoundException e) {
            keyInfoLayout.setVisibility(View.GONE);
            displayUnknownSecretKeyError();
        }
    }

    private void displayKeyInfo(final long masterKeyId, UserId userId) {
        keyInfoLayout.setVisibility(View.VISIBLE);
        TextView keyUserIdView = (TextView) findViewById(R.id.select_key_item_name);
        keyUserIdView.setText(userId.name);

        findViewById(R.id.display_key).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RequestKeyPermissionActivity.this, ViewKeyActivity.class);
                intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
                startActivity(intent);
            }
        });
    }

    private void displayRequestKeyChoice(final long masterKeyId) {
        viewAnimator.setDisplayedChild(0);

        findViewById(R.id.button_allow).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                allowAndFinish(masterKeyId);
            }
        });

        findViewById(R.id.button_disallow).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    private void allowAndFinish(long masterKeyId) {
        new ApiDataAccessObject(this).addAllowedKeyIdForApp(packageName, masterKeyId);

        finish();
    }

    private void displayNoSecretKeyError() {
        viewAnimator.setDisplayedChild(1);

    }

    private void displayUnknownSecretKeyError() {
        viewAnimator.setDisplayedChild(2);

    }

    private void checkPackageAllowed(String packageName) {
        ApiDataAccessObject apiDao = new ApiDataAccessObject(this);
        ApiPermissionHelper apiPermissionHelper = new ApiPermissionHelper(this, apiDao);
        boolean packageAllowed;
        try {
            packageAllowed = apiPermissionHelper.isPackageAllowed(packageName);
        } catch (WrongPackageCertificateException e) {
            packageAllowed = false;
        }
        if (!packageAllowed) {
            throw new IllegalStateException("Pending intent launched by unknown app!");
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_request_key_permission);
    }

}
