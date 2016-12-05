/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote.ui;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;

import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.remote.ui.adapter.SelectSignKeyAdapter;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.util.Log;

public class SelectSignKeyIdListFragment extends RecyclerFragment<SelectSignKeyAdapter>
        implements SelectSignKeyAdapter.SelectSignKeyListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_DATA_URI = "uri";
    private static final String ARG_PREF_UID = "pref_uid";
    public static final String ARG_DATA = "data";

    private Uri mDataUri;
    private Intent mResult;
    private String mPrefUid;
    private ApiDataAccessObject mApiDao;

    /**
     * Creates new instance of this fragment
     */
    public static SelectSignKeyIdListFragment newInstance(Uri dataUri, Intent data, String preferredUserId) {
        SelectSignKeyIdListFragment frag = new SelectSignKeyIdListFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putParcelable(ARG_DATA, data);
        args.putString(ARG_PREF_UID, preferredUserId);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApiDao = new ApiDataAccessObject(getActivity());
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mResult = getArguments().getParcelable(ARG_DATA);
        mPrefUid = getArguments().getString(ARG_PREF_UID);
        mDataUri = getArguments().getParcelable(ARG_DATA_URI);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        SelectSignKeyAdapter adapter = new SelectSignKeyAdapter(getContext(), null);
        adapter.setListener(this);

        setAdapter(adapter);
        setLayoutManager(new LinearLayoutManager(getContext()));

        // Start out with a progress indicator.
        hideList(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                KeyRings.USER_ID,
                KeyRings.IS_EXPIRED,
                KeyRings.IS_REVOKED,
                KeyRings.HAS_ENCRYPT,
                KeyRings.VERIFIED,
                KeyRings.HAS_ANY_SECRET,
                KeyRings.HAS_DUPLICATE_USER_ID,
                KeyRings.CREATION,
        };

        String selection = KeyRings.HAS_ANY_SECRET + " != 0";

        String orderBy = KeyRings.USER_ID + " ASC";
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, selection, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().swapCursor(CursorAdapter.KeyCursor.wrap(data));

        // The list should now be shown.
        if (isResumed()) {
            showList(true);
        } else {
            showList(false);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        getAdapter().swapCursor(null);
    }

    @Override
    public void onDestroy() {
        getAdapter().setListener(null);
        super.onDestroy();
    }

    @Override
    public void onCreateKeyDummyClicked() {
        OpenPgpUtils.UserId userIdSplit = KeyRing.splitUserId(mPrefUid);

        Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
        intent.putExtra(CreateKeyActivity.EXTRA_NAME, userIdSplit.name);
        intent.putExtra(CreateKeyActivity.EXTRA_EMAIL, userIdSplit.email);
        getActivity().startActivityForResult(intent, SelectSignKeyIdActivity.REQUEST_CODE_CREATE_KEY);
    }

    @Override
    public void onSelectKeyItemClicked(long masterKeyId) {
        Uri allowedKeysUri = mDataUri.buildUpon()
                .appendPath(KeychainContract.PATH_ALLOWED_KEYS)
                .build();

        mApiDao.addAllowedKeyIdForApp(allowedKeysUri, masterKeyId);
        mResult.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, masterKeyId);

        Log.d(Constants.TAG, "allowedKeyId: " + masterKeyId);
        Log.d(Constants.TAG, "allowedKeysUri: " + allowedKeysUri);

        getActivity().setResult(Activity.RESULT_OK, mResult);
        getActivity().finish();
    }
}
