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


import java.util.Iterator;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ExportKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

public class CreateKeyFinalFragment extends Fragment {

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
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mCreateOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mMoveToCardOpHelper;

    // queued results which may trigger delayed actions
    private EditKeyResult mQueuedSaveKeyResult;
    private OperationResult mQueuedFinishResult;
    private EditKeyResult mQueuedDisplayResult;

    public static CreateKeyFinalFragment newInstance() {
        CreateKeyFinalFragment frag = new CreateKeyFinalFragment();
        frag.setRetainInstance(true);

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

        // If this is a debug build, don't upload by default
        if (Constants.DEBUG) {
            mUploadCheckbox.setChecked(false);
        }

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCreateOpHelper != null) {
            mCreateOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        if (mMoveToCardOpHelper != null) {
            mMoveToCardOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        if (mSaveKeyringParcel == null) {
            mSaveKeyringParcel = new SaveKeyringParcel();

            if (createKeyActivity.mCreateYubiKey) {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        2048, null, KeyFlags.AUTHENTICATION, 0L));
                mEditText.setText(R.string.create_key_custom);
                mEditButton.setEnabled(false);

                // use empty passphrase
                mSaveKeyringParcel.mNewUnlock = new ChangeUnlockParcel(new Passphrase(), null);
            } else {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.SIGN_DATA, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Algorithm.RSA,
                        4096, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));

                mSaveKeyringParcel.mNewUnlock = createKeyActivity.mPassphrase != null
                        ? new ChangeUnlockParcel(createKeyActivity.mPassphrase, null)
                        : null;
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
        }

        // handle queued actions

        if (mQueuedFinishResult != null) {
            finishWithResult(mQueuedFinishResult);
            return;
        }

        if (mQueuedDisplayResult != null) {
            try {
                displayResult(mQueuedDisplayResult);
            } finally {
                // clear after operation, note that this may drop the operation if it didn't
                // work when called from here!
                mQueuedDisplayResult = null;
            }
        }

        if (mQueuedSaveKeyResult != null) {
            try {
                uploadKey(mQueuedSaveKeyResult);
            } finally {
                // see above
                mQueuedSaveKeyResult = null;
            }
        }

    }

    private void createKey() {
        CreateKeyActivity activity = (CreateKeyActivity) getActivity();
        if (activity == null) {
            // this is a ui-triggered action, nvm if it fails while detached!
            return;
        }

        final boolean createYubiKey = activity.mCreateYubiKey;

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> createKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {
            @Override
            public SaveKeyringParcel createOperationInput() {
                return mSaveKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {

                if (createYubiKey) {
                    moveToCard(result);
                    return;
                }

                if (result.mMasterKeyId != null && mUploadCheckbox.isChecked()) {
                    // result will be displayed after upload
                    uploadKey(result);
                    return;
                }

                finishWithResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                displayResult(result);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mCreateOpHelper = new CryptoOperationHelper<>(1, this, createKeyCallback, R.string.progress_building_key);
        mCreateOpHelper.cryptoOperation();
    }

    private void displayResult(EditKeyResult result) {
        Activity activity = getActivity();
        if (activity == null) {
            mQueuedDisplayResult = result;
            return;
        }
        result.createNotify(activity).show();
    }

    private void moveToCard(final EditKeyResult saveKeyResult) {
        CreateKeyActivity activity = (CreateKeyActivity) getActivity();

        final SaveKeyringParcel changeKeyringParcel;
        CachedPublicKeyRing key = (new ProviderHelper(activity))
                .getCachedPublicKeyRing(saveKeyResult.mMasterKeyId);
        try {
            changeKeyringParcel = new SaveKeyringParcel(key.getMasterKeyId(), key.getFingerprint());
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "Key that should be moved to YubiKey not found in database!");
            return;
        }

        // define subkeys that should be moved to the card
        Cursor cursor = activity.getContentResolver().query(
                KeychainContract.Keys.buildKeysUri(changeKeyringParcel.mMasterKeyId),
                new String[] { KeychainContract.Keys.KEY_ID, }, null, null, null
        );
        try {
            while (cursor != null && cursor.moveToNext()) {
                long subkeyId = cursor.getLong(0);
                changeKeyringParcel.getOrCreateSubkeyChange(subkeyId).mMoveKeyToCard = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // define new PIN and Admin PIN for the card
        changeKeyringParcel.mCardPin = activity.mYubiKeyPin;
        changeKeyringParcel.mCardAdminPin = activity.mYubiKeyAdminPin;

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> callback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return changeKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                handleResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                handleResult(result);
            }

            public void handleResult(EditKeyResult result) {
                // merge logs of createKey with moveToCard
                saveKeyResult.getLog().add(result, 0);

                if (result.mMasterKeyId != null && mUploadCheckbox.isChecked()) {
                    // result will be displayed after upload
                    uploadKey(saveKeyResult);
                    return;
                }

                finishWithResult(saveKeyResult);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };


        mMoveToCardOpHelper = new CryptoOperationHelper<>(2, this, callback, R.string.progress_modify);
        mMoveToCardOpHelper.cryptoOperation();
    }

    private void uploadKey(final EditKeyResult saveKeyResult) {
        Activity activity = getActivity();
        // if the activity is gone at this point, there is nothing we can do!
        if (activity == null) {
            mQueuedSaveKeyResult = saveKeyResult;
            return;
        }

        // set data uri as path to keyring
        final Uri blobUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(saveKeyResult.mMasterKeyId);
        // upload to favorite keyserver
        final String keyserver = Preferences.getPreferences(activity).getPreferredKeyserver();

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
                saveKeyResult.getLog().add(result, 0);
                finishWithResult(saveKeyResult);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mUploadOpHelper = new CryptoOperationHelper<>(3, this, callback, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

    public void finishWithResult(OperationResult result) {
        Activity activity = getActivity();
        if (activity == null) {
            mQueuedFinishResult = result;
            return;
        }

        Intent data = new Intent();
        data.putExtra(OperationResult.EXTRA_RESULT, result);
        activity.setResult(Activity.RESULT_OK, data);
        activity.finish();
    }

}
