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


import java.util.Date;

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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAddedAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAddedAdapter;
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyExpiryDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

public class EditKeyFragment extends QueueingCryptoOperationFragment<SaveKeyringParcel, OperationResult>
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";
    public static final String ARG_SAVE_KEYRING_PARCEL = "save_keyring_parcel";

    private ListView mUserIdsList;
    private ListView mSubkeysList;
    private ListView mUserIdsAddedList;
    private ListView mSubkeysAddedList;
    private View mChangePassphrase;
    private View mAddUserId;
    private View mAddSubkey;

    private static final int LOADER_ID_USER_IDS = 0;
    private static final int LOADER_ID_SUBKEYS = 1;

    // cursor adapter
    private UserIdsAdapter mUserIdsAdapter;
    private SubkeysAdapter mSubkeysAdapter;

    // array adapter
    private UserIdsAddedAdapter mUserIdsAddedAdapter;
    private SubkeysAddedAdapter mSubkeysAddedAdapter;

    private Uri mDataUri;

    private SaveKeyringParcel.Builder mSkpBuilder;

    private String mPrimaryUserId;

    /**
     * Creates new instance of this fragment
     */
    public static EditKeyFragment newInstance(Uri dataUri) {
        EditKeyFragment frag = new EditKeyFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    public static EditKeyFragment newInstance(SaveKeyringParcel saveKeyringParcel) {
        EditKeyFragment frag = new EditKeyFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_SAVE_KEYRING_PARCEL, saveKeyringParcel);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_key_fragment, superContainer, false);

        mUserIdsList = (ListView) view.findViewById(R.id.edit_key_user_ids);
        mSubkeysList = (ListView) view.findViewById(R.id.edit_key_keys);
        mUserIdsAddedList = (ListView) view.findViewById(R.id.edit_key_user_ids_added);
        mSubkeysAddedList = (ListView) view.findViewById(R.id.edit_key_subkeys_added);
        mChangePassphrase = view.findViewById(R.id.edit_key_action_change_passphrase);
        mAddUserId = view.findViewById(R.id.edit_key_action_add_user_id);
        mAddSubkey = view.findViewById(R.id.edit_key_action_add_key);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((EditKeyActivity) getActivity()).setFullScreenDialogDoneClose(
                R.string.btn_save,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // if we are working on an Uri, save directly
                        if (mDataUri == null) {
                            returnKeyringParcel();
                        } else {
                            cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
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
        SaveKeyringParcel saveKeyringParcel = getArguments().getParcelable(ARG_SAVE_KEYRING_PARCEL);
        if (dataUri == null && saveKeyringParcel == null) {
            Log.e(Constants.TAG, "Either a key Uri or ARG_SAVE_KEYRING_PARCEL is required!");
            getActivity().finish();
            return;
        }

        initView();
        if (dataUri != null) {
            loadData(dataUri);
        } else {
            loadSaveKeyringParcel(saveKeyringParcel);
        }
    }

    private void loadSaveKeyringParcel(SaveKeyringParcel saveKeyringParcel) {
        mSkpBuilder = SaveKeyringParcel.buildUpon(saveKeyringParcel);
        mPrimaryUserId = saveKeyringParcel.getChangePrimaryUserId();

        mUserIdsAddedAdapter = new UserIdsAddedAdapter(getActivity(), mSkpBuilder.getMutableAddUserIds(), true);
        mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);

        mSubkeysAddedAdapter = new SubkeysAddedAdapter(getActivity(), mSkpBuilder.getMutableAddSubKeys(), true);
        mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);
    }

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

        // load the secret key ring. we do verify here that the passphrase is correct, so cached won't do
        try {
            Uri secretUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
            CachedPublicKeyRing keyRing =
                    KeyRepository.create(getContext()).getCachedPublicKeyRing(secretUri);
            long masterKeyId = keyRing.getMasterKeyId();

            // check if this is a master secret key we can work with
            switch (keyRing.getSecretKeyType(masterKeyId)) {
                case GNU_DUMMY:
                    finishWithError(LogType.MSG_EK_ERROR_DUMMY);
                    return;
            }

            mSkpBuilder = SaveKeyringParcel.buildChangeKeyringParcel(masterKeyId, keyRing.getFingerprint());
            mPrimaryUserId = keyRing.getPrimaryUserIdWithFallback();

        } catch (PgpKeyNotFoundException | NotFoundException e) {
            finishWithError(LogType.MSG_EK_ERROR_NOT_FOUND);
            return;
        }

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, EditKeyFragment.this);
        getLoaderManager().initLoader(LOADER_ID_SUBKEYS, null, EditKeyFragment.this);

        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0);
        mUserIdsAdapter.setEditMode(mSkpBuilder);
        mUserIdsList.setAdapter(mUserIdsAdapter);

        // TODO: SaveParcel from savedInstance?!
        mUserIdsAddedAdapter = new UserIdsAddedAdapter(getActivity(), mSkpBuilder.getMutableAddUserIds(), false);
        mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);

        mSubkeysAdapter = new SubkeysAdapter(getActivity(), null, 0);
        mSubkeysAdapter.setEditMode(mSkpBuilder);
        mSubkeysList.setAdapter(mSubkeysAdapter);

        mSubkeysAddedAdapter = new SubkeysAddedAdapter(getActivity(), mSkpBuilder.getMutableAddSubKeys(), false);
        mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);
    }

    private void initView() {
        mChangePassphrase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassphrase();
            }
        });

        mAddUserId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserId();
            }
        });

        mAddSubkey.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubkey();
            }
        });

        mSubkeysList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editSubkey(position);
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

            case LOADER_ID_SUBKEYS: {
                Uri baseUri = KeychainContract.Keys.buildKeysUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        SubkeysAdapter.SUBKEYS_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(data);
                break;

            case LOADER_ID_SUBKEYS:
                mSubkeysAdapter.swapCursor(data);
                break;

        }
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
            case LOADER_ID_SUBKEYS:
                mSubkeysAdapter.swapCursor(null);
                break;
        }
    }

    private void changePassphrase() {
//        Intent passIntent = new Intent(getActivity(), PassphraseWizardActivity.class);
//        passIntent.setAction(PassphraseWizardActivity.CREATE_METHOD);
//        startActivityForResult(passIntent, 12);
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // cache new returned passphrase!
                    mSkpBuilder.setNewUnlock(ChangeUnlockParcel.createChangeUnlockParcel(
                            mSkpBuilder.getMasterKeyId(), mSkpBuilder.getFingerprint(),
                            (Passphrase) data.getParcelable(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE)));
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(
                messenger, R.string.title_change_passphrase);

        setPassphraseDialog.show(getActivity().getSupportFragmentManager(), "setPassphraseDialog");
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
                        String changePrimaryUserId = mSkpBuilder.getChangePrimaryUserId();
                        if (changePrimaryUserId != null && changePrimaryUserId.equals(userId)) {
                            mSkpBuilder.setChangePrimaryUserId(null);
                        } else {
                            mSkpBuilder.setChangePrimaryUserId(userId);
                        }
                        break;
                    case EditUserIdDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mSkpBuilder.getMutableRevokeUserIds().contains(userId)) {
                            mSkpBuilder.removeRevokeUserId(userId);
                        } else {
                            mSkpBuilder.addRevokeUserId(userId);
                            // not possible to revoke and change to primary user id
                            if (mSkpBuilder.getChangePrimaryUserId() != null
                                    && mSkpBuilder.getChangePrimaryUserId().equals(userId)) {
                                mSkpBuilder.setChangePrimaryUserId(null);
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

    private void editSubkey(final int position) {
        final long keyId = mSubkeysAdapter.getKeyId(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyDialogFragment.MESSAGE_CHANGE_EXPIRY:
                        editSubkeyExpiry(position);
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mSkpBuilder.getMutableRevokeSubKeys().contains(keyId)) {
                            mSkpBuilder.removeRevokeSubkey(keyId);
                        } else {
                            mSkpBuilder.addRevokeSubkey(keyId);
                        }
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_STRIP: {
                        SecretKeyType secretKeyType = mSubkeysAdapter.getSecretKeyType(position);
                        if (secretKeyType == SecretKeyType.GNU_DUMMY) {
                            // Key is already stripped; this is a no-op.
                            break;
                        }

                        SubkeyChange change = mSkpBuilder.getSubkeyChange(keyId);
                        if (change == null || !change.getDummyStrip()) {
                            mSkpBuilder.addOrReplaceSubkeyChange(SubkeyChange.createStripChange(keyId));
                        } else {
                            mSkpBuilder.removeSubkeyChange(change);
                        }
                        break;
                    }
                }
                getLoaderManager().getLoader(LOADER_ID_SUBKEYS).forceLoad();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                EditSubkeyDialogFragment dialogFragment =
                        EditSubkeyDialogFragment.newInstance(messenger);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "editSubkeyDialog");
            }
        });
    }

    private void editSubkeyExpiry(final int position) {
        final long keyId = mSubkeysAdapter.getKeyId(position);
        final Long creationDate = mSubkeysAdapter.getCreationDate(position);
        final Long expiryDate = mSubkeysAdapter.getExpiryDate(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyExpiryDialogFragment.MESSAGE_NEW_EXPIRY:
                        Long expiry = (Long) message.getData().getSerializable(
                                EditSubkeyExpiryDialogFragment.MESSAGE_DATA_EXPIRY);
                        mSkpBuilder.addOrReplaceSubkeyChange(
                                SubkeyChange.createFlagsOrExpiryChange(keyId, null, expiry));
                        break;
                }
                getLoaderManager().getLoader(LOADER_ID_SUBKEYS).forceLoad();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                EditSubkeyExpiryDialogFragment dialogFragment =
                        EditSubkeyExpiryDialogFragment.newInstance(messenger, creationDate, expiryDate);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "editSubkeyExpiryDialog");
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
        AddUserIdDialogFragment addUserIdDialog = AddUserIdDialogFragment.newInstance(messenger, predefinedName);

        addUserIdDialog.show(getActivity().getSupportFragmentManager(), "addUserIdDialog");
    }

    private void addSubkey() {
        // new subkey will never be a masterkey, as masterkey cannot be removed
        AddSubkeyDialogFragment addSubkeyDialogFragment =
                AddSubkeyDialogFragment.newInstance(false);
        addSubkeyDialogFragment
                .setOnAlgorithmSelectedListener(
                        new AddSubkeyDialogFragment.OnAlgorithmSelectedListener() {
                            @Override
                            public void onAlgorithmSelected(SaveKeyringParcel.SubkeyAdd newSubkey) {
                                mSubkeysAddedAdapter.add(newSubkey);
                            }
                        }
                );
        addSubkeyDialogFragment.show(getActivity().getSupportFragmentManager(), "addSubkeyDialog");
    }

    protected void returnKeyringParcel() {
        if (mSkpBuilder.getMutableAddUserIds().size() == 0) {
            Notify.create(getActivity(), R.string.edit_key_error_add_identity, Notify.Style.ERROR).show();
            return;
        }
        if (mSkpBuilder.getMutableAddSubKeys().size() == 0) {
            Notify.create(getActivity(), R.string.edit_key_error_add_subkey, Notify.Style.ERROR).show();
            return;
        }

        String firstUserId = mSkpBuilder.getMutableAddUserIds().get(0);
        mSkpBuilder.setChangePrimaryUserId(firstUserId);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSkpBuilder.build());
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
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public SaveKeyringParcel createOperationInput() {
        return mSkpBuilder.build();
    }

    @Override
    public void onQueuedOperationSuccess(OperationResult result) {

        // null-protected from Queueing*Fragment
        Activity activity = getActivity();

        // if good -> finish, return result to showkey and display there!
        Intent intent = new Intent();
        intent.putExtra(OperationResult.EXTRA_RESULT, result);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();

    }

}
