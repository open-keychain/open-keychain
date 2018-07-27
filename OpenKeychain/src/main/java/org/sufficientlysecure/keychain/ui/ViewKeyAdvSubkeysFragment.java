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


import java.util.ArrayList;
import java.util.List;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.FlexibleAdapter.OnItemClickListener;
import eu.davidea.flexibleadapter.items.IFlexible;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Builder;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvActivity.ViewKeyAdvViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SubkeyAddedItem;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.AddSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyExpiryDialogFragment;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;


public class ViewKeyAdvSubkeysFragment extends Fragment {
    public static final int SUBKEY_TYPE_DETAIL = 1;
    public static final int SUBKEY_TYPE_ADDED = 2;

    private RecyclerView subkeysList;
    private ViewAnimator subkeyAddFabLayout;

    private FlexibleAdapter<IFlexible> subkeysAdapter;

    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mEditKeyHelper;
    private SubkeyEditViewModel subkeyEditViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_adv_subkeys_fragment, viewGroup, false);

        subkeysList = view.findViewById(R.id.view_key_subkeys);
        subkeysList.setLayoutManager(new LinearLayoutManager(requireContext()));
        subkeysList.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL, false));

                subkeyAddFabLayout = view.findViewById(R.id.view_key_subkey_fab_layout);
        view.findViewById(R.id.view_key_subkey_fab).setOnClickListener(v -> addSubkey());

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        subkeysAdapter = new FlexibleAdapter<>(null, null, true);
        subkeysAdapter.addListener((OnItemClickListener) (view, position) -> editSubkey(position));
        subkeysList.setAdapter(subkeysAdapter);

        ViewKeyAdvViewModel viewModel = ViewModelProviders.of(requireActivity()).get(ViewKeyAdvViewModel.class);
        viewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadUnifiedKeyId);
        viewModel.getSubkeyLiveData(requireContext()).observe(this, this::onLoadSubKeys);

        subkeyEditViewModel = ViewModelProviders.of(this).get(SubkeyEditViewModel.class);
    }

    public static class SubkeyEditViewModel extends ViewModel {
        public Builder skpBuilder;
        UnifiedKeyInfo unifiedKeyInfo;
    }

    public void onLoadUnifiedKeyId(UnifiedKeyInfo unifiedKeyInfo) {
        subkeyEditViewModel.unifiedKeyInfo = unifiedKeyInfo;
    }

    private void onLoadSubKeys(List<SubKey> subKeys) {
        ArrayList<IFlexible> subKeyItems = new ArrayList<>(subKeys.size());
        for (SubKey subKey : subKeys) {
            subKeyItems.add(new SubKeyItem(subKey, subkeyEditViewModel));
        }
        subkeysAdapter.updateDataSet(subKeyItems);
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
                subkeyAddFabLayout.setDisplayedChild(1);
                subkeyEditViewModel.skpBuilder = SaveKeyringParcel.buildChangeKeyringParcel(
                        subkeyEditViewModel.unifiedKeyInfo.master_key_id(), subkeyEditViewModel.unifiedKeyInfo.fingerprint());
                subkeysAdapter.notifyDataSetChanged();

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
                subkeyEditViewModel.skpBuilder = null;
                subkeysAdapter.removeItemsOfType(2);
                subkeyAddFabLayout.setDisplayedChild(0);
                subkeysAdapter.notifyDataSetChanged();
            }
        });
    }

    private void addSubkey() {
        boolean willBeMasterKey = subkeysAdapter.getItemCount() == 0;

        AddSubkeyDialogFragment addSubkeyDialogFragment = AddSubkeyDialogFragment.newInstance(willBeMasterKey);
        addSubkeyDialogFragment.setOnAlgorithmSelectedListener(newSubkey -> {
            subkeyEditViewModel.skpBuilder.addSubkeyAdd(newSubkey);
            subkeysAdapter.addItem(new SubkeyAddedItem(newSubkey, subkeyEditViewModel));
        });
        addSubkeyDialogFragment.show(requireFragmentManager(), "addSubkeyDialog");
    }

    private boolean editSubkey(final int position) {
        if (subkeyEditViewModel.skpBuilder == null) {
            return false;
        }

        IFlexible item = subkeysAdapter.getItem(position);
        if (item instanceof SubKeyItem) {
            editSubkey(position, ((SubKeyItem) item));
        }

        return false;
    }

    private void editSubkey(int position, SubKeyItem item) {
        if (subkeyEditViewModel.skpBuilder.hasModificationsForSubkey(item.subkeyInfo.key_id())) {
            return;
        }

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyDialogFragment.MESSAGE_CHANGE_EXPIRY:
                        editSubkeyExpiry(item);
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_REVOKE:
                        SubKey subKey = item.subkeyInfo;
                        subkeyEditViewModel.skpBuilder.addRevokeSubkey(subKey.key_id());
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_STRIP: {
                        editSubkeyToggleStrip(item);
                        break;
                    }
                }
                subkeysAdapter.notifyItemChanged(position);
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(() -> {
            EditSubkeyDialogFragment dialogFragment = EditSubkeyDialogFragment.newInstance(messenger);
            dialogFragment.show(requireFragmentManager(), "editSubkeyDialog");
        });
    }

    private void editSubkeyToggleStrip(SubKeyItem item) {
        SubKey subKey = item.subkeyInfo;
        if (subKey.has_secret() == SecretKeyType.GNU_DUMMY) {
            // Key is already stripped; this is a no-op.
            return;
        }

        subkeyEditViewModel.skpBuilder.addOrReplaceSubkeyChange(SubkeyChange.createStripChange(subKey.key_id()));
    }

    private void editSubkeyExpiry(SubKeyItem item) {
        SubKey subKey = item.subkeyInfo;

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
                        subkeyEditViewModel.skpBuilder.addOrReplaceSubkeyChange(
                                SubkeyChange.createFlagsOrExpiryChange(keyId, null, expiry));
                        break;
                }
                subkeysAdapter.notifyDataSetChanged();
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
                return subkeyEditViewModel.skpBuilder.build();
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
