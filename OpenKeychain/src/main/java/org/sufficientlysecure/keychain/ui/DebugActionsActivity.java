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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.remote.ApiPendingIntentFactory;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import timber.log.Timber;


@TargetApi(VERSION_CODES.LOLLIPOP)
public class DebugActionsActivity extends Activity {

    private ApiPendingIntentFactory pendingIntentFactory;
    private KeyRepository keyRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Constants.DEBUG) {
            throw new UnsupportedOperationException();
        }

        pendingIntentFactory = new ApiPendingIntentFactory(getBaseContext());
        keyRepository = KeyRepository.create(getBaseContext());

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

        addButtonToLayout(context, verticalLayout, "Select Public Key").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectPublicKeyPendingIntent(
                    new Intent(), new long[] {}, new ArrayList<>(), new ArrayList<>(), false);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key (legacy)").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdLegacyPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, getPackageSig(), "test@openkeychain.org");
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, getPackageSig(), "test@openkeychain.org", false);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Select Signing Key (Autocrypt)").setOnClickListener((v) -> {
            PendingIntent pendingIntent = pendingIntentFactory.createSelectSignKeyIdPendingIntent(
                    new Intent(), BuildConfig.APPLICATION_ID, getPackageSig(), "test@openkeychain.org", true);
            startPendingIntent(pendingIntent);
        });
        addButtonToLayout(context, verticalLayout, "Deduplicate (dupl@mugenguild.com)").setOnClickListener((v) -> {
            ArrayList<String> duplicateEmails = new ArrayList<>();
            duplicateEmails.add("dupl@mugenguild.com");
            PendingIntent pendingIntent = pendingIntentFactory.createDeduplicatePendingIntent(
                    BuildConfig.APPLICATION_ID, new Intent(), duplicateEmails);
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

    @SuppressLint("PackageManagerGetSignatures")
    private byte[] getPackageSig() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo =
                    packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);
            return packageInfo.signatures[0].toByteArray();
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
                Notify.create(DebugActionsActivity.this, "Ok", Style.OK).show();
                Timber.d("result: ok, intent: %s, extras: %s", data.toString(), data.getExtras());
            } else {
                Timber.d("result: cancelled, intent: %s, extras: %s", data.toString(), data.getExtras());
            }
        } else {
            if (resultCode == RESULT_OK) {
                Notify.create(DebugActionsActivity.this, "Ok", Style.OK).show();
                Timber.d("result: ok, intent: null");
            } else {
                Timber.d("result: cancelled, intent: null");
            }
        }
    }
}
