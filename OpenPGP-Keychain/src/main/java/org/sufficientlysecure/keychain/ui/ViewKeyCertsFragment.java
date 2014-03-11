/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.util.Log;

import java.nio.ByteBuffer;
import java.util.HashMap;

import se.emilsjolander.stickylistheaders.ApiLevelTooLowException;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;


public class ViewKeyCertsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
        KeychainContract.Certs._ID,
        KeychainContract.Certs.VERIFIED,
        KeychainContract.Certs.RANK,
        KeychainContract.Certs.KEY_ID_CERTIFIER,
        KeychainContract.UserIds.USER_ID,
        "signer_uid"
    };

    // sort by our user id,
    static final String SORT_ORDER =
              KeychainDatabase.Tables.USER_IDS + "." + KeychainContract.UserIds.USER_ID + " ASC, "
            + KeychainDatabase.Tables.CERTS + "." + KeychainContract.Certs.VERIFIED + " DESC, "
            + "signer_uid ASC";

    public static final String ARG_KEYRING_ROW_ID = "row_id";

    private StickyListHeadersListView mStickyList;

    private CertListAdapter mAdapter;

    private Uri mDataUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_certs_fragment, container, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mStickyList = (StickyListHeadersListView) getActivity().findViewById(R.id.list);

        if (!getArguments().containsKey(ARG_KEYRING_ROW_ID)) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        long rowId = getArguments().getLong(ARG_KEYRING_ROW_ID);
        mDataUri = KeychainContract.Certs.buildCertsByKeyRowIdUri(Long.toString(rowId));

        mStickyList.setAreHeadersSticky(true);
        mStickyList.setDrawingListUnderStickyHeader(false);
        mStickyList.setFastScrollEnabled(true);

        try {
            mStickyList.setFastScrollAlwaysVisible(true);
        } catch (ApiLevelTooLowException e) {
        }

        // TODO this view is made visible if no data is available
        // mStickyList.setEmptyView(getActivity().findViewById(R.id.empty));


        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new CertListAdapter(getActivity(), null);
        mStickyList.setAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), mDataUri, PROJECTION, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        mStickyList.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * Implements StickyListHeadersAdapter from library
     */
    private class CertListAdapter extends CursorAdapter implements StickyListHeadersAdapter {
        private LayoutInflater mInflater;
        private int mIndexUserId, mIndexRank;
        private int mIndexSignerKeyId, mIndexSignerUserId;
        private int mIndexVerified;

        public CertListAdapter(Context context, Cursor c) {
            super(context, c, 0);

            mInflater = LayoutInflater.from(context);
            initIndex(c);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            initIndex(newCursor);

            return super.swapCursor(newCursor);
        }

        /**
         * Get column indexes for performance reasons just once in constructor and swapCursor. For a
         * performance comparison see http://stackoverflow.com/a/17999582
         *
         * @param cursor
         */
        private void initIndex(Cursor cursor) {
            if (cursor != null) {

                mIndexUserId = cursor.getColumnIndexOrThrow(KeychainContract.UserIds.USER_ID);
                mIndexRank = cursor.getColumnIndexOrThrow(KeychainContract.UserIds.RANK);
                mIndexVerified = cursor.getColumnIndexOrThrow(KeychainContract.Certs.VERIFIED);
                mIndexSignerKeyId = cursor.getColumnIndexOrThrow(KeychainContract.Certs.KEY_ID_CERTIFIER);
                mIndexSignerUserId = cursor.getColumnIndexOrThrow("signer_uid");
            }
        }

        /**
         * Bind cursor data to the item list view
         * <p/>
         * NOTE: CursorAdapter already implements the ViewHolder pattern in its getView() method.
         * Thus no ViewHolder is required here.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            // set name and stuff, common to both key types
            TextView wSignerKeyId = (TextView) view.findViewById(R.id.signerKeyId);
            TextView wSignerUserId = (TextView) view.findViewById(R.id.signerUserId);
            TextView wSignStatus = (TextView) view.findViewById(R.id.signStatus);

            String signerKeyId = PgpKeyHelper.convertKeyIdToHex(cursor.getLong(mIndexSignerKeyId));
            String signerUserId = cursor.getString(mIndexSignerUserId);
            String signStatus = cursor.getInt(mIndexVerified) > 0 ? "ok" : "unknown";

            wSignerUserId.setText(signerUserId);
            wSignerKeyId.setText(signerKeyId);
            wSignStatus.setText(signStatus);

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.view_key_certs_item, parent, false);
        }

        /**
         * Creates a new header view and binds the section headers to it. It uses the ViewHolder
         * pattern. Most functionality is similar to getView() from Android's CursorAdapter.
         * <p/>
         * NOTE: The variables mDataValid and mCursor are available due to the super class
         * CursorAdapter.
         */
        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            if (convertView == null) {
                holder = new HeaderViewHolder();
                convertView = mInflater.inflate(R.layout.view_key_certs_header, parent, false);
                holder.text = (TextView) convertView.findViewById(R.id.stickylist_header_text);
                holder.count = (TextView) convertView.findViewById(R.id.certs_num);
                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }

            if (!mDataValid) {
                // no data available at this point
                Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
                return convertView;
            }

            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            // set header text as first char in user id
            String userId = mCursor.getString(mIndexUserId);
            holder.text.setText(userId);
            holder.count.setVisibility(View.GONE);
            return convertView;
        }

        /**
         * Header IDs should be static, position=1 should always return the same Id that is.
         */
        @Override
        public long getHeaderId(int position) {
            if (!mDataValid) {
                // no data available at this point
                Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
                return -1;
            }

            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            // otherwise, return the first character of the name as ID
            return mCursor.getInt(mIndexRank);

            // sort by the first four characters (should be enough I guess?)
            // return ByteBuffer.wrap(userId.getBytes()).asLongBuffer().get(0);
        }

        class HeaderViewHolder {
            TextView text;
            TextView count;
        }

    }

}