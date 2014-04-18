/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.util.Log;

public class ImportKeysServerFragment extends Fragment {
    public static final String ARG_QUERY = "query";
    public static final String ARG_KEY_SERVER = "key_server";
    public static final String ARG_DISABLE_QUERY_EDIT = "disable_query_edit";

    private ImportKeysActivity mImportActivity;

    private BootstrapButton mSearchButton;
    private EditText mQueryEditText;
    private Spinner mServerSpinner;
    private ArrayAdapter<String> mServerAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysServerFragment newInstance(String query, String keyServer) {
        ImportKeysServerFragment frag = new ImportKeysServerFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putString(ARG_KEY_SERVER, keyServer);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_server_fragment, container, false);

        mSearchButton = (BootstrapButton) view.findViewById(R.id.import_server_search);
        mQueryEditText = (EditText) view.findViewById(R.id.import_server_query);
        mServerSpinner = (Spinner) view.findViewById(R.id.import_server_spinner);

        // add keyservers to spinner
        mServerAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, Preferences.getPreferences(getActivity())
                .getKeyServers());
        mServerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerSpinner.setAdapter(mServerAdapter);
        if (mServerAdapter.getCount() > 0) {
            mServerSpinner.setSelection(0);
        } else {
            mSearchButton.setEnabled(false);
        }

        mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = mQueryEditText.getText().toString();
                String keyServer = (String) mServerSpinner.getSelectedItem();
                search(query, keyServer);

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
                    String keyServer = (String) mServerSpinner.getSelectedItem();
                    search(query, keyServer);

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

                Log.d(Constants.TAG, "query: " + query);
            }

            if (getArguments().containsKey(ARG_KEY_SERVER)) {
                String keyServer = getArguments().getString(ARG_KEY_SERVER);
                int keyServerPos = mServerAdapter.getPosition(keyServer);
                mServerSpinner.setSelection(keyServerPos);

                Log.d(Constants.TAG, "keyServer: " + keyServer);
            }

            if (getArguments().getBoolean(ARG_DISABLE_QUERY_EDIT, false)) {
                mQueryEditText.setEnabled(false);
            }
        }
    }

    private void search(String query, String keyServer) {
        mImportActivity.loadCallback(null, null, query, keyServer, null);
    }

}
