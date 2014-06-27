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
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.ui.adapter.AsyncTaskResultWrapper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListKeybaseLoader;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListLoader;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListServerLoader;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportKeysListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {
    private static final String ARG_DATA_URI = "uri";
    private static final String ARG_BYTES = "bytes";
    private static final String ARG_SERVER_QUERY = "query";

    private Activity mActivity;
    private ImportKeysAdapter mAdapter;

    private LoaderState mLoaderState;

    private static final int LOADER_ID_BYTES = 0;
    private static final int LOADER_ID_SERVER_QUERY = 1;
    private static final int LOADER_ID_KEYBASE = 2;

    private LongSparseArray<ParcelableKeyRing> mCachedKeyData;

    public LoaderState getLoaderState() {
        return mLoaderState;
    }

    public List<ImportKeysListEntry> getData() {
        return mAdapter.getData();
    }

    public ArrayList<ParcelableKeyRing> getSelectedData() {
        ArrayList<ParcelableKeyRing> result = new ArrayList<ParcelableKeyRing>();
        for (ImportKeysListEntry entry : getSelectedEntries()) {
            result.add(mCachedKeyData.get(entry.hashCode()));
        }
        return result;
    }

    public ArrayList<ImportKeysListEntry> getSelectedEntries() {
        return mAdapter.getSelectedEntries();
    }

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysListFragment newInstance(byte[] bytes, Uri dataUri, String serverQuery) {
        ImportKeysListFragment frag = new ImportKeysListFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTES, bytes);
        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putString(ARG_SERVER_QUERY, serverQuery);

        frag.setArguments(args);

        return frag;
    }

    static public class LoaderState {
    }

    static public class BytesLoaderState extends LoaderState {
        byte[] keyBytes;
        Uri dataUri;

        BytesLoaderState(byte[] keyBytes, Uri dataUri) {
            this.keyBytes = keyBytes;
            this.dataUri = dataUri;
        }
    }

    static public class KeyserverLoaderState extends LoaderState {
        String serverQuery;
        String keyserver;

        KeyserverLoaderState(String serverQuery, String keyserver) {
            this.serverQuery = serverQuery;
            this.keyserver = keyserver;
        }
    }

    static public class KeybaseLoaderState extends LoaderState {
        String keybaseQuery;

        KeybaseLoaderState(String keybaseQuery) {
            this.keybaseQuery = keybaseQuery;
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

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        byte[] bytes = getArguments().getByteArray(ARG_BYTES);
        String query = getArguments().getString(ARG_SERVER_QUERY);

        if (dataUri != null || bytes != null) {
            mLoaderState = new BytesLoaderState(bytes, dataUri);
        } else if (query != null) {
            // TODO: this is used when updating a key.
            // Currently it simply uses keyserver nr 0
            String keyserver = Preferences.getPreferences(getActivity())
                    .getKeyServers()[0];
            mLoaderState = new KeyserverLoaderState(query, keyserver);
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

        restartLoaders();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

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
        if (getLoaderManager().getLoader(LOADER_ID_SERVER_QUERY) != null) {
            getLoaderManager().destroyLoader(LOADER_ID_SERVER_QUERY);
        }
        if (getLoaderManager().getLoader(LOADER_ID_KEYBASE) != null) {
            getLoaderManager().destroyLoader(LOADER_ID_KEYBASE);
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
        } else if (mLoaderState instanceof KeyserverLoaderState) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_SERVER_QUERY, null, this);
        } else if (mLoaderState instanceof KeybaseLoaderState) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_KEYBASE, null, this);
        }
    }

    @Override
    public Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>>
    onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_BYTES: {
                BytesLoaderState ls = (BytesLoaderState) mLoaderState;
                InputData inputData = getInputData(ls.keyBytes, ls.dataUri);
                return new ImportKeysListLoader(mActivity, inputData);
            }
            case LOADER_ID_SERVER_QUERY: {
                KeyserverLoaderState ls = (KeyserverLoaderState) mLoaderState;
                return new ImportKeysListServerLoader(getActivity(), ls.serverQuery, ls.keyserver);
            }
            case LOADER_ID_KEYBASE: {
                KeybaseLoaderState ls = (KeybaseLoaderState) mLoaderState;
                return new ImportKeysListKeybaseLoader(getActivity(), ls.keybaseQuery);
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

        Exception error = data.getError();

        // free old cached key data
        mCachedKeyData = null;

        switch (loader.getId()) {
            case LOADER_ID_BYTES:

                if (error == null) {
                    // No error
                    mCachedKeyData = ((ImportKeysListLoader) loader).getParcelableRings();
                } else if (error instanceof ImportKeysListLoader.FileHasNoContent) {
                    Notify.showNotify(getActivity(), R.string.error_import_file_no_content, Notify.Style.ERROR);
                } else if (error instanceof ImportKeysListLoader.NonPgpPart) {
                    Notify.showNotify(getActivity(),
                            ((ImportKeysListLoader.NonPgpPart) error).getCount() + " " + getResources().
                                    getQuantityString(R.plurals.error_import_non_pgp_part,
                                            ((ImportKeysListLoader.NonPgpPart) error).getCount()),
                            Notify.Style.OK
                    );
                } else {
                    Notify.showNotify(getActivity(), R.string.error_generic_report_bug, Notify.Style.ERROR);
                }
                break;

            case LOADER_ID_SERVER_QUERY:
            case LOADER_ID_KEYBASE:

                if (error == null) {
                    // No error
                } else if (error instanceof Keyserver.QueryTooShortException) {
                    Notify.showNotify(getActivity(), R.string.error_keyserver_insufficient_query, Notify.Style.ERROR);
                } else if (error instanceof Keyserver.TooManyResponsesException) {
                    Notify.showNotify(getActivity(), R.string.error_keyserver_too_many_responses, Notify.Style.ERROR);
                } else if (error instanceof Keyserver.QueryFailedException) {
                    Log.d(Constants.TAG,
                            "Unrecoverable keyserver query error: " + error.getLocalizedMessage());
                    String alert = getActivity().getString(R.string.error_searching_keys);
                    alert = alert + " (" + error.getLocalizedMessage() + ")";
                    Notify.showNotify(getActivity(), alert, Notify.Style.ERROR);
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
            case LOADER_ID_SERVER_QUERY:
                // Clear the data in the adapter.
                mAdapter.clear();
                break;
            case LOADER_ID_KEYBASE:
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
                int length = inputStream.available();

                inputData = new InputData(inputStream, length);
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "FileNotFoundException!", e);
            } catch (IOException e) {
                Log.e(Constants.TAG, "IOException!", e);
            }
        }

        return inputData;
    }

}
