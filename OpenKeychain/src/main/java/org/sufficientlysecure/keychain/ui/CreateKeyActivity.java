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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;

public class CreateKeyActivity extends BaseActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_FIRST_TIME = "first_time";
    public static final String EXTRA_ADDITIONAL_EMAILS = "additional_emails";
    public static final String EXTRA_PASSPHRASE = "passphrase";

    public static final String FRAGMENT_TAG = "currentFragment";

    String mName;
    String mEmail;
    ArrayList<String> mAdditionalEmails;
    Passphrase mPassphrase;
    boolean mFirstTime;

    Fragment mCurrentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mName = savedInstanceState.getString(EXTRA_NAME);
            mEmail = savedInstanceState.getString(EXTRA_EMAIL);
            mAdditionalEmails = savedInstanceState.getStringArrayList(EXTRA_ADDITIONAL_EMAILS);
            mPassphrase = savedInstanceState.getParcelable(EXTRA_PASSPHRASE);
            mFirstTime = savedInstanceState.getBoolean(EXTRA_FIRST_TIME);

            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {
            // Initialize members with default values for a new instance
            mName = getIntent().getStringExtra(EXTRA_NAME);
            mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
            mFirstTime = getIntent().getBooleanExtra(EXTRA_FIRST_TIME, false);

            // Start with first fragment of wizard
            CreateKeyStartFragment frag = CreateKeyStartFragment.newInstance();
            loadFragment(frag, FragAction.START);
        }

        if (mFirstTime) {
            setTitle(R.string.app_name);
            setActionBarIcon(R.drawable.ic_launcher);
        } else {
            setTitle(R.string.title_manage_my_keys);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_NAME, mName);
        outState.putString(EXTRA_EMAIL, mEmail);
        outState.putStringArrayList(EXTRA_ADDITIONAL_EMAILS, mAdditionalEmails);
        outState.putParcelable(EXTRA_PASSPHRASE, mPassphrase);
        outState.putBoolean(EXTRA_FIRST_TIME, mFirstTime);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_activity);
    }

    public static enum FragAction {
        START,
        TO_RIGHT,
        TO_LEFT
    }

    public void loadFragment(Fragment fragment, FragAction action) {
        mCurrentFragment = fragment;

        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (action) {
            case START:
                transaction.setCustomAnimations(0, 0);
                transaction.replace(R.id.create_key_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;
            case TO_LEFT:
                getSupportFragmentManager().popBackStackImmediate();
                break;
            case TO_RIGHT:
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right, R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.create_key_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;

        }
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
