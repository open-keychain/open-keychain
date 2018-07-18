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

package org.sufficientlysecure.keychain.remote.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import timber.log.Timber;


public class SelectSignKeyIdActivity extends BaseActivity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";
    public static final String EXTRA_USER_ID = OpenPgpApi.EXTRA_USER_ID;
    public static final String EXTRA_DATA = "data";

    protected static final int REQUEST_CODE_CREATE_KEY = 0x00008884;

    private String mPreferredUserId;
    private Intent mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar
        setFullScreenDialogClose(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });

        TextView noneButton = findViewById(R.id.api_select_sign_key_none);
        noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);

                setResult(Activity.RESULT_OK, mData);
                finish();
            }
        });

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        byte[] packageSignature = intent.getByteArrayExtra(EXTRA_PACKAGE_SIGNATURE);
        mPreferredUserId = intent.getStringExtra(EXTRA_USER_ID);
        mData = intent.getParcelableExtra(EXTRA_DATA);
        if (packageName == null) {
            Timber.e("Intent data missing. Should be Uri of app!");
            finish();
        } else {
            startListFragments(savedInstanceState, packageName, packageSignature, mData, mPreferredUserId);
        }
    }

    private void startListFragments(Bundle savedInstanceState, String packageName, byte[] packageSignature, Intent data,
            String preferredUserId) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragments
        SelectSignKeyIdListFragment listFragment = SelectSignKeyIdListFragment
                .newInstance(packageName, packageSignature, data, preferredUserId);
        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.api_select_sign_key_list_fragment, listFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_select_sign_key_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        }

        switch (requestCode) {
            case REQUEST_CODE_CREATE_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                        // TODO: select?
//                        EditKeyResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
//                        mSelectKeySpinner.setSelectedKeyId(result.mMasterKeyId);
                    } else {
                        Timber.e("missing result!");
                    }
                }
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

}
