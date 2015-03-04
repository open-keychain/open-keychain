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

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class CertifyFingerprintActivity extends BaseActivity {

    protected Uri mDataUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be uri of key!");
            finish();
            return;
        }

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        startFragment(savedInstanceState, mDataUri);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.certify_fingerprint_activity);
        changeToolbarColor();
    }

    private void startFragment(Bundle savedInstanceState, Uri dataUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        CertifyFingerprintFragment frag = CertifyFingerprintFragment.newInstance(dataUri);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.certify_fingerprint_fragment, frag)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    /**
     * Changes the color of our ToolBar.
     *
     * Currently Set to ORANGE
     */
    private void changeToolbarColor() {
        RelativeLayout mToolBarInclude = (RelativeLayout) findViewById(R.id.toolbar_include);

        // Changes the color of the Status Bar strip
        ImageView mStatusBar = (ImageView) mToolBarInclude.findViewById(R.id.status_bar);
        mStatusBar.setBackgroundResource(getResources().getColor(R.color.android_orange_dark));

        // Changes the color of our Tool Bar
        Toolbar toolbar = (Toolbar) mToolBarInclude.findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(getResources().getColor(R.color.android_orange_light));
    }

}
