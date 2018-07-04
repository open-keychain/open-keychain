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

package org.sufficientlysecure.keychain.remote.ui;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel;

public class SelectPublicKeyFragment extends RecyclerFragment<KeyChoiceAdapter> {
    public static final String ARG_PRESELECTED_KEY_IDS = "preselected_key_ids";

    private Set<Long> selectedMasterKeyIds;
    private KeyChoiceAdapter keyChoiceAdapter;
    private KeyRepository keyRepository;

    public static SelectPublicKeyFragment newInstance(long[] preselectedKeyIds) {
        SelectPublicKeyFragment frag = new SelectPublicKeyFragment();
        Bundle args = new Bundle();

        args.putLongArray(ARG_PRESELECTED_KEY_IDS, preselectedKeyIds);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyRepository = KeyRepository.create(requireContext());

        selectedMasterKeyIds = new HashSet<>();
        for (long preselectedKey : getArguments().getLongArray(ARG_PRESELECTED_KEY_IDS)) {
            selectedMasterKeyIds.add(preselectedKey);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.list_empty));
        setLayoutManager(new LinearLayoutManager(getContext()));
        hideList(false);

        GenericViewModel viewModel = ViewModelProviders.of(this).get(GenericViewModel.class);
        LiveData<List<UnifiedKeyInfo>> liveData = viewModel.getGenericLiveData(requireContext(), this::loadSortedUnifiedKeyInfo);
        liveData.observe(this, this::onLoadUnifiedKeyData);
    }

    @NonNull
    private List<UnifiedKeyInfo> loadSortedUnifiedKeyInfo() {
        List<UnifiedKeyInfo> keyInfos = keyRepository.getAllUnifiedKeyInfo();
        Collections.sort(keyInfos, sortKeysByPreselectionComparator());
        return keyInfos;
    }

    @NonNull
    private Comparator<UnifiedKeyInfo> sortKeysByPreselectionComparator() {
        return (first, second) -> {
            if (first == second) {
                return 0;
            }
            boolean firstIsPreselected = selectedMasterKeyIds.contains(first.master_key_id());
            boolean secondIsPreselected = selectedMasterKeyIds.contains(second.master_key_id());
            if (firstIsPreselected != secondIsPreselected) {
                return firstIsPreselected ? -1 : 1;
            }
            String firstUid = first.user_id();
            String secondUid = second.user_id();
            if (firstUid != null && secondUid != null) {
                return firstUid.compareTo(secondUid);
            } else {
                return firstUid == null ? -1 : -1;
            }
        };
    }

    public long[] getSelectedMasterKeyIds() {
        if (keyChoiceAdapter == null) {
            return null;
        }
        // *sigh
        Set<Long> selectionIds = keyChoiceAdapter.getSelectionIds();
        long[] result = new long[selectionIds.size()];
        int i = 0;
        for (Long selectionId : selectionIds) {
            result[i++] = selectionId;
        }
        return result;
    }

    public void onLoadUnifiedKeyData(List<UnifiedKeyInfo> data) {
        if (keyChoiceAdapter == null) {
            keyChoiceAdapter = KeyChoiceAdapter.createMultiChoiceAdapter(requireContext(), data, (keyInfo -> {
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
            setAdapter(keyChoiceAdapter);
            keyChoiceAdapter.setSelectionByIds(selectedMasterKeyIds);
        } else {
            keyChoiceAdapter.setUnifiedKeyInfoItems(data);
        }

        boolean animateShowList = !isResumed();
        showList(animateShowList);
    }
}
