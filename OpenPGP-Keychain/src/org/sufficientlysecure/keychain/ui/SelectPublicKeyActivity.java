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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SelectPublicKeyActivity extends SherlockFragmentActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String ACTION_SELECT_PUBLIC_KEYS = Constants.INTENT_PREFIX
            + "SELECT_PUBLIC_KEYRINGS";

    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "masterKeyIds";

    public static final String RESULT_EXTRA_MASTER_KEY_IDS = "masterKeyIds";
    public static final String RESULT_EXTRA_USER_IDS = "userIds";

    SelectPublicKeyFragment mSelectFragment;

    long selectedMasterKeyIds[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_cancel, null);

        ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text))
                .setText(R.string.btn_okay);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ok
                        okClicked();
                    }
                });
        ((TextView) customActionBarView.findViewById(R.id.actionbar_cancel_text))
                .setText(R.string.btn_doNotSave);
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(R.layout.select_public_key_activity);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mSelectFragment = (SelectPublicKeyFragment) getSupportFragmentManager().findFragmentById(
                R.id.select_public_key_fragment);

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

        handleIntent(getIntent());
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

        // preselected master keys
        selectedMasterKeyIds = intent.getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);
    }

    /**
     * returns preselected key ids, this is used in the fragment
     * 
     * @return
     */
    public long[] getSelectedMasterKeyIds() {
        return selectedMasterKeyIds;
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
