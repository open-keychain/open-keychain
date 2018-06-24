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

package org.sufficientlysecure.keychain.ui.linked;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.keyview.UnifiedKeyInfoViewModel;
import timber.log.Timber;


public class LinkedIdWizard extends BaseActivity {
    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";

    public static final int FRAG_ACTION_START = 0;
    public static final int FRAG_ACTION_TO_RIGHT = 1;
    public static final int FRAG_ACTION_TO_LEFT = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.title_linked_id_create));

        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(EXTRA_MASTER_KEY_ID)) {
            Timber.e("Missing required extra master_key_id!");
            finish();
            return;
        }

        long masterKeyId = extras.getLong(EXTRA_MASTER_KEY_ID);
        UnifiedKeyInfoViewModel viewModel = ViewModelProviders.of(this).get(UnifiedKeyInfoViewModel.class);
        viewModel.setMasterKeyId(masterKeyId);
        viewModel.getUnifiedKeyInfoLiveData(this).observe(this, this::onLoadUnifiedKeyInfo);

        hideKeyboard();

        // pass extras into fragment
        if (savedInstanceState == null) {
            LinkedIdSelectFragment frag = LinkedIdSelectFragment.newInstance();
            loadFragment(frag, FRAG_ACTION_START);
        }
    }

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (!unifiedKeyInfo.has_any_secret()) {
            Timber.e("Linked Identities can only be added to secret keys!");
            finish();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_activity);
    }

    public void loadFragment(Fragment fragment, int action) {
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
        getSupportFragmentManager().executePendingTransactions();
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = getCurrentFocus();
        if (v == null || inputManager == null) {
            return;
        }

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
