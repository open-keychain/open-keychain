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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.widget.NameEditText;

public class CreateKeyNameFragment extends Fragment {

    CreateKeyActivity mCreateKeyActivity;
    NameEditText mNameEdit;
    View mBackButton;
    View mNextButton;

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyNameFragment newInstance() {
        CreateKeyNameFragment frag = new CreateKeyNameFragment();

        Bundle args = new Bundle();

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_name_fragment, container, false);

        mNameEdit = (NameEditText) view.findViewById(R.id.create_key_name);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        // initial values
        mNameEdit.setText(mCreateKeyActivity.mName);

        // focus empty edit fields
        if (mCreateKeyActivity.mName == null) {
            mNameEdit.requestFocus();
        }
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
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
        // save state
        mCreateKeyActivity.mName = mNameEdit.getText().length() == 0 ? null : mNameEdit.getText().toString();

        CreateKeyEmailFragment frag = CreateKeyEmailFragment.newInstance();
        mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
    }

}
