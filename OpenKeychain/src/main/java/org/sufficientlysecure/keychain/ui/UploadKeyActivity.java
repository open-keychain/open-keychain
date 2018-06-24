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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * Sends the selected public key to a keyserver
 */
public class UploadKeyActivity extends BaseActivity
        implements CryptoOperationHelper.Callback<UploadKeyringParcel, UploadResult> {
    public static final String EXTRA_KEY_IDS = "extra_key_ids";

    private Spinner mKeyServerSpinner;

    // CryptoOperationHelper.Callback vars
    private HkpKeyserverAddress mKeyserver;
    private CryptoOperationHelper<UploadKeyringParcel, UploadResult> mUploadOpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View uploadButton = findViewById(R.id.upload_key_action_upload);
        mKeyServerSpinner = findViewById(R.id.upload_key_keyserver);

        MultiUserIdsFragment mMultiUserIdsFragment = (MultiUserIdsFragment)
                getSupportFragmentManager().findFragmentById(R.id.multi_user_ids_fragment);
        mMultiUserIdsFragment.setCheckboxVisibility(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getKeyserversArray()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeyServerSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mKeyServerSpinner.setSelection(0);
        } else {
            uploadButton.setEnabled(false);
        }

        uploadButton.setOnClickListener(v -> uploadKey());
    }

    private String[] getKeyserversArray() {
        ArrayList<HkpKeyserverAddress> keyservers = Preferences.getPreferences(this)
                .getKeyServers();
        String[] keyserversArray = new String[keyservers.size()];
        int i = 0;
        for (HkpKeyserverAddress k : keyservers) {
            keyserversArray[i] = k.getUrl();
            i++;
        }
        return keyserversArray;
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.upload_key_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mUploadOpHelper != null) {
            mUploadOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadKey() {
        String keyserverUrl = (String) mKeyServerSpinner.getSelectedItem();
        // TODO: Currently, not using onion addresses here!
        mKeyserver = HkpKeyserverAddress.createFromUri(keyserverUrl);

        mUploadOpHelper = new CryptoOperationHelper<>(1, this, this, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

    @Override
    public UploadKeyringParcel createOperationInput() {
        long[] masterKeyIds = getIntent().getLongArrayExtra(EXTRA_KEY_IDS);
        return UploadKeyringParcel.createWithKeyId(mKeyserver, masterKeyIds[0]);
    }

    @Override
    public void onCryptoOperationSuccess(UploadResult result) {
        result.createNotify(this).show();
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(UploadResult result) {
        result.createNotify(this).show();
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }
}
