/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.annotation.TargetApi;
import android.app.ProgressDialog;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.NoScrollableSwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.KeyUpdateHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.ListAwareSwipeRefreshLayout;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Public key list with sticky list headers. It does _not_ extend ListFragment because it uses
 * StickyListHeaders library which does not extend upon ListView.
 */
public class KeyListFragment extends LoaderFragment
        implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private KeyListAdapter mAdapter;
    private StickyListHeadersListView mStickyList;
    private ListAwareSwipeRefreshLayout mSwipeRefreshLayout;
    private Spinner mFilterSpinner;

    // saves the mode object for multiselect, needed for reset at some point
    private ActionMode mActionMode = null;

    private boolean mShowAllKeys = false;

    private String mQuery;
    private SearchView mSearchView;
    // empty list layout
    private Button mButtonEmptyCreate;
    private Button mButtonEmptyImport;

    boolean hideMenu = false;

    Long mExchangeMasterKeyId = null;

    private static final int REQUEST_CODE_SAFE_SLINGER = 2;

    /**
     * Load custom layout with StickyListView from library
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.key_list_fragment, getContainer());

        mStickyList = (StickyListHeadersListView) view.findViewById(R.id.key_list_list);
        mStickyList.setOnItemClickListener(this);

        // empty view
        mButtonEmptyCreate = (Button) view.findViewById(R.id.key_list_empty_button_create);
        mButtonEmptyCreate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
                startActivityForResult(intent, 0);
            }
        });
        mButtonEmptyImport = (Button) view.findViewById(R.id.key_list_empty_button_import);
        mButtonEmptyImport.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
                startActivityForResult(intent, 0);
            }
        });

        mSwipeRefreshLayout = (ListAwareSwipeRefreshLayout) view.findViewById(R.id.key_list_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new NoScrollableSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                KeychainIntentServiceHandler finishedHandler = new KeychainIntentServiceHandler(getActivity()) {
                    public void handleMessage(Message message) {
                        if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                };
                new KeyUpdateHelper().updateAllKeys(getActivity(), finishedHandler);
                updateActionbarForSwipe(false);
            }
        });
        mSwipeRefreshLayout.setColorScheme(
                R.color.android_purple_dark,
                R.color.android_purple_light,
                R.color.android_purple_dark,
                R.color.android_purple_light);
        mSwipeRefreshLayout.setStickyListHeadersListView(mStickyList);
        mSwipeRefreshLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    updateActionbarForSwipe(true);
                } else {
                    updateActionbarForSwipe(false);
                }
                return false;
            }
        });
        // Just disable for now
        mSwipeRefreshLayout.setIsLocked(true);

        return root;
    }

    private void updateActionbarForSwipe(boolean show) {
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        ActionBar bar = activity.getSupportActionBar();

        if (show) {
            bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            bar.setDisplayUseLogoEnabled(false);
            bar.setCustomView(R.layout.custom_actionbar);
            TextView title = (TextView) getActivity().findViewById(R.id.custom_actionbar_text);
            title.setText(R.string.swipe_to_update);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                hideMenu = true;
                activity.invalidateOptionsMenu();
            }
        } else {
            bar.setTitle(getActivity().getTitle());
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayUseLogoEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
            bar.setDisplayShowCustomEnabled(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                hideMenu = false;
                activity.invalidateOptionsMenu();
            }
        }
    }

    /*
    @Override
    public void onResume() {
        String[] servers = Preferences.getPreferences(getActivity()).getKeyServers();
        mSwipeRefreshLayout.setIsLocked(servers == null || servers.length == 0 || servers[0] == null);
        super.onResume();
    }
    */

    /**
     * Define Adapter and Loader on create of Activity
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFilterSpinner = (Spinner) getActivity().findViewById(R.id.key_list_filter_spinner);
        List<String> list = new ArrayList<String>();
        list.add(getString(R.string.key_list_filter_show_certified));
        list.add(getString(R.string.key_list_filter_show_all));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (getActivity(), android.R.layout.simple_spinner_item, list);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mFilterSpinner.setAdapter(dataAdapter);

        mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: {
                        mShowAllKeys = false;
                        getLoaderManager().restartLoader(0, null, KeyListFragment.this);
                        break;
                    }
                    case 1: {
                        mShowAllKeys = true;
                        getLoaderManager().restartLoader(0, null, KeyListFragment.this);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mStickyList.setOnItemClickListener(this);
        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);

        /*
         * Multi-selection is only available for Android >= 3.0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mStickyList.setFastScrollAlwaysVisible(true);

            mStickyList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mStickyList.getWrappedList().setMultiChoiceModeListener(new MultiChoiceModeListener() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    android.view.MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.key_list_multi, menu);
                    mActionMode = mode;
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
                            ids = mAdapter.getCurrentSelectedMasterKeyIds();
                            showDeleteKeyDialog(mode, ids, mAdapter.isAnySecretSelected());
                            break;
                        }
                        case R.id.menu_key_list_multi_export: {
                            ids = mAdapter.getCurrentSelectedMasterKeyIds();
                            ExportHelper mExportHelper = new ExportHelper((ActionBarActivity) getActivity());
                            mExportHelper.showExportKeysDialog(ids, Constants.Path.APP_DIR_FILE,
                                    mAdapter.isAnySecretSelected());
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
                    mActionMode = null;
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

        // Start out with a progress indicator.
        setContentShown(false);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new KeyListAdapter(getActivity(), null, 0);
        mStickyList.setAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeyRings._ID,
            KeyRings.MASTER_KEY_ID,
            KeyRings.USER_ID,
            KeyRings.IS_REVOKED,
            KeyRings.EXPIRY,
            KeyRings.VERIFIED,
            KeyRings.HAS_ANY_SECRET
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_EXPIRY = 4;
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;

    static final String ORDER =
            KeyRings.HAS_ANY_SECRET + " DESC, UPPER(" + KeyRings.USER_ID + ") ASC";


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();
        String where = null;
        String whereArgs[] = null;
        if (mQuery != null) {
            String[] words = mQuery.trim().split("\\s+");
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
        if (!mShowAllKeys) {
            if (where == null) {
                where = "";
            } else {
                where += " AND ";
            }
            where += KeyRings.VERIFIED + " != 0";
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, where, whereArgs, ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.setSearchQuery(mQuery);
        mAdapter.swapCursor(data);

        mStickyList.setAdapter(mAdapter);

        // this view is made visible if no data is available
        mStickyList.setEmptyView(getActivity().findViewById(R.id.key_list_empty));

        // end action mode, if any
        if (mActionMode != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mActionMode.finish();
            }
        }

        // The list should now be shown.
        if (isResumed()) {
            setContentShown(true);
        } else {
            setContentShownNoAnimation(true);
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
        Intent viewIntent = new Intent(getActivity(), ViewKeyActivity.class);
        viewIntent.setData(
                KeyRings.buildGenericKeyRingUri(mAdapter.getMasterKeyId(position)));
        startActivity(viewIntent);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void encrypt(ActionMode mode, long[] masterKeyIds) {
        Intent intent = new Intent(getActivity(), EncryptFilesActivity.class);
        intent.setAction(EncryptFilesActivity.ACTION_ENCRYPT_DATA);
        intent.putExtra(EncryptFilesActivity.EXTRA_ENCRYPTION_KEY_IDS, masterKeyIds);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, 0);

        mode.finish();
    }

    /**
     * Show dialog to delete key
     *
     * @param masterKeyIds
     * @param hasSecret    must contain whether the list of masterKeyIds contains a secret key or not
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showDeleteKeyDialog(final ActionMode mode, long[] masterKeyIds, boolean hasSecret) {
        // Can only work on singular secret keys
        if (hasSecret && masterKeyIds.length > 1) {
            Notify.showNotify(getActivity(), R.string.secret_cannot_multiple,
                    Notify.Style.ERROR);
            return;
        }

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
                masterKeyIds);

        deleteKeyDialog.show(getActivity().getSupportFragmentManager(), "deleteKeyDialog");
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // Get the searchview
        MenuItem searchItem = menu.findItem(R.id.menu_key_list_search);

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        // Execute this when searching
        mSearchView.setOnQueryTextListener(this);

        View searchPlate = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchPlate.setBackgroundResource(R.drawable.keychaintheme_searchview_holo_light);

        // Erase search result without focus
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    hideMenu = true;
                    getActivity().invalidateOptionsMenu();
                }

                // disable swipe-to-refresh
                // mSwipeRefreshLayout.setIsLocked(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mQuery = null;
                getLoaderManager().restartLoader(0, null, KeyListFragment.this);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    hideMenu = false;
                    getActivity().invalidateOptionsMenu();
                }
                // enable swipe-to-refresh
                // mSwipeRefreshLayout.setIsLocked(false);
                return true;
            }
        });

        if (hideMenu) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        Log.d(Constants.TAG, "onQueryTextChange s:" + s);
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        // If the nav drawer is opened, onQueryTextChange("") is executed.
        // This hack prevents restarting the loader.
        // TODO: better way to fix this?
        String tmp = (mQuery == null) ? "" : mQuery;
        if (!s.equals(tmp)) {
            mQuery = s;
            getLoaderManager().restartLoader(0, null, this);
        }
        return true;
    }

    /**
     * Implements StickyListHeadersAdapter from library
     */
    private class KeyListAdapter extends CursorAdapter implements StickyListHeadersAdapter {
        private String mQuery;
        private LayoutInflater mInflater;

        private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

        public KeyListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mInflater = LayoutInflater.from(context);
        }

        public void setSearchQuery(String query) {
            mQuery = query;
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            return super.swapCursor(newCursor);
        }

        private class ItemViewHolder {
            Long mMasterKeyId;
            TextView mMainUserId;
            TextView mMainUserIdRest;
            ImageView mStatus;
            View mSlinger;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.key_list_item, parent, false);
            final ItemViewHolder holder = new ItemViewHolder();
            holder.mMainUserId = (TextView) view.findViewById(R.id.mainUserId);
            holder.mMainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
            holder.mStatus = (ImageView) view.findViewById(R.id.status_icon);
            holder.mSlinger = view.findViewById(R.id.slinger_view);
            view.setTag(holder);
            view.findViewById(R.id.slinger_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.mMasterKeyId != null) {
                        startExchange(holder.mMasterKeyId);
                    }
                }
            });
            return view;
        }

        /**
         * Bind cursor data to the item list view
         * <p/>
         * NOTE: CursorAdapter already implements the ViewHolder pattern in its getView() method.
         * Thus no ViewHolder is required here.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Highlighter highlighter = new Highlighter(context, mQuery);
            ItemViewHolder h = (ItemViewHolder) view.getTag();

            { // set name and stuff, common to both key types
                String userId = cursor.getString(INDEX_USER_ID);
                String[] userIdSplit = KeyRing.splitUserId(userId);
                if (userIdSplit[0] != null) {
                    h.mMainUserId.setText(highlighter.highlight(userIdSplit[0]));
                } else {
                    h.mMainUserId.setText(R.string.user_id_no_name);
                }
                if (userIdSplit[1] != null) {
                    h.mMainUserIdRest.setText(highlighter.highlight(userIdSplit[1]));
                    h.mMainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    h.mMainUserIdRest.setVisibility(View.GONE);
                }
            }

            { // set edit button and status, specific by key type

                long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                boolean isSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
                boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
                boolean isExpired = !cursor.isNull(INDEX_EXPIRY)
                        && new Date(cursor.getLong(INDEX_EXPIRY) * 1000).before(new Date());
                boolean isVerified = cursor.getInt(INDEX_VERIFIED) > 0;

                h.mMasterKeyId = masterKeyId;

                // Note: order is important!
                if (isRevoked) {
                    KeyFormattingUtils.setStatusImage(getActivity(), h.mStatus, KeyFormattingUtils.STATE_REVOKED);
                    h.mStatus.setVisibility(View.VISIBLE);
                    h.mSlinger.setVisibility(View.GONE);
                } else if (isExpired) {
                    KeyFormattingUtils.setStatusImage(getActivity(), h.mStatus, KeyFormattingUtils.STATE_EXPIRED);
                    h.mStatus.setVisibility(View.VISIBLE);
                    h.mSlinger.setVisibility(View.GONE);
                } else if (isSecret) {
                    h.mStatus.setVisibility(View.GONE);
                    h.mSlinger.setVisibility(View.VISIBLE);
                } else {
                    if (isVerified) {
                        if (cursor.getInt(KeyListFragment.INDEX_HAS_ANY_SECRET) != 0) {
                            // this is a secret key
                            h.mStatus.setVisibility(View.GONE);
                        } else {
                            // this is a public key - show if it's verified
                            KeyFormattingUtils.setStatusImage(getActivity(), h.mStatus, KeyFormattingUtils.STATE_VERIFIED);
                            h.mStatus.setVisibility(View.VISIBLE);
                        }
                    } else {
                        h.mStatus.setVisibility(View.GONE);
                    }
                    h.mSlinger.setVisibility(View.GONE);
                }
            }

        }

        public boolean isSecretAvailable(int id) {
            if (!mCursor.moveToPosition(id)) {
                throw new IllegalStateException("couldn't move cursor to position " + id);
            }

            return mCursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
        }

        public long getMasterKeyId(int id) {
            if (!mCursor.moveToPosition(id)) {
                throw new IllegalStateException("couldn't move cursor to position " + id);
            }

            return mCursor.getLong(INDEX_MASTER_KEY_ID);
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

            if (mCursor.getInt(KeyListFragment.INDEX_HAS_ANY_SECRET) != 0) {
                { // set contact count
                    int num = mCursor.getCount();
                    String contactsTotal = getResources().getQuantityString(R.plurals.n_keys, num, num);
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
                headerText = "" + userId.charAt(0);
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
            if (mCursor.getInt(KeyListFragment.INDEX_HAS_ANY_SECRET) != 0) {
                return 1L;
            }
            // otherwise, return the first character of the name as ID
            String userId = mCursor.getString(KeyListFragment.INDEX_USER_ID);
            if (userId != null && userId.length() > 0) {
                return Character.toUpperCase(userId.charAt(0));
            } else {
                return Long.MAX_VALUE;
            }
        }

        private class HeaderViewHolder {
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

        public boolean isAnySecretSelected() {
            for (int pos : mSelection.keySet()) {
                if (mAdapter.isSecretAvailable(pos))
                    return true;
            }
            return false;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SAFE_SLINGER) {
            if (resultCode == ExchangeActivity.RESULT_EXCHANGE_CANCELED) {
                return;
            }

            final FragmentActivity activity = getActivity();

            // Message is received after importing is done in KeychainIntentService
            KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                    activity,
                    getString(R.string.progress_importing),
                    ProgressDialog.STYLE_HORIZONTAL,
                    true) {
                public void handleMessage(Message message) {
                    // handle messages by standard KeychainIntentServiceHandler first
                    super.handleMessage(message);

                    if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                        // get returned data bundle
                        Bundle returnData = message.getData();
                        if (returnData == null) {
                            return;
                        }
                        final ImportKeyResult result =
                                returnData.getParcelable(OperationResult.EXTRA_RESULT);
                        if (result == null) {
                            Log.e(Constants.TAG, "result == null");
                            return;
                        }

                        if ( ! result.success()) {
                            result.createNotify(activity).show();
                            return;
                        }

                        if (mExchangeMasterKeyId == null) {
                            return;
                        }

                        Intent certifyIntent = new Intent(activity, MultiCertifyKeyActivity.class);
                        certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_RESULT, result);
                        certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_KEY_IDS, result.getImportedMasterKeyIds());
                        certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_CERTIFY_KEY_ID, mExchangeMasterKeyId);
                        startActivityForResult(certifyIntent, KeyListActivity.REQUEST_CODE_RESULT_TO_LIST);

                        mExchangeMasterKeyId = null;
                    }
                }
            };

            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(activity, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // instead of giving the entries by Intent extra, cache them into a
            // file to prevent Java Binder problems on heavy imports
            // read FileImportCache for more info.
            try {
                // import exchanged keys
                ArrayList<ParcelableKeyRing> it = getSlingedKeys(data.getExtras());

                // We parcel this iteratively into a file - anything we can
                // display here, we should be able to import.
                ParcelableFileCache<ParcelableKeyRing> cache =
                        new ParcelableFileCache<ParcelableKeyRing>(activity, "key_import.pcl");
                cache.writeCache(it.size(), it.iterator());

                // fill values for this action
                Bundle bundle = new Bundle();
                intent.putExtra(KeychainIntentService.EXTRA_DATA, bundle);

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                saveHandler.showProgressDialog(activity);

                // start service with intent
                activity.startService(intent);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.showNotify(activity, "Problem writing cache file!", Notify.Style.ERROR);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private static ArrayList<ParcelableKeyRing> getSlingedKeys(Bundle extras) {

        ArrayList<ParcelableKeyRing> list = new ArrayList<ParcelableKeyRing>();

        if (extras != null) {
            byte[] d;
            int i = 0;
            do {
                d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                if (d != null) {
                    list.add(new ParcelableKeyRing(d));
                    i++;
                }
            } while (d != null);
        }

        return list;

    }

    private void startExchange(long masterKeyId) {
        mExchangeMasterKeyId = masterKeyId;
        // retrieve public key blob and start SafeSlinger
        Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(masterKeyId);
        try {
            byte[] keyBlob = (byte[]) new ProviderHelper(getActivity()).getGenericData(
                    uri, KeychainContract.KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);

            Intent slingerIntent = new Intent(getActivity(), ExchangeActivity.class);
            slingerIntent.putExtra(ExchangeConfig.extra.USER_DATA, keyBlob);
            slingerIntent.putExtra(ExchangeConfig.extra.HOST_NAME, Constants.SAFESLINGER_SERVER);
            startActivityForResult(slingerIntent, REQUEST_CODE_SAFE_SLINGER);
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "personal key not found", e);
        }
    }


}
