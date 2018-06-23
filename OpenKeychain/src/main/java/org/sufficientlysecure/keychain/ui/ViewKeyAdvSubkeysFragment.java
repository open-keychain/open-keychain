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
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvActivity.ViewKeyAdvViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAddedAdapter;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.AddSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyExpiryDialogFragment;


public class ViewKeyAdvSubkeysFragment extends Fragment {
    private ListView mSubkeysList;
    private ListView mSubkeysAddedList;
    private View mSubkeysAddedLayout;
    private ViewAnimator mSubkeyAddFabLayout;

    private SubkeysAdapter mSubkeysAdapter;
    private SubkeysAddedAdapter mSubkeysAddedAdapter;

    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mEditKeyHelper;

    private SaveKeyringParcel.Builder mEditModeSkpBuilder;
    private UnifiedKeyInfo unifiedKeyInfo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_adv_subkeys_fragment, viewGroup, false);

        mSubkeysList = view.findViewById(R.id.view_key_subkeys);
        mSubkeysAddedList = view.findViewById(R.id.view_key_subkeys_added);
        mSubkeysAddedLayout = view.findViewById(R.id.view_key_subkeys_add_layout);

        mSubkeysList.setOnItemClickListener((parent, view1, position, id) -> editSubkey(position));

        View footer = new View(getActivity());
        int spacing = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()
        );
        android.widget.AbsListView.LayoutParams params = new android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.MATCH_PARENT,
                spacing
        );
        footer.setLayoutParams(params);
        mSubkeysAddedList.addFooterView(footer, null, false);

        mSubkeyAddFabLayout = view.findViewById(R.id.view_key_subkey_fab_layout);
        view.findViewById(R.id.view_key_subkey_fab).setOnClickListener(v -> addSubkey());

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create an empty adapter we will use to display the loaded data.
        mSubkeysAdapter = new SubkeysAdapter(requireContext());
        mSubkeysList.setAdapter(mSubkeysAdapter);

        ViewKeyAdvViewModel viewModel = ViewModelProviders.of(requireActivity()).get(ViewKeyAdvViewModel.class);
        viewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadFinished);
        viewModel.getSubkeyLiveData(requireContext()).observe(this, this::onLoadSubKeys);
    }

    public void onLoadFinished(UnifiedKeyInfo unifiedKeyInfo) {
        // Avoid NullPointerExceptions, if we get an empty result set.
        if (unifiedKeyInfo == null) {
            return;
        }

        this.unifiedKeyInfo = unifiedKeyInfo;
    }

    private void onLoadSubKeys(List<SubKey> subKeys) {
        mSubkeysAdapter.setData(subKeys);
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

                mEditModeSkpBuilder = SaveKeyringParcel.buildChangeKeyringParcel(unifiedKeyInfo.master_key_id(), unifiedKeyInfo.fingerprint());

                mSubkeysAddedAdapter = new SubkeysAddedAdapter(
                        getActivity(), mEditModeSkpBuilder.getMutableAddSubKeys(), false);
                mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);
                mSubkeysAddedLayout.setVisibility(View.VISIBLE);
                mSubkeyAddFabLayout.setDisplayedChild(1);

                mSubkeysAdapter.setEditMode(mEditModeSkpBuilder);

                mode.setTitle(R.string.title_edit_subkeys);
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
                mEditModeSkpBuilder = null;
                mSubkeysAdapter.setEditMode(null);
                mSubkeysAddedLayout.setVisibility(View.GONE);
                mSubkeyAddFabLayout.setDisplayedChild(0);
            }
        });
    }

    private void addSubkey() {
        boolean willBeMasterKey;
        if (mSubkeysAdapter != null) {
            willBeMasterKey = mSubkeysAdapter.getCount() == 0 && mSubkeysAddedAdapter.getCount() == 0;
        } else {
            willBeMasterKey = mSubkeysAddedAdapter.getCount() == 0;
        }

        AddSubkeyDialogFragment addSubkeyDialogFragment =
                AddSubkeyDialogFragment.newInstance(willBeMasterKey);
        addSubkeyDialogFragment
                .setOnAlgorithmSelectedListener(newSubkey -> mSubkeysAddedAdapter.add(newSubkey));
        addSubkeyDialogFragment.show(requireFragmentManager(), "addSubkeyDialog");
    }

    private void editSubkey(final int position) {
        final SubKey subKey = mSubkeysAdapter.getItem(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyDialogFragment.MESSAGE_CHANGE_EXPIRY:
                        editSubkeyExpiry(position);
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mEditModeSkpBuilder.getMutableRevokeSubKeys().contains(subKey.key_id())) {
                            mEditModeSkpBuilder.removeRevokeSubkey(subKey.key_id());
                        } else {
                            mEditModeSkpBuilder.addRevokeSubkey(subKey.key_id());
                        }
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_STRIP: {
                        if (subKey.has_secret() == SecretKeyType.GNU_DUMMY) {
                            // Key is already stripped; this is a no-op.
                            break;
                        }

                        SubkeyChange change = mEditModeSkpBuilder.getSubkeyChange(subKey.key_id());
                        if (change == null || !change.getDummyStrip()) {
                            mEditModeSkpBuilder.addOrReplaceSubkeyChange(SubkeyChange.createStripChange(subKey.key_id()));
                        } else {
                            mEditModeSkpBuilder.removeSubkeyChange(change);
                        }
                        break;
                    }
                }
                mSubkeysAdapter.notifyDataSetChanged();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(() -> {
            EditSubkeyDialogFragment dialogFragment =
                    EditSubkeyDialogFragment.newInstance(messenger);

            dialogFragment.show(requireFragmentManager(), "editSubkeyDialog");
        });
    }

    private void editSubkeyExpiry(final int position) {
        SubKey subKey = mSubkeysAdapter.getItem(position);
        final long keyId = subKey.key_id();
        final Long creationDate = subKey.creation();
        final Long expiryDate = subKey.expiry();

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyExpiryDialogFragment.MESSAGE_NEW_EXPIRY:
                        Long expiry = (Long) message.getData().getSerializable(
                                EditSubkeyExpiryDialogFragment.MESSAGE_DATA_EXPIRY);
                        mEditModeSkpBuilder.addOrReplaceSubkeyChange(
                                SubkeyChange.createFlagsOrExpiryChange(keyId, null, expiry));
                        break;
                }
                mSubkeysAdapter.notifyDataSetChanged();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(() -> {
            EditSubkeyExpiryDialogFragment dialogFragment =
                    EditSubkeyExpiryDialogFragment.newInstance(messenger, creationDate, expiryDate);

            dialogFragment.show(requireFragmentManager(), "editSubkeyExpiryDialog");
        });
    }


    private void editKey(final ActionMode mode) {
        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> editKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return mEditModeSkpBuilder.build();
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
