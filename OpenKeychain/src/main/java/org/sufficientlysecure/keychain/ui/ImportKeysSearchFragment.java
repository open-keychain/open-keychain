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
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.MenuItemCompat.OnActionExpandListener;
import androidx.fragment.app.Fragment;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.processing.CloudLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Preferences.CloudSearchPrefs;

import static androidx.appcompat.widget.SearchView.OnQueryTextListener;

/**
 * Consists of the search bar, search button, and search settings button
 */
public class ImportKeysSearchFragment extends Fragment {

    public static final String ARG_QUERY = "query";
    public static final String ARG_CLOUD_SEARCH_PREFS = "cloud_search_prefs";

    private Activity mActivity;
    private ImportKeysListener mCallback;

    /**
     * Creates new instance of this fragment
     *
     * @param query            query to search for
     * @param cloudSearchPrefs search parameters to use. If null will retrieve from user's
     *                         preferences.
     */
    public static ImportKeysSearchFragment newInstance(String query,
                                                       CloudSearchPrefs cloudSearchPrefs) {

        ImportKeysSearchFragment frag = new ImportKeysSearchFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putParcelable(ARG_CLOUD_SEARCH_PREFS, cloudSearchPrefs);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        // no view, just search view
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (ImportKeysListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ImportKeysListener");
        }

        mActivity = (Activity) context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.import_keys_cloud_fragment, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_import_keys_cloud_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                search(searchView.getQuery().toString().trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mActivity.finish();
                return true;
            }
        });

        searchItem.expandActionView();

        String query = getArguments().getString(ARG_QUERY);
        if (query != null) {
            searchView.setQuery(query, false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_import_keys_cloud_settings:
                Intent intent = new Intent(mActivity, SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        SettingsActivity.CloudSearchPrefsFragment.class.getName());
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void search(String query) {
        CloudSearchPrefs cloudSearchPrefs
                = getArguments().getParcelable(ARG_CLOUD_SEARCH_PREFS);

        // no explicit search preferences passed
        if (cloudSearchPrefs == null) {
            cloudSearchPrefs = Preferences.getPreferences(getActivity()).getCloudSearchPrefs();
        }

        mCallback.loadKeys(new CloudLoaderState(query, cloudSearchPrefs));
        toggleKeyboard(false);
    }

    private void toggleKeyboard(boolean show) {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getActivity().getCurrentFocus();
        if (v == null) {
            return;
        }

        if (show) {
            inputManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        } else {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

}
