/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.keychain.R;

public class SelectSecretKeyActivity extends ActionBarActivity {

    public static final String EXTRA_FILTER_CERTIFY = "filter_certify";

    public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";

    private boolean mFilterCertify;
    private SelectSecretKeyFragment mSelectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_secret_key_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        mFilterCertify = getIntent().getBooleanExtra(EXTRA_FILTER_CERTIFY, false);

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.select_secret_key_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mSelectFragment = SelectSecretKeyFragment.newInstance(mFilterCertify);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.select_secret_key_fragment_container, mSelectFragment).commit();
        }
    }

    /**
     * This is executed by SelectSecretKeyFragment after clicking on an item
     *
     * @param selectedUri
     */
    public void afterListSelection(Uri selectedUri) {
        Intent data = new Intent();
        data.setData(selectedUri);

        setResult(RESULT_OK, data);
        finish();
    }

}
