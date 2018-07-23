/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAddedAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAddedAdapter;
import org.sufficientlysecure.keychain.ui.dialog.AddSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import timber.log.Timber;


public class EditKeyFragment extends Fragment {
    private static final String ARG_SAVE_KEYRING_PARCEL = "save_keyring_parcel";

    private ListView mUserIdsAddedList;
    private ListView mSubkeysAddedList;
    private View mChangePassphrase;
    private View mAddUserId;
    private View mAddSubkey;

    // array adapter
    private UserIdsAddedAdapter mUserIdsAddedAdapter;
    private SubkeysAddedAdapter mSubkeysAddedAdapter;

    private SaveKeyringParcel.Builder mSkpBuilder;

    private String mPrimaryUserId;

    public static EditKeyFragment newInstance(SaveKeyringParcel saveKeyringParcel) {
        EditKeyFragment frag = new EditKeyFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_SAVE_KEYRING_PARCEL, saveKeyringParcel);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_key_fragment, superContainer, false);

        mUserIdsAddedList = view.findViewById(R.id.edit_key_user_ids_added);
        mSubkeysAddedList = view.findViewById(R.id.edit_key_subkeys_added);
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
                v -> {
                    // if we are working on an Uri, save directly
                    returnKeyringParcel();
                },
                v -> {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                });

        SaveKeyringParcel saveKeyringParcel = getArguments().getParcelable(ARG_SAVE_KEYRING_PARCEL);
        if (saveKeyringParcel == null) {
            Timber.e("Either a key Uri or ARG_SAVE_KEYRING_PARCEL is required!");
            getActivity().finish();
            return;
        }

        initView();
        loadSaveKeyringParcel(saveKeyringParcel);
    }

    private void loadSaveKeyringParcel(SaveKeyringParcel saveKeyringParcel) {
        mSkpBuilder = SaveKeyringParcel.buildUpon(saveKeyringParcel);
        mPrimaryUserId = saveKeyringParcel.getChangePrimaryUserId();

        mUserIdsAddedAdapter = new UserIdsAddedAdapter(getActivity(), mSkpBuilder.getMutableAddUserIds(), true);
        mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);

        mSubkeysAddedAdapter = new SubkeysAddedAdapter(getActivity(), mSkpBuilder.getMutableAddSubKeys());
        mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);
    }

    private void initView() {
        mChangePassphrase.setOnClickListener(v -> changePassphrase());
        mAddUserId.setOnClickListener(v -> addUserId());
        mAddSubkey.setOnClickListener(v -> addSubkey());
    }

    private void changePassphrase() {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // cache new returned passphrase!
                    mSkpBuilder.setNewUnlock(ChangeUnlockParcel.createChangeUnlockParcel(
                            mSkpBuilder.getMasterKeyId(), mSkpBuilder.getFingerprint(),
                            data.getParcelable(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE)));
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(
                messenger, R.string.title_change_passphrase);

        setPassphraseDialog.show(getActivity().getSupportFragmentManager(), "setPassphraseDialog");
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
}
