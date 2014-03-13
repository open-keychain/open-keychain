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
import android.view.View;
import android.widget.ListView;
import com.devspark.appmsg.AppMsg;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.ui.adapter.*;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.KeyServer;
import org.sufficientlysecure.keychain.util.Log;

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

    private byte[] mKeyBytes;
    private Uri mDataUri;
    private String mServerQuery;
    private String mKeyServer;

    private static final int LOADER_ID_BYTES = 0;
    private static final int LOADER_ID_SERVER_QUERY = 1;

    public byte[] getKeyBytes() {
        return mKeyBytes;
    }

    public Uri getDataUri() {
        return mDataUri;
    }

    public String getServerQuery() {
        return mServerQuery;
    }

    public String getKeyServer() {
        return mKeyServer;
    }

    public List<ImportKeysListEntry> getData() {
        return mAdapter.getData();
    }

    public ArrayList<ImportKeysListEntry> getSelectedData() {
        return mAdapter.getSelectedData();
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

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(mActivity.getString(R.string.error_nothing_import));

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new ImportKeysAdapter(mActivity);
        setListAdapter(mAdapter);

        mDataUri = getArguments().getParcelable(ARG_DATA_URI);
        mKeyBytes = getArguments().getByteArray(ARG_BYTES);
        mServerQuery = getArguments().getString(ARG_SERVER_QUERY);

        // TODO: this is used when scanning QR Code. Currently it simply uses keyserver nr 0
        mKeyServer = Preferences.getPreferences(getActivity())
                .getKeyServers()[0];

        if (mDataUri != null || mKeyBytes != null) {
            // Start out with a progress indicator.
            setListShown(false);

            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            // give arguments to onCreateLoader()
            getLoaderManager().initLoader(LOADER_ID_BYTES, null, this);
        }

        if (mServerQuery != null && mKeyServer != null) {
            // Start out with a progress indicator.
            setListShown(false);

            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            // give arguments to onCreateLoader()
            getLoaderManager().initLoader(LOADER_ID_SERVER_QUERY, null, this);
        }
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

    public void loadNew(byte[] keyBytes, Uri dataUri, String serverQuery, String keyServer) {
        mKeyBytes = keyBytes;
        mDataUri = dataUri;
        mServerQuery = serverQuery;
        mKeyServer = keyServer;

        if (mKeyBytes != null || mDataUri != null) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_BYTES, null, this);
        }

        if (mServerQuery != null && mKeyServer != null) {
            // Start out with a progress indicator.
            setListShown(false);

            getLoaderManager().restartLoader(LOADER_ID_SERVER_QUERY, null, this);
        }
    }

    @Override
    public Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>>
                                        onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_BYTES: {
                InputData inputData = getInputData(mKeyBytes, mDataUri);
                return new ImportKeysListLoader(mActivity, inputData);
            }
            case LOADER_ID_SERVER_QUERY: {
                return new ImportKeysListServerLoader(getActivity(), mServerQuery, mKeyServer);
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

        switch (loader.getId()) {
            case LOADER_ID_BYTES:

                if (error == null) {
                    // No error
                } else if (error instanceof ImportKeysListLoader.FileHasNoContent) {
                    AppMsg.makeText(getActivity(), R.string.error_import_file_no_content,
                            AppMsg.STYLE_ALERT).show();
                } else if (error instanceof ImportKeysListLoader.NonPgpPart) {
                    AppMsg.makeText(getActivity(),
                            ((ImportKeysListLoader.NonPgpPart) error).getCount() + " " + getResources().
                                    getQuantityString(R.plurals.error_import_non_pgp_part,
                                            ((ImportKeysListLoader.NonPgpPart) error).getCount()),
                            new AppMsg.Style(AppMsg.LENGTH_LONG, R.color.confirm)).show();
                } else {
                    AppMsg.makeText(getActivity(), R.string.error_generic_report_bug,
                            new AppMsg.Style(AppMsg.LENGTH_LONG, R.color.alert)).show();
                }
                break;

            case LOADER_ID_SERVER_QUERY:

                if (error == null) {
                    AppMsg.makeText(
                            getActivity(), getResources().getQuantityString(R.plurals.keys_found,
                            mAdapter.getCount(), mAdapter.getCount()),
                            AppMsg.STYLE_INFO
                    ).show();
                } else if (error instanceof KeyServer.InsufficientQuery) {
                    AppMsg.makeText(getActivity(), R.string.error_keyserver_insufficient_query,
                            AppMsg.STYLE_ALERT).show();
                } else if (error instanceof KeyServer.QueryException) {
                    AppMsg.makeText(getActivity(), R.string.error_keyserver_query,
                            AppMsg.STYLE_ALERT).show();
                } else if (error instanceof KeyServer.TooManyResponses) {
                    AppMsg.makeText(getActivity(), R.string.error_keyserver_too_many_responses,
                            AppMsg.STYLE_ALERT).show();
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
