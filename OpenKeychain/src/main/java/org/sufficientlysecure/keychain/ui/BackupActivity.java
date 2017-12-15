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


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class BackupActivity extends BaseActivity {

    public static final String EXTRA_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_SECRET = "export_secret";

    @Override
    protected void initLayout() {
        setContentView(R.layout.backup_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            boolean exportSecret = intent.getBooleanExtra(EXTRA_SECRET, false);
            long[] masterKeyIds = intent.getLongArrayExtra(EXTRA_MASTER_KEY_IDS);

            Fragment frag = BackupCodeFragment.newInstance(masterKeyIds, exportSecret, true);

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                .setCustomAnimations(0, 0)
                .replace(R.id.content_frame, frag)
                .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragMan = getSupportFragmentManager();
                // pop from back stack, or if nothing was on there finish activity
                if ( ! fragMan.popBackStackImmediate()) {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Overridden in RemoteBackupActivity
     */
    public void handleBackupOperation(CryptoInputParcel inputParcel) {
        // only used for RemoteBackupActivity
    }

}
