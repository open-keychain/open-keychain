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

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class SelectSecretKeyLayoutFragment extends Fragment {

    private TextView mKeyUserId;
    private TextView mKeyUserIdRest;
    private BootstrapButton mSelectKeyButton;

    private SelectSecretKeyCallback mCallback;

    private static final int REQUEST_CODE_SELECT_KEY = 8882;

    public interface SelectSecretKeyCallback {
        void onKeySelected(long secretKeyId);
    }

    public void setCallback(SelectSecretKeyCallback callback) {
        mCallback = callback;
    }

    public void selectKey(long secretKeyId) {
        if (secretKeyId == Id.key.none) {
            mKeyUserId.setText(R.string.api_settings_no_key);
            mKeyUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                    getActivity(), secretKeyId);
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
            mKeyUserId.setText(uid);
            mKeyUserIdRest.setText(uidExtra);
        }
    }

    public void setError(String error) {
        mKeyUserId.requestFocus();
        mKeyUserId.setError(error);
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_secret_key_layout_fragment, container, false);

        mKeyUserId = (TextView) view.findViewById(R.id.select_secret_key_user_id);
        mKeyUserIdRest = (TextView) view.findViewById(R.id.select_secret_key_user_id_rest);
        mSelectKeyButton = (BootstrapButton) view
                .findViewById(R.id.select_secret_key_select_key_button);
        mSelectKeyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSelectKeyActivity();
            }
        });

        return view;
    }

    private void startSelectKeyActivity() {
        Intent intent = new Intent(getActivity(), SelectSecretKeyActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode & 0xFFFF) {
        case REQUEST_CODE_SELECT_KEY: {
            long secretKeyId;
            if (resultCode == Activity.RESULT_OK) {
                Bundle bundle = data.getExtras();
                secretKeyId = bundle.getLong(SelectSecretKeyActivity.RESULT_EXTRA_MASTER_KEY_ID);

                selectKey(secretKeyId);

                // remove displayed errors
                mKeyUserId.setError(null);

                // give value back to callback
                mCallback.onKeySelected(secretKeyId);
            }
            break;
        }

        default:
            super.onActivityResult(requestCode, resultCode, data);

            break;
        }
    }
}
