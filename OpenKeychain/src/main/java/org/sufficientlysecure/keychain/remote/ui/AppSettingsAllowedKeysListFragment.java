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

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
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
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.SelectKeyCursorAdapter;
import org.sufficientlysecure.keychain.ui.widget.FixedListView;
import org.sufficientlysecure.keychain.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class AppSettingsAllowedKeysListFragment extends ListFragmentWorkaround implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_DATA_URI = "uri";

    private SelectKeyCursorAdapter mAdapter;
    private Set<Long> mSelectedMasterKeyIds;
    private ProviderHelper mProviderHelper;

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

        mProviderHelper = new ProviderHelper(getActivity());
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

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SecretKeyCursorAdapter(getActivity(), null, 0, getListView());

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        mSelectedMasterKeyIds = mProviderHelper.getAllKeyIdsForApp(mDataUri);
        Log.d(Constants.TAG, "allowed: " + mSelectedMasterKeyIds.toString());

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Selects items based on master key ids in list view
     *
     * @param masterKeyIds
     */
    private void preselectMasterKeyIds(Set<Long> masterKeyIds) {
        for (int i = 0; i < getListView().getCount(); ++i) {
            long listKeyId = mAdapter.getMasterKeyId(i);
            for (long keyId : masterKeyIds) {
                if (listKeyId == keyId) {
                    getListView().setItemChecked(i, true);
                    break;
                }
            }
        }
    }

    /**
     * Returns all selected master key ids
     *
     * @return
     */
    public Set<Long> getSelectedMasterKeyIds() {
        // mListView.getCheckedItemIds() would give the row ids of the KeyRings not the master key
        // ids!
        Set<Long> keyIds = new HashSet<>();
        for (int i = 0; i < getListView().getCount(); ++i) {
            if (getListView().isItemChecked(i)) {
                keyIds.add(mAdapter.getMasterKeyId(i));
            }
        }

        return keyIds;
    }

    /**
     * Returns all selected user ids
     *
     * @return
     */
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
    }

    public void saveAllowedKeys() {
        try {
            mProviderHelper.saveAllowedKeyIdsForApp(mDataUri, getSelectedMasterKeyIds());
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(Constants.TAG, "Problem saving allowed key ids!", e);
        }
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

        String inMasterKeyList = null;
        if (mSelectedMasterKeyIds != null && mSelectedMasterKeyIds.size() > 0) {
            inMasterKeyList = Tables.KEYS + "." + KeyRings.MASTER_KEY_ID + " IN (";
            Iterator iter = mSelectedMasterKeyIds.iterator();
            while (iter.hasNext()) {
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + iter.next());
                if (iter.hasNext()) {
                    inMasterKeyList += ", ";
                }
            }
            inMasterKeyList += ")";
        }

        String selection = KeyRings.HAS_ANY_SECRET + " != 0";

        String orderBy = KeyRings.USER_ID + " ASC";
        if (inMasterKeyList != null) {
            // sort by selected master keys
            orderBy = inMasterKeyList + " DESC, " + orderBy;
        }
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

        // preselect given master keys
        preselectMasterKeyIds(mSelectedMasterKeyIds);
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

            // We care about the checkbox
            h.selected.setVisibility(View.VISIBLE);
            // the getListView works because this is not a static subclass!
            h.selected.setChecked(getListView().isItemChecked(cursor.getPosition()));

            boolean enabled = false;
            if ((Boolean) h.statusIcon.getTag()) {
                h.statusIcon.setVisibility(View.GONE);
                enabled = true;
            }

            h.setEnabled(enabled);
        }

    }

}
