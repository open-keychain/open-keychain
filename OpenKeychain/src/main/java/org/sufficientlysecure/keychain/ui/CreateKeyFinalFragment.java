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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.ExportKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.Iterator;

public class CreateKeyFinalFragment
        extends CryptoOperationFragment<SaveKeyringParcel, EditKeyResult> {

    public static final int REQUEST_EDIT_KEY = 0x00008007;

    TextView mNameEdit;
    TextView mEmailEdit;
    CheckBox mUploadCheckbox;
    View mBackButton;
    View mCreateButton;
    TextView mEditText;
    View mEditButton;

    SaveKeyringParcel mSaveKeyringParcel;

    private CryptoOperationHelper<ExportKeyringParcel, ExportResult> mUploadOpHelper;

    public static CreateKeyFinalFragment newInstance() {
        CreateKeyFinalFragment frag = new CreateKeyFinalFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_final_fragment, container, false);

        mNameEdit = (TextView) view.findViewById(R.id.name);
        mEmailEdit = (TextView) view.findViewById(R.id.email);
        mUploadCheckbox = (CheckBox) view.findViewById(R.id.create_key_upload);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mCreateButton = view.findViewById(R.id.create_key_next_button);
        mEditText = (TextView) view.findViewById(R.id.create_key_edit_text);
        mEditButton = view.findViewById(R.id.create_key_edit_button);

        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        // set values
        mNameEdit.setText(createKeyActivity.mName);
        if (createKeyActivity.mAdditionalEmails != null && createKeyActivity.mAdditionalEmails.size() > 0) {
            String emailText = createKeyActivity.mEmail + ", ";
            Iterator<?> it = createKeyActivity.mAdditionalEmails.iterator();
            while (it.hasNext()) {
                Object next = it.next();
                emailText += next;
                if (it.hasNext()) {
                    emailText += ", ";
                }
            }
            mEmailEdit.setText(emailText);
        } else {
            mEmailEdit.setText(createKeyActivity.mEmail);
        }

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();
                if (createKeyActivity != null) {
                    createKeyActivity.loadFragment(null, FragAction.TO_LEFT);
                }
            }
        });

        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent edit = new Intent(getActivity(), EditKeyActivity.class);
                edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
                startActivityForResult(edit, REQUEST_EDIT_KEY);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mUploadOpHelper != null) {
            mUploadOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        switch (requestCode) {
            case REQUEST_EDIT_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    mSaveKeyringParcel = data.getParcelableExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL);
                    mEditText.setText(R.string.create_key_custom);
                }
                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected SaveKeyringParcel createOperationInput() {
        return mSaveKeyringParcel;
    }

    @Override
    protected void onCryptoOperationSuccess(EditKeyResult result) {
        if (result.mMasterKeyId != null && mUploadCheckbox.isChecked()) {
            // result will be displayed after upload
            uploadKey(result);
        } else {
            Intent data = new Intent();
            data.putExtra(OperationResult.EXTRA_RESULT, result);
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        }
    }

    @Override
    protected void onCryptoOperationResult(EditKeyResult result) {
        // do something else?
        super.onCryptoOperationResult(result);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        if (mSaveKeyringParcel == null) {
            mSaveKeyringParcel = new SaveKeyringParcel();
            if (createKeyActivity.mUseSmartCardSettings) {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.AUTHENTICATION, 0L));
                mEditText.setText(R.string.create_key_custom);
            } else {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.SIGN_DATA, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
            }
            String userId = KeyRing.createUserId(
                    new KeyRing.UserId(createKeyActivity.mName, createKeyActivity.mEmail, null)
            );
            mSaveKeyringParcel.mAddUserIds.add(userId);
            mSaveKeyringParcel.mChangePrimaryUserId = userId;
            if (createKeyActivity.mAdditionalEmails != null
                    && createKeyActivity.mAdditionalEmails.size() > 0) {
                for (String email : createKeyActivity.mAdditionalEmails) {
                    String thisUserId = KeyRing.createUserId(
                            new KeyRing.UserId(createKeyActivity.mName, email, null)
                    );
                    mSaveKeyringParcel.mAddUserIds.add(thisUserId);
                }
            }
            mSaveKeyringParcel.mNewUnlock = createKeyActivity.mPassphrase != null
                    ? new ChangeUnlockParcel(createKeyActivity.mPassphrase, null)
                    : null;
        }
    }


    private void createKey() {
        super.setProgressMessageResource(R.string.progress_building_key);
        super.cryptoOperation();
    }

    // TODO move into EditKeyOperation
    private void uploadKey(final EditKeyResult saveKeyResult) {

        // set data uri as path to keyring
        final Uri blobUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(
                saveKeyResult.mMasterKeyId);
        // upload to favorite keyserver
        final String keyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();

        CryptoOperationHelper.Callback<ExportKeyringParcel, ExportResult> callback
                = new CryptoOperationHelper.Callback<ExportKeyringParcel, ExportResult>() {

            @Override
            public ExportKeyringParcel createOperationInput() {
                return new ExportKeyringParcel(keyserver, blobUri);
            }

            @Override
            public void onCryptoOperationSuccess(ExportResult result) {
                handleResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(ExportResult result) {
                handleResult(result);
            }

            public void handleResult(ExportResult result) {
                // TODO: upload operation needs a result! "result" is not currenlty used
                // TODO: then combine these results (saveKeyResult and update op result)
                //if (result.getResult() == OperationResultParcel.RESULT_OK) {
                //Notify.create(getActivity(), R.string.key_send_success,
                //Notify.Style.OK).show();

                Intent data = new Intent();
                data.putExtra(OperationResult.EXTRA_RESULT, saveKeyResult);
                getActivity().setResult(Activity.RESULT_OK, data);
                getActivity().finish();
            }
        };


        mUploadOpHelper = new CryptoOperationHelper<>(this, callback, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

}
