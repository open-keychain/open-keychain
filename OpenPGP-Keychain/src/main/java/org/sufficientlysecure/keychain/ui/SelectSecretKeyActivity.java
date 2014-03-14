/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;

public class SelectSecretKeyActivity extends ActionBarActivity {

    // Actions for internal use only:
    public static final String ACTION_SELECT_SECRET_KEY = Constants.INTENT_PREFIX
            + "SELECT_SECRET_KEYRING";

    public static final String EXTRA_FILTER_CERTIFY = "filter_certify";

    public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String RESULT_EXTRA_USER_ID = "user_id";

    private boolean mFilterCertify = false;
    private SelectSecretKeyFragment mSelectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_secret_key_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        // TODO: reimplement!
        // mFilterLayout = findViewById(R.id.layout_filter);
        // mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
        // mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);
        //
        // mClearFilterButton.setOnClickListener(new OnClickListener() {
        // public void onClick(View v) {
        // handleIntent(new Intent());
        // }
        // });

        mFilterCertify = getIntent().getBooleanExtra(EXTRA_FILTER_CERTIFY, false);

        handleIntent(getIntent());

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.select_secret_key_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mSelectFragment = SelectSecretKeyFragment.newInstance(mFilterCertify);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.select_secret_key_fragment_container, mSelectFragment).commit();
        }
    }

    /**
     * This is executed by SelectSecretKeyFragment after clicking on an item
     *
     * @param masterKeyId
     * @param userId
     */
    public void afterListSelection(long masterKeyId, String userId) {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_MASTER_KEY_ID, masterKeyId);
        data.putExtra(RESULT_EXTRA_USER_ID, (String) userId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // TODO: reimplement!

        // String searchString = null;
        // if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        // searchString = intent.getStringExtra(SearchManager.QUERY);
        // if (searchString != null && searchString.trim().length() == 0) {
        // searchString = null;
        // }
        // }

        // if (searchString == null) {
        // mFilterLayout.setVisibility(View.GONE);
        // } else {
        // mFilterLayout.setVisibility(View.VISIBLE);
        // mFilterInfo.setText(getString(R.string.filterInfo, searchString));
        // }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: reimplement!
        // menu.add(0, Id.menu.option.search, 0, R.string.menu_search).setIcon(
        // android.R.drawable.ic_menu_search);
        return true;
    }

}
