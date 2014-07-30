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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;

import java.util.regex.Matcher;

public class CreateKeyInputFragment extends Fragment {

    CreateKeyActivity mCreateKeyActivity;

    AutoCompleteTextView mNameEdit;
    AutoCompleteTextView mEmailEdit;
    EditText mPassphraseEdit;
    EditText mPassphraseEditAgain;
    View mCreateButton;

    public static final String ARG_NAME = "name";
    public static final String ARG_EMAIL = "email";

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyInputFragment newInstance(String name, String email) {
        CreateKeyInputFragment frag = new CreateKeyInputFragment();

        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_EMAIL, email);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_input_fragment, container, false);

        mNameEdit = (AutoCompleteTextView) view.findViewById(R.id.name);
        mEmailEdit = (AutoCompleteTextView) view.findViewById(R.id.email);
        mPassphraseEdit = (EditText) view.findViewById(R.id.passphrase);
        mPassphraseEditAgain = (EditText) view.findViewById(R.id.passphrase_again);
        mCreateButton = view.findViewById(R.id.create_key_button);

        // initial values
        String name = getArguments().getString(ARG_NAME);
        String email = getArguments().getString(ARG_EMAIL);
        mNameEdit.setText(name);
        mEmailEdit.setText(email);

        // focus non-empty edit fields
        if (name != null && email != null) {
            mPassphraseEdit.requestFocus();
        } else if (name != null) {
            mEmailEdit.requestFocus();
        }

        mEmailEdit.setThreshold(1); // Start working from first character
        mEmailEdit.setAdapter(
                new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserEmails(getActivity())
                        )
        );
        mEmailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = editable.toString();
                if (email.length() > 0) {
                    Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
                    if (emailMatcher.matches()) {
                        mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_ok, 0);
                    } else {
                        mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });

        mNameEdit.setThreshold(1); // Start working from first character
        mNameEdit.setAdapter(
                new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserNames(getActivity())
                        )
        );

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKeyCheck();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void createKeyCheck() {
        if (isEditTextNotEmpty(getActivity(), mNameEdit)
                && isEditTextNotEmpty(getActivity(), mEmailEdit)
                && isEditTextNotEmpty(getActivity(), mPassphraseEdit)
                && areEditTextsEqual(getActivity(), mPassphraseEdit, mPassphraseEditAgain)) {

            CreateKeyFinalFragment frag =
                    CreateKeyFinalFragment.newInstance(
                            mNameEdit.getText().toString(),
                            mEmailEdit.getText().toString(),
                            mPassphraseEdit.getText().toString()
                    );

            hideKeyboard();
            mCreateKeyActivity.loadFragment(null, frag, CreateKeyActivity.ANIM_TO_RIGHT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        //check if no view has focus:
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
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

}
