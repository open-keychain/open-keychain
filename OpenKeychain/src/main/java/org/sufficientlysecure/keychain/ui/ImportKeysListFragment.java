/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.databinding.ImportKeysListFragmentBinding;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.processing.AsyncTaskResultWrapper;
import org.sufficientlysecure.keychain.keyimport.processing.BytesLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.CloudLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListCloudLoader;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListLoader;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.keyimport.processing.LoaderState;
import org.sufficientlysecure.keychain.operations.results.GetKeyResult;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter;
import org.sufficientlysecure.keychain.ui.util.PermissionsUtil;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Preferences.CloudSearchPrefs;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.util.ArrayList;

public class ImportKeysListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    private static final String ARG_DATA_URI = "uri";
    private static final String ARG_BYTES = "bytes";
    public static final String ARG_SERVER_QUERY = "query";
    public static final String ARG_NON_INTERACTIVE = "non_interactive";
    public static final String ARG_CLOUD_SEARCH_PREFS = "cloud_search_prefs";

    private FragmentActivity mActivity;
    private ImportKeysListener mListener;

    private ImportKeysListFragmentBinding mBinding;
    private ParcelableProxy mParcelableProxy;

    private ImportKeysAdapter mAdapter;
    private LoaderState mLoaderState;

    public static final int STATUS_FIRST = 0;
    public static final int STATUS_LOADING = 1;
    public static final int STATUS_LOADED = 2;
    public static final int STATUS_EMPTY = 3;

    private static final int LOADER_ID_BYTES = 0;
    private static final int LOADER_ID_CLOUD = 1;

    private boolean mShowingOrbotDialog;

    /**
     * Creates an interactive ImportKeyListFragment which reads keyrings from bytes, or file specified
     * by dataUri, or searches a keyserver for serverQuery, if parameter is not null, in that order
     * Will immediately load data if non-null bytes/dataUri/serverQuery
     *
     * @param bytes            byte data containing list of keyrings to be imported
     * @param dataUri          file from which keyrings are to be imported
     * @param serverQuery      query to search for on keyserver
     * @param cloudSearchPrefs search parameters to use. If null will retrieve from user's
     *                         preferences.
     * @return fragment with arguments set based on passed parameters
     */
    public static ImportKeysListFragment newInstance(byte[] bytes, Uri dataUri, String serverQuery,
                                                     CloudSearchPrefs cloudSearchPrefs) {

        return newInstance(bytes, dataUri, serverQuery, false, cloudSearchPrefs);
    }

    /**
     * Visually consists of a list of keyrings with checkboxes to specify which are to be imported
     * Will immediately load data if non-null bytes/dataUri/serverQuery is supplied
     *
     * @param bytes            byte data containing list of keyrings to be imported
     * @param dataUri          file from which keyrings are to be imported
     * @param serverQuery      query to search for on keyserver
     * @param nonInteractive   if true, users will not be able to check/uncheck items in the list
     * @param cloudSearchPrefs search parameters to use. If null will retrieve from user's
     *                         preferences.
     * @return fragment with arguments set based on passed parameters
     */
    public static ImportKeysListFragment newInstance(byte[] bytes,
                                                     Uri dataUri,
                                                     String serverQuery,
                                                     boolean nonInteractive,
                                                     CloudSearchPrefs cloudSearchPrefs) {

        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTES, bytes);
        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putString(ARG_SERVER_QUERY, serverQuery);
        args.putBoolean(ARG_NON_INTERACTIVE, nonInteractive);
        args.putParcelable(ARG_CLOUD_SEARCH_PREFS, cloudSearchPrefs);

        ImportKeysListFragment frag = new ImportKeysListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.import_keys_list_fragment, container, false);
        mBinding.setStatus(STATUS_FIRST);
        View view = mBinding.getRoot();

        mActivity = getActivity();

        Bundle args = getArguments();
        Uri dataUri = args.getParcelable(ARG_DATA_URI);
        byte[] bytes = args.getByteArray(ARG_BYTES);
        String query = args.getString(ARG_SERVER_QUERY);
        boolean nonInteractive = args.getBoolean(ARG_NON_INTERACTIVE, false);
        mBinding.basic.setNonInteractive(nonInteractive);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new ImportKeysAdapter(mActivity, mListener, nonInteractive);
        mBinding.recyclerView.setAdapter(mAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));

        if (dataUri != null || bytes != null) {
            loadState(new BytesLoaderState(bytes, dataUri));
        } else if (query != null) {
            CloudSearchPrefs cloudSearchPrefs = args.getParcelable(ARG_CLOUD_SEARCH_PREFS);
            if (cloudSearchPrefs == null) {
                cloudSearchPrefs = Preferences.getPreferences(mActivity).getCloudSearchPrefs();
            }
            loadState(new CloudLoaderState(query, cloudSearchPrefs));
        }

        // mBinding.basic is only used for file import
        mBinding.basic.importKeys.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.importKeys(mAdapter.getEntries());
            }
        });
        mBinding.basic.listKeys.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mBinding.setAdvanced(true);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (ImportKeysListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ImportKeysListener");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (PermissionsUtil.checkReadPermissionResult(mActivity, requestCode, grantResults)) {
            restartLoaders();
        } else {
            mActivity.setResult(Activity.RESULT_CANCELED);
            mActivity.finish();
        }
    }

    /**
     * User may want to go back to single card view if he's now in full key list
     * Check if we are in full key list and if this import operation supports basic mode
     *
     * @return true if activity's back pressed can be performed
     */
    public boolean onBackPressed() {
        boolean advanced = mBinding.getAdvanced();
        if (advanced && mLoaderState.isBasicModeSupported()) {
            mBinding.setAdvanced(false);
            return false;
        }
        return true;
    }

    public void loadState(LoaderState loaderState) {
        mLoaderState = loaderState;

        if (mLoaderState instanceof BytesLoaderState) {
            BytesLoaderState ls = (BytesLoaderState) mLoaderState;

            if (ls.mDataUri != null &&
                    !PermissionsUtil.checkAndRequestReadPermission(this, ls.mDataUri)) {
                return;
            }
        }
        mBinding.setAdvanced(!mLoaderState.isBasicModeSupported());

        restartLoaders();
    }

    private void restartLoaders() {
        LoaderManager loaderManager = getLoaderManager();

        if (mLoaderState instanceof BytesLoaderState) {
            loaderManager.restartLoader(LOADER_ID_BYTES, null, this);
        } else if (mLoaderState instanceof CloudLoaderState) {
            loaderManager.restartLoader(LOADER_ID_CLOUD, null, this);
        }
    }

    @Override
    public Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> onCreateLoader(
            int id, Bundle args) {

        Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader = null;
        switch (id) {
            case LOADER_ID_BYTES: {
                loader = new ImportKeysListLoader(mActivity, (BytesLoaderState) mLoaderState);
                break;
            }
            case LOADER_ID_CLOUD: {
                loader = new ImportKeysListCloudLoader(mActivity, (CloudLoaderState) mLoaderState,
                        mParcelableProxy);
                break;
            }
        }

        if (loader != null) {
            mBinding.setStatus(STATUS_LOADING);
        }

        return loader;
    }

    @Override
    public void onLoadFinished(
            Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader,
            AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> data) {

        mAdapter.setData(data.getResult());
        int size = mAdapter.getItemCount();

        mBinding.setNumber(size);
        mBinding.setStatus(size > 0 ? STATUS_LOADED : STATUS_EMPTY);

        GetKeyResult getKeyResult = (GetKeyResult) data.getOperationResult();
        switch (loader.getId()) {
            case LOADER_ID_BYTES:
                if (!getKeyResult.success()) {
                    getKeyResult.createNotify(mActivity).show();
                }
                break;
            case LOADER_ID_CLOUD:
                if (getKeyResult.isPending()) {
                    if (getKeyResult.getRequiredInputParcel().mType ==
                            RequiredInputParcel.RequiredInputType.ENABLE_ORBOT) {
                        if (mShowingOrbotDialog) {
                            // to prevent dialogs stacking
                            return;
                        }

                        // this is because we can't commit fragment dialogs in onLoadFinished
                        Runnable showOrbotDialog = new Runnable() {
                            @Override
                            public void run() {
                                OrbotHelper.DialogActions dialogActions =
                                        new OrbotHelper.DialogActions() {
                                            @Override
                                            public void onOrbotStarted() {
                                                mShowingOrbotDialog = false;
                                                restartLoaders();
                                            }

                                            @Override
                                            public void onNeutralButton() {
                                                mParcelableProxy = ParcelableProxy
                                                        .getForNoProxy();
                                                mShowingOrbotDialog = false;
                                                restartLoaders();
                                            }

                                            @Override
                                            public void onCancel() {
                                                mShowingOrbotDialog = false;
                                            }
                                        };

                                if (OrbotHelper.putOrbotInRequiredState(dialogActions, mActivity)) {
                                    // looks like we didn't have to show the
                                    // dialog after all
                                    mShowingOrbotDialog = false;
                                    restartLoaders();
                                }
                            }
                        };
                        new Handler().post(showOrbotDialog);
                        mShowingOrbotDialog = true;
                    }
                } else if (!getKeyResult.success()) {
                    getKeyResult.createNotify(mActivity).show();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(
            Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader) {

        mAdapter.clearData();
    }

}
