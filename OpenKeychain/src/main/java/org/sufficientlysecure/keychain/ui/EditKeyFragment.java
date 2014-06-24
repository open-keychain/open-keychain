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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.AddUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

public class EditKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private ListView mUserIdsList;
    private ListView mKeysList;
    private ListView mUserIdsAddedList;
    private ListView mKeysAddedList;
    private View mChangePassphrase;
    private View mAddUserId;
    private View mAddKey;

    private static final int LOADER_ID_USER_IDS = 0;
    private static final int LOADER_ID_KEYS = 1;

    private UserIdsAdapter mUserIdsAdapter;
    private SubkeysAdapter mKeysAdapter;

    private Uri mDataUri;

    private SaveKeyringParcel mSaveKeyringParcel;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.edit_key_fragment, getContainer());

        mUserIdsList = (ListView) view.findViewById(R.id.edit_key_user_ids);
        mKeysList = (ListView) view.findViewById(R.id.edit_key_keys);
        mUserIdsAddedList = (ListView) view.findViewById(R.id.edit_key_user_ids_added);
        mKeysAddedList = (ListView) view.findViewById(R.id.edit_key_keys_added);
        mChangePassphrase = view.findViewById(R.id.edit_key_action_change_passphrase);
        mAddUserId = view.findViewById(R.id.edit_key_action_add_user_id);
        mAddKey = view.findViewById(R.id.edit_key_action_add_key);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        ActionBarHelper.setTwoButtonView(((ActionBarActivity) getActivity()).getSupportActionBar(),
                R.string.btn_save, R.drawable.ic_action_save,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Save
                        save();
                    }
                }, R.string.menu_key_edit_cancel, R.drawable.ic_action_cancel,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Cancel
                        getActivity().finish();
                    }
                }
        );

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }


    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        try {
            Uri secretUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
            WrappedSecretKeyRing keyRing =
                    new ProviderHelper(getActivity()).getWrappedSecretKeyRing(secretUri);

            mSaveKeyringParcel = new SaveKeyringParcel(keyRing.getMasterKeyId(),
                    keyRing.getUncachedKeyRing().getFingerprint());
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "Keyring not found: " + e.getMessage(), e);
            Toast.makeText(getActivity(), R.string.error_no_secret_key_found, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

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

        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, mSaveKeyringParcel);
        mUserIdsList.setAdapter(mUserIdsAdapter);

        mUserIdsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String userId = mUserIdsAdapter.getUserId(position);
                editUserId(userId);
            }
        });

        mKeysAdapter = new SubkeysAdapter(getActivity(), null, 0);
        mKeysList.setAdapter(mKeysAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        getLoaderManager().initLoader(LOADER_ID_KEYS, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_USER_IDS: {
                Uri baseUri = KeychainContract.UserIds.buildUserIdsUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        UserIdsAdapter.USER_IDS_PROJECTION, null, null, null);
            }

            case LOADER_ID_KEYS: {
                Uri baseUri = KeychainContract.Keys.buildKeysUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        SubkeysAdapter.KEYS_PROJECTION, null, null, null);
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

            case LOADER_ID_KEYS:
                mKeysAdapter.swapCursor(data);
                break;

        }
        setContentShown(true);
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
            case LOADER_ID_KEYS:
                mKeysAdapter.swapCursor(null);
                break;
        }
    }

    private void changePassphrase() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // set new returned passphrase!
                    String newPassphrase = data
                            .getString(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE);

//                    updatePassphraseButtonText();
//                    somethingChanged();
                    mSaveKeyringParcel.newPassphrase = newPassphrase;
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        // set title based on isPassphraseSet()
//        int title;
//        if (isPassphraseSet()) {
//            title = R.string.title_change_passphrase;
//        } else {
//            title = R.string.title_set_passphrase;
//        }

        SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(
                messenger, R.string.title_change_passphrase);

        setPassphraseDialog.show(getActivity().getSupportFragmentManager(), "setPassphraseDialog");
    }

    private void editUserId(final String userId) {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditUserIdDialogFragment.MESSAGE_CHANGE_PRIMARY_USER_ID:
                        // toggle
                        if (mSaveKeyringParcel.changePrimaryUserId != null
                                && mSaveKeyringParcel.changePrimaryUserId.equals(userId)) {
                            mSaveKeyringParcel.changePrimaryUserId = null;
                        } else {
                            mSaveKeyringParcel.changePrimaryUserId = userId;
                        }
                        break;
                    case EditUserIdDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mSaveKeyringParcel.revokeUserIds.contains(userId)) {
                            mSaveKeyringParcel.revokeUserIds.remove(userId);
                        } else {
                            mSaveKeyringParcel.revokeUserIds.add(userId);
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
                        EditUserIdDialogFragment.newInstance(messenger);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "editUserIdDialog");
            }
        });
    }

    private void addUserId() {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == AddUserIdDialogFragment.MESSAGE_OK) {

                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                AddUserIdDialogFragment dialogFragment =
                        AddUserIdDialogFragment.newInstance(messenger);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "addUserIdDialog");
            }
        });
    }

    private void save() {
        getActivity().finish();
        // TODO
    }


}
