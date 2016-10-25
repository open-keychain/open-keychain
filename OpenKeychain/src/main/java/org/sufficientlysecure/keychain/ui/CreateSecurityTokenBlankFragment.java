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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;

public class CreateSecurityTokenBlankFragment extends Fragment {

    CreateKeyActivity mCreateKeyActivity;
    View mBackButton;
    View mNextButton;

    private byte[] mAid;

    /**
     * Creates new instance of this fragment
     */
    public static CreateSecurityTokenBlankFragment newInstance(byte[] aid) {
        CreateSecurityTokenBlankFragment frag = new CreateSecurityTokenBlankFragment();

        Bundle args = new Bundle();

        frag.mAid = aid;
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_yubi_key_blank_fragment, container, false);

        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                } else {
                    mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
                }
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClicked();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void nextClicked() {
        mCreateKeyActivity.mCreateSecurityToken = true;
        mCreateKeyActivity.mSecurityTokenAid = mAid;

        CreateKeyNameFragment frag = CreateKeyNameFragment.newInstance();
        mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
    }

}
