/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListLoader;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

public class ImportKeysListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<List<ImportKeysListEntry>> {
    private static final String ARG_FILENAME = "filename";
    private static final String ARG_BYTES = "bytes";

    private Activity mActivity;
    private ImportKeysAdapter mAdapter;

    private byte[] mKeyBytes;
    private String mImportFilename;

    public byte[] getKeyBytes() {
        return mKeyBytes;
    }

    public String getImportFilename() {
        return mImportFilename;
    }

    public List<ImportKeysListEntry> getData() {
        return mAdapter.getData();
    }

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysListFragment newInstance(byte[] bytes, String filename) {
        ImportKeysListFragment frag = new ImportKeysListFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTES, bytes);
        args.putString(ARG_FILENAME, filename);

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

        if (getArguments() != null) {
            mImportFilename = getArguments().getString(ARG_FILENAME);
            mKeyBytes = getArguments().getByteArray(ARG_BYTES);
        }

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(mActivity.getString(R.string.error_nothing_import));

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new ImportKeysAdapter(mActivity);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        // give arguments to onCreateLoader()
        getLoaderManager().initLoader(0, null, this);
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

    public void loadNew(byte[] importData, String importFilename) {
        this.mKeyBytes = importData;
        this.mImportFilename = importFilename;

        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<List<ImportKeysListEntry>> onCreateLoader(int id, Bundle args) {
        InputData inputData = getInputData(mKeyBytes, mImportFilename);
        return new ImportKeysListLoader(mActivity, inputData);
    }

    private InputData getInputData(byte[] importBytes, String importFilename) {
        InputData inputData = null;
        if (importBytes != null) {
            inputData = new InputData(new ByteArrayInputStream(importBytes), importBytes.length);
        } else if (importFilename != null) {
            try {
                inputData = new InputData(new FileInputStream(importFilename),
                        importFilename.length());
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "Failed to init FileInputStream!", e);
            }
        }

        return inputData;
    }

    @Override
    public void onLoadFinished(Loader<List<ImportKeysListEntry>> loader,
            List<ImportKeysListEntry> data) {
        Log.d(Constants.TAG, "data: " + data);

        // swap in the real data!
        mAdapter.setData(data);
        mAdapter.notifyDataSetChanged();

        setListAdapter(mAdapter);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ImportKeysListEntry>> loader) {
        // Clear the data in the adapter.
        mAdapter.clear();
    }

}
