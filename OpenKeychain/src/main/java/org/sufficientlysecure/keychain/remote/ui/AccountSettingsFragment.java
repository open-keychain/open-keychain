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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.ui.widget.SignKeySpinner;
import org.sufficientlysecure.keychain.util.Log;

public class AccountSettingsFragment extends Fragment {

    private static final int REQUEST_CODE_CREATE_KEY = 0x00008884;

    // model
    private AccountSettings mAccSettings;

    // view
    private TextView mAccNameView;

    private SignKeySpinner mSelectKeySpinner;
    private View mCreateKeyButton;

    public AccountSettings getAccSettings() {
        return mAccSettings;
    }

    public void setAccSettings(AccountSettings accountSettings) {
        this.mAccSettings = accountSettings;

        mAccNameView.setText(accountSettings.getAccountName());
        mSelectKeySpinner.setPreSelectedKeyId(accountSettings.getKeyId());
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.api_account_settings_fragment, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mSelectKeySpinner = (SignKeySpinner) view.findViewById(R.id.api_account_settings_key_spinner);
        mAccNameView = (TextView) view.findViewById(R.id.api_account_settings_acc_name);
        mCreateKeyButton = view.findViewById(R.id.api_account_settings_create_key);

        mSelectKeySpinner.setOnKeyChangedListener(new KeySpinner.OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                mAccSettings.setKeyId(masterKeyId);
            }
        });

        mCreateKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });
    }

    private void createKey() {
        KeyRing.UserId userId = KeyRing.splitUserId(mAccSettings.getAccountName());

        Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
        intent.putExtra(CreateKeyActivity.EXTRA_NAME, userId.name);
        intent.putExtra(CreateKeyActivity.EXTRA_EMAIL, userId.email);
        startActivityForResult(intent, REQUEST_CODE_CREATE_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATE_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                        EditKeyResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                        mSelectKeySpinner.setPreSelectedKeyId(result.mMasterKeyId);
                    } else {
                        Log.e(Constants.TAG, "missing result!");
                    }
                }
                break;
            }
        }

        // execute activity's onActivityResult to show log notify
        super.onActivityResult(requestCode, resultCode, data);
    }
}
