/*
 * Copyright (C) 2017 Tobias Schülke
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class PrivateKeyExportActivity extends BaseActivity {
    public static String EXTRA_MASTER_KEY_ID = "master_key_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            long masterKeyId = intent.getLongExtra(EXTRA_MASTER_KEY_ID, 0);

            Fragment frag = PrivateKeyExportFragment.newInstance(masterKeyId);

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.content_frame, frag)
                    .commit();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.private_key_import_export_activity);
    }
}
