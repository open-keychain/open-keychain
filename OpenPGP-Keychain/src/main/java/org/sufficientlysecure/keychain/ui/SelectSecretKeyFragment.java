/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.ui.adapter.SelectKeyCursorAdapter;

import java.util.Date;

public class SelectSecretKeyFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SelectSecretKeyActivity mActivity;
    private SelectKeyCursorAdapter mAdapter;
    private ListView mListView;

    private boolean mFilterCertify;

    private static final String ARG_FILTER_CERTIFY = "filter_certify";

    /**
     * Creates new instance of this fragment
     */
    public static SelectSecretKeyFragment newInstance(boolean filterCertify) {
        SelectSecretKeyFragment frag = new SelectSecretKeyFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_FILTER_CERTIFY, filterCertify);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterCertify = getArguments().getBoolean(ARG_FILTER_CERTIFY);
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = (SelectSecretKeyActivity) getActivity();
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
        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SelectKeyCursorAdapter(mActivity, null, 0, mListView, Id.type.secret_key);

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
        Uri baseUri = KeyRings.buildSecretKeyRingsUri();

        String capFilter = null;
        if (mFilterCertify) {
            capFilter = "(cert > 0)";
        }

        // These are the rows that we will retrieve.
        long now = new Date().getTime() / 1000;
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                UserIds.USER_ID,
                "(SELECT COUNT(cert_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS cert_keys WHERE cert_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeychainDatabase.Tables.KEY_RINGS + "." + KeyRings._ID + " AND cert_keys."
                        + Keys.CAN_CERTIFY + " = '1') AS cert",
                "(SELECT COUNT(available_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS available_keys WHERE available_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeychainDatabase.Tables.KEY_RINGS + "." + KeyRings._ID
                        + " AND available_keys." + Keys.IS_REVOKED + " = '0' AND  available_keys."
                        + Keys.CAN_SIGN + " = '1') AS "
                        + SelectKeyCursorAdapter.PROJECTION_ROW_AVAILABLE,
                "(SELECT COUNT(valid_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS valid_keys WHERE valid_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeychainDatabase.Tables.KEY_RINGS + "." + KeyRings._ID + " AND valid_keys."
                        + Keys.IS_REVOKED + " = '0' AND valid_keys." + Keys.CAN_SIGN
                        + " = '1' AND valid_keys." + Keys.CREATION + " <= '" + now + "' AND "
                        + "(valid_keys." + Keys.EXPIRY + " IS NULL OR valid_keys." + Keys.EXPIRY
                        + " >= '" + now + "')) AS " + SelectKeyCursorAdapter.PROJECTION_ROW_VALID,};

        String orderBy = UserIds.USER_ID + " ASC";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, capFilter, null, orderBy);
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
