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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferFragment;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class CreateKeyStartFragment extends Fragment {
    public static final int REQUEST_CODE_IMPORT_KEY = 0x00007012;


    CreateKeyActivity mCreateKeyActivity;

    View mCreateKey;
    View mImportKey;
    View mSecurityToken;
    TextView mSkipOrCancel;
    View mSecureDeviceSetup;


    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyStartFragment newInstance() {
        CreateKeyStartFragment frag = new CreateKeyStartFragment();

        Bundle args = new Bundle();

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_start_fragment, container, false);

        mCreateKey = view.findViewById(R.id.create_key_create_key_button);
        mImportKey = view.findViewById(R.id.create_key_import_button);
        mSecurityToken = view.findViewById(R.id.create_key_security_token_button);
        mSkipOrCancel = view.findViewById(R.id.create_key_cancel);
        mSecureDeviceSetup = view.findViewById(R.id.create_key_secure_device_setup);

        if (mCreateKeyActivity.mFirstTime) {
            mSkipOrCancel.setText(R.string.first_time_skip);
        } else {
            mSkipOrCancel.setText(R.string.btn_do_not_save);
        }

        mCreateKey.setOnClickListener(v -> {
            CreateKeyNameFragment frag = CreateKeyNameFragment.newInstance();
            mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
        });

        mSecurityToken.setOnClickListener(v -> {
            CreateSecurityTokenWaitFragment frag = new CreateSecurityTokenWaitFragment();
            mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
        });

        mImportKey.setOnClickListener(v -> {
            Intent intent = new Intent(mCreateKeyActivity, ImportKeysActivity.class);
            intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
            startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
        });

        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            mSecureDeviceSetup.setOnClickListener(v -> {
                TransferFragment frag = new TransferFragment();
                mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
            });
        } else {
            mSecureDeviceSetup.setVisibility(View.GONE);
        }

        mSkipOrCancel.setOnClickListener(v -> {
            if (!mCreateKeyActivity.mFirstTime) {
                mCreateKeyActivity.setResult(Activity.RESULT_CANCELED);
            }
            mCreateKeyActivity.finish();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMPORT_KEY) {
            if (resultCode == Activity.RESULT_OK) {
                if (mCreateKeyActivity.mFirstTime) {
                    Preferences prefs = Preferences.getPreferences(mCreateKeyActivity);
                    prefs.setFirstTime(false);
                    mCreateKeyActivity.finish();
                } else {
                    // just finish activity and return data
                    mCreateKeyActivity.setResult(Activity.RESULT_OK, data);
                    mCreateKeyActivity.finish();
                }
            }
        } else {
            Timber.e("No valid request code!");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

}
