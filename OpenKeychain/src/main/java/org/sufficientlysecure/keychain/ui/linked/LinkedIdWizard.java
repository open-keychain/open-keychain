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

package org.sufficientlysecure.keychain.ui.linked;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

public class LinkedIdWizard extends BaseActivity {

    public static final int FRAG_ACTION_START = 0;
    public static final int FRAG_ACTION_TO_RIGHT = 1;
    public static final int FRAG_ACTION_TO_LEFT = 2;

    long mMasterKeyId;
    byte[] mFingerprint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.title_linked_id_create));

        try {
            Uri uri = getIntent().getData();
            uri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(uri);
            CachedPublicKeyRing ring = new ProviderHelper(this).mReader.getCachedPublicKeyRing(uri);
            if (!ring.hasAnySecret()) {
                Log.e(Constants.TAG, "Linked Identities can only be added to secret keys!");
                finish();
                return;
            }

            mMasterKeyId = ring.extractOrGetMasterKeyId();
            mFingerprint = ring.getFingerprint();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "Invalid uri given, key does not exist!");
            finish();
            return;
        }

        // pass extras into fragment
        LinkedIdSelectFragment frag = LinkedIdSelectFragment.newInstance();
        loadFragment(null, frag, FRAG_ACTION_START);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_activity);
    }

    public void loadFragment(Bundle savedInstanceState, Fragment fragment, int action) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        hideKeyboard();

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (action) {
            case FRAG_ACTION_START:
                transaction.setCustomAnimations(0, 0);
                transaction.replace(R.id.create_key_fragment_container, fragment)
                        .commitAllowingStateLoss();
                break;
            case FRAG_ACTION_TO_LEFT:
                getSupportFragmentManager().popBackStackImmediate();
                break;
            case FRAG_ACTION_TO_RIGHT:
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right, R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.create_key_fragment_container, fragment)
                        .commitAllowingStateLoss();
                break;

        }
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            navigateBack();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                navigateBack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateBack() {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.setData(KeyRings.buildGenericKeyRingUri(mMasterKeyId));
        // This activity is NOT part of this app's task, so create a new task
        // when navigating up, with a synthesized back stack.
        TaskStackBuilder.create(this)
                // Add all of this activity's parents to the back stack
                .addNextIntentWithParentStack(upIntent)
                // Navigate up to the closest parent
                .startActivities();
    }

}
