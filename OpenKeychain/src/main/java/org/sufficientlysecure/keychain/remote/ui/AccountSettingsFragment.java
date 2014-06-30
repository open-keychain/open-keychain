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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.ui.EditKeyActivity;
import org.sufficientlysecure.keychain.ui.SelectSecretKeyLayoutFragment;
import org.sufficientlysecure.keychain.ui.adapter.KeyValueSpinnerAdapter;
import org.sufficientlysecure.keychain.util.AlgorithmNames;
import org.sufficientlysecure.keychain.util.Log;

public class AccountSettingsFragment extends Fragment implements
        SelectSecretKeyLayoutFragment.SelectSecretKeyCallback {

    private static final int REQUEST_CODE_CREATE_KEY = 0x00008884;

    // model
    private AccountSettings mAccSettings;

    // view
    private TextView mAccNameView;
    private Spinner mEncryptionAlgorithm;
    private Spinner mHashAlgorithm;
    private Spinner mCompression;

    private SelectSecretKeyLayoutFragment mSelectKeyFragment;
    private Button mCreateKeyButton;

    KeyValueSpinnerAdapter mEncryptionAdapter;
    KeyValueSpinnerAdapter mHashAdapter;
    KeyValueSpinnerAdapter mCompressionAdapter;

    public AccountSettings getAccSettings() {
        return mAccSettings;
    }

    public void setAccSettings(AccountSettings accountSettings) {
        this.mAccSettings = accountSettings;

        mAccNameView.setText(accountSettings.getAccountName());
        mSelectKeyFragment.selectKey(accountSettings.getKeyId());
        mEncryptionAlgorithm.setSelection(mEncryptionAdapter.getPosition(accountSettings
                .getEncryptionAlgorithm()));
        mHashAlgorithm.setSelection(mHashAdapter.getPosition(accountSettings.getHashAlgorithm()));
        mCompression.setSelection(mCompressionAdapter.getPosition(accountSettings.getCompression()));
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

    /**
     * Set error String on key selection
     *
     * @param error
     */
    public void setErrorOnSelectKeyFragment(String error) {
        mSelectKeyFragment.setError(error);
    }

    private void initView(View view) {
        mSelectKeyFragment = (SelectSecretKeyLayoutFragment) getFragmentManager().findFragmentById(
                R.id.api_account_settings_select_key_fragment);
        mSelectKeyFragment.setCallback(this);

        mAccNameView = (TextView) view.findViewById(R.id.api_account_settings_acc_name);
        mEncryptionAlgorithm = (Spinner) view
                .findViewById(R.id.api_account_settings_encryption_algorithm);
        mHashAlgorithm = (Spinner) view.findViewById(R.id.api_account_settings_hash_algorithm);
        mCompression = (Spinner) view.findViewById(R.id.api_account_settings_compression);
        mCreateKeyButton = (Button) view.findViewById(R.id.api_account_settings_create_key);

        mCreateKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });

        AlgorithmNames algorithmNames = new AlgorithmNames(getActivity());

        mEncryptionAdapter = new KeyValueSpinnerAdapter(getActivity(),
                algorithmNames.getEncryptionNames());
        mEncryptionAlgorithm.setAdapter(mEncryptionAdapter);
        mEncryptionAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAccSettings.setEncryptionAlgorithm((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mHashAdapter = new KeyValueSpinnerAdapter(getActivity(), algorithmNames.getHashNames());
        mHashAlgorithm.setAdapter(mHashAdapter);
        mHashAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAccSettings.setHashAlgorithm((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mCompressionAdapter = new KeyValueSpinnerAdapter(getActivity(),
                algorithmNames.getCompressionNames());
        mCompression.setAdapter(mCompressionAdapter);
        mCompression.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAccSettings.setCompression((int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void createKey() {
        Intent intent = new Intent(getActivity(), EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
        // set default user id to account name
        intent.putExtra(EditKeyActivity.EXTRA_USER_IDS, mAccSettings.getAccountName());
        startActivityForResult(intent, REQUEST_CODE_CREATE_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATE_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    // select newly created key
                    try {
                        long masterKeyId = new ProviderHelper(getActivity())
                                .getCachedPublicKeyRing(data.getData())
                                .extractOrGetMasterKeyId();
                        mSelectKeyFragment.selectKey(masterKeyId);
                    } catch (PgpGeneralException e) {
                        Log.e(Constants.TAG, "key not found!", e);
                    }
                }
                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }

    /**
     * callback from select secret key fragment
     */
    @Override
    public void onKeySelected(long secretKeyId) {
        mAccSettings.setKeyId(secretKeyId);
    }

}
