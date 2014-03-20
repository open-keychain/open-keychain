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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.AbsListView.MultiChoiceModeListener;
import com.beardedhen.androidbootstrap.BootstrapButton;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyTypes;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.HighlightQueryCursorAdapter;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import se.emilsjolander.stickylistheaders.ApiLevelTooLowException;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Public key list with sticky list headers. It does _not_ extend ListFragment because it uses
 * StickyListHeaders library which does not extend upon ListView.
 */
public class KeyListFragment extends Fragment
        implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private KeyListAdapter mAdapter;
    private StickyListHeadersListView mStickyList;

    // rebuild functionality of ListFragment, http://stackoverflow.com/a/12504097
    boolean mListShown;
    View mProgressContainer;
    View mListContainer;

    private String mCurQuery;
    private SearchView mSearchView;
    // empty list layout
    private BootstrapButton mButtonEmptyCreate;
    private BootstrapButton mButtonEmptyImport;


    /**
     * Load custom layout with StickyListView from library
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.key_list_fragment, container, false);

        mStickyList = (StickyListHeadersListView) root.findViewById(R.id.key_list_list);
        mStickyList.setOnItemClickListener(this);


        // empty view
        mButtonEmptyCreate = (BootstrapButton) root.findViewById(R.id.key_list_empty_button_create);
        mButtonEmptyCreate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditKeyActivity.class);
                intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
                intent.putExtra(EditKeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
                intent.putExtra(EditKeyActivity.EXTRA_USER_IDS, ""); // show user id view
                startActivityForResult(intent, 0);
            }
        });
        mButtonEmptyImport = (BootstrapButton) root.findViewById(R.id.key_list_empty_button_import);
        mButtonEmptyImport.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE);
                startActivityForResult(intent, 0);
            }
        });

        // rebuild functionality of ListFragment, http://stackoverflow.com/a/12504097
        mListContainer = root.findViewById(R.id.key_list_list_container);
        mProgressContainer = root.findViewById(R.id.key_list_progress_container);
        mListShown = true;

        return root;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mStickyList.setOnItemClickListener(this);
        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);
        try {
            mStickyList.setFastScrollAlwaysVisible(true);
        } catch (ApiLevelTooLowException e) {
        }

        // this view is made visible if no data is available
        mStickyList.setEmptyView(getActivity().findViewById(R.id.key_list_empty));

        /*
         * ActionBarSherlock does not support MultiChoiceModeListener. Thus multi-selection is only
         * available for Android >= 3.0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mStickyList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mStickyList.getWrappedList().setMultiChoiceModeListener(new MultiChoiceModeListener() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    android.view.MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.key_list_multi, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                    // get IDs for checked positions as long array
                    long[] ids;

                    switch (item.getItemId()) {
                        case R.id.menu_key_list_multi_encrypt: {
                            ids = mAdapter.getCurrentSelectedMasterKeyIds();
                            encrypt(mode, ids);
                            break;
                        }
                        case R.id.menu_key_list_multi_delete: {
                            ids = mStickyList.getWrappedList().getCheckedItemIds();
                            showDeleteKeyDialog(mode, ids);
                            break;
                        }
                        case R.id.menu_key_list_multi_export: {
                            ids = mStickyList.getWrappedList().getCheckedItemIds();
                            long[] masterKeyIds = new long[2*ids.length];
                            ArrayList<Long> allPubRowIds =
                                    ProviderHelper.getPublicKeyRingsRowIds(getActivity());
                            for (int i = 0; i < ids.length; i++) {
                                if (allPubRowIds.contains(ids[i])) {
                                    masterKeyIds[i] = ProviderHelper.getPublicMasterKeyId(getActivity(), ids[i]);
                                } else {
                                    masterKeyIds[i] = ProviderHelper.getSecretMasterKeyId(getActivity(), ids[i]);
                                }
                            }
                            ExportHelper mExportHelper = new ExportHelper((ActionBarActivity) getActivity());
                            mExportHelper
                                    .showExportKeysDialog(masterKeyIds, Id.type.public_key,
                                            Constants.Path.APP_DIR_FILE_PUB,
                                            getString(R.string.also_export_secret_keys));
                            break;
                        }
                        case R.id.menu_key_list_multi_select_all: {
                            // select all
                            for (int i = 0; i < mStickyList.getCount(); i++) {
                                mStickyList.setItemChecked(i, true);
                            }
                            break;
                        }
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mAdapter.clearSelection();
                }

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                                      boolean checked) {
                    if (checked) {
                        mAdapter.setNewSelection(position, checked);
                    } else {
                        mAdapter.removeSelection(position);
                    }
                    int count = mStickyList.getCheckedItemCount();
                    String keysSelected = getResources().getQuantityString(
                            R.plurals.key_list_selected_keys, count, count);
                    mode.setTitle(keysSelected);
                }

            });
        }

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // NOTE: Not supported by StickyListHeader, but reimplemented here
        // Start out with a progress indicator.
        setListShown(false);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new KeyListAdapter(getActivity(), null, Id.type.public_key);
        mStickyList.setAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.TYPE,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.UserIds.USER_ID,
            KeychainContract.Keys.IS_REVOKED
    };

    static final int INDEX_TYPE = 1;
    static final int INDEX_MASTER_KEY_ID = 2;
    static final int INDEX_USER_ID = 3;
    static final int INDEX_IS_REVOKED = 4;

    static final String SORT_ORDER =
            // show secret before public key
            KeychainDatabase.Tables.KEY_RINGS + "." + KeyRings.TYPE + " DESC, "
                    // sort by user id otherwise
                    + UserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();
        String where = null;
        String whereArgs[] = null;
        if (mCurQuery != null) {
            where = KeychainContract.UserIds.USER_ID + " LIKE ?";
            whereArgs = new String[]{"%" + mCurQuery + "%"};
        }
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, where, whereArgs, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.setSearchQuery(mCurQuery);
        mAdapter.swapCursor(data);

        mStickyList.setAdapter(mAdapter);

        // NOTE: Not supported by StickyListHeader, but reimplemented here
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

    /**
     * On click on item, start key view activity
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent viewIntent = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            viewIntent = new Intent(getActivity(), ViewKeyActivity.class);
        } else {
            viewIntent = new Intent(getActivity(), ViewKeyActivityJB.class);
        }
        viewIntent.setData(
                KeychainContract
                        .KeyRings.buildPublicKeyRingsByMasterKeyIdUri(
                                            Long.toString(mAdapter.getMasterKeyId(position))));
        startActivity(viewIntent);
    }

    @TargetApi(11)
    protected void encrypt(ActionMode mode, long[] keyRingMasterKeyIds) {
        Intent intent = new Intent(getActivity(), EncryptActivity.class);
        intent.setAction(EncryptActivity.ACTION_ENCRYPT);
        intent.putExtra(EncryptActivity.EXTRA_ENCRYPTION_KEY_IDS, keyRingMasterKeyIds);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, 0);

        mode.finish();
    }

    /**
     * Show dialog to delete key
     *
     * @param keyRingRowIds
     */
    @TargetApi(11)
    // TODO: this method needs an overhaul to handle both public and secret keys gracefully!
    public void showDeleteKeyDialog(final ActionMode mode, long[] keyRingRowIds) {
        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == DeleteKeyDialogFragment.MESSAGE_OKAY) {
                    mode.finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                keyRingRowIds);

        deleteKeyDialog.show(getActivity().getSupportFragmentManager(), "deleteKeyDialog");
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // Get the searchview
        MenuItem searchItem = menu.findItem(R.id.menu_key_list_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        // Execute this when searching
        mSearchView.setOnQueryTextListener(this);

        // Erase search result without focus
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mCurQuery = null;
                mSearchView.setQuery("", true);
                getLoaderManager().restartLoader(0, null, KeyListFragment.this);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mCurQuery = !TextUtils.isEmpty(s) ? s : null;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    // rebuild functionality of ListFragment, http://stackoverflow.com/a/12504097
    public void setListShown(boolean shown, boolean animate) {
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }

    // rebuild functionality of ListFragment, http://stackoverflow.com/a/12504097
    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    // rebuild functionality of ListFragment, http://stackoverflow.com/a/12504097
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    /**
     * Implements StickyListHeadersAdapter from library
     */
    private class KeyListAdapter extends HighlightQueryCursorAdapter implements StickyListHeadersAdapter {
        private LayoutInflater mInflater;

        private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

        public KeyListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mInflater = LayoutInflater.from(context);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            return super.swapCursor(newCursor);
        }

        /**
         * Bind cursor data to the item list view
         * <p/>
         * NOTE: CursorAdapter already implements the ViewHolder pattern in its getView() method. Thus
         * no ViewHolder is required here.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            { // set name and stuff, common to both key types
                TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
                TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);

                String userId = cursor.getString(INDEX_USER_ID);
                String[] userIdSplit = PgpKeyHelper.splitUserId(userId);
                if (userIdSplit[0] != null) {
                    mainUserId.setText(highlightSearchQuery(userIdSplit[0]));
                } else {
                    mainUserId.setText(R.string.user_id_no_name);
                }
                if (userIdSplit[1] != null) {
                    mainUserIdRest.setText(highlightSearchQuery(userIdSplit[1]));
                    mainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mainUserIdRest.setVisibility(View.GONE);
                }
            }

            { // set edit button and revoked info, specific by key type
                Button button = (Button) view.findViewById(R.id.edit);
                TextView revoked = (TextView) view.findViewById(R.id.revoked);

                if (cursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET) {
                    // this is a secret key - show the edit button
                    revoked.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);

                    final long id = cursor.getLong(INDEX_MASTER_KEY_ID);
                    button.setOnClickListener(new OnClickListener() {
                        public void onClick(View view) {
                            Intent editIntent = new Intent(getActivity(), EditKeyActivity.class);
                            editIntent.setData(
                                    KeychainContract.KeyRings
                                            .buildSecretKeyRingsByMasterKeyIdUri(Long.toString(id)));
                            editIntent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
                            startActivityForResult(editIntent, 0);
                        }
                    });
                } else {
                    // this is a public key - hide the edit button, show if it's revoked
                    button.setVisibility(View.GONE);

                    boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
                    revoked.setVisibility(isRevoked ? View.VISIBLE : View.GONE);
                }
            }

        }

        public long getMasterKeyId(int id) {
            if (!mCursor.moveToPosition(id)) {
                throw new IllegalStateException("couldn't move cursor to position " + id);
            }

            return mCursor.getLong(INDEX_MASTER_KEY_ID);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.key_list_item, parent, false);
        }

        /**
         * Creates a new header view and binds the section headers to it. It uses the ViewHolder
         * pattern. Most functionality is similar to getView() from Android's CursorAdapter.
         * <p/>
         * NOTE: The variables mDataValid and mCursor are available due to the super class
         * CursorAdapter.
         */
        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            if (convertView == null) {
                holder = new HeaderViewHolder();
                convertView = mInflater.inflate(R.layout.key_list_header, parent, false);
                holder.mText = (TextView) convertView.findViewById(R.id.stickylist_header_text);
                holder.mCount = (TextView) convertView.findViewById(R.id.contacts_num);
                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }

            if (!mDataValid) {
                // no data available at this point
                Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
                return convertView;
            }

            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            if (mCursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET) {
                { // set contact count
                    int num = mCursor.getCount();
                    String contactsTotal = getResources().getQuantityString(R.plurals.n_contacts, num, num);
                    holder.mCount.setText(contactsTotal);
                    holder.mCount.setVisibility(View.VISIBLE);
                }

                holder.mText.setText(convertView.getResources().getString(R.string.my_keys));
                return convertView;
            }

            // set header text as first char in user id
            String userId = mCursor.getString(KeyListFragment.INDEX_USER_ID);
            String headerText = convertView.getResources().getString(R.string.user_id_no_name);
            if (userId != null && userId.length() > 0) {
                headerText = "" +
                            mCursor.getString(KeyListFragment.INDEX_USER_ID).subSequence(0, 1).charAt(0);
            }
            holder.mText.setText(headerText);
            holder.mCount.setVisibility(View.GONE);
            return convertView;
        }

        /**
         * Header IDs should be static, position=1 should always return the same Id that is.
         */
        @Override
        public long getHeaderId(int position) {
            if (!mDataValid) {
                // no data available at this point
                Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
                return -1;
            }

            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            // early breakout: all secret keys are assigned id 0
            if (mCursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET) {
                return 1L;
            }
            // otherwise, return the first character of the name as ID
            String userId = mCursor.getString(KeyListFragment.INDEX_USER_ID);
            if (userId != null && userId.length() > 0) {
                return userId.charAt(0);
            } else {
                return Long.MAX_VALUE;
            }
        }

        class HeaderViewHolder {
            TextView mText;
            TextView mCount;
        }

        /**
         * -------------------------- MULTI-SELECTION METHODS --------------
         */
        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value);
            notifyDataSetChanged();
        }

        public long[] getCurrentSelectedMasterKeyIds() {
            long[] ids = new long[mSelection.size()];
            int i = 0;
            // get master key ids
            for (int pos : mSelection.keySet()) {
                ids[i++] = mAdapter.getMasterKeyId(pos);
            }
            return ids;
        }

        public void removeSelection(int position) {
            mSelection.remove(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            mSelection.clear();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // let the adapter handle setting up the row views
            View v = super.getView(position, convertView, parent);

            /**
             * Change color for multi-selection
             */
            if (mSelection.get(position) != null) {
                // selected position color
                v.setBackgroundColor(parent.getResources().getColor(R.color.emphasis));
            } else {
                // default color
                v.setBackgroundColor(Color.TRANSPARENT);
            }

            return v;
        }

    }

}
