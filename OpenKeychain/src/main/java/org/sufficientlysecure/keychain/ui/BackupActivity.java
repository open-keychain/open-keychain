/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.Passphrase;


public class BackupActivity extends BaseActivity {

    public static final String EXTRA_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_PASSPHRASES = "passphrases";
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
            ParcelableHashMap<ParcelableLong, Passphrase> passphrases
                    = intent.getParcelableExtra(EXTRA_PASSPHRASES);

            Fragment frag = BackupCodeFragment.newInstance(
                    masterKeyIds, exportSecret, passphrases, true);

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
