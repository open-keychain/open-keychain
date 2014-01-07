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

import java.util.Set;

import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.ui.adapter.KeyListPublicAdapter;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;

import se.emilsjolander.stickylistheaders.ApiLevelTooLowException;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Public key list with sticky list headers. It does _not_ extend ListFragment because it uses
 * StickyListHeaders library which does not extend upon ListView.
 */
public class KeyListPublicFragment extends Fragment implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

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
        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);
        try {
            mStickyList.setFastScrollAlwaysVisible(true);
        } catch (ApiLevelTooLowException e) {
        }

        // this view is made visible if no data is available
        mStickyList.setEmptyView(getActivity().findViewById(R.id.empty));

        /*
         * ActionBarSherlock does not support MultiChoiceModeListener. Thus multi-selection is only
         * available for Android >= 3.0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mStickyList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mStickyList.getWrappedList().setMultiChoiceModeListener(new MultiChoiceModeListener() {

                private int count = 0;

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    android.view.MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.key_list_multi_selection, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    Set<Integer> positions = mAdapter.getCurrentCheckedPosition();
                    switch (item.getItemId()) {
                    case R.id.delete_entry:

                        // get IDs for checked positions as long array
                        long[] ids = new long[positions.size()];
                        int i = 0;
                        for (int pos : positions) {
                            ids[i] = mAdapter.getItemId(pos);
                            i++;
                        }
                        showDeleteKeyDialog(ids);

                        break;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    count = 0;
                    mAdapter.clearSelection();
                }

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                        boolean checked) {
                    if (checked) {
                        count++;
                        mAdapter.setNewSelection(position, checked);
                    } else {
                        count--;
                        mAdapter.removeSelection(position);
                    }

                    String keysSelected = getResources().getQuantityString(
                            R.plurals.key_list_selected_keys, count, count);
                    mode.setTitle(keysSelected);
                }

            });
        }

        // NOTE: Not supported by StickyListHeader, thus no indicator is shown while loading
        // Start out with a progress indicator.
        // setListShown(false);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new KeyListPublicAdapter(mKeyListPublicActivity, null, Id.type.public_key,
                USER_ID_INDEX);
        mStickyList.setAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] { KeyRings._ID, KeyRings.MASTER_KEY_ID,
            UserIds.USER_ID };

    static final int USER_ID_INDEX = 2;

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
        mAdapter.swapCursor(data);

        mStickyList.setAdapter(mAdapter);

        // NOTE: Not supported by StickyListHeader, thus no indicator is shown while loading
        // The list should now be shown.
        // if (isResumed()) {
        // setListShown(true);
        // } else {
        // setListShownNoAnimation(true);
        // }
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

    /**
     * Show dialog to delete key
     * 
     * @param keyRingRowIds
     */
    public void showDeleteKeyDialog(long[] keyRingRowIds) {
        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(null,
                keyRingRowIds, Id.type.public_key);

        deleteKeyDialog.show(getActivity().getSupportFragmentManager(), "deleteKeyDialog");
    }

}