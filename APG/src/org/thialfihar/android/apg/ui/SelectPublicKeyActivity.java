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

package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class SelectPublicKeyActivity extends SherlockFragmentActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String ACTION_SELECT_PUBLIC_KEYS = Constants.INTENT_PREFIX
            + "SELECT_PUBLIC_KEYS";

    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "masterKeyIds";

    public static final String RESULT_EXTRA_MASTER_KEY_IDS = "masterKeyIds";
    public static final String RESULT_EXTRA_USER_IDS = "userIds";

    SelectPublicKeyFragment mSelectFragment;

    long selectedMasterKeyIds[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_public_key_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: reimplement!
        // menu.add(0, Id.menu.option.search, 0, R.string.menu_search).setIcon(
        // android.R.drawable.ic_menu_search);
        menu.add(1, Id.menu.option.cancel, 0, R.string.btn_doNotSave).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.okay, 1, R.string.btn_okay).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    /**
     * Menu Options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        case Id.menu.option.okay:
            okClicked();
            return true;

        case Id.menu.option.cancel:
            cancelClicked();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
