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
import android.preference.PreferenceActivity;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.processing.CloudLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * Consists of the search bar, search button, and search settings button
 */
public class ImportKeysCloudFragment extends Fragment {

    public static final String ARG_QUERY = "query";
    public static final String ARG_DISABLE_QUERY_EDIT = "disable_query_edit";
    public static final String ARG_CLOUD_SEARCH_PREFS = "cloud_search_prefs";

    private Activity mActivity;
    private ImportKeysListener mCallback;

    /**
     * Creates new instance of this fragment
     *
     * @param query            query to search for
     * @param disableQueryEdit if true, user cannot edit query
     * @param cloudSearchPrefs search parameters to use. If null will retrieve from user's
     *                         preferences.
     */
    public static ImportKeysCloudFragment newInstance(String query, boolean disableQueryEdit,
                                                      Preferences.CloudSearchPrefs
                                                              cloudSearchPrefs) {
        ImportKeysCloudFragment frag = new ImportKeysCloudFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putBoolean(ARG_DISABLE_QUERY_EDIT, disableQueryEdit);
        args.putParcelable(ARG_CLOUD_SEARCH_PREFS, cloudSearchPrefs);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = activity;

        try {
            mCallback = (ImportKeysListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ImportKeysListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.import_keys_cloud_fragment, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_import_keys_cloud_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        Preferences.CloudSearchPrefs cloudSearchPrefs
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
