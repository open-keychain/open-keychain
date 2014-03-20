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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.beardedhen.androidbootstrap.BootstrapButton;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;

public class SelectSecretKeyLayoutFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private TextView mKeyUserId;
    private TextView mKeyUserIdRest;
    private TextView mKeyMasterKeyIdHex;
    private TextView mNoKeySelected;
    private BootstrapButton mSelectKeyButton;
    private Boolean mFilterCertify;

    private Uri mReceivedUri = null;

    private SelectSecretKeyCallback mCallback;

    private static final int REQUEST_CODE_SELECT_KEY = 8882;

    //Loader ID needs to be different from the usual 0
    private static final int LOADER_ID = 2;

    //The Projection we will retrieve, Master Key ID is for convenience sake,
    //to avoid having to pass the Key Around
    final String[] PROJECTION = new String[]{KeychainContract.UserIds.USER_ID
            , KeychainContract.KeyRings.MASTER_KEY_ID};
    final int INDEX_USER_ID = 0;
    final int INDEX_MASTER_KEY_ID = 1;

    public interface SelectSecretKeyCallback {
        void onKeySelected(long secretKeyId);

    }

    public void setCallback(SelectSecretKeyCallback callback) {
        mCallback = callback;
    }

    public void setFilterCertify(Boolean filterCertify) {
        mFilterCertify = filterCertify;
    }

    public void setNoKeySelected() {
        mNoKeySelected.setVisibility(View.VISIBLE);
        mKeyUserId.setVisibility(View.GONE);
        mKeyUserIdRest.setVisibility(View.GONE);
        mKeyMasterKeyIdHex.setVisibility(View.GONE);
    }

    public void setSelectedKeyData(String userName, String email, String masterKeyHex) {

        mNoKeySelected.setVisibility(View.GONE);

        mKeyUserId.setText(userName);
        mKeyUserIdRest.setText(email);
        mKeyMasterKeyIdHex.setText(masterKeyHex);

        mKeyUserId.setVisibility(View.VISIBLE);
        mKeyUserIdRest.setVisibility(View.VISIBLE);
        mKeyMasterKeyIdHex.setVisibility(View.VISIBLE);

    }

    public void setError(String error) {
        mNoKeySelected.requestFocus();
        mNoKeySelected.setError(error);
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_secret_key_layout_fragment, container, false);

        mNoKeySelected = (TextView) view.findViewById(R.id.no_key_selected);
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

    //For AppSettingsFragment
    public void selectKey(long masterKeyId){
        Uri buildUri = KeychainContract.KeyRings.buildSecretKeyRingsByMasterKeyIdUri(String.valueOf(masterKeyId));
        mReceivedUri=buildUri;
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    private void startSelectKeyActivity() {
        Intent intent = new Intent(getActivity(), SelectSecretKeyActivity.class);
        intent.putExtra(SelectSecretKeyActivity.EXTRA_FILTER_CERTIFY, mFilterCertify);
        startActivityForResult(intent, REQUEST_CODE_SELECT_KEY);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //We don't care about the Loader id
        return new CursorLoader(getActivity(), mReceivedUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String userName, email, masterKeyHex;
            String userID = data.getString(INDEX_USER_ID);
            long masterKeyID = data.getLong(INDEX_MASTER_KEY_ID);

            String splitUserID[] = PgpKeyHelper.splitUserId(userID);

            if (splitUserID[0] != null) {
                userName = splitUserID[0];
            } else {
                userName = getActivity().getResources().getString(R.string.user_id_no_name);
            }

            if (splitUserID[1] != null) {
                email = splitUserID[1];
            } else {
                email = getActivity().getResources().getString(R.string.error_user_id_no_email);
            }

            //TODO Can the cursor return invalid values for the Master Key ?
            masterKeyHex = PgpKeyHelper.convertKeyIdToHexShort(masterKeyID);

            //Set the data
            setSelectedKeyData(userName, email, masterKeyHex);

            //Give value to the callback
            mCallback.onKeySelected(masterKeyID);
        } else {
            //Set The empty View
            setNoKeySelected();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        return;
    }

    // Select Secret Key Activity delivers the intent which was sent by it using interface to Select
    // Secret Key Fragment.Intent contains the passed Uri
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode & 0xFFFF) {
            case REQUEST_CODE_SELECT_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    mReceivedUri = data.getData();

                    //Must be restartLoader() or the data will not be updated on selecting a new key
                    getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);

                    mKeyUserId.setError(null);

                }
                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }
}
