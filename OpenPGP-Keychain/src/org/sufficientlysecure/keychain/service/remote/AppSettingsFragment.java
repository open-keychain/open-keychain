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

package org.sufficientlysecure.keychain.service.remote;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.SelectSecretKeyActivity;
import org.sufficientlysecure.keychain.ui.adapter.KeyValueSpinnerAdapter;
import org.sufficientlysecure.keychain.util.AlgorithmNames;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class AppSettingsFragment extends Fragment {

    // model
    private AppSettings appSettings;

    // view
    private LinearLayout mAdvancedSettingsContainer;
    private Button mAdvancedSettingsButton;
    private TextView mAppNameView;
    private ImageView mAppIconView;
    private TextView mKeyUserId;
    private TextView mKeyUserIdRest;
    private Button mSelectKeyButton;
    private Spinner mEncryptionAlgorithm;
    private Spinner mHashAlgorithm;
    private Spinner mCompression;

    KeyValueSpinnerAdapter encryptionAdapter;
    KeyValueSpinnerAdapter hashAdapter;
    KeyValueSpinnerAdapter compressionAdapter;

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(AppSettings appSettings) {
        this.appSettings = appSettings;
        setPackage(appSettings.getPackageName());
        updateSelectedKeyView(appSettings.getKeyId());
        mEncryptionAlgorithm.setSelection(encryptionAdapter.getPosition(appSettings
                .getEncryptionAlgorithm()));
        mHashAlgorithm.setSelection(hashAdapter.getPosition(appSettings.getHashAlgorithm()));
        mCompression.setSelection(compressionAdapter.getPosition(appSettings.getCompression()));
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.api_app_settings_fragment, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mAdvancedSettingsButton = (Button) view.findViewById(R.id.api_app_settings_advanced_button);
        mAdvancedSettingsContainer = (LinearLayout) view
                .findViewById(R.id.api_app_settings_advanced);

        mAppNameView = (TextView) view.findViewById(R.id.api_app_settings_app_name);
        mAppIconView = (ImageView) view.findViewById(R.id.api_app_settings_app_icon);
        mKeyUserId = (TextView) view.findViewById(R.id.api_app_settings_user_id);
        mKeyUserIdRest = (TextView) view.findViewById(R.id.api_app_settings_user_id_rest);
        mSelectKeyButton = (Button) view.findViewById(R.id.api_app_settings_select_key_button);
        mEncryptionAlgorithm = (Spinner) view
                .findViewById(R.id.api_app_settings_encryption_algorithm);
        mHashAlgorithm = (Spinner) view.findViewById(R.id.api_app_settings_hash_algorithm);
        mCompression = (Spinner) view.findViewById(R.id.api_app_settings_compression);

        AlgorithmNames algorithmNames = new AlgorithmNames(getActivity());

        encryptionAdapter = new KeyValueSpinnerAdapter(getActivity(),
                algorithmNames.getEncryptionNames());
        mEncryptionAlgorithm.setAdapter(encryptionAdapter);
        mEncryptionAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                appSettings.setEncryptionAlgorithm((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        hashAdapter = new KeyValueSpinnerAdapter(getActivity(), algorithmNames.getHashNames());
        mHashAlgorithm.setAdapter(hashAdapter);
        mHashAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                appSettings.setHashAlgorithm((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        compressionAdapter = new KeyValueSpinnerAdapter(getActivity(),
                algorithmNames.getCompressionNames());
        mCompression.setAdapter(compressionAdapter);
        mCompression.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                appSettings.setCompression((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSelectKeyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectSecretKey();
            }
        });

        final Animation visibleAnimation = new AlphaAnimation(0.0f, 1.0f);
        visibleAnimation.setDuration(250);
        final Animation invisibleAnimation = new AlphaAnimation(1.0f, 0.0f);
        invisibleAnimation.setDuration(250);

        // TODO: Better: collapse/expand animation
        // final Animation animation2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        // Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        // Animation.RELATIVE_TO_SELF, 0.0f);
        // animation2.setDuration(150);

        mAdvancedSettingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mAdvancedSettingsContainer.getVisibility() == View.VISIBLE) {
                    mAdvancedSettingsContainer.startAnimation(invisibleAnimation);
                    mAdvancedSettingsContainer.setVisibility(View.INVISIBLE);
                    mAdvancedSettingsButton.setText(R.string.api_settings_show_advanced);
                } else {
                    mAdvancedSettingsContainer.startAnimation(visibleAnimation);
                    mAdvancedSettingsContainer.setVisibility(View.VISIBLE);
                    mAdvancedSettingsButton.setText(R.string.api_settings_hide_advanced);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(getActivity(), SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    private void setPackage(String packageName) {
        PackageManager pm = getActivity().getApplicationContext().getPackageManager();

        // get application name and icon from package manager
        String appName = null;
        Drawable appIcon = null;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

            appName = (String) pm.getApplicationLabel(ai);
            appIcon = pm.getApplicationIcon(ai);
        } catch (final NameNotFoundException e) {
            // fallback
            appName = packageName;
        }
        mAppNameView.setText(appName);
        mAppIconView.setImageDrawable(appIcon);
    }

    private void updateSelectedKeyView(long secretKeyId) {
        if (secretKeyId == Id.key.none) {
            mKeyUserId.setText(R.string.api_settings_no_key);
            mKeyUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                    getActivity(), secretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpKeyHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PgpKeyHelper.getMainUserIdSafe(getActivity(), key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mKeyUserId.setText(uid);
            mKeyUserIdRest.setText(uidExtra);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Constants.TAG, "onactivityresult     " + requestCode + "   " + resultCode);
        switch (requestCode) {

        case Id.request.secret_keys: {
            long secretKeyId;
            if (resultCode == Activity.RESULT_OK) {
                Bundle bundle = data.getExtras();
                secretKeyId = bundle.getLong(SelectSecretKeyActivity.RESULT_EXTRA_MASTER_KEY_ID);

            } else {
                secretKeyId = Id.key.none;
            }
            appSettings.setKeyId(secretKeyId);
            updateSelectedKeyView(secretKeyId);
            break;
        }

        default: {
            break;
        }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
