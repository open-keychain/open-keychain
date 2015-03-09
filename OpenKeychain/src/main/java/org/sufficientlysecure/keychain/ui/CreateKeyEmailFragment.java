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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;

public class CreateKeyEmailFragment extends Fragment {

    public static final String ARG_NAME = "name";
    public static final String ARG_EMAIL = "email";

    CreateKeyActivity mCreateKeyActivity;
    EmailEditText mEmailEdit;
    View mBackButton;
    View mNextButton;

    String mName;

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyEmailFragment newInstance(String name, String email) {
        CreateKeyEmailFragment frag = new CreateKeyEmailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_EMAIL, email);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().toString().length() == 0) {
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_email_fragment, container, false);

        mEmailEdit = (EmailEditText) view.findViewById(R.id.create_key_email);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        // initial values
        mName = getArguments().getString(ARG_NAME);
        String email = getArguments().getString(ARG_EMAIL);
        mEmailEdit.setText(email);

        // focus empty edit fields
        if (email == null) {
            mEmailEdit.requestFocus();
        }
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, null, FragAction.TO_LEFT);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKeyCheck();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void createKeyCheck() {
        if (isEditTextNotEmpty(getActivity(), mEmailEdit)) {

            CreateKeyPassphraseFragment frag =
                    CreateKeyPassphraseFragment.newInstance(
                            mName,
                            mEmailEdit.getText().toString()
                    );

            mCreateKeyActivity.loadFragment(null, frag, FragAction.TO_RIGHT);
        }
    }

}
