/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.CryptoInputParcelCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.BackupActivity;
import org.sufficientlysecure.keychain.ui.BackupCodeFragment;

public class RemoteBackupActivity extends BackupActivity {

    public static final String EXTRA_DATA = "data";

    private Intent mPendingIntentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            boolean exportSecret = intent.getBooleanExtra(EXTRA_SECRET, false);
            long[] masterKeyIds = intent.getLongArrayExtra(EXTRA_MASTER_KEY_IDS);
            mPendingIntentData = getIntent().getParcelableExtra(EXTRA_DATA);
            // TODO: wip, need to put passphrases in here for secret key backup, null for now. look at remote passphrase dialog
            // NOTE: return backup!
            Fragment frag = BackupCodeFragment.newInstance(masterKeyIds, exportSecret, null, false);

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.content_frame, frag)
                    .commit();
        }

    }

    @Override
    public void handleBackupOperation(CryptoInputParcel inputParcel) {
        // instead of handling the operation here directly,
        // cache inputParcel containing the backup code and return to client
        // Next time, the actual operation is directly executed.
        CryptoInputParcelCacheService.addCryptoInputParcel(this, mPendingIntentData, inputParcel);
        setResult(RESULT_OK, mPendingIntentData);
        finish();
    }

}
