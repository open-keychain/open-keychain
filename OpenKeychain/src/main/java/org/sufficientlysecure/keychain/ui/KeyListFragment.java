/*
 * Copyright (C) 2016 Tobias Erthal
 * Copyright (C) 2013-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewAnimator;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.BenchmarkResult;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.BenchmarkInputParcel;
import org.sufficientlysecure.keychain.service.ConsolidateInputParcel;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.adapter.KeySectionedListAdapter;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.KeyHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.KeyItem;
import org.sufficientlysecure.keychain.util.FabContainer;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.flexibleadapter.utils.Utils;

import static org.sufficientlysecure.keychain.Constants.TAG;

public class KeyListFragment extends RecyclerFragment<KeyListFragment.KeyFlexibleAdapter<KeyItem>>
        implements SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<Cursor>, FabContainer,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.OnItemClickListener {

    static final int REQUEST_ACTION = 1;
    private static final int REQUEST_DELETE = 2;
    private static final int REQUEST_VIEW_KEY = 3;

    private boolean mConsumed = false;

    private List<KeyItem> mKeyItems = new ArrayList<>();
    // saves the mode object for multiselect, needed for reset at some point
    private ActionMode mActionMode = null;
    private ActionModeHelper mActionModeHelper;

    private Button vSearchButton;
    private ViewAnimator vSearchContainer;

    private FloatingActionsMenu mFab;

    // for CryptoOperationHelper import
    private ArrayList<ParcelableKeyRing> mKeyList;
    private ParcelableHkpKeyserver mKeyserver;
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mImportOpHelper;

    // for ConsolidateOperation
    private CryptoOperationHelper<ConsolidateInputParcel, ConsolidateResult> mConsolidateOpHelper;

    // Callbacks related to listview and menu events
    private final ActionMode.Callback mActionCallback
            = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.key_list_multi, menu);
            getAdapter().setMode(FlexibleAdapter.MODE_MULTI);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_key_list_multi_encrypt: {
                    long[] keyIds = getAdapter().getSelectedMasterKeyIds();
                    Intent intent = new Intent(getActivity(), EncryptFilesActivity.class);
                    intent.setAction(EncryptFilesActivity.ACTION_ENCRYPT_DATA);
                    intent.putExtra(EncryptFilesActivity.EXTRA_ENCRYPTION_KEY_IDS, keyIds);

                    startActivityForResult(intent, REQUEST_ACTION);
                    mode.finish();
                    break;
                }

                case R.id.menu_key_list_multi_delete: {
                    long[] keyIds = getAdapter().getSelectedMasterKeyIds();
                    boolean hasSecret = getAdapter().isAnySecretKeySelected();
                    Intent intent = new Intent(getActivity(), DeleteKeyDialogActivity.class);
                    intent.putExtra(DeleteKeyDialogActivity.EXTRA_DELETE_MASTER_KEY_IDS, keyIds);
                    intent.putExtra(DeleteKeyDialogActivity.EXTRA_HAS_SECRET, hasSecret);
                    if (hasSecret) {
                        intent.putExtra(DeleteKeyDialogActivity.EXTRA_KEYSERVER,
                                Preferences.getPreferences(getActivity()).getPreferredKeyserver());
                    }

                    startActivityForResult(intent, REQUEST_DELETE);
                    break;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;

            if (getAdapter() != null) {
                getAdapter().setMode(FlexibleAdapter.MODE_IDLE);
            }
        }
    };

    private void initializeActionModeHelper(int mode) {
        mActionModeHelper = new ActionModeHelper(getAdapter(), R.menu.key_list_multi, mActionCallback) {
            @Override
            public void updateContextTitle(int count) {
                if (mActionMode != null) {
                    mActionMode.setTitle(getResources().getQuantityString(R.plurals.key_list_selected_keys,
                            count, count));
                }
            }
        }.withDefaultMode(mode);
    }

    @Override
    public void onItemLongClick(int position) {
        mActionModeHelper.onLongClick((AppCompatActivity) getActivity(), position);
    }

    @Override
    public boolean onItemClick(int position) {
        // Action on elements are allowed if Mode is IDLE, otherwise selection has priority
        if (getAdapter().getMode() != FlexibleAdapter.MODE_IDLE && mActionModeHelper != null) {
            mConsumed = mActionModeHelper.onClick(position);
            return mConsumed;
        }
        // Handle the item click listener
        // We don't need to activate anything
        mConsumed = false;
        return false;
    }

    private final KeyItem.KeyListListener mKeyListener
            = new KeyItem.KeyListListener() {
        @Override
        public void onKeyDummyItemClicked() {
            createKey();
        }

        @Override
        public void onKeyItemClicked(int position) {
            if ((getAdapter().getMode() != FlexibleAdapter.MODE_IDLE && mActionModeHelper != null)
                    || mConsumed) {
                return;
            }

            long masterKeyId = getAdapter().getItemId(position);
            Intent viewIntent = new Intent(getActivity(), ViewKeyActivity.class);
            viewIntent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
            startActivityForResult(viewIntent, REQUEST_VIEW_KEY);
        }

        @Override
        public void onSlingerButtonClicked(int position) {
            long masterKeyId = getAdapter().getItemId(position);
            Intent safeSlingerIntent = new Intent(getActivity(), SafeSlingerActivity.class);
            safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
            startActivityForResult(safeSlingerIntent, REQUEST_ACTION);
        }

    };


    /**
     * Load custom layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.key_list_fragment, container, false);

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


        return view;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // show app name instead of "keys" from nav drawer
        final FragmentActivity activity = getActivity();
        activity.setTitle(R.string.app_name);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Start out with a progress indicator.
        hideList(false);

        // click on search button (in empty view) starts query for search string
        vSearchContainer = (ViewAnimator) activity.findViewById(R.id.search_container);
        vSearchButton = (Button) activity.findViewById(R.id.search_button);
        vSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearchForQuery();
            }
        });

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    private void startSearchForQuery() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent searchIntent = new Intent(activity, ImportKeysActivity.class);
        searchIntent.putExtra(ImportKeysActivity.EXTRA_QUERY, getQuery());
        searchIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER);
        startActivity(searchIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri uri;
        if (!TextUtils.isEmpty(getQuery())) {
            uri = KeyRings.buildUnifiedKeyRingsFindByUserIdUri(getQuery());
        } else {
            uri = KeyRings.buildUnifiedKeyRingsUri();
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), uri,
                PROJECTION, null, null,
                ORDER);
    }

    public static final String ORDER = KeychainContract.KeyRings.HAS_ANY_SECRET
            + " DESC, " + KeychainContract.KeyRings.USER_ID + " COLLATE NOCASE ASC";

    public static final String[] PROJECTION = {
            KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.IS_SECURE,
            KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID,
            KeychainContract.KeyRings.CREATION,
            KeychainContract.KeyRings.NAME,
            KeychainContract.KeyRings.EMAIL,
            KeychainContract.KeyRings.COMMENT,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.HAS_ENCRYPT};

    /* static {
        ArrayList<String> arr = new ArrayList<>();
        // arr.addAll(Arrays.asList(PROJECTION));
        arr.addAll(Arrays.asList(
                KeyRings._ID,
                KeychainContract.KeyRings.MASTER_KEY_ID,
                KeychainContract.KeyRings.USER_ID,
                KeychainContract.KeyRings.IS_REVOKED,
                KeychainContract.KeyRings.IS_EXPIRED,
                KeychainContract.KeyRings.IS_SECURE,
                KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID,
                KeychainContract.KeyRings.CREATION,
                KeychainContract.KeyRings.NAME,
                KeychainContract.KeyRings.EMAIL,
                KeychainContract.KeyRings.COMMENT,
                KeychainContract.KeyRings.VERIFIED,
                KeychainContract.KeyRings.HAS_ANY_SECRET,
                KeychainContract.KeyRings.FINGERPRINT,
                KeychainContract.KeyRings.HAS_ENCRYPT
        ));

        PROJECTION = arr.toArray(new String[arr.size()]);
    } */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mKeyItems.clear();

        if (data.moveToFirst()) {
            while (!data.isAfterLast()) {
                mKeyItems.add(new KeyItem(null, data, mKeyListener));
                data.moveToNext();
            }
        }

        data.close();

        KeyItem.setHeaders(getContext(), mKeyItems);

        List<KeyItem> keyItemList = new ArrayList<>(mKeyItems);
        if (getAdapter() == null) {
            KeyFlexibleAdapter<KeyItem> adapter = new KeyFlexibleAdapter<>(keyItemList);
            adapter.setDisplayHeadersAtStartUp(true)
                    .setStickyHeaders(true)
                    .setAnimationOnScrolling(true);
            setLayoutManager(new SmoothScrollLinearLayoutManager(getActivity()));
            setAdapter(adapter);
        } else {
            getAdapter().updateDataSet(keyItemList, true);
            getAdapter().notifyDataSetChanged();    // to refresh the existing view holder
        }
        getAdapter().addListener(this);
        initializeActionModeHelper(FlexibleAdapter.MODE_MULTI);

        FastScroller fastScroller = (FastScroller) getActivity().findViewById(R.id.fast_scroller);
        getAdapter().setFastScroller(fastScroller, Utils.colorAccent);

        // end action mode, if any
        if (mActionMode != null) {
            mActionMode.finish();
        }

        // The list should now be shown.
        showList(isResumed());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Since we retrieve all data at beginning now, it's no necessity
        // to reset the cursor.
    }

    private String getQuery() {
        if (getAdapter() == null) {
            return "";
        }
        return getAdapter().getSearchText();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.key_list, menu);

        if (Constants.DEBUG) {
            menu.findItem(R.id.menu_key_list_debug_cons).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_bench).setVisible(true);
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
                getAdapter().setSearchText(null);
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
            case R.id.menu_key_list_create: {
                createKey();
                return true;
            }
            case R.id.menu_key_list_update_all_keys: {
                updateAllKeys();
                return true;
            }
            case R.id.menu_key_list_debug_read: {
                try {
                    KeychainDatabase.debugBackup(getActivity(), true);
                    Notify.create(getActivity(), "Restored debug_backup.db", Notify.Style.OK).show();
                    getActivity().getContentResolver().notifyChange(KeychainContract.KeyRings.CONTENT_URI, null);
                } catch (IOException e) {
                    Log.e(TAG, "IO Error", e);
                    Notify.create(getActivity(), "IO Error " + e.getMessage(), Notify.Style.ERROR).show();
                }
                return true;
            }
            case R.id.menu_key_list_debug_write: {
                try {
                    KeychainDatabase.debugBackup(getActivity(), false);
                    Notify.create(getActivity(), "Backup to debug_backup.db completed", Notify.Style.OK).show();
                } catch (IOException e) {
                    Log.e(TAG, "IO Error", e);
                    Notify.create(getActivity(), "IO Error: " + e.getMessage(), Notify.Style.ERROR).show();
                }
                return true;
            }
            case R.id.menu_key_list_debug_first_time: {
                Preferences prefs = Preferences.getPreferences(getActivity());
                prefs.setFirstTime(true);
                Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
                intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true);
                startActivity(intent);
                getActivity().finish();
                return true;
            }
            case R.id.menu_key_list_debug_cons: {
                consolidate();
                return true;
            }
            case R.id.menu_key_list_debug_bench: {
                benchmark();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        Log.d(TAG, "onQueryTextChange s:" + s);
        // Called when the action bar search text has changed.  Update the
        // search filter, and restart the loader to do a new query with this
        // filter.
        // If the nav drawer is opened, onQueryTextChange("") is executed.
        // This hack prevents restarting the loader.

        if (!s.equals(getQuery())) {
            getAdapter().setSearchText(s);
            getLoaderManager().restartLoader(0, null, this);
        }

        if (s.length() > 2) {
            vSearchButton.setText(getString(R.string.btn_search_for_query, getQuery()));
            vSearchContainer.setDisplayedChild(1);
            vSearchContainer.setVisibility(View.VISIBLE);
        } else {
            vSearchContainer.setDisplayedChild(0);
            vSearchContainer.setVisibility(View.GONE);
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
            Notify.create(activity, R.string.error_loading_keys, Notify.Style.ERROR);
            return;
        }

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                byte[] blob = cursor.getBlob(0); //fingerprint column is 0
                String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);
                ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null, null, null);
                keyList.add(keyEntry);
            }
            mKeyList = keyList;
        } finally {
            cursor.close();
        }

        // search config
        mKeyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();

        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> callback
                = new CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult>() {

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

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mImportOpHelper = new CryptoOperationHelper<>(1, this, callback, R.string.progress_updating);
        mImportOpHelper.setProgressCancellable(true);
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

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mConsolidateOpHelper = new CryptoOperationHelper<>(2, this, callback, R.string.progress_importing);
        mConsolidateOpHelper.cryptoOperation();
    }

    private void benchmark() {
        CryptoOperationHelper.Callback<BenchmarkInputParcel, BenchmarkResult> callback
                = new CryptoOperationHelper.Callback<BenchmarkInputParcel, BenchmarkResult>() {

            @Override
            public BenchmarkInputParcel createOperationInput() {
                return new BenchmarkInputParcel(); // we want to perform a full consolidate
            }

            @Override
            public void onCryptoOperationSuccess(BenchmarkResult result) {
                result.createNotify(getActivity()).show();
            }

            @Override
            public void onCryptoOperationCancelled() {
            }

            @Override
            public void onCryptoOperationError(BenchmarkResult result) {
                result.createNotify(getActivity()).show();
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        CryptoOperationHelper opHelper = new CryptoOperationHelper<>(2, this, callback, R.string.progress_importing);
        opHelper.cryptoOperation();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mImportOpHelper != null) {
            mImportOpHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (mConsolidateOpHelper != null) {
            mConsolidateOpHelper.handleActivityResult(requestCode, resultCode, data);
        }

        switch (requestCode) {
            case REQUEST_DELETE: {
                if (mActionMode != null) {
                    mActionMode.finish();
                }

                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                    result.createNotify(getActivity()).show();
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                break;
            }
            case REQUEST_ACTION: {
                // if a result has been returned, display a notify
                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                    result.createNotify(getActivity()).show();
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                break;
            }
            case REQUEST_VIEW_KEY: {
                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                    result.createNotify(getActivity()).show();
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                break;
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

    static final class KeyFlexibleAdapter<T extends IFlexible> extends FlexibleAdapter<T> {

        KeyFlexibleAdapter(@Nullable List<T> items) {
            super(items);
        }

        @Override
        public String onCreateBubbleText(int position) {
            if (getItem(position) instanceof KeyItem) {
                KeyItem keyItem = (KeyItem) getItem(position);
                if (keyItem.isSecret()) {
                    return "My";
                } else if (keyItem.getName() != null) {
                    return keyItem.getSection();
                } else {
                    return null;
                }
            } else if (getItem(position) instanceof KeyHeaderItem) {
                KeyHeaderItem keyHeaderItem = (KeyHeaderItem) getItem(position);
                return keyHeaderItem.getTitle();
            }
            return null;
        }

        long[] getSelectedMasterKeyIds() {
            long[] ids = new long[getSelectedItemCount()];
            for (int i = 0; i < getSelectedPositions().size(); i++) {
                int position = getSelectedPositions().get(i);
                ids[i] = (((KeyItem) getItem(position)).getKeyId());
            }
            return ids;
        }

        boolean isAnySecretKeySelected() {
            for (Integer position : getSelectedPositions()) {
                if (((KeyItem) getItem(position)).isSecret()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long getItemId(int position) {
            return ((KeyItem) getItem(position)).getKeyId();
        }
    }
}
