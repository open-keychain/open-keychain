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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Log;

import java.util.List;

public class ImportKeysCloudFragment extends Fragment {
    public static final String ARG_QUERY = "query";
    public static final String ARG_DISABLE_QUERY_EDIT = "disable_query_edit";

    private ImportKeysActivity mImportActivity;

    private View mSearchButton;
    private AutoCompleteTextView mQueryEditText;
    private View mConfigButton;
    private ArrayAdapter<String> mServerAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysCloudFragment newInstance(String query, String keyserver, boolean doKeyserver, boolean doKeybase) {
        ImportKeysCloudFragment frag = new ImportKeysCloudFragment();

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
        View view = inflater.inflate(R.layout.import_keys_cloud_fragment, container, false);

        mSearchButton = view.findViewById(R.id.cloud_import_server_search);
        mQueryEditText = (AutoCompleteTextView) view.findViewById(R.id.cloud_import_server_query);
        mConfigButton = view.findViewById(R.id.cloud_import_server_config_button);

        List<String> namesAndEmails = ContactHelper.getContactNames(getActivity());
        namesAndEmails.addAll(ContactHelper.getContactMails(getActivity()));
        mQueryEditText.setThreshold(3);
        mQueryEditText.setAdapter(
                new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_dropdown_item,
                                namesAndEmails
                        )
        );

        mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                search(mQueryEditText.getText().toString());

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
                    search(mQueryEditText.getText().toString());

                    // Don't return true to let the keyboard close itself after pressing search
                    return false;
                }
                return false;
            }
        });

        mConfigButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mImportActivity, PreferencesActivity.class);
                // GRR, for some reason I can’t set the Action or I get an incomprehensible
                //  exception about “modern two-pane layouts”
                // i.setAction(PreferencesActivity.ACTION_PREFS_CLOUD);
                startActivity(i);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set displayed values
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_QUERY)) {
                String query = getArguments().getString(ARG_QUERY);
                mQueryEditText.setText(query, TextView.BufferType.EDITABLE);

                Log.d(Constants.TAG, "query: " + query);
            }

            if (getArguments().getBoolean(ARG_DISABLE_QUERY_EDIT, false)) {
                mQueryEditText.setEnabled(false);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mImportActivity = (ImportKeysActivity) activity;
    }

    private void search(String query) {
        Preferences prefs = Preferences.getPreferences(getActivity());
        mImportActivity.loadCallback(new ImportKeysListFragment.CloudLoaderState(query, prefs.getCloudSearchPrefs()));
        hideKeyboard();
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
