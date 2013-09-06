package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockListFragment;

public class RegisteredAppsListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // This is the Adapter being used to display the list's data.
    RegisteredAppsAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // edit app settings
                Intent intent = new Intent(getActivity(), AppSettingsActivity.class);
                intent.setData(ContentUris.withAppendedId(KeychainContract.ApiApps.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.api_no_apps));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new RegisteredAppsAdapter(getActivity(), null, 0);
        setListAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] CONSUMERS_SUMMARY_PROJECTION = new String[] { ApiApps._ID,
            ApiApps.PACKAGE_NAME };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri = ApiApps.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, CONSUMERS_SUMMARY_PROJECTION, null, null,
                ApiApps.PACKAGE_NAME + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

}