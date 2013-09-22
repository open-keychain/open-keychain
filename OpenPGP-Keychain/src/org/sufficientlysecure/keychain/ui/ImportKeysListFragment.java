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

import java.util.List;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListLoader;
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
    public static ImportKeysListFragment newInstance() {
        ImportKeysListFragment frag = new ImportKeysListFragment();

        return frag;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();

        // register long press context menu
        registerForContextMenu(getListView());

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(mActivity.getString(R.string.error_nothingImport));

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new ImportKeysAdapter(getActivity());
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
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

    public void load(byte[] importData, String importFilename) {
        this.mKeyBytes = importData;
        this.mImportFilename = importFilename;
    }

    @Override
    public Loader<List<ImportKeysListEntry>> onCreateLoader(int id, Bundle args) {
        return new ImportKeysListLoader(mActivity, mKeyBytes, mImportFilename);
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
