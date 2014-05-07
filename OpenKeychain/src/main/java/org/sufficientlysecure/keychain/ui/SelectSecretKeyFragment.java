/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

import android.content.Context;
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.adapter.SelectKeyCursorAdapter;

public class SelectSecretKeyFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SelectSecretKeyActivity mActivity;
    private SelectKeyCursorAdapter mAdapter;

    private boolean mFilterCertify, mFilterSign;

    private static final String ARG_FILTER_CERTIFY = "filter_certify";
    private static final String ARG_FILTER_SIGN = "filter_sign";

    /**
     * Creates new instance of this fragment
     *
     * filterCertify and filterSign must not both be set!
     */
    public static SelectSecretKeyFragment newInstance(boolean filterCertify, boolean filterSign) {
        SelectSecretKeyFragment frag = new SelectSecretKeyFragment();

        Bundle args = new Bundle();
        args.putBoolean(ARG_FILTER_CERTIFY, filterCertify);
        args.putBoolean(ARG_FILTER_SIGN, filterSign);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterCertify = getArguments().getBoolean(ARG_FILTER_CERTIFY);
        mFilterSign = getArguments().getBoolean(ARG_FILTER_SIGN);
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = (SelectSecretKeyActivity) getActivity();

        ListView listView = getListView();
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                long masterKeyId = mAdapter.getMasterKeyId(position);
                Uri result = KeyRings.buildGenericKeyRingUri(String.valueOf(masterKeyId));

                // return data to activity, which results in finishing it
                mActivity.afterListSelection(result);
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SelectSecretKeyCursorAdapter(mActivity, null, 0, listView);

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
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                KeyRings.USER_ID,
                KeyRings.EXPIRY,
                KeyRings.IS_REVOKED,
                // can certify info only related to master key
                KeyRings.CAN_CERTIFY,
                // has sign may be any subkey
                KeyRings.HAS_SIGN,
                KeyRings.HAS_ANY_SECRET,
                KeyRings.HAS_SECRET
        };

        String where = KeyRings.HAS_ANY_SECRET + " = 1";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, where, null, null);
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

    private class SelectSecretKeyCursorAdapter extends SelectKeyCursorAdapter {

        private int mIndexHasSign, mIndexCanCertify, mIndexHasSecret;

        public SelectSecretKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView) {
            super(context, c, flags, listView);
        }

        @Override
        protected void initIndex(Cursor cursor) {
            super.initIndex(cursor);
            if (cursor != null) {
                mIndexCanCertify = cursor.getColumnIndexOrThrow(KeyRings.CAN_CERTIFY);
                mIndexHasSign = cursor.getColumnIndexOrThrow(KeyRings.HAS_SIGN);
                mIndexHasSecret = cursor.getColumnIndexOrThrow(KeyRings.HAS_SECRET);
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            ViewHolderItem h = (SelectKeyCursorAdapter.ViewHolderItem) view.getTag();

            // We don't care about the checkbox
            h.selected.setVisibility(View.GONE);

            // Special from superclass: Te
            boolean enabled = false;
            if((Boolean) h.status.getTag()) {
                // Check if key is viable for our purposes (certify or sign)
                if(mFilterCertify) {
                    // Only enable if can certify
                    if (cursor.getInt(mIndexCanCertify) == 0
                            || cursor.getInt(mIndexHasSecret) == 0) {
                        h.status.setText(R.string.can_certify_not);
                    } else {
                        h.status.setText(R.string.can_certify);
                        enabled = true;
                    }
                } else if(mFilterSign) {
                    // Only enable if can sign
                    if (cursor.getInt(mIndexHasSign) == 0) {
                        h.status.setText(R.string.can_sign_not);
                    } else {
                        h.status.setText(R.string.can_sign);
                        enabled = true;
                    }
                } else {
                    // No filters, just enable
                    enabled = true;
                }
            }
            h.setEnabled(enabled);
            // refresh this, too, for use in the ItemClickListener above
            h.status.setTag(enabled);
        }

    }

}