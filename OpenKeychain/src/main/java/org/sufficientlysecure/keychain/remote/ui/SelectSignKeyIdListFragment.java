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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ListFragmentWorkaround;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.adapter.SelectKeyCursorAdapter;
import org.sufficientlysecure.keychain.ui.widget.FixedListView;
import org.sufficientlysecure.keychain.util.Log;

public class SelectSignKeyIdListFragment extends ListFragmentWorkaround implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_DATA_URI = "uri";
    public static final String ARG_DATA = "data";

    private SelectKeyCursorAdapter mAdapter;
    private ApiDataAccessObject mApiDao;

    private Uri mDataUri;

    /**
     * Creates new instance of this fragment
     */
    public static SelectSignKeyIdListFragment newInstance(Uri dataUri, Intent data) {
        SelectSignKeyIdListFragment frag = new SelectSignKeyIdListFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putParcelable(ARG_DATA, data);

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

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDataUri = getArguments().getParcelable(ARG_DATA_URI);
        final Intent resultData = getArguments().getParcelable(ARG_DATA);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long masterKeyId = mAdapter.getMasterKeyId(position);

                Uri allowedKeysUri = mDataUri.buildUpon().appendPath(KeychainContract.PATH_ALLOWED_KEYS).build();
                Log.d(Constants.TAG, "allowedKeysUri: " + allowedKeysUri);
                mApiDao.addAllowedKeyIdForApp(allowedKeysUri, masterKeyId);

                resultData.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, masterKeyId);

                getActivity().setResult(Activity.RESULT_OK, resultData);
                getActivity().finish();
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SecretKeyCursorAdapter(getActivity(), null, 0, getListView());

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                KeyRings.USER_ID,
                KeyRings.IS_EXPIRED,
                KeyRings.IS_REVOKED,
                KeyRings.HAS_ENCRYPT,
                KeyRings.VERIFIED,
                KeyRings.HAS_ANY_SECRET,
                KeyRings.HAS_DUPLICATE_USER_ID,
                KeyRings.CREATION,
        };

        String selection = KeyRings.HAS_ANY_SECRET + " != 0";

        String orderBy = KeyRings.USER_ID + " ASC";
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, selection, null, orderBy);
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

    private class SecretKeyCursorAdapter extends SelectKeyCursorAdapter {

        public SecretKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView) {
            super(context, c, flags, listView);
        }

        @Override
        protected void initIndex(Cursor cursor) {
            super.initIndex(cursor);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            ViewHolderItem h = (ViewHolderItem) view.getTag();

            h.selected.setVisibility(View.GONE);

            boolean enabled = false;
            if ((Boolean) h.statusIcon.getTag()) {
                h.statusIcon.setVisibility(View.GONE);
                enabled = true;
            }
            h.setEnabled(enabled);
        }

    }

}
