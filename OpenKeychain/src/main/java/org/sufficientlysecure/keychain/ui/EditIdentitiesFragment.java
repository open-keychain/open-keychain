/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAddedAdapter;
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;

public class EditIdentitiesFragment extends QueueingCryptoOperationFragment<SaveKeyringParcel, OperationResult>
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private ListView mUserIdsList;
    private ListView mUserIdsAddedList;
    private View mAddUserId;

    private static final int LOADER_ID_USER_IDS = 0;

    private UserIdsAdapter mUserIdsAdapter;
    private UserIdsAddedAdapter mUserIdsAddedAdapter;

    private Uri mDataUri;

    private SaveKeyringParcel mSaveKeyringParcel;

    private String mPrimaryUserId;

    /**
     * Creates new instance of this fragment
     */
    public static EditIdentitiesFragment newInstance(Uri dataUri) {
        EditIdentitiesFragment frag = new EditIdentitiesFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_identities_fragment, null);

        mUserIdsList = (ListView) view.findViewById(R.id.edit_key_user_ids);
        mUserIdsAddedList = (ListView) view.findViewById(R.id.edit_key_user_ids_added);
        mAddUserId = view.findViewById(R.id.edit_key_action_add_user_id);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((EditIdentitiesActivity) getActivity()).setFullScreenDialogDoneClose(
                R.string.btn_save,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // if we are working on an Uri, save directly
                        if (mDataUri == null) {
                            returnKeyringParcel();
                        } else {
                            cryptoOperation(new CryptoInputParcel(new Date()));
                        }
                    }
                }, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().setResult(Activity.RESULT_CANCELED);
                        getActivity().finish();
                    }
                });

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Either a key Uri is required!");
            getActivity().finish();
            return;
        }

        initView();
        loadData(dataUri);
    }

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

        // load the secret key ring. we do verify here that the passphrase is correct, so cached won't do
        try {
            Uri secretUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
            CachedPublicKeyRing keyRing =
                    new ProviderHelper(getActivity()).getCachedPublicKeyRing(secretUri);
            long masterKeyId = keyRing.getMasterKeyId();

            // check if this is a master secret key we can work with
            switch (keyRing.getSecretKeyType(masterKeyId)) {
                case GNU_DUMMY:
                    finishWithError(LogType.MSG_EK_ERROR_DUMMY);
                    return;
            }

            mSaveKeyringParcel = new SaveKeyringParcel(masterKeyId, keyRing.getFingerprint());
            mPrimaryUserId = keyRing.getPrimaryUserIdWithFallback();

        } catch (PgpKeyNotFoundException | NotFoundException e) {
            finishWithError(LogType.MSG_EK_ERROR_NOT_FOUND);
            return;
        }

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, EditIdentitiesFragment.this);

        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0);
        mUserIdsAdapter.setEditMode(mSaveKeyringParcel);
        mUserIdsList.setAdapter(mUserIdsAdapter);

        // TODO: SaveParcel from savedInstance?!
        mUserIdsAddedAdapter = new UserIdsAddedAdapter(getActivity(), mSaveKeyringParcel.mAddUserIds, false);
        mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);
    }

    private void initView() {
        mAddUserId.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserId();
            }
        });

        mUserIdsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editUserId(position);
            }
        });
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case LOADER_ID_USER_IDS: {
                Uri baseUri = UserPackets.buildUserIdsUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        UserIdsAdapter.USER_PACKETS_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS: {
                mUserIdsAdapter.swapCursor(data);
                break;
            }
        }
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS: {
                mUserIdsAdapter.swapCursor(null);
                break;
            }
        }
    }

    private void editUserId(final int position) {
        final String userId = mUserIdsAdapter.getUserId(position);
        final boolean isRevoked = mUserIdsAdapter.getIsRevoked(position);
        final boolean isRevokedPending = mUserIdsAdapter.getIsRevokedPending(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditUserIdDialogFragment.MESSAGE_CHANGE_PRIMARY_USER_ID:
                        // toggle
                        if (mSaveKeyringParcel.mChangePrimaryUserId != null
                                && mSaveKeyringParcel.mChangePrimaryUserId.equals(userId)) {
                            mSaveKeyringParcel.mChangePrimaryUserId = null;
                        } else {
                            mSaveKeyringParcel.mChangePrimaryUserId = userId;
                        }
                        break;
                    case EditUserIdDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mSaveKeyringParcel.mRevokeUserIds.contains(userId)) {
                            mSaveKeyringParcel.mRevokeUserIds.remove(userId);
                        } else {
                            mSaveKeyringParcel.mRevokeUserIds.add(userId);
                            // not possible to revoke and change to primary user id
                            if (mSaveKeyringParcel.mChangePrimaryUserId != null
                                    && mSaveKeyringParcel.mChangePrimaryUserId.equals(userId)) {
                                mSaveKeyringParcel.mChangePrimaryUserId = null;
                            }
                        }
                        break;
                }
                getLoaderManager().getLoader(LOADER_ID_USER_IDS).forceLoad();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                EditUserIdDialogFragment dialogFragment =
                        EditUserIdDialogFragment.newInstance(messenger, isRevoked, isRevokedPending);
                dialogFragment.show(getActivity().getSupportFragmentManager(), "editUserIdDialog");
            }
        });
    }

    private void addUserId() {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // add new user id
                    mUserIdsAddedAdapter.add(data
                            .getString(AddUserIdDialogFragment.MESSAGE_DATA_USER_ID));
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        // pre-fill out primary name
        String predefinedName = KeyRing.splitUserId(mPrimaryUserId).name;
        AddUserIdDialogFragment addUserIdDialog = AddUserIdDialogFragment.newInstance(messenger,
                predefinedName);

        addUserIdDialog.show(getActivity().getSupportFragmentManager(), "addUserIdDialog");
    }


    protected void returnKeyringParcel() {
        if (mSaveKeyringParcel.mAddUserIds.size() == 0) {
            Notify.create(getActivity(), R.string.edit_key_error_add_identity, Notify.Style.ERROR).show();
            return;
        }

        // use first user id as primary
        mSaveKeyringParcel.mChangePrimaryUserId = mSaveKeyringParcel.mAddUserIds.get(0);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
        getActivity().setResult(Activity.RESULT_OK, returnIntent);
        getActivity().finish();
    }

    /**
     * Closes this activity, returning a result parcel with a single error log entry.
     */
    void finishWithError(LogType reason) {
        // Prepare an intent with an EXTRA_RESULT
        Intent intent = new Intent();
        intent.putExtra(OperationResult.EXTRA_RESULT,
                new SingletonResult(SingletonResult.RESULT_ERROR, reason));

        // Finish with result
        getActivity().setResult(EditKeyActivity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public SaveKeyringParcel createOperationInput() {
        return mSaveKeyringParcel;
    }

    @Override
    public void onQueuedOperationSuccess(OperationResult result) {

        // null-protected from Queueing*Fragment
        Activity activity = getActivity();

        // if good -> finish, return result to showkey and display there!
        Intent intent = new Intent();
        intent.putExtra(OperationResult.EXTRA_RESULT, result);
        activity.setResult(EditKeyActivity.RESULT_OK, intent);
        activity.finish();

    }

}
