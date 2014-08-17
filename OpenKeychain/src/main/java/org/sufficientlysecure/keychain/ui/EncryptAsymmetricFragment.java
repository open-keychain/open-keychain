/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.widget.EncryptKeyCompletionView;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EncryptAsymmetricFragment extends Fragment implements EncryptActivityInterface.UpdateListener {
    ProviderHelper mProviderHelper;

    // view
    private KeySpinner mSign;
    private EncryptKeyCompletionView mEncryptKeyView;

    // model
    private EncryptActivityInterface mEncryptInterface;

    @Override
    public void onNotifyUpdate() {
        if (mSign != null) {
            mSign.setSelectedKeyId(mEncryptInterface.getSignatureKey());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    private void setSignatureKeyId(long signatureKeyId) {
        mEncryptInterface.setSignatureKey(signatureKeyId);
    }

    private void setEncryptionKeyIds(long[] encryptionKeyIds) {
        mEncryptInterface.setEncryptionKeys(encryptionKeyIds);
    }

    private void setEncryptionUserIds(String[] encryptionUserIds) {
        mEncryptInterface.setEncryptionUsers(encryptionUserIds);
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
                setSignatureKeyId(masterKeyId);
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
        preselectKeys();

        mEncryptKeyView.setTokenListener(new TokenCompleteTextView.TokenListener() {
            @Override
            public void onTokenAdded(Object token) {
                if (token instanceof EncryptKeyCompletionView.EncryptionKey) {
                    updateEncryptionKeys();
                }
            }

            @Override
            public void onTokenRemoved(Object token) {
                if (token instanceof EncryptKeyCompletionView.EncryptionKey) {
                    updateEncryptionKeys();
                }
            }
        });
    }

    /**
     * If an Intent gives a signatureMasterKeyId and/or encryptionMasterKeyIds, preselect those!
     */
    private void preselectKeys() {
        // TODO all of this works under the assumption that the first suitable subkey is always used!
        // not sure if we need to distinguish between different subkeys here?
        long signatureKey = mEncryptInterface.getSignatureKey();
        if (signatureKey != Constants.key.none) {
            try {
                CachedPublicKeyRing keyring = mProviderHelper.getCachedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingUri(signatureKey));
                if(keyring.hasAnySecret()) {
                    setSignatureKeyId(keyring.getMasterKeyId());
                    mSign.setSelectedKeyId(mEncryptInterface.getSignatureKey());
                }
            } catch (PgpGeneralException e) {
                Log.e(Constants.TAG, "key not found!", e);
            }
        }

        long[] encryptionKeyIds = mEncryptInterface.getEncryptionKeys();
        if (encryptionKeyIds != null) {
            for (long preselectedId : encryptionKeyIds) {
                try {
                    CachedPublicKeyRing ring = mProviderHelper.getCachedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(preselectedId));
                    mEncryptKeyView.addObject(mEncryptKeyView.new EncryptionKey(ring));
                } catch (PgpGeneralException e) {
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
        List<Long> keyIds = new ArrayList<Long>();
        List<String> userIds = new ArrayList<String>();
        for (Object object : objects) {
            if (object instanceof EncryptKeyCompletionView.EncryptionKey) {
                keyIds.add(((EncryptKeyCompletionView.EncryptionKey) object).getKeyId());
                userIds.add(((EncryptKeyCompletionView.EncryptionKey) object).getUserId());
            }
        }
        long[] keyIdsArr = new long[keyIds.size()];
        Iterator<Long> iterator = keyIds.iterator();
        for (int i = 0; i < keyIds.size(); i++) {
            keyIdsArr[i] = iterator.next();
        }
        setEncryptionKeyIds(keyIdsArr);
        setEncryptionUserIds(userIds.toArray(new String[userIds.size()]));
    }
}
