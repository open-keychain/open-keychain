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


import java.util.List;
import java.util.Set;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel;


public class AppSettingsAllowedKeysListFragment extends RecyclerFragment<KeyChoiceAdapter> {
    private static final String ARG_PACKAGE_NAME = "package_name";

    private KeyChoiceAdapter keyChoiceAdapter;
    private ApiAppDao apiAppDao;

    private String packageName;

    public static AppSettingsAllowedKeysListFragment newInstance(String packageName) {
        AppSettingsAllowedKeysListFragment frag = new AppSettingsAllowedKeysListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, packageName);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiAppDao = ApiAppDao.getInstance(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        packageName = getArguments().getString(ARG_PACKAGE_NAME);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));

        getRecyclerView().setLayoutManager(new LinearLayoutManager(requireContext()));

        // Start out with a progress indicator.
        hideList(false);

        KeyRepository keyRepository = KeyRepository.create(requireContext());
        GenericViewModel viewModel = ViewModelProviders.of(this).get(GenericViewModel.class);
        LiveData<List<UnifiedKeyInfo>> liveData =
                viewModel.getGenericLiveData(requireContext(), keyRepository::getAllUnifiedKeyInfoWithSecret);
        liveData.observe(this, this::onLoadUnifiedKeyData);
    }

    public void saveAllowedKeys() {
        Set<Long> longs = keyChoiceAdapter.getSelectionIds();
        apiAppDao.saveAllowedKeyIdsForApp(packageName, longs);
    }

    public void onLoadUnifiedKeyData(List<UnifiedKeyInfo> data) {
        if (keyChoiceAdapter == null) {
            keyChoiceAdapter = KeyChoiceAdapter.createMultiChoiceAdapter(requireContext(), data, null);
            setAdapter(keyChoiceAdapter);
            Set<Long> checkedIds = apiAppDao.getAllowedKeyIdsForApp(packageName);
            keyChoiceAdapter.setSelectionByIds(checkedIds);
        } else {
            keyChoiceAdapter.setUnifiedKeyInfoItems(data);
        }

        boolean animateShowList = !isResumed();
        showList(animateShowList);
    }

}
