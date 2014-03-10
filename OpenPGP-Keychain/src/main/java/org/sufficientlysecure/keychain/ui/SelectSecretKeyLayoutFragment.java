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

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class SelectSecretKeyLayoutFragment extends Fragment {

    private TextView mKeyUserId;
    private TextView mKeyUserIdRest;
    private TextView mKeyMasterKeyIdHex;
    private BootstrapButton mSelectKeyButton;
    private Boolean mFilterCertify;

    private SelectSecretKeyCallback mCallback;

    private static final int REQUEST_CODE_SELECT_KEY = 8882;

    public interface SelectSecretKeyCallback {
        void onKeySelected(long secretKeyId);
    }

    public void setCallback(SelectSecretKeyCallback callback) {
        mCallback = callback;
    }

    public void setFilterCertify(Boolean filterCertify) {
        mFilterCertify = filterCertify;
    }

    public void selectKey(long secretKeyId) {
        if (secretKeyId == Id.key.none) {
            mKeyMasterKeyIdHex.setText(R.string.api_settings_no_key);
            mKeyUserIdRest.setText("");
            mKeyUserId.setVisibility(View.GONE);
            mKeyUserIdRest.setVisibility(View.GONE);

        } else {
            String uid = getResources().getString(R.string.user_id_no_name);
            String uidExtra = "";
            String masterkeyIdHex = "";

            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                    getActivity(), secretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpKeyHelper.getMasterKey(keyRing);
                masterkeyIdHex = PgpKeyHelper.convertKeyIdToHex(secretKeyId);

                if (key != null) {
                    String userId = PgpKeyHelper.getMainUserIdSafe(getActivity(), key);
                    /*String chunks[] = mUserId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }*/
                    String[] userIdSplit = PgpKeyHelper.splitUserId(userId);
                    String  userName, userEmail;

                    if (userIdSplit[0] != null) {   userName = userIdSplit[0];  }
                    else {  userName = getActivity().getResources().getString(R.string.user_id_no_name);   }

                    if (userIdSplit[1] != null) {   userEmail = userIdSplit[1];    }
                    else {    userEmail = getActivity().getResources().getString(R.string.error_user_id_no_email);    }

                    mKeyMasterKeyIdHex.setText(masterkeyIdHex);
                    mKeyUserId.setText(userName);
                    mKeyUserIdRest.setText(userEmail);
                    mKeyUserId.setVisibility(View.VISIBLE);
                    mKeyUserIdRest.setVisibility(View.VISIBLE);
                }
                else{
                    mKeyMasterKeyIdHex.setText(getActivity().getResources().getString(R.string.no_key));
                    mKeyUserId.setVisibility(View.GONE);
                    mKeyUserIdRest.setVisibility(View.GONE);

                }

            }
            else{
                    mKeyMasterKeyIdHex.setText(getActivity().getResources().getString(R.string.no_keys_added_or_updated)+" for master id: "+secretKeyId);
                    mKeyUserId.setVisibility(View.GONE);
                    mKeyUserIdRest.setVisibility(View.GONE);
            }

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
        mKeyMasterKeyIdHex = (TextView) view.findViewById(R.id.select_secret_key_master_key_hex);
        mSelectKeyButton = (BootstrapButton) view
                .findViewById(R.id.select_secret_key_select_key_button);
        mFilterCertify = false;
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
        intent.putExtra(SelectSecretKeyActivity.EXTRA_FILTER_CERTIFY, mFilterCertify);
        startActivityForResult(intent, REQUEST_CODE_SELECT_KEY);
    }

    //Select Secret Key Activity delivers the intent which was sent by it using interface to Select
    // Secret Key Fragment.Intent contains Master Key Id, User Email, User Name, Master Key Id Hex.
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
