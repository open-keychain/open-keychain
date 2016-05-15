/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.util.Log;

public class SelectSignKeyIdActivity extends BaseActivity {

    public static final String EXTRA_USER_ID = OpenPgpApi.EXTRA_USER_ID;
    public static final String EXTRA_DATA = "data";

    private static final int REQUEST_CODE_CREATE_KEY = 0x00008884;

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

        TextView createKeyButton = (TextView) findViewById(R.id.api_select_sign_key_create_key);
        createKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey(mPreferredUserId);
            }
        });
        TextView noneButton = (TextView) findViewById(R.id.api_select_sign_key_none);
        noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);

                setResult(Activity.RESULT_OK, mData);
                finish();
            }
        });

        Intent intent = getIntent();
        Uri appUri = intent.getData();
        mPreferredUserId = intent.getStringExtra(EXTRA_USER_ID);
        mData = intent.getParcelableExtra(EXTRA_DATA);
        if (appUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + appUri);
            startListFragments(savedInstanceState, appUri, mData);
        }
    }

    private void createKey(String userId) {
        OpenPgpUtils.UserId userIdSplit = KeyRing.splitUserId(userId);

        Intent intent = new Intent(this, CreateKeyActivity.class);
        intent.putExtra(CreateKeyActivity.EXTRA_NAME, userIdSplit.name);
        intent.putExtra(CreateKeyActivity.EXTRA_EMAIL, userIdSplit.email);
        startActivityForResult(intent, REQUEST_CODE_CREATE_KEY);
    }

    private void startListFragments(Bundle savedInstanceState, Uri dataUri, Intent data) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragments
        SelectSignKeyIdListFragment listFragment = SelectSignKeyIdListFragment.newInstance(dataUri, data);
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
                        Log.e(Constants.TAG, "missing result!");
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
