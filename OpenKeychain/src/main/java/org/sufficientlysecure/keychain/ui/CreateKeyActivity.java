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
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.keychain.R;

public class CreateKeyActivity extends ActionBarActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";

    public static final int ANIM_NO = 0;
    public static final int ANIM_TO_RIGHT = 1;
    public static final int ANIM_TO_LEFT = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_key_activity);

        // pass extras into fragment
        CreateKeyInputFragment frag =
                CreateKeyInputFragment.newInstance(
                        getIntent().getStringExtra(EXTRA_NAME),
                        getIntent().getStringExtra(EXTRA_EMAIL)
                );
        loadFragment(null, frag, ANIM_NO);
    }

    public void loadFragment(Bundle savedInstanceState, Fragment fragment, int animation) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (animation) {
            case ANIM_NO:
                transaction.setCustomAnimations(0, 0);
                break;
            case ANIM_TO_LEFT:
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                break;
            case ANIM_TO_RIGHT:
                transaction.setCustomAnimations(R.anim.frag_slide_out_to_left, R.anim.frag_slide_in_from_right,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack("back");
                break;

        }
        transaction.replace(R.id.create_key_fragment_container, fragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
