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

import java.util.Date;

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.provider.ApgContract.KeyRings;
import org.thialfihar.android.apg.provider.ApgContract.Keys;
import org.thialfihar.android.apg.provider.ApgContract.UserIds;
import org.thialfihar.android.apg.provider.ApgDatabase.Tables;
import org.thialfihar.android.apg.ui.widget.SelectKeyCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SelectSecretKeyFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SelectSecretKeyActivity mActivity;
    private SelectKeyCursorAdapter mAdapter;
    private ListView mListView;

    public final static String PROJECTION_ROW_AVAILABLE = "available";
    public final static String PROJECTION_ROW_VALID = "valid";

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = (SelectSecretKeyActivity) getSherlockActivity();
        mListView = getListView();

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                long masterKeyId = mAdapter.getMasterKeyId(position);
                String userId = mAdapter.getUserId(position);

                // return data to activity, which results in finishing it
                mActivity.afterListSelection(masterKeyId, userId);
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.listEmpty));

        mAdapter = new SelectKeyCursorAdapter(mActivity, mListView, null, Id.type.secret_key);

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildPublicKeyRingsUri();

        // These are the rows that we will retrieve.
        long now = new Date().getTime() / 1000;
        String[] projection = new String[] {
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                UserIds.USER_ID,
                "(SELECT COUNT(tmp." + Keys._ID + ") FROM " + Tables.KEYS + " AS tmp WHERE tmp."
                        + Keys.IS_REVOKED + " = '0' AND  tmp." + Keys.CAN_SIGN + " = '1') AS "
                        + PROJECTION_ROW_AVAILABLE,
                "(SELECT COUNT(tmp." + Keys._ID + ") FROM " + Tables.KEYS + " AS tmp WHERE tmp."
                        + Keys.IS_REVOKED + " = '0' AND " + Keys.CAN_SIGN + " = '1' AND tmp."
                        + Keys.CREATION + " <= '" + now + "' AND " + "(tmp." + Keys.EXPIRY
                        + " IS NULL OR tmp." + Keys.EXPIRY + " >= '" + now + "')) AS "
                        + PROJECTION_ROW_VALID, };

        // if (searchString != null && searchString.trim().length() > 0) {
        // String[] chunks = searchString.trim().split(" +");
        // qb.appendWhere("EXISTS (SELECT tmp." + UserIds._ID + " FROM " + UserIds.TABLE_NAME
        // + " AS tmp WHERE " + "tmp." + UserIds.KEY_ID + " = " + Keys.TABLE_NAME + "."
        // + Keys._ID);
        // for (int i = 0; i < chunks.length; ++i) {
        // qb.appendWhere(" AND tmp." + UserIds.USER_ID + " LIKE ");
        // qb.appendWhereEscapeString("%" + chunks[i] + "%");
        // }
        // qb.appendWhere(")");
        // }

        String orderBy = UserIds.USER_ID + " ASC";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, null, null, orderBy);
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
