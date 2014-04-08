/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;

public class SelectPublicKeyActivity extends ActionBarActivity {

    // Actions for internal use only:
    public static final String ACTION_SELECT_PUBLIC_KEYS = Constants.INTENT_PREFIX
            + "SELECT_PUBLIC_KEYRINGS";

    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "master_key_ids";

    public static final String RESULT_EXTRA_MASTER_KEY_IDS = "master_key_ids";
    public static final String RESULT_EXTRA_USER_IDS = "user_ids";

    SelectPublicKeyFragment mSelectFragment;

    long mSelectedMasterKeyIds[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        ActionBarHelper.setTwoButtonView(getSupportActionBar(), R.string.btn_okay, R.drawable.ic_action_done,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ok
                        okClicked();
                    }
                }, R.string.btn_do_not_save, R.drawable.ic_action_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                }
        );

        setContentView(R.layout.select_public_key_activity);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        handleIntent(getIntent());

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.select_public_key_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mSelectFragment = SelectPublicKeyFragment.newInstance(mSelectedMasterKeyIds);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.select_public_key_fragment_container, mSelectFragment).commit();
        }

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

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // TODO: reimplement search!

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

        // preselected master keys
        mSelectedMasterKeyIds = intent.getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void okClicked() {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_MASTER_KEY_IDS, mSelectFragment.getSelectedMasterKeyIds());
        data.putExtra(RESULT_EXTRA_USER_IDS, mSelectFragment.getSelectedUserIds());
        setResult(RESULT_OK, data);
        finish();
    }

}
