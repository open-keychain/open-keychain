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

package org.sufficientlysecure.keychain.remote.ui.dialog;


import java.util.List;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.v7.widget.RecyclerView.Adapter;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.remote.AutocryptInteractor;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteDeduplicateActivity.DeduplicateViewModel;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter;


class RemoteDeduplicatePresenter {
    private final Context context;
    private final LifecycleOwner lifecycleOwner;


    private AutocryptInteractor autocryptInteractor;

    private DeduplicateViewModel viewModel;
    private RemoteDeduplicateView view;
    private KeyChoiceAdapter keyChoiceAdapter;


    RemoteDeduplicatePresenter(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setView(RemoteDeduplicateView view) {
        this.view = view;
    }

    void setupFromViewModel(DeduplicateViewModel viewModel) {
        this.viewModel = viewModel;
        this.autocryptInteractor = AutocryptInteractor.getInstance(context, viewModel.getPackageName());

        view.setAddressText(viewModel.getDuplicateAddress());

        viewModel.getKeyInfoLiveData(context).observe(lifecycleOwner, this::onLoadKeyInfos);
    }

    private void onLoadKeyInfos(List<UnifiedKeyInfo> data) {
        if (keyChoiceAdapter == null) {
            keyChoiceAdapter = KeyChoiceAdapter.createSingleChoiceAdapter(context, data, (keyInfo -> {
                if (keyInfo.is_revoked()) {
                    return R.string.keychoice_revoked;
                } else if (keyInfo.is_expired()) {
                    return R.string.keychoice_expired;
                } else if (!keyInfo.is_secure()) {
                    return R.string.keychoice_insecure;
                } else if (!keyInfo.has_encrypt_key()) {
                    return R.string.keychoice_cannot_encrypt;
                } else {
                    return null;
                }
            }));
            view.setKeyListAdapter(keyChoiceAdapter);
        } else {
            keyChoiceAdapter.setUnifiedKeyInfoItems(data);
        }
    }

    void onClickSelect() {
        UnifiedKeyInfo activeItem = keyChoiceAdapter.getActiveItem();
        if (activeItem == null) {
            view.showNoSelectionError();
            return;
        }
        long masterKeyId = activeItem.master_key_id();
        autocryptInteractor.updateKeyGossipFromDedup(viewModel.getDuplicateAddress(), masterKeyId);

        view.finish();
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    public void onCancel() {
        view.finishAsCancelled();
    }

    interface RemoteDeduplicateView {
        void showNoSelectionError();
        void finish();
        void finishAsCancelled();

        void setAddressText(String text);

        void setKeyListAdapter(Adapter adapter);
    }
}
