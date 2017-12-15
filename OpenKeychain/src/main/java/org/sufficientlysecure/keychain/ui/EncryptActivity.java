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
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class EncryptActivity extends BaseActivity {

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_ID";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_IDS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // preselect keys given by intent
            long signingKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
            long[] encryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

            Fragment modeFragment = EncryptModeAsymmetricFragment.newInstance(signingKeyId, encryptionKeyIds);
            transaction.replace(R.id.encrypt_mode_container, modeFragment);
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.check_use_symmetric: {
                item.setChecked(!item.isChecked());
                setModeFragment(item.isChecked());
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.encrypt_activity, menu);

        Fragment frag =
                getSupportFragmentManager().findFragmentById(R.id.encrypt_mode_container);
        boolean isSymmetric = frag instanceof EncryptModeSymmetricFragment;
        menu.findItem(R.id.check_use_symmetric).setChecked(isSymmetric);

        return super.onCreateOptionsMenu(menu);
    }

    private void setModeFragment(boolean symmetric) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.encrypt_mode_container,
                symmetric
                        ? EncryptModeSymmetricFragment.newInstance()
                        : EncryptModeAsymmetricFragment.newInstance(0, null)
        );

        // doesn't matter if the user doesn't look at the activity
        transaction.commitAllowingStateLoss();
    }

    public EncryptModeFragment getModeFragment() {
        return (EncryptModeFragment)
                getSupportFragmentManager().findFragmentById(R.id.encrypt_mode_container);
    }
}
