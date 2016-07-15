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
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.HashMap;

public class RemoteBackupActivity extends BackupActivity {

    public static final String EXTRA_DATA = "data";

    private Intent mPendingIntentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mPendingIntentData = getIntent().getParcelableExtra(EXTRA_DATA);
        }
    }

    @Override
    public void handleBackupOperation(Passphrase symmetricPassphrase, HashMap<Long, Passphrase> keyRingPassphrases) {
        // instead of handling the operation here directly,
        // cache the backup code & keyring passphrases and return to client
        // Next time, the actual operation is directly executed.
        CryptoInputParcelCacheService.addCryptoInputParcel(this,
                mPendingIntentData,
                new CryptoInputParcel(symmetricPassphrase, keyRingPassphrases));
        setResult(RESULT_OK, mPendingIntentData);
        finish();
    }

    @Override
    public void showBackupCodeFragment(long[] masterKeyIds, boolean exportSecret, HashMap<Long, Passphrase> passphrases) {
        Fragment frag = BackupCodeFragment.newInstance(masterKeyIds, exportSecret, passphrases, false);
        FragmentManager fragMan = getSupportFragmentManager();
        fragMan.beginTransaction()
                .setCustomAnimations(0, 0)
                .add(R.id.content_frame, frag)
                .commit();
    }
}
