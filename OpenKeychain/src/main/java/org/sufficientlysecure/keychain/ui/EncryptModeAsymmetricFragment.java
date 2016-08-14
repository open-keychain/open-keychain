/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.tokenautocomplete.TokenCompleteTextView.TokenListener;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader.NotFoundException;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;
import org.sufficientlysecure.keychain.ui.widget.EncryptKeyCompletionView;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner.OnKeyChangedListener;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EncryptModeAsymmetricFragment extends EncryptModeFragment {

    ProviderHelper mProviderHelper;

    private KeySpinner mSignKeySpinner;
    private EncryptKeyCompletionView mEncryptKeyView;

    public static final String ARG_SINGATURE_KEY_ID = "signature_key_id";
    public static final String ARG_ENCRYPTION_KEY_IDS = "encryption_key_ids";


    /**
     * Creates new instance of this fragment
     */
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_asymmetric_fragment, container, false);

        mSignKeySpinner = (KeySpinner) view.findViewById(R.id.sign);
        mEncryptKeyView = (EncryptKeyCompletionView) view.findViewById(R.id.recipient_list);
        mEncryptKeyView.setThreshold(1); // Start working from first character

        final ViewAnimator vSignatureIcon = (ViewAnimator) view.findViewById(R.id.result_signature_icon);
        mSignKeySpinner.setOnKeyChangedListener(new OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                int child = masterKeyId != Constants.key.none ? 1 : 0;
                if (vSignatureIcon.getDisplayedChild() != child) {
                    vSignatureIcon.setDisplayedChild(child);
                }
            }
        });

        final ViewAnimator vEncryptionIcon = (ViewAnimator) view.findViewById(R.id.result_encryption_icon);
        mEncryptKeyView.setTokenListener(new TokenListener<KeyItem>() {
            @Override
            public void onTokenAdded(KeyItem o) {
                if (vEncryptionIcon.getDisplayedChild() != 1) {
                    vEncryptionIcon.setDisplayedChild(1);
                }
            }

            @Override
            public void onTokenRemoved(KeyItem o) {
                int child = mEncryptKeyView.getObjects().isEmpty() ? 0 : 1;
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
        mProviderHelper = new ProviderHelper(getActivity());

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

    /**
     * If an Intent gives a signatureMasterKeyId and/or encryptionMasterKeyIds, preselect those!
     */
    private void preselectKeys(Long signatureKeyId, long[] encryptionKeyIds) {
        if (signatureKeyId != null) {
            try {
                CachedPublicKeyRing keyring = mProviderHelper.mReader.getCachedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingUri(signatureKeyId));
                if (keyring.hasAnySecret()) {
                    mSignKeySpinner.setPreSelectedKeyId(signatureKeyId);
                }
            } catch (PgpKeyNotFoundException e) {
                Log.e(Constants.TAG, "key not found!", e);
            }
        }

        if (encryptionKeyIds != null) {
            for (long preselectedId : encryptionKeyIds) {
                try {
                    CanonicalizedPublicKeyRing ring =
                            mProviderHelper.mReader.getCanonicalizedPublicKeyRing(preselectedId);
                    mEncryptKeyView.addObject(new KeyItem(ring));
                } catch (NotFoundException e) {
                    Log.e(Constants.TAG, "key not found!", e);
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
        for (Object object : mEncryptKeyView.getObjects()) {
            if (object instanceof KeyItem) {
                keyIds.add(((KeyItem) object).mKeyId);
            }
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
        for (Object object : mEncryptKeyView.getObjects()) {
            if (object instanceof KeyItem) {
                userIds.add(((KeyItem) object).mUserIdFull);
            }
        }

        return userIds.toArray(new String[userIds.size()]);

    }

    @Override
    public Passphrase getSymmetricPassphrase() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

}
