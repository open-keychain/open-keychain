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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyTypes;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import se.emilsjolander.stickylistheaders.ApiLevelTooLowException;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

/**
 * Public key list with sticky list headers. It does _not_ extend ListFragment because it uses
 * StickyListHeaders library which does not extend upon ListView.
 */
public class KeyListFragment extends Fragment implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private KeyListAdapter mAdapter;
    private StickyListHeadersListView mStickyList;

    // empty list layout
    private BootstrapButton mButtonEmptyCreate;
    private BootstrapButton mButtonEmptyImport;

    /**
     * Load custom layout with StickyListView from library
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.key_list_fragment, container, false);

        mButtonEmptyCreate = (BootstrapButton) view.findViewById(R.id.key_list_empty_button_create);
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

        mButtonEmptyImport = (BootstrapButton) view.findViewById(R.id.key_list_empty_button_import);
        mButtonEmptyImport.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE);
                startActivityForResult(intent, 0);
            }
        });

        return view;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
                            ids = mAdapter.getCurrentSelectedItemIds();
                            showDeleteKeyDialog(mode, ids);
                            break;
                        }
                    }
                    return true;
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
    static final int INDEX_UID = 3;
    static final String SORT_ORDER = UserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

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
        Intent viewIntent = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            viewIntent = new Intent(getActivity(), ViewKeyActivity.class);
        } else {
            viewIntent = new Intent(getActivity(), ViewKeyActivityJB.class);
        }
        viewIntent.setData(KeychainContract.KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(mAdapter.getMasterKeyId(position))));
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
                    Bundle returnData = message.getData();
                    if (returnData != null
                            && returnData.containsKey(DeleteKeyDialogFragment.MESSAGE_NOT_DELETED)) {
                        ArrayList<String> notDeleted =
                                returnData.getStringArrayList(DeleteKeyDialogFragment.MESSAGE_NOT_DELETED);
                        String notDeletedMsg = "";
                        for (String userId : notDeleted) {
                            notDeletedMsg += userId + "\n";
                        }
                        Toast.makeText(getActivity(), getString(R.string.error_can_not_delete_contacts, notDeletedMsg)
                                + getResources().getQuantityString(R.plurals.error_can_not_delete_info, notDeleted.size()),
                                Toast.LENGTH_LONG).show();

                        mode.finish();
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                keyRingRowIds, Id.type.public_key);

        deleteKeyDialog.show(getActivity().getSupportFragmentManager(), "deleteKeyDialog");
    }

    /**
     * Implements StickyListHeadersAdapter from library
     */
    private class KeyListAdapter extends CursorAdapter implements StickyListHeadersAdapter {
        private LayoutInflater mInflater;
        private int mIndexUserId;
        private int mIndexIsRevoked;
        private int mMasterKeyId;

        @SuppressLint("UseSparseArrays")
        private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

        public KeyListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mInflater = LayoutInflater.from(context);
            initIndex(c);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            initIndex(newCursor);

            return super.swapCursor(newCursor);
        }

        /**
         * Get column indexes for performance reasons just once in constructor and swapCursor. For a
         * performance comparison see http://stackoverflow.com/a/17999582
         *
         * @param cursor
         */
        private void initIndex(Cursor cursor) {
            if (cursor != null) {
                mIndexUserId = cursor.getColumnIndexOrThrow(KeychainContract.UserIds.USER_ID);
                mIndexIsRevoked = cursor.getColumnIndexOrThrow(KeychainContract.Keys.IS_REVOKED);
                mMasterKeyId = cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.MASTER_KEY_ID);
            }
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

                String userId = cursor.getString(mIndexUserId);
                String[] userIdSplit = PgpKeyHelper.splitUserId(userId);
                if (userIdSplit[0] != null) {
                    mainUserId.setText(userIdSplit[0]);
                } else {
                    mainUserId.setText(R.string.user_id_no_name);
                }
                if (userIdSplit[1] != null) {
                    mainUserIdRest.setText(userIdSplit[1]);
                    mainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mainUserIdRest.setVisibility(View.GONE);
                }
            }

            { // set edit button and revoked info, specific by key type
                Button button = (Button) view.findViewById(R.id.edit);
                TextView revoked = (TextView) view.findViewById(R.id.revoked);

                if(cursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET) {
                    // this is a secret key - show the edit button
                    revoked.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);

                    final long id = cursor.getLong(mMasterKeyId);
                    button.setOnClickListener(new OnClickListener() {
                        public void onClick(View view) {
                            Intent editIntent = new Intent(getActivity(), EditKeyActivity.class);
                            editIntent.setData(KeychainContract.KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(id)));
                            editIntent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
                            startActivityForResult(editIntent, 0);
                        }
                    });
                } else {
                    // this is a public key - hide the edit button, show if it's revoked
                    button.setVisibility(View.GONE);

                    boolean isRevoked = cursor.getInt(mIndexIsRevoked) > 0;
                    revoked.setVisibility(isRevoked ? View.VISIBLE : View.GONE);
                }
            }

        }

        public long getMasterKeyId(int id) {

            if (!mCursor.moveToPosition(id)) {
                throw new IllegalStateException("couldn't move cursor to position " + id);
            }

            return mCursor.getLong(mMasterKeyId);

        }

        public int getKeyType(int position) {

            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            return mCursor.getInt(KeyListFragment.INDEX_TYPE);

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
                holder.text = (TextView) convertView.findViewById(R.id.stickylist_header_text);
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

            if(mCursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET) {
                holder.text.setText(convertView.getResources().getString(R.string.my_keys));
                return convertView;
            }

            // set header text as first char in user id
            String userId = mCursor.getString(KeyListFragment.INDEX_UID);
            String headerText = convertView.getResources().getString(R.string.user_id_no_name);
            if (userId != null && userId.length() > 0) {
                headerText = "" + mCursor.getString(KeyListFragment.INDEX_UID).subSequence(0, 1).charAt(0);
            }
            holder.text.setText(headerText);
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
            if(mCursor.getInt(KeyListFragment.INDEX_TYPE) == KeyTypes.SECRET)
                return 1L;

            // otherwise, return the first character of the name as ID
            String userId = mCursor.getString(KeyListFragment.INDEX_UID);
            if (userId != null && userId.length() > 0) {
                return userId.charAt(0);
            } else {
                return Long.MAX_VALUE;
            }
        }

        class HeaderViewHolder {
            TextView text;
        }

        /**
         * -------------------------- MULTI-SELECTION METHODS --------------
         */
        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value);
            notifyDataSetChanged();
        }

        public boolean isPositionChecked(int position) {
            Boolean result = mSelection.get(position);
            return result == null ? false : result;
        }

        public long[] getCurrentSelectedItemIds() {
            long[] ids = new long[mSelection.size()];
            int i = 0;
            // get master key ids
            for (int pos : mSelection.keySet())
                ids[i++] = mAdapter.getItemId(pos);
            return ids;
        }

        public long[] getCurrentSelectedMasterKeyIds() {
            long[] ids = new long[mSelection.size()];
            int i = 0;
            // get master key ids
            for (int pos : mSelection.keySet())
                ids[i++] = mAdapter.getMasterKeyId(pos);
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
            // default color
            v.setBackgroundColor(Color.TRANSPARENT);
            if (mSelection.get(position) != null) {
                // this is a selected position, change color!
                v.setBackgroundColor(parent.getResources().getColor(R.color.emphasis));
            }
            return v;
        }

    }

}
