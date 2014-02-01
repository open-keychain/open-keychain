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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class ImportKeysServerFragment extends Fragment {
    public static final String ARG_QUERY = "query";

    private ImportKeysActivity mImportActivity;

    private BootstrapButton mOldButton;

    private BootstrapButton mSearchButton;
    private EditText mQueryEditText;
    private Spinner mServerSpinner;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysServerFragment newInstance(String query) {
        ImportKeysServerFragment frag = new ImportKeysServerFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_server_fragment, container, false);

        mSearchButton = (BootstrapButton) view.findViewById(R.id.import_server_button);
        mQueryEditText = (EditText) view.findViewById(R.id.import_server_query);
        mServerSpinner = (Spinner) view.findViewById(R.id.import_server_spinner);

        // add keyservers to spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, Preferences.getPreferences(getActivity())
                .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mServerSpinner.setSelection(0);
        } else {
            mSearchButton.setEnabled(false);
        }

        mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = mQueryEditText.getText().toString();

            }
        });
        // TODO: not supported by BootstrapButton
//        mSearchButton.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
//                    String query = mQueryEditText.getText().toString();
//                    search(query);
//                    // FIXME This is a hack to hide a keyboard after search
//                    // http://tinyurl.com/pwdc3q9
//                    return false;
//                }
//                return false;
//            }
//        });

        // TODO: remove:
        mOldButton = (BootstrapButton) view.findViewById(R.id.import_server_button);
        mOldButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), KeyServerQueryActivity.class), 0);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();

        // if query has been set on instantiation, search immediately!
        if (getArguments() != null && getArguments().containsKey(ARG_QUERY)) {
            String query = getArguments().getString(ARG_QUERY);
            mQueryEditText.setText(query);
            search(query);
        }
    }

    private void search(String query) {

    }

}
