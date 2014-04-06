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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.widget.FixedListView;
import org.sufficientlysecure.keychain.util.Log;

public class AccountsListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_DATA_URI = "uri";

    // This is the Adapter being used to display the list's data.
    AccountsAdapter mAdapter;

    private Uri mDataUri;

    /**
     * Creates new instance of this fragment
     */
    public static AccountsListFragment newInstance(Uri dataUri) {
        AccountsListFragment frag = new AccountsListFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container,
                savedInstanceState);
        ListView lv = (ListView) layout.findViewById(android.R.id.list);
        ViewGroup parent = (ViewGroup) lv.getParent();

        /*
         * http://stackoverflow.com/a/15880684
         * Remove ListView and add FixedListView in its place.
         * This is done here programatically to be still able to use the progressBar of ListFragment.
         *
         * We want FixedListView to be able to put this ListFragment inside a ScrollView
         */
        int lvIndex = parent.indexOfChild(lv);
        parent.removeViewAt(lvIndex);
        FixedListView newLv = new FixedListView(getActivity());
        newLv.setId(android.R.id.list);
        parent.addView(newLv, lvIndex, lv.getLayoutParams());
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDataUri = getArguments().getParcelable(ARG_DATA_URI);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedAccountName = mAdapter.getItemAccountName(position);
                Uri accountUri = mDataUri.buildUpon().appendEncodedPath(selectedAccountName).build();
                Log.d(Constants.TAG, "accountUri: " + accountUri);

                // edit account settings
                Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                intent.setData(accountUri);
                startActivity(intent);
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.api_settings_accounts_empty));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new AccountsAdapter(getActivity(), null, 0);
        setListAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.ApiAccounts._ID, // 0
            KeychainContract.ApiAccounts.ACCOUNT_NAME // 1
    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), mDataUri, PROJECTION, null, null,
                KeychainContract.ApiAccounts.ACCOUNT_NAME + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private class AccountsAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public AccountsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mInflater = LayoutInflater.from(context);
        }

        /**
         * Similar to CursorAdapter.getItemId().
         * Required to build Uris for api accounts, which are not based on row ids
         *
         * @param position
         * @return
         */
        public String getItemAccountName(int position) {
            if (mDataValid && mCursor != null) {
                if (mCursor.moveToPosition(position)) {
                    return mCursor.getString(1);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView) view.findViewById(R.id.api_accounts_adapter_item_name);

            String accountName = cursor.getString(1);
            text.setText(accountName);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.api_accounts_adapter_list_item, null);
        }
    }

}
