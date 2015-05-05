/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

public class DecryptFilesActivity extends BaseActivity {

    /* Intents */
    public static final String ACTION_DECRYPT_DATA = OpenKeychainIntents.DECRYPT_DATA;

    // intern
    public static final String ACTION_DECRYPT_DATA_OPEN = Constants.INTENT_PREFIX + "DECRYPT_DATA_OPEN";

    DecryptFilesFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }, false);

        // Handle intent actions
        handleActions(savedInstanceState, getIntent());
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.decrypt_files_activity);
    }

    /**
     * Handles all actions with this intent
     *
     * @param intent
     */
    private void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();

        Bundle mFileFragmentBundle = new Bundle();

        /*
         * Android's Action
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to Keychain Decrypt via share menu
            // Binary via content provider (could also be files)
            // override uri to get stream from send
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            action = ACTION_DECRYPT_DATA;
        } else if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)

            // override action
            action = ACTION_DECRYPT_DATA;
        }

        /**
         * Main Actions
         */
        if (ACTION_DECRYPT_DATA.equals(action) && uri != null) {
            mFileFragmentBundle.putParcelable(DecryptFilesFragment.ARG_URI, uri);

            loadFragment(savedInstanceState, uri, false);
        } else if (ACTION_DECRYPT_DATA_OPEN.equals(action)) {
            loadFragment(savedInstanceState, null, true);
        } else if (ACTION_DECRYPT_DATA.equals(action)) {
            Log.e(Constants.TAG,
                    "Include an Uri with setInputData() in your Intent!");
        }
    }

    private void loadFragment(Bundle savedInstanceState, Uri uri, boolean openDialog) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mFragment = DecryptFilesFragment.newInstance(uri, openDialog);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.decrypt_files_fragment_container, mFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
