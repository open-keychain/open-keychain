/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ListFragmentWorkaround;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.ui.adapter.SelectKeyCursorAdapter;

import java.util.Vector;

public class SelectPublicKeyFragment extends ListFragmentWorkaround implements TextWatcher,
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_PRESELECTED_KEY_IDS = "preselected_key_ids";

    private SelectKeyCursorAdapter mAdapter;
    private EditText mSearchView;
    private long mSelectedMasterKeyIds[];
    private String mCurQuery;

    // copied from ListFragment
    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;
    // added for search view
    static final int SEARCH_ID = 0x00ff0004;

    /**
     * Creates new instance of this fragment
     */
    public static SelectPublicKeyFragment newInstance(long[] preselectedKeyIds) {
        SelectPublicKeyFragment frag = new SelectPublicKeyFragment();
        Bundle args = new Bundle();

        args.putLongArray(ARG_PRESELECTED_KEY_IDS, preselectedKeyIds);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedMasterKeyIds = getArguments().getLongArray(ARG_PRESELECTED_KEY_IDS);
    }

    /**
     * Copied from ListFragment and added EditText for search on top of list.
     * We do not use a custom layout here, because this breaks the progress bar functionality
     * of ListFragment.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView tv = new TextView(getActivity());
        tv.setId(INTERNAL_EMPTY_ID);
        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // Added for search view: linearLayout, mSearchView
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        mSearchView = new EditText(context);
        mSearchView.setId(SEARCH_ID);
        mSearchView.setHint(R.string.menu_search);
        mSearchView.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_action_search), null, null, null);

        linearLayout.addView(mSearchView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ListView lv = new ListView(getActivity());
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        linearLayout.addView(lv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        lframe.addView(linearLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return root;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        mSearchView.addTextChangedListener(this);

        mAdapter = new SelectPublicKeyCursorAdapter(getActivity(), null, 0, getListView());

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Selects items based on master key ids in list view
     *
     * @param masterKeyIds
     */
    private void preselectMasterKeyIds(long[] masterKeyIds) {
        if (masterKeyIds != null) {
            for (int i = 0; i < getListView().getCount(); ++i) {
                long keyId = mAdapter.getMasterKeyId(i);
                for (long masterKeyId : masterKeyIds) {
                    if (keyId == masterKeyId) {
                        getListView().setItemChecked(i, true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns all selected master key ids
     *
     * @return
     */
    public long[] getSelectedMasterKeyIds() {
        // mListView.getCheckedItemIds() would give the row ids of the KeyRings not the master key
        // ids!
        Vector<Long> vector = new Vector<Long>();
        for (int i = 0; i < getListView().getCount(); ++i) {
            if (getListView().isItemChecked(i)) {
                vector.add(mAdapter.getMasterKeyId(i));
            }
        }

        // convert to long array
        long[] selectedMasterKeyIds = new long[vector.size()];
        for (int i = 0; i < vector.size(); ++i) {
            selectedMasterKeyIds[i] = vector.get(i);
        }

        return selectedMasterKeyIds;
    }

    /**
     * Returns all selected user ids
     *
     * @return
     */
    public String[] getSelectedUserIds() {
        Vector<String> userIds = new Vector<String>();
        for (int i = 0; i < getListView().getCount(); ++i) {
            if (getListView().isItemChecked(i)) {
                userIds.add(mAdapter.getUserId(i));
            }
        }

        // make empty array to not return null
        String userIdArray[] = new String[0];
        return userIds.toArray(userIdArray);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                KeyRings.USER_ID,
                KeyRings.EXPIRY,
                KeyRings.IS_REVOKED,
                KeyRings.HAS_ENCRYPT,
        };

        String inMasterKeyList = null;
        if (mSelectedMasterKeyIds != null && mSelectedMasterKeyIds.length > 0) {
            inMasterKeyList = Tables.KEYS + "." + KeyRings.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < mSelectedMasterKeyIds.length; ++i) {
                if (i != 0) {
                    inMasterKeyList += ", ";
                }
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + mSelectedMasterKeyIds[i]);
            }
            inMasterKeyList += ")";
        }

        String orderBy = KeyRings.USER_ID + " ASC";
        if (inMasterKeyList != null) {
            // sort by selected master keys
            orderBy = inMasterKeyList + " DESC, " + orderBy;
        }
        String where = null;
        String whereArgs[] = null;
        if (mCurQuery != null) {
            String[] words = mCurQuery.trim().split("\\s+");
            whereArgs = new String[words.length];
            for (int i = 0; i < words.length; ++i) {
                if (where == null) {
                    where = "";
                } else {
                    where += " AND ";
                }
                where += KeyRings.USER_ID + " LIKE ?";
                whereArgs[i] = "%" + words[i] + "%";
            }
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, where, whereArgs, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.setSearchQuery(mCurQuery);
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

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        mCurQuery = !TextUtils.isEmpty(editable.toString()) ? editable.toString() : null;
        getLoaderManager().restartLoader(0, null, this);
    }

    private class SelectPublicKeyCursorAdapter extends SelectKeyCursorAdapter {

        private int mIndexHasEncrypt;

        public SelectPublicKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView) {
            super(context, c, flags, listView);
        }

        @Override
        protected void initIndex(Cursor cursor) {
            super.initIndex(cursor);
            if (cursor != null) {
                mIndexHasEncrypt = cursor.getColumnIndexOrThrow(KeyRings.HAS_ENCRYPT);
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            ViewHolderItem h = (SelectKeyCursorAdapter.ViewHolderItem) view.getTag();

            // We care about the checkbox
            h.selected.setVisibility(View.VISIBLE);
            // the getListView works because this is not a static subclass!
            h.selected.setChecked(getListView().isItemChecked(cursor.getPosition()));

            boolean enabled = false;
            if((Boolean) h.status.getTag()) {
                // Check if key is viable for our purposes
                if (cursor.getInt(mIndexHasEncrypt) == 0) {
                    h.status.setText(R.string.no_key);
                } else {
                    h.status.setText(R.string.can_encrypt);
                    enabled = true;
                }
            }

            h.setEnabled(enabled);
        }

    }

}
