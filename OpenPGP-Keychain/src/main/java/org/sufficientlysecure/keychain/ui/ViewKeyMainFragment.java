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
import android.graphics.Color;
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
import android.widget.ListView;
import android.widget.TextView;
import com.beardedhen.androidbootstrap.BootstrapButton;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.ViewKeyKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ViewKeyUserIdsAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;


public class ViewKeyMainFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private TextView mName;
    private TextView mEmail;
    private TextView mComment;
    private TextView mAlgorithm;
    private TextView mKeyId;
    private TextView mExpiry;
    private TextView mCreation;
    private TextView mFingerprint;
    private TextView mSecretKey;
    private BootstrapButton mActionEdit;
    private BootstrapButton mActionEncrypt;
    private BootstrapButton mActionCertify;

    private ListView mUserIds;
    private ListView mKeys;

    private static final int LOADER_ID_KEYRING = 0;
    private static final int LOADER_ID_USER_IDS = 1;
    private static final int LOADER_ID_KEYS = 2;

    private ViewKeyUserIdsAdapter mUserIdsAdapter;
    private ViewKeyKeysAdapter mKeysAdapter;

    private Uri mDataUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_main_fragment, container, false);

        mName = (TextView) view.findViewById(R.id.name);
        mEmail = (TextView) view.findViewById(R.id.email);
        mComment = (TextView) view.findViewById(R.id.comment);
        mKeyId = (TextView) view.findViewById(R.id.key_id);
        mAlgorithm = (TextView) view.findViewById(R.id.algorithm);
        mCreation = (TextView) view.findViewById(R.id.creation);
        mExpiry = (TextView) view.findViewById(R.id.expiry);
        mFingerprint = (TextView) view.findViewById(R.id.fingerprint);
        mSecretKey = (TextView) view.findViewById(R.id.secret_key);
        mUserIds = (ListView) view.findViewById(R.id.user_ids);
        mKeys = (ListView) view.findViewById(R.id.keys);
        mActionEdit = (BootstrapButton) view.findViewById(R.id.action_edit);
        mActionEncrypt = (BootstrapButton) view.findViewById(R.id.action_encrypt);
        mActionCertify = (BootstrapButton) view.findViewById(R.id.action_certify);

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
        if (dataUri.equals(mDataUri)) {
            Log.d(Constants.TAG, "Same URI, no need to load the data again!");
            return;
        }

        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        { // label whether secret key is available, and edit button if it is
            final long masterKeyId = ProviderHelper.getMasterKeyId(getActivity(), mDataUri);
            if (ProviderHelper.hasSecretKeyByMasterKeyId(getActivity(), masterKeyId)) {
                // set this attribute. this is a LITTLE unclean, but we have the info available
                // right here, so why not.
                mSecretKey.setTextColor(getResources().getColor(R.color.emphasis));
                mSecretKey.setText(R.string.secret_key_yes);

                // certify button
                // TODO this button MIGHT be useful if the user wants to
                // certify a private key with another...
                // mActionCertify.setVisibility(View.GONE);

                // edit button
                mActionEdit.setVisibility(View.VISIBLE);
                mActionEdit.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent editIntent = new Intent(getActivity(), EditKeyActivity.class);
                        editIntent.setData(
                                KeychainContract
                                        .KeyRings.buildSecretKeyRingsByMasterKeyIdUri(
                                                                Long.toString(masterKeyId)));
                        editIntent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
                        startActivityForResult(editIntent, 0);
                    }
                });
            } else {
                mSecretKey.setTextColor(Color.BLACK);
                mSecretKey.setText(getResources().getString(R.string.secret_key_no));

                // certify button
                mActionCertify.setVisibility(View.VISIBLE);
                // edit button
                mActionEdit.setVisibility(View.GONE);
            }

            // TODO see todo note above, doing this here for now
            mActionCertify.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    certifyKey(KeychainContract.KeyRings.buildPublicKeyRingsByMasterKeyIdUri(
                            Long.toString(masterKeyId)
                    ));
                }
            });

        }

        mActionEncrypt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                encryptToContact(mDataUri);
            }
        });

        mUserIdsAdapter = new ViewKeyUserIdsAdapter(getActivity(), null, 0);
        mUserIds.setAdapter(mUserIdsAdapter);

        mKeysAdapter = new ViewKeyKeysAdapter(getActivity(), null, 0);
        mKeys.setAdapter(mKeysAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getActivity().getSupportLoaderManager().initLoader(LOADER_ID_KEYRING, null, this);
        getActivity().getSupportLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        getActivity().getSupportLoaderManager().initLoader(LOADER_ID_KEYS, null, this);
    }

    static final String[] KEYRING_PROJECTION =
            new String[]{KeychainContract.KeyRings._ID, KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.UserIds.USER_ID};
    static final int KEYRING_INDEX_ID = 0;
    static final int KEYRING_INDEX_MASTER_KEY_ID = 1;
    static final int KEYRING_INDEX_USER_ID = 2;

    static final String[] USER_IDS_PROJECTION =
            new String[]{
                KeychainContract.UserIds._ID,
                KeychainContract.UserIds.USER_ID,
                KeychainContract.UserIds.RANK,
            };
    static final String USER_IDS_SORT_ORDER =
            KeychainContract.UserIds.RANK + " COLLATE LOCALIZED ASC";

    static final String[] KEYS_PROJECTION =
            new String[]{KeychainContract.Keys._ID, KeychainContract.Keys.KEY_ID,
            KeychainContract.Keys.IS_MASTER_KEY, KeychainContract.Keys.ALGORITHM,
            KeychainContract.Keys.KEY_SIZE, KeychainContract.Keys.CAN_CERTIFY,
            KeychainContract.Keys.CAN_SIGN, KeychainContract.Keys.CAN_ENCRYPT,
            KeychainContract.Keys.IS_REVOKED, KeychainContract.Keys.CREATION,
            KeychainContract.Keys.EXPIRY, KeychainContract.Keys.FINGERPRINT};
    static final String KEYS_SORT_ORDER = KeychainContract.Keys.RANK + " ASC";
    static final int KEYS_INDEX_ID = 0;
    static final int KEYS_INDEX_KEY_ID = 1;
    static final int KEYS_INDEX_IS_MASTER_KEY = 2;
    static final int KEYS_INDEX_ALGORITHM = 3;
    static final int KEYS_INDEX_KEY_SIZE = 4;
    static final int KEYS_INDEX_CAN_CERTIFY = 5;
    static final int KEYS_INDEX_CAN_SIGN = 6;
    static final int KEYS_INDEX_CAN_ENCRYPT = 7;
    static final int KEYS_INDEX_IS_REVOKED = 8;
    static final int KEYS_INDEX_CREATION = 9;
    static final int KEYS_INDEX_EXPIRY = 10;
    static final int KEYS_INDEX_FINGERPRINT = 11;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_KEYRING: {
                Uri baseUri = mDataUri;

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                return new CursorLoader(getActivity(), baseUri, KEYRING_PROJECTION, null, null, null);
            }
            case LOADER_ID_USER_IDS: {
                Uri baseUri = KeychainContract.UserIds.buildUserIdsUri(mDataUri);

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                return new CursorLoader(getActivity(), baseUri, USER_IDS_PROJECTION, null, null,
                        USER_IDS_SORT_ORDER);
            }
            case LOADER_ID_KEYS: {
                Uri baseUri = KeychainContract.Keys.buildKeysUri(mDataUri);

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                return new CursorLoader(getActivity(), baseUri, KEYS_PROJECTION, null, null, KEYS_SORT_ORDER);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_KEYRING:
                if (data.moveToFirst()) {
                    // get name, email, and comment from USER_ID
                    String[] mainUserId = PgpKeyHelper.splitUserId(data
                            .getString(KEYRING_INDEX_USER_ID));
                    if (mainUserId[0] != null) {
                        getActivity().setTitle(mainUserId[0]);
                        mName.setText(mainUserId[0]);
                    } else {
                        getActivity().setTitle(R.string.user_id_no_name);
                        mName.setText(R.string.user_id_no_name);
                    }
                    mEmail.setText(mainUserId[1]);
                    mComment.setText(mainUserId[2]);
                }

                break;
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(data);
                break;
            case LOADER_ID_KEYS:
                // the first key here is our master key
                if (data.moveToFirst()) {
                    // get key id from MASTER_KEY_ID
                    long keyId = data.getLong(KEYS_INDEX_KEY_ID);
                    String keyIdStr = PgpKeyHelper.convertKeyIdToHex(keyId);
                    mKeyId.setText(keyIdStr);

                    // get creation date from CREATION
                    if (data.isNull(KEYS_INDEX_CREATION)) {
                        mCreation.setText(R.string.none);
                    } else {
                        Date creationDate = new Date(data.getLong(KEYS_INDEX_CREATION) * 1000);

                        mCreation.setText(
                                DateFormat.getDateFormat(getActivity().getApplicationContext()).format(
                                creationDate));
                    }

                    // get expiry date from EXPIRY
                    if (data.isNull(KEYS_INDEX_EXPIRY)) {
                        mExpiry.setText(R.string.none);
                    } else {
                        Date expiryDate = new Date(data.getLong(KEYS_INDEX_EXPIRY) * 1000);

                        mExpiry.setText(
                                DateFormat.getDateFormat(getActivity().getApplicationContext()).format(
                                expiryDate));
                    }

                    String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                            data.getInt(KEYS_INDEX_ALGORITHM), data.getInt(KEYS_INDEX_KEY_SIZE));
                    mAlgorithm.setText(algorithmStr);

                    byte[] fingerprintBlob = data.getBlob(KEYS_INDEX_FINGERPRINT);
                    if (fingerprintBlob == null) {
                        // FALLBACK for old database entries
                        fingerprintBlob = ProviderHelper.getFingerprint(getActivity(), mDataUri);
                    }
                    String fingerprint = PgpKeyHelper.convertFingerprintToHex(fingerprintBlob);

                    mFingerprint.setText(PgpKeyHelper.colorizeFingerprint(fingerprint));
                }

                mKeysAdapter.swapCursor(data);
                break;

            default:
                break;
        }
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_KEYRING:
                // No resources need to be freed for this ID
                break;
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(null);
                break;
            case LOADER_ID_KEYS:
                mKeysAdapter.swapCursor(null);
                break;
            default:
                break;
        }
    }


    private void encryptToContact(Uri dataUri) {
        long keyId = ProviderHelper.getMasterKeyId(getActivity(), dataUri);

        long[] encryptionKeyIds = new long[]{keyId};
        Intent intent = new Intent(getActivity(), EncryptActivity.class);
        intent.setAction(EncryptActivity.ACTION_ENCRYPT);
        intent.putExtra(EncryptActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, 0);
    }

    private void certifyKey(Uri dataUri) {
        Intent signIntent = new Intent(getActivity(), CertifyKeyActivity.class);
        signIntent.setData(dataUri);
        startActivity(signIntent);
    }

}
