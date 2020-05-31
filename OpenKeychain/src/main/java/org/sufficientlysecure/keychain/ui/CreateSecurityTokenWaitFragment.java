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

package org.sufficientlysecure.keychain.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;

import de.cotech.sweetspot.NfcSweetspotData;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment;


public class CreateSecurityTokenWaitFragment extends Fragment {
    public static final int REQUEST_CODE_SWEETSPOT = 0;

    public static boolean sDisableFragmentAnimations = false;

    CreateKeyActivity mCreateKeyActivity;
    View mBackButton;

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (this.getActivity() instanceof BaseSecurityTokenActivity) {
            ((BaseSecurityTokenActivity) this.getActivity()).checkDeviceConnection();
        }

        setHasOptionsMenu(Constants.DEBUG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.token_debug, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_token_debug_uri:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugUri()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_keyserver:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugKeyserver()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_locked:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugLocked()), FragAction.TO_RIGHT);
                break;
            case R.id.menu_token_debug_locked_hard:
                mCreateKeyActivity.loadFragment(ManageSecurityTokenFragment.newInstance(
                        SecurityTokenInfo.newInstanceDebugLockedHard()), FragAction.TO_RIGHT);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_security_token_wait_fragment, container, false);

        boolean showLocateSweetspot = NfcSweetspotData.hasSweetspotData();
        View buttonLocateSweetspot = view.findViewById(R.id.button_locate_nfc);
        buttonLocateSweetspot.setVisibility(showLocateSweetspot ? View.VISIBLE : View.GONE);
        buttonLocateSweetspot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ShowNfcSweetspotActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SWEETSPOT);
            }
        });

        mBackButton = view.findViewById(R.id.create_key_back_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SWEETSPOT) {
            FragmentActivity activity = getActivity();
            if (activity instanceof BaseSecurityTokenActivity && data != null &&
                    data.hasExtra(ShowNfcSweetspotActivity.EXTRA_TOKEN_INFO)) {
                SecurityTokenInfo tokenInfo = data.getParcelableExtra(ShowNfcSweetspotActivity.EXTRA_TOKEN_INFO);
                ((CreateKeyActivity) activity).handleTokenInfo(tokenInfo);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    /**
     * hack from http://stackoverflow.com/a/11253987
     */
    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (sDisableFragmentAnimations) {
            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

}
