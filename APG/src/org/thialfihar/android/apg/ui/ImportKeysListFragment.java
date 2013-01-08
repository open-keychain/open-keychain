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

package org.thialfihar.android.apg.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.ui.widget.ImportKeysListLoader;
import org.thialfihar.android.apg.util.Log;

import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ImportKeysListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<List<Map<String, String>>> {
    public static String ARG_KEYRING_BYTES = "bytes";
    public static String ARG_IMPORT_FILENAME = "filename";

    byte[] mKeyringBytes;
    String mImportFilename;

    private Activity mActivity;
    private SimpleAdapter mAdapter;

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Map<String, String> item = (Map<String, String>) listView.getItemAtPosition(position);
        // String userId = item.get(ImportKeysListLoader.MAP_ATTR_USER_ID);
    }

    /**
     * Resume is called after rotating
     */
    @Override
    public void onResume() {
        super.onResume();

        // Start out with a progress indicator.
        setListShown(false);

        // reload list
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = this.getActivity();

        mKeyringBytes = getArguments().getByteArray(ARG_KEYRING_BYTES);
        mImportFilename = getArguments().getString(ARG_IMPORT_FILENAME);

        // register long press context menu
        registerForContextMenu(getListView());

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(mActivity.getString(R.string.error_nothingImport));

        // Create an empty adapter we will use to display the loaded data.
        String[] from = new String[] {};
        int[] to = new int[] {};
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        mAdapter = new SimpleAdapter(getActivity(), data, android.R.layout.two_line_list_item,
                from, to);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<Map<String, String>>> onCreateLoader(int id, Bundle args) {
        return new ImportKeysListLoader(mActivity, mKeyringBytes, mImportFilename);
    }

    @Override
    public void onLoadFinished(Loader<List<Map<String, String>>> loader,
            List<Map<String, String>> data) {
        // Set the new data in the adapter.
        // for (String item : data) {
        // mAdapter.add(item);
        // }
        Log.d(Constants.TAG, "data: " + data);
        // TODO: real swapping the data to the already defined adapter doesn't work
        // Workaround: recreate adapter!
        // http://stackoverflow.com/questions/2356091/android-add-function-of-arrayadapter-not-working
        // mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.two_line_list_item,
        // data);

        String[] from = new String[] { ImportKeysListLoader.MAP_ATTR_USER_ID,
                ImportKeysListLoader.MAP_ATTR_FINGERPINT };
        int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
        mAdapter = new SimpleAdapter(getActivity(), data, android.R.layout.two_line_list_item,
                from, to);

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
    public void onLoaderReset(Loader<List<Map<String, String>>> loader) {
        // Clear the data in the adapter.
        // Not available in SimpleAdapter!
        // mAdapter.clear();
    }

}
