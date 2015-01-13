/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
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

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.util.ActionBarHelper;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.Log;

public class ViewKeyAdvancedActivity extends BaseActivity {

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(this);
        mProviderHelper = new ProviderHelper(this);

        // Inflate a "Done" custom action bar
        ActionBarHelper.setOneButtonView(getSupportActionBar(),
                R.string.btn_okay, R.drawable.ic_action_done,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        finish();
                    }
                }
        );

        Uri dataUri = getIntent().getData();
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be uri of key!");
            finish();
            return;
        }

        Log.i(Constants.TAG, "mDataUri: " + dataUri.toString());

        startFragment(savedInstanceState, dataUri);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_advanced_activity);
    }

    private void startFragment(Bundle savedInstanceState, Uri dataUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        ViewKeyAdvancedFragment frag = ViewKeyAdvancedFragment.newInstance(dataUri);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.view_key_advanced_fragment, frag)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
