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

import android.app.ProgressDialog;
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
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.OperationResults;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAddedAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAddedAdapter;
import org.sufficientlysecure.keychain.ui.dialog.EditUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class EditKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

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

    private ArrayList<UserIdsAddedAdapter.UserIdModel> mUserIdsAddedData;

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
        mSubkeysList = (ListView) view.findViewById(R.id.edit_key_keys);
        mUserIdsAddedList = (ListView) view.findViewById(R.id.edit_key_user_ids_added);
        mSubkeysAddedList = (ListView) view.findViewById(R.id.edit_key_subkeys_added);
        mChangePassphrase = view.findViewById(R.id.edit_key_action_change_passphrase);
        mAddUserId = view.findViewById(R.id.edit_key_action_add_user_id);
        mAddSubkey = view.findViewById(R.id.edit_key_action_add_key);

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

        mAddSubkey.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubkey();
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

        // TODO: mUserIdsAddedData and SaveParcel from savedInstance?!
        mUserIdsAddedData = new ArrayList<UserIdsAddedAdapter.UserIdModel>();
        mUserIdsAddedAdapter = new UserIdsAddedAdapter(getActivity(), mUserIdsAddedData);
        mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);

        mSubkeysAdapter = new SubkeysAdapter(getActivity(), null, 0);
        mSubkeysList.setAdapter(mSubkeysAdapter);

        mSubkeysAddedAdapter = new SubkeysAddedAdapter(getActivity(), mSaveKeyringParcel.addSubKeys);
        mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        getLoaderManager().initLoader(LOADER_ID_SUBKEYS, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_USER_IDS: {
                Uri baseUri = KeychainContract.UserIds.buildUserIdsUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        UserIdsAdapter.USER_IDS_PROJECTION, null, null, null);
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
            case LOADER_ID_SUBKEYS:
                mSubkeysAdapter.swapCursor(null);
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

                    mSaveKeyringParcel.newPassphrase = newPassphrase;
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

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
        mUserIdsAddedAdapter.add(new UserIdsAddedAdapter.UserIdModel());
    }

    private void addSubkey() {
        // default values
        mSubkeysAddedAdapter.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.SIGN_DATA, null));
    }

    private void save() {
        String passphrase = PassphraseCacheService.getCachedPassphrase(getActivity(),
                mSaveKeyringParcel.mMasterKeyId);
        if (passphrase == null) {
            PassphraseDialogFragment.show(getActivity(), mSaveKeyringParcel.mMasterKeyId,
                    new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                String passphrase =
                                        message.getData().getString(PassphraseDialogFragment.MESSAGE_DATA_PASSPHRASE);
                                Log.d(Constants.TAG, "after caching passphrase");
                                saveFinal(passphrase);
                            }
                        }
                    }
            );
        } else {
            saveFinal(passphrase);
        }
    }

    private void saveFinal(String passphrase) {
        Log.d(Constants.TAG, "add userids to parcel: " + mUserIdsAddedAdapter.getDataAsStringList());
        mSaveKeyringParcel.addUserIds = mUserIdsAddedAdapter.getDataAsStringList();

        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                getActivity(),
                getString(R.string.progress_saving),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }
                    final OperationResults.SaveKeyringResult result =
                            returnData.getParcelable(KeychainIntentService.RESULT);
                    if (result == null) {
                        return;
                    }

                    // if good -> finish, return result to showkey and display there!
                    // if bad -> display here!

//                    result.displayNotify(ImportKeysActivity.this);

//                    getActivity().finish();
                }
            }
        };

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SAVE_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();
        data.putString(KeychainIntentService.SAVE_KEYRING_PASSPHRASE, passphrase);
        data.putParcelable(KeychainIntentService.SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }
}