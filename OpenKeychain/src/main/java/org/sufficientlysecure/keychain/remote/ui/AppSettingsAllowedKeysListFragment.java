/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.util.Set;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ListFragmentWorkaround;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;
import org.sufficientlysecure.keychain.ui.adapter.KeySelectableAdapter;
import org.sufficientlysecure.keychain.ui.widget.FixedListView;
import org.sufficientlysecure.keychain.util.Log;

public class AppSettingsAllowedKeysListFragment extends ListFragmentWorkaround implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_DATA_URI = "uri";

    private KeySelectableAdapter mAdapter;
    private ApiDataAccessObject mApiDao;

    private Uri mDataUri;

    /**
     * Creates new instance of this fragment
     */
    public static AppSettingsAllowedKeysListFragment newInstance(Uri dataUri) {
        AppSettingsAllowedKeysListFragment frag = new AppSettingsAllowedKeysListFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApiDao = new ApiDataAccessObject(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);
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

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDataUri = getArguments().getParcelable(ARG_DATA_URI);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        Set<Long> checked = mApiDao.getAllowedKeyIdsForApp(mDataUri);
        mAdapter = new KeySelectableAdapter(getActivity(), null, 0, checked);
        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);

    }
    /** Returns all selected master key ids. */
    public Set<Long> getSelectedMasterKeyIds() {
        return mAdapter.getSelectedMasterKeyIds();
    }

    /** Returns all selected user ids.
    public String[] getSelectedUserIds() {
        Vector<String> userIds = new Vector<>();
        for (int i = 0; i < getListView().getCount(); ++i) {
            if (getListView().isItemChecked(i)) {
                userIds.add(mAdapter.getUserId(i));
            }
        }

        // make empty array to not return null
        String userIdArray[] = new String[0];
        return userIds.toArray(userIdArray);
    } */

    public void saveAllowedKeys() {
        try {
            mApiDao.saveAllowedKeyIdsForApp(mDataUri, getSelectedMasterKeyIds());
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(Constants.TAG, "Problem saving allowed key ids!", e);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle data) {
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingsUri();
        String where = KeychainContract.KeyRings.HAS_ANY_SECRET + " = 1";

        return new CursorLoader(getActivity(), baseUri, KeyAdapter.PROJECTION, where, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

}
