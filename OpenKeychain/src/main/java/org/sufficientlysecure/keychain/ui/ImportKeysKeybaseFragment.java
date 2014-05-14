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
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

/**
 * Import public keys from the Keybase.io directory.  First cut: just raw search.
 *   TODO: make a pick list of the people you’re following on keybase
 */
public class ImportKeysKeybaseFragment extends Fragment {

    private ImportKeysActivity mImportActivity;
    private BootstrapButton mSearchButton;
    private EditText mQueryEditText;

    public static final String ARG_QUERY = "query";

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysKeybaseFragment newInstance() {
        ImportKeysKeybaseFragment frag = new ImportKeysKeybaseFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_keybase_fragment, container, false);

        mQueryEditText = (EditText) view.findViewById(R.id.import_keybase_query);

        mSearchButton = (BootstrapButton) view.findViewById(R.id.import_keybase_search);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = mQueryEditText.getText().toString();
                search(query);

                // close keyboard after pressing search
                InputMethodManager imm =
                        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mQueryEditText.getWindowToken(), 0);
            }
        });

        mQueryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = mQueryEditText.getText().toString();
                    search(query);

                    // Don't return true to let the keyboard close itself after pressing search
                    return false;
                }
                return false;
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();

        // set displayed values
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_QUERY)) {
                String query = getArguments().getString(ARG_QUERY);
                mQueryEditText.setText(query, TextView.BufferType.EDITABLE);
            }
        }
    }

    private void search(String query) {
        mImportActivity.loadCallback(null, null, null, null, query);
    }
}
