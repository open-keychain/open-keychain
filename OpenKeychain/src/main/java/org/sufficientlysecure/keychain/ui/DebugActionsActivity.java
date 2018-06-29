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

package org.sufficientlysecure.keychain.ui;


import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.ApiApp;
import org.sufficientlysecure.keychain.provider.ApiAppDao;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.remote.ApiPendingIntentFactory;
import timber.log.Timber;


@TargetApi(VERSION_CODES.LOLLIPOP)
public class DebugActionsActivity extends Activity {
    private byte[] packageSig;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        packageSig = registerSelfAsApiApp();

        setContentView(createView());
    }

    private View createView() {
        Context context = getBaseContext();

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setPadding(0, 40, 0, 0);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("Debug Actions");
        verticalLayout.addView(toolbar, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ApiPendingIntentFactory pendingIntentFactory = new ApiPendingIntentFactory(context);
        addButtonToLayout(context, verticalLayout, "Select Public Key").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectPublicKeyPendingIntent(
                    new Intent(), new long[] {}, new ArrayList<>(), new ArrayList<>(), false);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key (legacy)").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdLegacyPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, "test@openkeychain.org");
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, packageSig, "test@openkeychain.org", false);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key (Autocrypt)").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, packageSig, "test@openkeychain.org", true);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Request Permission (first secret key)").setOnClickListener((v) -> {
            KeyRepository keyRepository = KeyRepository.create(getBaseContext());
            long firstMasterKeyId = keyRepository.getAllUnifiedKeyInfoWithSecret().get(0).master_key_id();
            PendingIntent pendingIntent = pendingIntentFactory.createRequestKeyPermissionPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, firstMasterKeyId);
            startPendingIntent(pendingIntent);
        });

        ScrollView view = new ScrollView(context);
        view.addView(verticalLayout, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return view;
    }

    private TextView addButtonToLayout(Context context, ViewGroup buttonContainer, String buttonLabel) {
        TextView button = new TextView(context, null, 0, R.style.DebugButton);
        button.setText(buttonLabel);
        buttonContainer.addView(button, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return button;
    }

    private byte[] registerSelfAsApiApp() {
        try {
            PackageManager packageManager = getPackageManager();
            ApiAppDao apiAppDao = ApiAppDao.getInstance(getBaseContext());
            @SuppressLint("PackageManagerGetSignatures")
            byte[] packageSig = packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES).signatures[0].toByteArray();
            apiAppDao.insertApiApp(ApiApp.create(BuildConfig.APPLICATION_ID, packageSig));
            return packageSig;
        } catch (NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private void startPendingIntent(PendingIntent pendingIntent) {
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(), 0, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (resultCode == RESULT_OK) {
                Timber.d("result: ok, intent: %s, extras: %s", data.toString(), data.getExtras());
            } else {
                Timber.d("result: cancelled, intent: %s, extras: %s", data.toString(), data.getExtras());
            }
        } else {
            if (resultCode == RESULT_OK) {
                Timber.d("result: ok, intent: null");
            } else {
                Timber.d("result: cancelled, intent: null");
            }
        }
    }
}
