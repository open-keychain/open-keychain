package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.SecretUserIds;
import org.thialfihar.android.apg.ui.widget.ExpandableListFragment;
import org.thialfihar.android.apg.ui.widget.KeyListAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;

public class KeyListSecretFragment extends ExpandableListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private FragmentActivity mActivity;
    private KeyListAdapter mAdapter;

    // private long mCurrentRowId;

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
        setEmptyText("TODO empty");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        mAdapter = new KeyListAdapter(mActivity, getLoaderManager(), null, Id.type.secret_key);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(-1, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] { SecretKeyRings._ID,
            SecretKeyRings.MASTER_KEY_ID, SecretUserIds.USER_ID };

    static final String SORT_ORDER = SecretUserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = SecretKeyRings.buildSecretKeyRingsUri();

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.setGroupCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.setGroupCursor(null);
    }

}
