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

package org.sufficientlysecure.keychain.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class FirstTimeActivity extends BaseActivity {

    public static final String FRAGMENT_TAG = "currentFragment";

    Fragment mCurrentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state

            mCurrentFragment = getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {

            // Add the sync fragment
            SettingsActivity.SyncPrefsFragment frag = new SettingsActivity.SyncPrefsFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(0, 0);
            transaction.replace(R.id.first_time_fragment_container, frag, FRAGMENT_TAG)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
        }

        setTitle(R.string.app_name);
        mToolbar.setNavigationIcon(null);
        mToolbar.setNavigationOnClickListener(null);

        View nextButton = findViewById(R.id.first_time_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstTimeActivity.this,
                        CreateKeyActivity.class);
                intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.first_time_activity);
    }
}
