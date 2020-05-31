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
import java.util.Iterator;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import org.sufficientlysecure.materialchips.ChipsInput.SimpleChipsListener;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput.EncryptRecipientChip;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Passphrase;


public class EncryptModeAsymmetricFragment extends EncryptModeFragment {
    public static final String ARG_SINGATURE_KEY_ID = "signature_key_id";
    public static final String ARG_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    KeyRepository keyRepository;

    private KeySpinner mSignKeySpinner;
    private EncryptRecipientChipsInput mEncryptKeyView;

    public static EncryptModeAsymmetricFragment newInstance(long signatureKey, long[] encryptionKeyIds) {
        EncryptModeAsymmetricFragment frag = new EncryptModeAsymmetricFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_SINGATURE_KEY_ID, signatureKey);
        args.putLongArray(ARG_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyRepository = KeyRepository.create(requireContext());
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_asymmetric_fragment, container, false);

        mSignKeySpinner = view.findViewById(R.id.sign_key_spinner);
        mEncryptKeyView = view.findViewById(R.id.recipient_list);

        ViewGroup filterableListAnchor = view.findViewById(R.id.anchor_dropdown_encrypt);
        mEncryptKeyView.setFilterableListLayout(filterableListAnchor);

        final ViewAnimator vSignatureIcon = view.findViewById(R.id.result_signature_icon);
        mSignKeySpinner.setOnKeyChangedListener(masterKeyId -> {
            int child = masterKeyId != Constants.key.none ? 1 : 0;
            if (vSignatureIcon.getDisplayedChild() != child) {
                vSignatureIcon.setDisplayedChild(child);
            }
        });
        mSignKeySpinner.setShowNone(R.string.cert_none);

        final ViewAnimator vEncryptionIcon = view.findViewById(R.id.result_encryption_icon);
        mEncryptKeyView.addChipsListener(new SimpleChipsListener<EncryptRecipientChip>() {
            @Override
            public void onChipAdded(EncryptRecipientChip chipInterface, int newSize) {
                if (vEncryptionIcon.getDisplayedChild() != 1) {
                    vEncryptionIcon.setDisplayedChild(1);
                }
            }

            @Override
            public void onChipRemoved(EncryptRecipientChip chipInterface, int newSize) {
                int child = newSize == 0 ? 0 : 1;
                if (vEncryptionIcon.getDisplayedChild() != child) {
                    vEncryptionIcon.setDisplayedChild(child);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EncryptModeViewModel viewModel = ViewModelProviders.of(this).get(EncryptModeViewModel.class);
        viewModel.getSignKeyLiveData(requireContext()).observe(this, mSignKeySpinner::setData);
        viewModel.getEncryptRecipientLiveData(requireContext()).observe(this, mEncryptKeyView::setData);

        // preselect keys given, from state or arguments
        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            preselectKeysFromArguments(arguments);
        }
    }

    private void preselectKeysFromArguments(Bundle arguments) {
        long preselectedSignatureKeyId = arguments.getLong(ARG_SINGATURE_KEY_ID);
        if (preselectedSignatureKeyId != Constants.key.none) {
            mSignKeySpinner.setPreSelectedKeyId(preselectedSignatureKeyId);
        }
        long[] preselectedEncryptionKeyIds = arguments.getLongArray(ARG_ENCRYPTION_KEY_IDS);
        if (preselectedEncryptionKeyIds != null) {
            mEncryptKeyView.setPreSelectedKeyIds(preselectedEncryptionKeyIds);
        }
    }

    public static class EncryptModeViewModel extends ViewModel {
        private LiveData<List<UnifiedKeyInfo>> signKeyLiveData;
        private LiveData<List<EncryptRecipientChip>> encryptRecipientLiveData;

        LiveData<List<UnifiedKeyInfo>> getSignKeyLiveData(Context context) {
            if (signKeyLiveData == null) {
                signKeyLiveData = new GenericLiveData<>(context, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    return keyRepository.getAllUnifiedKeyInfoWithSecret();
                });
            }
            return signKeyLiveData;
        }

        LiveData<List<EncryptRecipientChip>> getEncryptRecipientLiveData(Context context) {
            if (encryptRecipientLiveData == null) {
                encryptRecipientLiveData = new GenericLiveData<>(context, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    List<UnifiedKeyInfo> keyInfos = keyRepository.getAllUnifiedKeyInfo();
                    ArrayList<EncryptRecipientChip> result = new ArrayList<>();
                    for (UnifiedKeyInfo keyInfo : keyInfos) {
                        EncryptRecipientChip chip = EncryptRecipientChipsInput.chipFromUnifiedKeyInfo(keyInfo);
                        result.add(chip);
                    }
                    return result;
                });
            }
            return encryptRecipientLiveData;
        }
    }

    @Override
    public boolean isAsymmetric() {
        return true;
    }

    @Override
    public long getAsymmetricSigningKeyId() {
        return mSignKeySpinner.getSelectedKeyId();
    }

    @Override
    public long[] getAsymmetricEncryptionKeyIds() {
        List<Long> keyIds = new ArrayList<>();
        for (EncryptRecipientChip chip : mEncryptKeyView.getSelectedChipList()) {
            keyIds.add(chip.keyInfo.master_key_id());
        }

        long[] keyIdsArr = new long[keyIds.size()];
        Iterator<Long> iterator = keyIds.iterator();
        for (int i = 0; i < keyIds.size(); i++) {
            keyIdsArr[i] = iterator.next();
        }

        return keyIdsArr;
    }

    @Override
    public String[] getAsymmetricEncryptionUserIds() {
        List<String> userIds = new ArrayList<>();
        for (EncryptRecipientChip chip : mEncryptKeyView.getSelectedChipList()) {
            userIds.add(chip.keyInfo.user_id());
        }

        return userIds.toArray(new String[userIds.size()]);
    }

    @Override
    public Passphrase getSymmetricPassphrase() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

}
