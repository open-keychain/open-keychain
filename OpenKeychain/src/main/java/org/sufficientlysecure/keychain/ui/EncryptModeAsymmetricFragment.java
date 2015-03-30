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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tokenautocomplete.TokenCompleteTextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;
import org.sufficientlysecure.keychain.ui.widget.EncryptKeyCompletionView;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EncryptModeAsymmetricFragment extends Fragment {

    public interface IAsymmetric {

        public void onSignatureKeyIdChanged(long signatureKeyId);

        public void onEncryptionKeyIdsChanged(long[] encryptionKeyIds);

        public void onEncryptionUserIdsChanged(String[] encryptionUserIds);
    }

    ProviderHelper mProviderHelper;

    // view
    private KeySpinner mSign;
    private EncryptKeyCompletionView mEncryptKeyView;

    // model
    private IAsymmetric mEncryptInterface;

//    @Override
//    public void onNotifyUpdate() {
//        if (mSign != null) {
//            mSign.setSelectedKeyId(mEncryptInterface.getSignatureKey());
//        }
//    }

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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (IAsymmetric) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement IAsymmetric");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_asymmetric_fragment, container, false);

        mSign = (KeySpinner) view.findViewById(R.id.sign);
        mSign.setOnKeyChangedListener(new KeySpinner.OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                mEncryptInterface.onSignatureKeyIdChanged(masterKeyId);
            }
        });
        mEncryptKeyView = (EncryptKeyCompletionView) view.findViewById(R.id.recipient_list);
        mEncryptKeyView.setThreshold(1); // Start working from first character

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProviderHelper = new ProviderHelper(getActivity());

        // preselect keys given
        long signatureKeyId = getArguments().getLong(ARG_SINGATURE_KEY_ID);
        long[] encryptionKeyIds = getArguments().getLongArray(ARG_ENCRYPTION_KEY_IDS);
        preselectKeys(signatureKeyId, encryptionKeyIds);

        mEncryptKeyView.setTokenListener(new TokenCompleteTextView.TokenListener() {
            @Override
            public void onTokenAdded(Object token) {
                if (token instanceof KeyItem) {
                    updateEncryptionKeys();
                }
            }

            @Override
            public void onTokenRemoved(Object token) {
                if (token instanceof KeyItem) {
                    updateEncryptionKeys();
                }
            }
        });
    }

    /**
     * If an Intent gives a signatureMasterKeyId and/or encryptionMasterKeyIds, preselect those!
     */
    private void preselectKeys(long signatureKeyId, long[] encryptionKeyIds) {
        if (signatureKeyId != Constants.key.none) {
            try {
                CachedPublicKeyRing keyring = mProviderHelper.getCachedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingUri(signatureKeyId));
                if (keyring.hasAnySecret()) {
                    mEncryptInterface.onSignatureKeyIdChanged(keyring.getMasterKeyId());
                    mSign.setSelectedKeyId(signatureKeyId);
                }
            } catch (PgpKeyNotFoundException e) {
                Log.e(Constants.TAG, "key not found!", e);
            }
        }

        if (encryptionKeyIds != null) {
            for (long preselectedId : encryptionKeyIds) {
                try {
                    CanonicalizedPublicKeyRing ring =
                            mProviderHelper.getCanonicalizedPublicKeyRing(preselectedId);
                    mEncryptKeyView.addObject(new KeyItem(ring));
                } catch (NotFoundException e) {
                    Log.e(Constants.TAG, "key not found!", e);
                }
            }
            // This is to work-around a rendering bug in TokenCompleteTextView
            mEncryptKeyView.requestFocus();
            updateEncryptionKeys();
        }

    }

    private void updateEncryptionKeys() {
        List<Object> objects = mEncryptKeyView.getObjects();
        List<Long> keyIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        for (Object object : objects) {
            if (object instanceof KeyItem) {
                keyIds.add(((KeyItem) object).mKeyId);
                userIds.add(((KeyItem) object).mUserIdFull);
            }
        }
        long[] keyIdsArr = new long[keyIds.size()];
        Iterator<Long> iterator = keyIds.iterator();
        for (int i = 0; i < keyIds.size(); i++) {
            keyIdsArr[i] = iterator.next();
        }
        mEncryptInterface.onEncryptionKeyIdsChanged(keyIdsArr);
        mEncryptInterface.onEncryptionUserIdsChanged(userIds.toArray(new String[userIds.size()]));
    }
}
