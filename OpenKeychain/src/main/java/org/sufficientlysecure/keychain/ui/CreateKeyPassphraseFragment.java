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
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.widget.PassphraseEditText;

public class CreateKeyPassphraseFragment extends Fragment {

    public static final String ARG_NAME = "name";
    public static final String ARG_EMAIL = "email";

    // model
    String mName;
    String mEmail;

    // view
    CreateKeyActivity mCreateKeyActivity;
    PassphraseEditText mPassphraseEdit;
    EditText mPassphraseEditAgain;
    CheckBox mShowPassphrase;
    View mBackButton;
    View mNextButton;

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyPassphraseFragment newInstance(String name, String email) {
        CreateKeyPassphraseFragment frag = new CreateKeyPassphraseFragment();

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

    private static boolean areEditTextsEqual(Context context, EditText editText1, EditText editText2) {
        boolean output = true;
        if (!editText1.getText().toString().equals(editText2.getText().toString())) {
            editText2.setError(context.getString(R.string.create_key_passphrases_not_equal));
            editText2.requestFocus();
            output = false;
        } else {
            editText2.setError(null);
        }

        return output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_passphrase_fragment, container, false);

        mPassphraseEdit = (PassphraseEditText) view.findViewById(R.id.create_key_passphrase);
        mPassphraseEditAgain = (EditText) view.findViewById(R.id.create_key_passphrase_again);
        mShowPassphrase = (CheckBox) view.findViewById(R.id.create_key_show_passphrase);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        // initial values
        mName = getArguments().getString(ARG_NAME);
        mEmail = getArguments().getString(ARG_EMAIL);
        mPassphraseEdit.requestFocus();
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKeyCheck();
            }
        });
        mShowPassphrase.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPassphraseEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    mPassphraseEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });


        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void back() {
        hideKeyboard();
        mCreateKeyActivity.loadFragment(null, null, FragAction.TO_LEFT);
    }

    private void createKeyCheck() {
        if (isEditTextNotEmpty(getActivity(), mPassphraseEdit)
                && areEditTextsEqual(getActivity(), mPassphraseEdit, mPassphraseEditAgain)) {

            CreateKeyFinalFragment frag =
                    CreateKeyFinalFragment.newInstance(
                            mName,
                            mEmail,
                            mPassphraseEdit.getText().toString()
                    );

            hideKeyboard();
            mCreateKeyActivity.loadFragment(null, frag, FragAction.TO_RIGHT);
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
