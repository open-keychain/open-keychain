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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.ViewKeyUserIdsAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;

public class ViewKeyMainFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private LinearLayout mContainer;
    private View mActionEdit;
    private View mActionEditDivider;
    private View mActionEncrypt;
    private View mActionCertify;
    private View mActionCertifyDivider;

    private ListView mUserIds;

    private static final int LOADER_ID_UNIFIED = 0;
    private static final int LOADER_ID_USER_IDS = 1;

    private ViewKeyUserIdsAdapter mUserIdsAdapter;

    private Uri mDataUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_main_fragment, container, false);

        mContainer = (LinearLayout) view.findViewById(R.id.container);
        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);
        mActionEdit = view.findViewById(R.id.view_key_action_edit);
        mActionEditDivider = view.findViewById(R.id.view_key_action_edit_divider);
        mActionEncrypt = view.findViewById(R.id.view_key_action_encrypt);
        mActionCertify = view.findViewById(R.id.view_key_action_certify);
        mActionCertifyDivider = view.findViewById(R.id.view_key_action_certify_divider);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }

    private void loadData(Uri dataUri) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        mContainer.setVisibility(View.GONE);

        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        mActionEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(mDataUri);
            }
        });
        mActionCertify.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                certify(mDataUri);
            }
        });
        mActionEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                editKey(mDataUri);
            }
        });

        mUserIdsAdapter = new ViewKeyUserIdsAdapter(getActivity(), null, 0);
        mUserIds.setAdapter(mUserIdsAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
    }

    static final String[] UNIFIED_PROJECTION = new String[]{
            KeyRings._ID, KeyRings.MASTER_KEY_ID,
            KeyRings.HAS_ANY_SECRET, KeyRings.IS_REVOKED, KeyRings.EXPIRY, KeyRings.HAS_ENCRYPT
    };
    static final int INDEX_UNIFIED_MASTER_KEY_ID = 1;
    static final int INDEX_UNIFIED_HAS_ANY_SECRET = 2;
    static final int INDEX_UNIFIED_IS_REVOKED = 3;
    static final int INDEX_UNIFIED_EXPIRY = 4;
    static final int INDEX_UNIFIED_HAS_ENCRYPT = 5;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
            }
            case LOADER_ID_USER_IDS: {
                Uri baseUri = UserIds.buildUserIdsUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        ViewKeyUserIdsAdapter.USER_IDS_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        // Avoid NullPointerExceptions...
        if (data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {
                    if (data.getInt(INDEX_UNIFIED_HAS_ANY_SECRET) != 0) {
                        // certify button
                        mActionCertify.setVisibility(View.GONE);
                        mActionCertifyDivider.setVisibility(View.GONE);

                        // edit button
                        mActionEdit.setVisibility(View.VISIBLE);
                        mActionEditDivider.setVisibility(View.VISIBLE);
                    } else {
                        // certify button
                        mActionCertify.setVisibility(View.VISIBLE);
                        mActionCertifyDivider.setVisibility(View.VISIBLE);

                        // edit button
                        mActionEdit.setVisibility(View.GONE);
                        mActionEditDivider.setVisibility(View.GONE);
                    }

                    // If this key is revoked, it cannot be used for anything!
                    if (data.getInt(INDEX_UNIFIED_IS_REVOKED) != 0) {
                        mActionEdit.setEnabled(false);
                        mActionCertify.setEnabled(false);
                        mActionEncrypt.setEnabled(false);
                    } else {
                        mActionEdit.setEnabled(true);

                        Date expiryDate = new Date(data.getLong(INDEX_UNIFIED_EXPIRY) * 1000);
                        if (!data.isNull(INDEX_UNIFIED_EXPIRY) && expiryDate.before(new Date())) {
                            mActionCertify.setEnabled(false);
                            mActionEncrypt.setEnabled(false);
                        } else {
                            mActionCertify.setEnabled(true);
                            mActionEncrypt.setEnabled(true);
                        }
                    }

                    break;
                }
            }

            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(data);
                break;

        }
        getActivity().setProgressBarIndeterminateVisibility(false);
        mContainer.setVisibility(View.VISIBLE);
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(null);
                break;
        }
    }

    private void encrypt(Uri dataUri) {
        try {
            long keyId = new ProviderHelper(getActivity()).extractOrGetMasterKeyId(dataUri);
            long[] encryptionKeyIds = new long[]{keyId};
            Intent intent = new Intent(getActivity(), EncryptActivity.class);
            intent.setAction(EncryptActivity.ACTION_ENCRYPT);
            intent.putExtra(EncryptActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
            // used instead of startActivity set actionbar based on callingPackage
            startActivityForResult(intent, 0);
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
    }

    private void certify(Uri dataUri) {
        Intent signIntent = new Intent(getActivity(), CertifyKeyActivity.class);
        signIntent.setData(dataUri);
        startActivity(signIntent);
    }

    private void editKey(Uri dataUri) {
        Intent editIntent = new Intent(getActivity(), EditKeyActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(dataUri));
        editIntent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
        startActivityForResult(editIntent, 0);
    }

}
