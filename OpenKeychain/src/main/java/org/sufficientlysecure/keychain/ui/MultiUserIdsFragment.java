/*
 * Copyright (C) 2013-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.ui.adapter.MultiUserIdsAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class MultiUserIdsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String ARG_CHECK_STATES = "check_states";
    public static final String EXTRA_KEY_IDS = "extra_key_ids";
    private boolean checkboxVisibility = true;

    ListView mUserIds;
    private MultiUserIdsAdapter mUserIdsAdapter;

    private long[] mPubMasterKeyIds;

    public static final String[] USER_IDS_PROJECTION = new String[]{
            KeychainContract.UserPackets._ID,
            KeychainContract.UserPackets.MASTER_KEY_ID,
            KeychainContract.UserPackets.USER_ID,
            KeychainContract.UserPackets.IS_PRIMARY,
            KeychainContract.UserPackets.IS_REVOKED
    };
    private static final int INDEX_MASTER_KEY_ID = 1;
    private static final int INDEX_USER_ID = 2;
    @SuppressWarnings("unused")
    private static final int INDEX_IS_PRIMARY = 3;
    @SuppressWarnings("unused")
    private static final int INDEX_IS_REVOKED = 4;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.multi_user_ids_fragment, null);

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPubMasterKeyIds = getActivity().getIntent().getLongArrayExtra(EXTRA_KEY_IDS);
        if (mPubMasterKeyIds == null) {
            Log.e(Constants.TAG, "List of key ids to certify missing!");
            getActivity().finish();
            return;
        }

        ArrayList<Boolean> checkedStates = null;
        if (savedInstanceState != null) {
            checkedStates = (ArrayList<Boolean>) savedInstanceState.getSerializable(ARG_CHECK_STATES);
        }

        mUserIdsAdapter = new MultiUserIdsAdapter(getActivity(), null, 0, checkedStates, checkboxVisibility);
        mUserIds.setAdapter(mUserIdsAdapter);
        mUserIds.setDividerHeight(0);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<Boolean> states = mUserIdsAdapter.getCheckStates();
        // no proper parceling method available :(
        outState.putSerializable(ARG_CHECK_STATES, states);
    }

    public ArrayList<CertifyActionsParcel.CertifyAction> getSelectedCertifyActions() {
        if (!checkboxVisibility) {
            throw new AssertionError("Item selection not allowed");
        }

        return mUserIdsAdapter.getSelectedCertifyActions();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = KeychainContract.UserPackets.buildUserIdsUri();

        String selection, ids[];
        {
            // generate placeholders and string selection args
            ids = new String[mPubMasterKeyIds.length];
            StringBuilder placeholders = new StringBuilder("?");
            for (int i = 0; i < mPubMasterKeyIds.length; i++) {
                ids[i] = Long.toString(mPubMasterKeyIds[i]);
                if (i != 0) {
                    placeholders.append(",?");
                }
            }
            // put together selection string
            selection = KeychainContract.UserPackets.IS_REVOKED + " = 0" + " AND "
                    + KeychainDatabase.Tables.USER_PACKETS + "." + KeychainContract.UserPackets.MASTER_KEY_ID
                    + " IN (" + placeholders + ")";
        }

        return new CursorLoader(getActivity(), uri,
                USER_IDS_PROJECTION, selection, ids,
                KeychainDatabase.Tables.USER_PACKETS + "." + KeychainContract.UserPackets.MASTER_KEY_ID + " ASC"
                        + ", " + KeychainDatabase.Tables.USER_PACKETS + "." + KeychainContract.UserPackets.USER_ID + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        MatrixCursor matrix = new MatrixCursor(new String[]{
                "_id", "user_data", "grouped"
        }) {
            @Override
            public byte[] getBlob(int column) {
                return super.getBlob(column);
            }
        };
        data.moveToFirst();

        long lastMasterKeyId = 0;
        String lastName = "";
        ArrayList<String> uids = new ArrayList<>();

        boolean header = true;

        // Iterate over all rows
        while (!data.isAfterLast()) {
            long masterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
            String userId = data.getString(INDEX_USER_ID);
            OpenPgpUtils.UserId pieces = KeyRing.splitUserId(userId);

            // Two cases:

            boolean grouped = masterKeyId == lastMasterKeyId;
            boolean subGrouped = data.isFirst() || grouped && lastName.equals(pieces.name);
            // Remember for next loop
            lastName = pieces.name;

            Log.d(Constants.TAG, Long.toString(masterKeyId, 16) + (grouped ? "grouped" : "not grouped"));

            if (!subGrouped) {
                // 1. This name should NOT be grouped with the previous, so we flush the buffer

                Parcel p = Parcel.obtain();
                p.writeStringList(uids);
                byte[] d = p.marshall();
                p.recycle();

                matrix.addRow(new Object[]{
                        lastMasterKeyId, d, header ? 1 : 0
                });
                // indicate that we have a header for this masterKeyId
                header = false;

                // Now clear the buffer, and add the new user id, for the next round
                uids.clear();

            }

            // 2. This name should be grouped with the previous, just add to buffer
            uids.add(userId);
            lastMasterKeyId = masterKeyId;

            // If this one wasn't grouped, the next one's gotta be a header
            if (!grouped) {
                header = true;
            }

            // Regardless of the outcome, move to next entry
            data.moveToNext();

        }

        // If there is anything left in the buffer, flush it one last time
        if (!uids.isEmpty()) {

            Parcel p = Parcel.obtain();
            p.writeStringList(uids);
            byte[] d = p.marshall();
            p.recycle();

            matrix.addRow(new Object[]{
                    lastMasterKeyId, d, header ? 1 : 0
            });

        }

        mUserIdsAdapter.swapCursor(matrix);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mUserIdsAdapter.swapCursor(null);
    }

    public void setCheckboxVisibility(boolean checkboxVisibility) {
        this.checkboxVisibility = checkboxVisibility;
    }
}
