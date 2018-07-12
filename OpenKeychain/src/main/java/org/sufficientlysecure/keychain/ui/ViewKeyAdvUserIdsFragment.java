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


import java.util.List;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvActivity.ViewKeyAdvViewModel;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAddedAdapter;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.AddUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditUserIdDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;


public class ViewKeyAdvUserIdsFragment extends Fragment {
    private ListView mUserIds;
    private ListView mUserIdsAddedList;
    private View mUserIdsAddedLayout;
    private ViewAnimator mUserIdAddFabLayout;

    private UserIdsAdapter mUserIdsAdapter;
    private UserIdsAddedAdapter mUserIdsAddedAdapter;

    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mEditKeyHelper;

    private SaveKeyringParcel.Builder mSkpBuilder;
    private UnifiedKeyInfo unifiedKeyInfo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_adv_user_ids_fragment, viewGroup, false);

        mUserIds = view.findViewById(R.id.view_key_user_ids);
        mUserIdsAddedList = view.findViewById(R.id.view_key_user_ids_added);
        mUserIdsAddedLayout = view.findViewById(R.id.view_key_user_ids_add_layout);

        mUserIds.setOnItemClickListener((parent, view1, position, id) -> showOrEditUserIdInfo(position));

        View footer = new View(getActivity());
        int spacing = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()
        );
        android.widget.AbsListView.LayoutParams params = new android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.MATCH_PARENT,
                spacing
        );
        footer.setLayoutParams(params);
        mUserIdsAddedList.addFooterView(footer, null, false);

        mUserIdAddFabLayout = view.findViewById(R.id.view_key_subkey_fab_layout);
        view.findViewById(R.id.view_key_subkey_fab).setOnClickListener(v -> addUserId());

        setHasOptionsMenu(true);

        return view;
    }

    private void showOrEditUserIdInfo(final int position) {
        if (mSkpBuilder != null) {
            editUserId(position);
        } else {
            showUserIdInfo(position);
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
                        if (mSkpBuilder.getChangePrimaryUserId() != null
                                && mSkpBuilder.getChangePrimaryUserId().equals(userId)) {
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
                mUserIdsAdapter.notifyDataSetChanged();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(() -> {
            EditUserIdDialogFragment dialogFragment =
                    EditUserIdDialogFragment.newInstance(messenger, isRevoked, isRevokedPending);
            dialogFragment.show(requireFragmentManager(), "editUserIdDialog");
        });
    }

    private void showUserIdInfo(final int position) {

        final boolean isRevoked = mUserIdsAdapter.getIsRevoked(position);
        final boolean isVerified = mUserIdsAdapter.getVerificationStatus(position) == VerificationStatus.VERIFIED_SECRET;

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(() -> {
            UserIdInfoDialogFragment dialogFragment =
                    UserIdInfoDialogFragment.newInstance(isRevoked, isVerified);

            dialogFragment.show(requireFragmentManager(), "userIdInfoDialog");
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
        AddUserIdDialogFragment addUserIdDialog = AddUserIdDialogFragment.newInstance(messenger, "");

        addUserIdDialog.show(requireFragmentManager(), "addUserIdDialog");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUserIdsAdapter = new UserIdsAdapter(getActivity(), false);
        mUserIds.setAdapter(mUserIdsAdapter);

        ViewKeyAdvViewModel viewModel = ViewModelProviders.of(requireActivity()).get(ViewKeyAdvViewModel.class);
        viewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadUnifiedKeyInfo);
        viewModel.getUserIdLiveData(requireContext()).observe(this, this::onLoadUserIds);
    }

    public void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }
        this.unifiedKeyInfo = unifiedKeyInfo;
    }

    private void onLoadUserIds(List<UserId> userIds) {
        mUserIdsAdapter.setData(userIds);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mEditKeyHelper != null) {
            mEditKeyHelper.handleActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_mode_edit:
                enterEditMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void enterEditMode() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mSkpBuilder = SaveKeyringParcel.buildChangeKeyringParcel(
                        unifiedKeyInfo.master_key_id(), unifiedKeyInfo.fingerprint());

                mUserIdsAddedAdapter =
                        new UserIdsAddedAdapter(getActivity(), mSkpBuilder.getMutableAddUserIds(), false);
                mUserIdsAddedList.setAdapter(mUserIdsAddedAdapter);
                mUserIdsAddedLayout.setVisibility(View.VISIBLE);
                mUserIdAddFabLayout.setDisplayedChild(1);

                mUserIdsAdapter.setEditMode(mSkpBuilder);
                mUserIdsAdapter.notifyDataSetChanged();

                mode.setTitle(R.string.title_edit_identities);
                mode.getMenuInflater().inflate(R.menu.action_edit_uids, menu);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                editKey(mode);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mSkpBuilder = null;
                mUserIdsAdapter.setEditMode(null);
                mUserIdsAddedLayout.setVisibility(View.GONE);
                mUserIdAddFabLayout.setDisplayedChild(0);
                mUserIdsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void editKey(final ActionMode mode) {
        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> editKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return mSkpBuilder.build();
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                mode.finish();
                result.createNotify(getActivity()).show();
            }

            @Override
            public void onCryptoOperationCancelled() {
                mode.finish();
            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                mode.finish();
                result.createNotify(getActivity()).show();
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };
        mEditKeyHelper = new CryptoOperationHelper<>(1, this, editKeyCallback, R.string.progress_saving);
        mEditKeyHelper.cryptoOperation();
    }

}
