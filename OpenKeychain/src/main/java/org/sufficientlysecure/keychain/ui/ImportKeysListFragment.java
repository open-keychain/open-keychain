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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.GetKeyResult;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.AsyncTaskResultWrapper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListCloudLoader;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListLoader;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportKeysListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    private static final String ARG_DATA_URI = "uri";
    private static final String ARG_BYTES = "bytes";
    public static final String ARG_SERVER_QUERY = "query";
    public static final String ARG_NON_INTERACTIVE = "non_interactive";
    public static final String ARG_KEYSERVER_URL = "keyserver_url";

    private Activity mActivity;
    private ImportKeysAdapter mAdapter;
    private ParcelableProxy mParcelableProxy;

    private LoaderState mLoaderState;

    private static final int LOADER_ID_BYTES = 0;
    private static final int LOADER_ID_CLOUD = 1;

    private LongSparseArray<ParcelableKeyRing> mCachedKeyData;
    private boolean mNonInteractive;

    private boolean mShowingOrbotDialog;

    public LoaderState getLoaderState() {
        return mLoaderState;
    }

    public List<ImportKeysListEntry> getData() {
        return mAdapter.getData();
    }

    /**
     * Returns an Iterator (with size) of the selected data items.
     * This iterator is sort of a tradeoff, it's slightly more complex than an
     * ArrayList would have been, but we save some memory by just returning
     * relevant elements on demand.
     */
    public IteratorWithSize<ParcelableKeyRing> getSelectedData() {
        final ArrayList<ImportKeysListEntry> entries = getSelectedEntries();
        final Iterator<ImportKeysListEntry> it = entries.iterator();
        return new IteratorWithSize<ParcelableKeyRing>() {

            @Override
            public int getSize() {
                return entries.size();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public ParcelableKeyRing next() {
                // throws NoSuchElementException if it doesn't exist, but that's not our problem
                return mCachedKeyData.get(it.next().hashCode());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    public ArrayList<ImportKeysListEntry> getSelectedEntries() {
        if (mAdapter != null) {
            return mAdapter.getSelectedEntries();
        } else {
            Log.e(Constants.TAG, "Adapter not initialized, returning empty list");
            return new ArrayList<>();
        }

    }

    /**
     * Creates an interactive ImportKeyListFragment which reads keyrings from bytes, or file specified
     * by dataUri, or searches a keyserver for serverQuery, if parameter is not null, in that order
     * Will immediately load data if non-null bytes/dataUri/serverQuery
     *
     * @param bytes       byte data containing list of keyrings to be imported
     * @param dataUri     file from which keyrings are to be imported
     * @param serverQuery query to search for on keyserver
     * @param keyserver   if not null, will perform search on specified keyserver. Else, uses
     *                    keyserver specified in user preferences
     * @return fragment with arguments set based on passed parameters
     */
    public static ImportKeysListFragment newInstance(byte[] bytes, Uri dataUri, String serverQuery,
                                                     String keyserver) {
        return newInstance(bytes, dataUri, serverQuery, false, keyserver);
    }

    /**
     * Visually consists of a list of keyrings with checkboxes to specify which are to be imported
     * Will immediately load data if non-null bytes/dataUri/serverQuery is supplied
     *
     * @param bytes          byte data containing list of keyrings to be imported
     * @param dataUri        file from which keyrings are to be imported
     * @param serverQuery    query to search for on keyserver
     * @param nonInteractive if true, users will not be able to check/uncheck items in the list
     * @param keyserver      if set, will perform search on specified keyserver. If null, falls back
     *                       to keyserver specified in user preferences
     * @return fragment with arguments set based on passed parameters
     */
    public static ImportKeysListFragment newInstance(byte[] bytes, Uri dataUri, String serverQuery,
                                                     boolean nonInteractive, String keyserver) {
        ImportKeysListFragment frag = new ImportKeysListFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTES, bytes);
        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putString(ARG_SERVER_QUERY, serverQuery);
        args.putBoolean(ARG_NON_INTERACTIVE, nonInteractive);
        args.putString(ARG_KEYSERVER_URL, keyserver);

        frag.setArguments(args);

        return frag;
    }

    static public class LoaderState {
    }

    static public class BytesLoaderState extends LoaderState {
        byte[] mKeyBytes;
        Uri mDataUri;

        BytesLoaderState(byte[] keyBytes, Uri dataUri) {
            mKeyBytes = keyBytes;
            mDataUri = dataUri;
        }
    }

    static public class CloudLoaderState extends LoaderState {
        Preferences.CloudSearchPrefs mCloudPrefs;
        String mServerQuery;

        CloudLoaderState(String serverQuery, Preferences.CloudSearchPrefs cloudPrefs) {
            mServerQuery = serverQuery;
            mCloudPrefs = cloudPrefs;
        }
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();

        // Give some text to display if there is no data.
        setEmptyText(mActivity.getString(R.string.error_nothing_import));

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new ImportKeysAdapter(mActivity);
        setListAdapter(mAdapter);

        Bundle args = getArguments();
        Uri dataUri = args.getParcelable(ARG_DATA_URI);
        byte[] bytes = args.getByteArray(ARG_BYTES);
        String query = args.getString(ARG_SERVER_QUERY);
        String keyserver = args.getString(ARG_KEYSERVER_URL);
        mNonInteractive = args.getBoolean(ARG_NON_INTERACTIVE, false);

        if (dataUri != null || bytes != null) {
            mLoaderState = new BytesLoaderState(bytes, dataUri);
        } else if (query != null) {
            Preferences.CloudSearchPrefs cloudSearchPrefs;
            if (keyserver == null) {
                cloudSearchPrefs = Preferences.getPreferences(getActivity()).getCloudSearchPrefs();
            } else {
                cloudSearchPrefs = new Preferences.CloudSearchPrefs(true, true, keyserver);
            }

            mLoaderState = new CloudLoaderState(query, cloudSearchPrefs);
        }

        getListView().setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mAdapter.isEmpty()) {
                    mActivity.onTouchEvent(event);
                }
                return false;
            }
        });

        getListView().setFastScrollEnabled(true);

        restartLoaders();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mNonInteractive) {
            return;
        }

        // Select checkbox!
        // Update underlying data and notify adapter of change. The adapter will
        // update the view automatically.

        ImportKeysListEntry entry = mAdapter.getItem(position);
        entry.setSelected(!entry.isSelected());
        mAdapter.notifyDataSetChanged();
    }

    public void loadNew(LoaderState loaderState) {

        mLoaderState = loaderState;

        restartLoaders();
    }

    public void destroyLoader() {
        if (getLoaderManager().getLoader(LOADER_ID_BYTES) != null) {
            getLoaderManager().destroyLoader(LOADER_ID_BYTES);
        }
        if (getLoaderManager().getLoader(LOADER_ID_CLOUD) != null) {
            getLoaderManager().destroyLoader(LOADER_ID_CLOUD);
        }
        if (getView() != null) {
            setListShown(true);
        }
    }

    private void restartLoaders() {
        if (mLoaderState instanceof BytesLoaderState) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_BYTES, null, this);
        } else if (mLoaderState instanceof CloudLoaderState) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_CLOUD, null, this);
        }
    }

    @Override
    public Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>>
    onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_BYTES: {
                BytesLoaderState ls = (BytesLoaderState) mLoaderState;
                InputData inputData = getInputData(ls.mKeyBytes, ls.mDataUri);
                return new ImportKeysListLoader(mActivity, inputData);
            }
            case LOADER_ID_CLOUD: {
                CloudLoaderState ls = (CloudLoaderState) mLoaderState;
                return new ImportKeysListCloudLoader(getActivity(), ls.mServerQuery, ls.mCloudPrefs,
                        mParcelableProxy);
            }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader,
                               AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)

        Log.d(Constants.TAG, "data: " + data.getResult());

        // swap in the real data!
        mAdapter.setData(data.getResult());
        mAdapter.notifyDataSetChanged();

        setListAdapter(mAdapter);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }

        // free old cached key data
        mCachedKeyData = null;

        GetKeyResult getKeyResult = (GetKeyResult) data.getOperationResult();
        switch (loader.getId()) {
            case LOADER_ID_BYTES:

                if (getKeyResult.success()) {
                    // No error
                    mCachedKeyData = ((ImportKeysListLoader) loader).getParcelableRings();
                } else {
                    getKeyResult.createNotify(getActivity()).show();
                }
                break;

            case LOADER_ID_CLOUD:

                if (getKeyResult.success()) {
                    // No error
                } else if (getKeyResult.isPending()) {
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

                                if (OrbotHelper.putOrbotInRequiredState(dialogActions,
                                        getActivity())) {
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
                } else {
                    getKeyResult.createNotify(getActivity()).show();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader) {
        switch (loader.getId()) {
            case LOADER_ID_BYTES:
                // Clear the data in the adapter.
                mAdapter.clear();
                break;
            case LOADER_ID_CLOUD:
                // Clear the data in the adapter.
                mAdapter.clear();
                break;
            default:
                break;
        }
    }

    private InputData getInputData(byte[] importBytes, Uri dataUri) {
        InputData inputData = null;
        if (importBytes != null) {
            inputData = new InputData(new ByteArrayInputStream(importBytes), importBytes.length);
        } else if (dataUri != null) {
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(dataUri);
                long length = FileHelper.getFileSize(getActivity(), dataUri, -1);

                inputData = new InputData(inputStream, length);
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "FileNotFoundException!", e);
                return null;
            }
        }

        return inputData;
    }

}
