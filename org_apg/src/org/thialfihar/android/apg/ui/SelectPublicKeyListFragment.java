package org.thialfihar.android.apg.ui;

import java.util.Date;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.PublicUserIds;
import org.thialfihar.android.apg.ui.widget.SelectPublicKeyCursorAdapter;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;

public class SelectPublicKeyListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SherlockFragmentActivity mActivity;
    private SelectPublicKeyCursorAdapter mAdapter;

    private long mCurrentRowId;

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getSherlockActivity();

        // register long press context menu
        registerForContextMenu(getListView());

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText("TODO empty");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        mAdapter = new SelectPublicKeyCursorAdapter(mActivity, null);

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    static final String SORT_ORDER = PublicUserIds.USER_ID + " ASC";

    // static final String SELECTION =

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = PublicKeyRings.buildPublicKeyRingsUri();

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.

        // These are the rows that we will retrieve.
        long now = new Date().getTime() / 1000;
        String[] projection = new String[] {
                PublicKeyRings._ID, // 0
                PublicKeyRings.MASTER_KEY_ID, // 1
                PublicUserIds.USER_ID, // 2
                "(SELECT COUNT(" + PublicKeys._ID + ") WHERE " + PublicKeys.IS_REVOKED
                        + " = '0' AND " + PublicKeys.CAN_ENCRYPT + " = '1')", // 3
                "(SELECT COUNT(" + PublicKeys._ID + ") WHERE " + PublicKeys.IS_REVOKED
                        + " = '0' AND " + PublicKeys.CAN_ENCRYPT + " = '1' AND "
                        + PublicKeys.CREATION + " <= '" + now + "' AND " + "(" + PublicKeys.EXPIRY
                        + " IS NULL OR " + PublicKeys.EXPIRY + " >= '" + now + "'))", // 4
        };

        return new CursorLoader(getActivity(), baseUri, projection, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

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
        mAdapter.swapCursor(null);
    }
}
