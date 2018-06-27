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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.ChipsInput.ChipsListener;
import com.pchmn.materialchips.model.Chip;
import com.pchmn.materialchips.model.ChipInterface;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Passphrase;
import timber.log.Timber;


public class EncryptModeAsymmetricFragment extends EncryptModeFragment {
    KeyRepository mKeyRepository;

    private KeySpinner mSignKeySpinner;
    private ChipsInput mEncryptKeyView;

    public static final String ARG_SINGATURE_KEY_ID = "signature_key_id";
    public static final String ARG_ENCRYPTION_KEY_IDS = "encryption_key_ids";


    public static EncryptModeAsymmetricFragment newInstance(long signatureKey, long[] encryptionKeyIds) {
        EncryptModeAsymmetricFragment frag = new EncryptModeAsymmetricFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_SINGATURE_KEY_ID, signatureKey);
        args.putLongArray(ARG_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        frag.setArguments(args);

        return frag;
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
        mEncryptKeyView.addChipsListener(new ChipsListener() {
            @Override
            public void onChipAdded(ChipInterface chipInterface, int newSize) {
                if (vEncryptionIcon.getDisplayedChild() != 1) {
                    vEncryptionIcon.setDisplayedChild(1);
                }
            }

            @Override
            public void onChipRemoved(ChipInterface chipInterface, int newSize) {
                int child = newSize == 0 ? 0 : 1;
                if (vEncryptionIcon.getDisplayedChild() != child) {
                    vEncryptionIcon.setDisplayedChild(child);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence) {

            }

            @Override
            public void onActionDone(CharSequence charSequence) {

            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EncryptModeViewModel viewModel = ViewModelProviders.of(this).get(EncryptModeViewModel.class);
        viewModel.getSignKeyLiveData(requireContext()).observe(this, mSignKeySpinner::setData);
        viewModel.getEncryptRecipientLiveData(requireContext()).observe(this, this::onLoadEncryptRecipients);

        // preselect keys given, from state or arguments
        if (savedInstanceState == null) {
            Long signatureKeyId = getArguments().getLong(ARG_SINGATURE_KEY_ID);
            if (signatureKeyId == Constants.key.none) {
                signatureKeyId = null;
            }
            long[] encryptionKeyIds = getArguments().getLongArray(ARG_ENCRYPTION_KEY_IDS);
            preselectKeys(signatureKeyId, encryptionKeyIds);
        }
    }

    private void onLoadEncryptRecipients(List<? extends ChipInterface> keyInfoChips) {
        mEncryptKeyView.setFilterableList(keyInfoChips);
    }

    public static class EncryptModeViewModel extends ViewModel {
        private LiveData<List<UnifiedKeyInfo>> signKeyLiveData;
        private LiveData<List<Chip>> encryptRecipientLiveData;

        LiveData<List<UnifiedKeyInfo>> getSignKeyLiveData(Context context) {
            if (signKeyLiveData == null) {
                signKeyLiveData = new GenericLiveData<>(context, null, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    return keyRepository.getAllUnifiedKeyInfoWithSecret();
                });
            }
            return signKeyLiveData;
        }

        LiveData<List<Chip>> getEncryptRecipientLiveData(Context context) {
            if (encryptRecipientLiveData == null) {
                encryptRecipientLiveData = new GenericLiveData<>(context, null, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    List<UnifiedKeyInfo> keyInfos = keyRepository.getAllUnifiedKeyInfo();
                    ArrayList<Chip> result = new ArrayList<>();
                    for (UnifiedKeyInfo keyInfo : keyInfos) {
                        result.add(new Chip(keyInfo.master_key_id(), keyInfo.name(), keyInfo.email()));
                    }
                    return result;
                });
            }
            return encryptRecipientLiveData;
        }
    }

    /**
     * If an Intent gives a signatureMasterKeyId and/or encryptionMasterKeyIds, preselect those!
     */
    private void preselectKeys(Long signatureKeyId, long[] encryptionKeyIds) {
        if (signatureKeyId != null) {
            UnifiedKeyInfo unifiedKeyInfo = mKeyRepository.getUnifiedKeyInfo(signatureKeyId);
            if (unifiedKeyInfo == null) {
                String beautifyKeyId = KeyFormattingUtils.beautifyKeyId(signatureKeyId);
                Notify.create(getActivity(), getString(R.string.error_preselect_sign_key, beautifyKeyId), Style.ERROR).show();
            } else if (unifiedKeyInfo.has_any_secret()) {
                mSignKeySpinner.setPreSelectedKeyId(signatureKeyId);
            }
        }

        if (encryptionKeyIds != null) {
            for (long preselectedId : encryptionKeyIds) {
                try {
                    CanonicalizedPublicKeyRing ring =
                            mKeyRepository.getCanonicalizedPublicKeyRing(preselectedId);
                    Chip infooo = new Chip(ring.getMasterKeyId(), ring.getPrimaryUserIdWithFallback(), "infooo");
                    mEncryptKeyView.addChip(infooo);
                } catch (NotFoundException e) {
                    Timber.e(e, "key not found for encryption!");
                    Notify.create(getActivity(), getString(R.string.error_preselect_encrypt_key,
                            KeyFormattingUtils.beautifyKeyId(preselectedId)),
                            Style.ERROR).show();
                }
            }
            // This is to work-around a rendering bug in TokenCompleteTextView
            mEncryptKeyView.requestFocus();
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
        for (ChipInterface chip : mEncryptKeyView.getSelectedChipList()) {
            keyIds.add((long) chip.getId());
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
        for (ChipInterface chip : mEncryptKeyView.getSelectedChipList()) {
            userIds.add(chip.getInfo());
        }

        return userIds.toArray(new String[userIds.size()]);
    }

    @Override
    public Passphrase getSymmetricPassphrase() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

}
