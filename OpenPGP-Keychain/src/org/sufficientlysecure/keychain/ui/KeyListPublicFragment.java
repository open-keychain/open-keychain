/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.ui.adapter.KeyListPublicAdapter;

import se.emilsjolander.stickylistheaders.ApiLevelTooLowException;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.annotation.SuppressLint;
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

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Public key list with sticky list headers.
 * 
 * - uses StickyListHeaders library
 * 
 * - custom adapter: KeyListPublicAdapter
 * 
 * TODO: - fix view holder in adapter, fix loader
 * 
 */
public class KeyListPublicFragment extends SherlockFragment implements
        AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private KeyListPublicActivity mKeyListPublicActivity;
    private KeyListPublicAdapter mAdapter;
    private StickyListHeadersListView mStickyList;

    /**
     * Load custom layout with StickyListView from library
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.key_list_public_fragment, container, false);
        return view;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyListPublicActivity = (KeyListPublicActivity) getActivity();
        mStickyList = (StickyListHeadersListView) getActivity().findViewById(R.id.list);

        mStickyList.setOnItemClickListener(this);
        // mStickyList.addHeaderView(inflater.inflate(R.layout.list_header, null));
        // mStickyList.addFooterView(inflater.inflate(R.layout.list_footer, null));
        mStickyList.setEmptyView(getActivity().findViewById(R.id.empty));
        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);
        try {
            mStickyList.setFastScrollAlwaysVisible(true);
        } catch (ApiLevelTooLowException e) {
        }

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        // setEmptyText(getString(R.string.list_empty));

        // Start out with a progress indicator.
        // setListShown(false);

        // Create an empty adapter we will use to display the loaded data.
        // mAdapter = new KeyListPublicAdapter(mKeyListPublicActivity, null, Id.type.public_key);
        // setListAdapter(mAdapter);
        // stickyList.setAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] { KeyRings._ID, KeyRings.MASTER_KEY_ID,
            UserIds.USER_ID };

    static final String SORT_ORDER = UserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildPublicKeyRingsUri();

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        // mAdapter.swapCursor(data);
        int userIdIndex = data.getColumnIndex(UserIds.USER_ID);

        mAdapter = new KeyListPublicAdapter(mKeyListPublicActivity, data, Id.type.public_key,
                userIdIndex);

        mStickyList.setAdapter(mAdapter);

        // The list should now be shown.
        if (isResumed()) {
            // setListShown(true);
        } else {
            // setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * On click on item, start key view activity
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent detailsIntent = new Intent(mKeyListPublicActivity, KeyViewActivity.class);
        detailsIntent.setData(KeychainContract.KeyRings.buildPublicKeyRingsUri(Long.toString(id)));
        startActivity(detailsIntent);
    }

}
