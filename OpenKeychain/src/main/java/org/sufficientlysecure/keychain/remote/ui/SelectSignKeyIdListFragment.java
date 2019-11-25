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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel;


public class SelectSignKeyIdListFragment extends RecyclerFragment<KeyChoiceAdapter> {
    private static final String ARG_PACKAGE_NAME = "package_name";
    private static final String ARG_PACKAGE_SIGNATURE = "package_signature";
    private static final String ARG_PREF_UID = "pref_uid";
    public static final String ARG_DATA = "data";

    private KeyRepository keyRepository;

    private KeyChoiceAdapter keyChoiceAdapter;

    private Intent resultIntent;
    private String prefUid;
    private String packageName;
    private byte[] packageSignature;

    public static SelectSignKeyIdListFragment newInstance(String packageName, byte[] packageSignature,
            Intent data, String preferredUserId) {
        SelectSignKeyIdListFragment frag = new SelectSignKeyIdListFragment();
        Bundle args = new Bundle();

        args.putString(ARG_PACKAGE_NAME, packageName);
        args.putByteArray(ARG_PACKAGE_SIGNATURE, packageSignature);
        args.putParcelable(ARG_DATA, data);
        args.putString(ARG_PREF_UID, preferredUserId);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        keyRepository = KeyRepository.create(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        LinearLayout linearLayout = new LinearLayout(requireContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        View dummyItemView = inflater.inflate(R.layout.select_dummy_key_item, linearLayout, false);
        linearLayout.addView(dummyItemView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View recyclerView = super.onCreateView(inflater, parent, savedInstanceState);
        linearLayout.addView(recyclerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dummyItemView.setOnClickListener((v) -> onCreateKeyDummyClicked());

        return linearLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        resultIntent = getArguments().getParcelable(ARG_DATA);
        prefUid = getArguments().getString(ARG_PREF_UID);
        packageName = getArguments().getString(ARG_PACKAGE_NAME);
        packageSignature = getArguments().getByteArray(ARG_PACKAGE_SIGNATURE);

        setEmptyText(getString(R.string.list_empty));
        setLayoutManager(new LinearLayoutManager(getContext()));
        hideList(false);

        GenericViewModel viewModel = ViewModelProviders.of(this).get(GenericViewModel.class);
        LiveData<List<UnifiedKeyInfo>> liveData = viewModel.getGenericLiveData(
                requireContext(), keyRepository::getAllUnifiedKeyInfoWithSecret);
        liveData.observe(this, this::onLoadUnifiedKeyData);
    }

    public void onLoadUnifiedKeyData(List<UnifiedKeyInfo> data) {
        if (keyChoiceAdapter == null) {
            keyChoiceAdapter = KeyChoiceAdapter.createSingleClickableAdapter(requireContext(), data, this::onSelectKeyItemClicked, (keyInfo -> {
                if (keyInfo.is_revoked()) {
                    return R.string.keychoice_revoked;
                } else if (keyInfo.is_expired()) {
                    return R.string.keychoice_expired;
                } else if (!keyInfo.is_secure()) {
                    return R.string.keychoice_insecure;
                } else if (!keyInfo.has_sign_key()) {
                    return R.string.keychoice_cannot_sign;
                } else {
                    return null;
                }
            }));
            setAdapter(keyChoiceAdapter);
        } else {
            keyChoiceAdapter.setUnifiedKeyInfoItems(data);
        }

        boolean animateShowList = !isResumed();
        showList(animateShowList);
    }

    public void onCreateKeyDummyClicked() {
        OpenPgpUtils.UserId userIdSplit = KeyRing.splitUserId(prefUid);

        Intent intent = new Intent(getActivity(), CreateKeyActivity.class);
        intent.putExtra(CreateKeyActivity.EXTRA_NAME, userIdSplit.name);
        intent.putExtra(CreateKeyActivity.EXTRA_EMAIL, userIdSplit.email);

        requireActivity().startActivityForResult(intent, SelectSignKeyIdActivity.REQUEST_CODE_CREATE_KEY);
    }

    private void onSelectKeyItemClicked(UnifiedKeyInfo keyInfo) {
        resultIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, keyInfo.master_key_id());

        Activity activity = requireActivity();
        activity.setResult(Activity.RESULT_OK, resultIntent);
        activity.finish();
    }
}
