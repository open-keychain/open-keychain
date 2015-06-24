/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014-2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ConsolidateInputParcel;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.FabContainer;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Public key list with sticky list headers. It does _not_ extend ListFragment because it uses
 * StickyListHeaders library which does not extend upon ListView.
 */
public class KeyListFragment extends LoaderFragment
        implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, FabContainer,
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    static final int REQUEST_REPEAT_PASSPHRASE = 1;
    static final int REQUEST_ACTION = 2;

    ExportHelper mExportHelper;

    private KeyListAdapter mAdapter;
    private StickyListHeadersListView mStickyList;

    // saves the mode object for multiselect, needed for reset at some point
    private ActionMode mActionMode = null;

    private String mQuery;

    private FloatingActionsMenu mFab;

    // for CryptoOperationHelper import
    private ArrayList<ParcelableKeyRing> mKeyList;
    private String mKeyserver;
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mImportOpHelper;

    // for ConsolidateOperation
    private CryptoOperationHelper<ConsolidateInputParcel, ConsolidateResult> mConsolidateOpHelper;

    // This ids for multiple key export.
    private ArrayList<Long> mIdsForRepeatAskPassphrase;
    // This index for remembering the number of master key.
    private int mIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(getActivity());
    }

    /**
     * Load custom layout with StickyListView from library
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.key_list_fragment, getContainer());

        mStickyList = (StickyListHeadersListView) view.findViewById(R.id.key_list_list);
        mStickyList.setOnItemClickListener(this);

        mFab = (FloatingActionsMenu) view.findViewById(R.id.fab_main);

        FloatingActionButton fabQrCode = (FloatingActionButton) view.findViewById(R.id.fab_add_qr_code);
        FloatingActionButton fabCloud = (FloatingActionButton) view.findViewById(R.id.fab_add_cloud);
        FloatingActionButton fabFile = (FloatingActionButton) view.findViewById(R.id.fab_add_file);

        fabQrCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFab.collapse();
                scanQrCode();
            }
        });
        fabCloud.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFab.collapse();
                searchCloud();
            }
        });
        fabFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFab.collapse();
                importFile();
            }
        });


        return root;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // show app name instead of "keys" from nav drawer
        getActivity().setTitle(R.string.app_name);

        mStickyList.setOnItemClickListener(this);
        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);

        // Adds an empty footer view so that the Floating Action Button won't block content
        // in last few rows.
        View footer = new View(getActivity());

        int spacing = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()
        );

        android.widget.AbsListView.LayoutParams params = new android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.MATCH_PARENT,
                spacing
        );

        footer.setLayoutParams(params);
        mStickyList.addFooterView(footer, null, false);

        /*
         * Multi-selection
         */
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
                        showMultiExportDialog(ids);
                        break;
                    }
                    case R.id.menu_key_list_multi_select_all: {
                        // select all
                        for (int i = 0; i < mAdapter.getCount(); i++) {
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
                    mAdapter.setNewSelection(position, true);
                } else {
                    mAdapter.removeSelection(position);
                }
                int count = mStickyList.getCheckedItemCount();
                String keysSelected = getResources().getQuantityString(
                        R.plurals.key_list_selected_keys, count, count);
                mode.setTitle(keysSelected);
            }

        });

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

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri,
                KeyListAdapter.PROJECTION, where, whereArgs, ORDER);
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
            mActionMode.finish();
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

    protected void encrypt(ActionMode mode, long[] masterKeyIds) {
        Intent intent = new Intent(getActivity(), EncryptFilesActivity.class);
        intent.setAction(EncryptFilesActivity.ACTION_ENCRYPT_DATA);
        intent.putExtra(EncryptFilesActivity.EXTRA_ENCRYPTION_KEY_IDS, masterKeyIds);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, REQUEST_ACTION);

        mode.finish();
    }

    /**
     * Show dialog to delete key
     *
     * @param hasSecret must contain whether the list of masterKeyIds contains a secret key or not
     */
    public void showDeleteKeyDialog(final ActionMode mode, long[] masterKeyIds, boolean hasSecret) {
        // Can only work on singular secret keys
        if (hasSecret && masterKeyIds.length > 1) {
            Notify.create(getActivity(), R.string.secret_cannot_multiple,
                    Notify.Style.ERROR).show();
            return;
        }

        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.arg1 == DeleteKeyDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    if (data != null) {
                        DeleteResult result = data.getParcelable(DeleteResult.EXTRA_RESULT);
                        if (result != null) {
                            result.createNotify(getActivity()).show();
                        }
                    }
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
        inflater.inflate(R.menu.key_list, menu);

        if (Constants.DEBUG) {
            menu.findItem(R.id.menu_key_list_debug_cons).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_read).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_write).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_first_time).setVisible(true);
        }

        // Get the searchview
        MenuItem searchItem = menu.findItem(R.id.menu_key_list_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        // Execute this when searching
        searchView.setOnQueryTextListener(this);

        // Erase search result without focus
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                // disable swipe-to-refresh
                // mSwipeRefreshLayout.setIsLocked(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mQuery = null;
                getLoaderManager().restartLoader(0, null, KeyListFragment.this);

                // enable swipe-to-refresh
                // mSwipeRefreshLayout.setIsLocked(false);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_key_list_create:
                createKey();
                return true;

            case R.id.menu_key_list_export:
                mExportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, true);
                return true;

            case R.id.menu_key_list_update_all_keys:
                updateAllKeys();
                return true;

            case R.id.menu_key_list_debug_cons:
                consolidate();
                return true;

            case R.id.menu_key_list_debug_read:
                try {
                    KeychainDatabase.debugBackup(getActivity(), true);
                    Notify.create(getActivity(), "Restored debug_backup.db", Notify.Style.OK).show();
                    getActivity().getContentResolver().notifyChange(KeychainContract.KeyRings.CONTENT_URI, null);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.create(getActivity(), "IO Error " + e.getMessage(), Notify.Style.ERROR).show();
                }
                return true;

            case R.id.menu_key_list_debug_write:
                try {
                    KeychainDatabase.debugBackup(getActivity(), false);
                    Notify.create(getActivity(), "Backup to debug_backup.db completed", Notify.Style.OK).show();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.create(getActivity(), "IO Error: " + e.getMessage(), Notify.Style.ERROR).show();
                }
                return true;

            case R.id.menu_key_list_debug_first_time:
                Preferences prefs = Preferences.getPreferences(getActivity());
                prefs.setFirstTime(true);
                Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
                intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true);
                startActivity(intent);
                getActivity().finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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

    private void searchCloud() {
        Intent importIntent = new Intent(getActivity(), ImportKeysActivity.class);
        importIntent.putExtra(ImportKeysActivity.EXTRA_QUERY, (String) null); // hack to show only cloud tab
        startActivity(importIntent);
    }

    private void scanQrCode() {
        Intent scanQrCode = new Intent(getActivity(), ImportKeysProxyActivity.class);
        scanQrCode.setAction(ImportKeysProxyActivity.ACTION_SCAN_IMPORT);
        startActivityForResult(scanQrCode, REQUEST_ACTION);
    }

    private void importFile() {
        Intent intentImportExisting = new Intent(getActivity(), ImportKeysActivity.class);
        intentImportExisting.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
        startActivityForResult(intentImportExisting, REQUEST_ACTION);
    }

    private void createKey() {
        Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
        startActivityForResult(intent, REQUEST_ACTION);
    }

    private void updateAllKeys() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        ProviderHelper providerHelper = new ProviderHelper(activity);

        Cursor cursor = providerHelper.getContentResolver().query(
                KeyRings.buildUnifiedKeyRingsUri(), new String[]{
                        KeyRings.FINGERPRINT
                }, null, null, null
        );

        if (cursor == null) {
            Notify.create(activity, R.string.error_loading_keys, Style.ERROR);
            return;
        }

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                byte[] blob = cursor.getBlob(0);//fingerprint column is 0
                String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);
                ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null, null);
                keyList.add(keyEntry);
            }
            mKeyList = keyList;
        } finally {
            cursor.close();
        }

        // search config
        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            mKeyserver = cloudPrefs.keyserver;
        }

        mImportOpHelper = new CryptoOperationHelper<>(this,
                this, R.string.progress_updating);
        mImportOpHelper.cryptoOperation();
    }

    private void consolidate() {

        CryptoOperationHelper.Callback<ConsolidateInputParcel, ConsolidateResult> callback
                = new CryptoOperationHelper.Callback<ConsolidateInputParcel, ConsolidateResult>() {

            @Override
            public ConsolidateInputParcel createOperationInput() {
                return new ConsolidateInputParcel(false); // we want to perform a full consolidate
            }

            @Override
            public void onCryptoOperationSuccess(ConsolidateResult result) {
                result.createNotify(getActivity()).show();
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(ConsolidateResult result) {
                result.createNotify(getActivity()).show();
            }
        };

        mConsolidateOpHelper =
                new CryptoOperationHelper<>(this, callback, R.string.progress_importing);

        mConsolidateOpHelper.cryptoOperation();
    }

    private void showMultiExportDialog(long[] masterKeyIds) {
        mIdsForRepeatAskPassphrase = new ArrayList<>();
        for (long id : masterKeyIds) {
            try {
                if (PassphraseCacheService.getCachedPassphrase(
                        getActivity(), id, id) == null) {
                    mIdsForRepeatAskPassphrase.add(id);
                }
            } catch (PassphraseCacheService.KeyNotFoundException e) {
                // This happens when the master key is stripped
                // and ignore this key.
            }
        }
        mIndex = 0;
        if (mIdsForRepeatAskPassphrase.size() != 0) {
            startPassphraseActivity();
            return;
        }
        long[] idsForMultiExport = new long[mIdsForRepeatAskPassphrase.size()];
        for (int i = 0; i < mIdsForRepeatAskPassphrase.size(); ++i) {
            idsForMultiExport[i] = mIdsForRepeatAskPassphrase.get(i);
        }
        mExportHelper.showExportKeysDialog(idsForMultiExport,
                Constants.Path.APP_DIR_FILE,
                mAdapter.isAnySecretSelected());
    }

    private void startPassphraseActivity() {
        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        long masterKeyId = mIdsForRepeatAskPassphrase.get(mIndex++);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, masterKeyId);
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mImportOpHelper != null) {
            mImportOpHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (mConsolidateOpHelper != null) {
            mConsolidateOpHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == REQUEST_REPEAT_PASSPHRASE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (mIndex < mIdsForRepeatAskPassphrase.size()) {
                startPassphraseActivity();
                return;
            }
            long[] idsForMultiExport = new long[mIdsForRepeatAskPassphrase.size()];
            for (int i = 0; i < mIdsForRepeatAskPassphrase.size(); ++i) {
                idsForMultiExport[i] = mIdsForRepeatAskPassphrase.get(i);
            }
            mExportHelper.showExportKeysDialog(idsForMultiExport,
                    Constants.Path.APP_DIR_FILE,
                    mAdapter.isAnySecretSelected());
        }

        if (requestCode == REQUEST_ACTION) {
            // if a result has been returned, display a notify
            if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                result.createNotify(getActivity()).show();
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void fabMoveUp(int height) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mFab, "translationY", 0, -height);
        // we're a little behind, so skip 1/10 of the time
        anim.setDuration(270);
        anim.start();
    }

    @Override
    public void fabRestorePosition() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mFab, "translationY", 0);
        // we're a little ahead, so wait a few ms
        anim.setStartDelay(70);
        anim.setDuration(300);
        anim.start();
    }

    // CryptoOperationHelper.Callback methods
    @Override
    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        result.createNotify(getActivity()).show();
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        result.createNotify(getActivity()).show();
    }

    public class KeyListAdapter extends KeyAdapter implements StickyListHeadersAdapter {

        private HashMap<Integer, Boolean> mSelection = new HashMap<>();

        public KeyListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);

            final KeyItemViewHolder holder = (KeyItemViewHolder) view.getTag();

            holder.mSlinger.setVisibility(View.VISIBLE);
            holder.mSlingerButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.mMasterKeyId != null) {
                        Intent safeSlingerIntent = new Intent(mContext, SafeSlingerActivity.class);
                        safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, holder.mMasterKeyId);
                        startActivityForResult(safeSlingerIntent, REQUEST_ACTION);
                    }
                }
            });

            return view;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // let the adapter handle setting up the row views
            View v = super.getView(position, convertView, parent);

            if (mSelection.get(position) != null) {
                // selected position color
                v.setBackgroundColor(parent.getResources().getColor(R.color.emphasis));
            } else {
                // default color
                v.setBackgroundColor(Color.TRANSPARENT);
            }

            return v;
        }

        private class HeaderViewHolder {
            TextView mText;
            TextView mCount;
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

            if (mCursor.getInt(INDEX_HAS_ANY_SECRET) != 0) {
                { // set contact count
                    int num = mCursor.getCount();
                    String contactsTotal = mContext.getResources().getQuantityString(R.plurals.n_keys, num, num);
                    holder.mCount.setText(contactsTotal);
                    holder.mCount.setVisibility(View.VISIBLE);
                }

                holder.mText.setText(convertView.getResources().getString(R.string.my_keys));
                return convertView;
            }

            // set header text as first char in user id
            String userId = mCursor.getString(INDEX_USER_ID);
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
            if (mCursor.getInt(INDEX_HAS_ANY_SECRET) != 0) {
                return 1L;
            }
            // otherwise, return the first character of the name as ID
            String userId = mCursor.getString(INDEX_USER_ID);
            if (userId != null && userId.length() > 0) {
                return Character.toUpperCase(userId.charAt(0));
            } else {
                return Long.MAX_VALUE;
            }
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
                if (isSecretAvailable(pos))
                    return true;
            }
            return false;
        }

        public long[] getCurrentSelectedMasterKeyIds() {
            long[] ids = new long[mSelection.size()];
            int i = 0;
            // get master key ids
            for (int pos : mSelection.keySet()) {
                ids[i++] = getMasterKeyId(pos);
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

    }

}
