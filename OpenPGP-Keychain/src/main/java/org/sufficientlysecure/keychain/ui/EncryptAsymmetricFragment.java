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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

import java.util.Vector;

public class EncryptAsymmetricFragment extends Fragment {
    public static final String ARG_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String ARG_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    public static final int RESULT_CODE_PUBLIC_KEYS = 0x00007001;
    public static final int RESULT_CODE_SECRET_KEYS = 0x00007002;

    OnAsymmetricKeySelection mKeySelectionListener;

    // view
    private BootstrapButton mSelectKeysButton;
    private CheckBox mSign;
    private TextView mMainUserId;
    private TextView mMainUserIdRest;

    // model
    private long mSecretKeyId = Id.key.none;
    private long mEncryptionKeyIds[] = null;

    // Container Activity must implement this interface
    public interface OnAsymmetricKeySelection {
        public void onSigningKeySelected(long signingKeyId);

        public void onEncryptionKeysSelected(long[] encryptionKeyIds);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mKeySelectionListener = (OnAsymmetricKeySelection) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAsymmetricKeySelection");
        }
    }

    private void setSignatureKeyId(long signatureKeyId) {
        mSecretKeyId = signatureKeyId;
        // update key selection in EncryptActivity
        mKeySelectionListener.onSigningKeySelected(signatureKeyId);
        updateView();
    }

    private void setEncryptionKeyIds(long[] encryptionKeyIds) {
        mEncryptionKeyIds = encryptionKeyIds;
        // update key selection in EncryptActivity
        mKeySelectionListener.onEncryptionKeysSelected(encryptionKeyIds);
        updateView();
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_asymmetric_fragment, container, false);

        mSelectKeysButton = (BootstrapButton) view.findViewById(R.id.btn_selectEncryptKeys);
        mSign = (CheckBox) view.findViewById(R.id.sign);
        mMainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mMainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mSelectKeysButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectPublicKeys();
            }
        });
        mSign.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    selectSecretKey();
                } else {
                    setSignatureKeyId(Id.key.none);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long signatureKeyId = getArguments().getLong(ARG_SIGNATURE_KEY_ID);
        long[] encryptionKeyIds = getArguments().getLongArray(ARG_ENCRYPTION_KEY_IDS);

        // preselect keys given by arguments (given by Intent to EncryptActivity)
        preselectKeys(signatureKeyId, encryptionKeyIds);
    }

    /**
     * If an Intent gives a signatureKeyId and/or encryptionKeyIds, preselect those!
     *
     * @param preselectedSignatureKeyId
     * @param preselectedEncryptionKeyIds
     */
    private void preselectKeys(long preselectedSignatureKeyId, long[] preselectedEncryptionKeyIds) {
        if (preselectedSignatureKeyId != 0) {
            // TODO: don't use bouncy castle objects!
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(getActivity(),
                    preselectedSignatureKeyId);
            PGPSecretKey masterKey;
            if (keyRing != null) {
                masterKey = PgpKeyHelper.getMasterKey(keyRing);
                if (masterKey != null) {
                    Vector<PGPSecretKey> signKeys = PgpKeyHelper.getUsableSigningKeys(keyRing);
                    if (signKeys.size() > 0) {
                        setSignatureKeyId(masterKey.getKeyID());
                    }
                }
            }
        }

        if (preselectedEncryptionKeyIds != null) {
            Vector<Long> goodIds = new Vector<Long>();
            for (int i = 0; i < preselectedEncryptionKeyIds.length; ++i) {
                // TODO: don't use bouncy castle objects!

                PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(getActivity(),
                        preselectedEncryptionKeyIds[i]);
                PGPPublicKey masterKey;
                if (keyRing == null) {
                    continue;
                }
                masterKey = PgpKeyHelper.getMasterKey(keyRing);
                if (masterKey == null) {
                    continue;
                }
                Vector<PGPPublicKey> encryptKeys = PgpKeyHelper.getUsableEncryptKeys(keyRing);
                if (encryptKeys.size() == 0) {
                    continue;
                }
                goodIds.add(masterKey.getKeyID());
            }
            if (goodIds.size() > 0) {
                long[] keyIds = new long[goodIds.size()];
                for (int i = 0; i < goodIds.size(); ++i) {
                    keyIds[i] = goodIds.get(i);
                }
                setEncryptionKeyIds(keyIds);
            }
        }
    }

    private void updateView() {
        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(getString(R.string.select_keys_button_default));
        } else {
            mSelectKeysButton.setText(getResources().getQuantityString(
                    R.plurals.select_keys_button, mEncryptionKeyIds.length,
                    mEncryptionKeyIds.length));
        }

        if (mSecretKeyId == Id.key.none) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.user_id_no_name);
            String uidExtra = "";
            // TODO: don't use bouncy castle objects!
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(getActivity(),
                    mSecretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpKeyHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PgpKeyHelper.getMainUserIdSafe(getActivity(), key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mMainUserId.setText(uid);
            mMainUserIdRest.setText(uidExtra);
            mSign.setChecked(true);
        }
    }

    private void selectPublicKeys() {
        Intent intent = new Intent(getActivity(), SelectPublicKeyActivity.class);
        Vector<Long> keyIds = new Vector<Long>();
        if (mSecretKeyId != 0) {
            keyIds.add(mSecretKeyId);
        }
        if (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) {
            for (int i = 0; i < mEncryptionKeyIds.length; ++i) {
                keyIds.add(mEncryptionKeyIds[i]);
            }
        }
        long[] initialKeyIds = null;
        if (keyIds.size() > 0) {
            initialKeyIds = new long[keyIds.size()];
            for (int i = 0; i < keyIds.size(); ++i) {
                initialKeyIds[i] = keyIds.get(i);
            }
        }
        intent.putExtra(SelectPublicKeyActivity.EXTRA_SELECTED_MASTER_KEY_IDS, initialKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(getActivity(), SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_CODE_PUBLIC_KEYS: {
                if (resultCode == Activity.RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    setEncryptionKeyIds(bundle
                            .getLongArray(SelectPublicKeyActivity.RESULT_EXTRA_MASTER_KEY_IDS));
                }
                break;
            }

            case RESULT_CODE_SECRET_KEYS: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri uriMasterKey = data.getData();
                    setSignatureKeyId(Long.valueOf(uriMasterKey.getLastPathSegment()));
                } else {
                    setSignatureKeyId(Id.key.none);
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

}
