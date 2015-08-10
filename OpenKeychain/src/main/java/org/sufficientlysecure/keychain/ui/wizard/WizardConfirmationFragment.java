/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.wizard;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import org.sufficientlysecure.keychain.ui.EditKeyActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Confirmation fragment before creating the key
 */
public class WizardConfirmationFragment extends WizardFragment {
    public static final int REQUEST_EDIT_KEY = 0x00008007;
    private CheckBox mCreateKeyUpload;
    private TextView mCreateKeyEditText;
    private TextView mEmailsText;
    private TextView mNameText;
    private TextView mEditButton;
    private SaveKeyringParcel mSaveKeyringParcel;
    private String mName;
    private String mEmail;
    private ArrayList<String> mAdditionalEmails;
    private Passphrase mPassphrase;
    private CryptoOperationHelper<ExportKeyringParcel, ExportResult> mUploadOpHelper;
    private CryptoOperationHelper mCreateOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mMoveToCardOpHelper;

    // queued results which may trigger delayed actions
    private EditKeyResult mQueuedSaveKeyResult;
    private OperationResult mQueuedFinishResult;
    private EditKeyResult mQueuedDisplayResult;

    public static WizardConfirmationFragment newInstance() {
        return new WizardConfirmationFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_confirmation_fragment_, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditButton = (TextView) view.findViewById(R.id.create_key_edit_button);
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateKeyClicked();
            }
        });

        mCreateKeyEditText = (TextView) view.findViewById(R.id.create_key_edit_text);
        mCreateKeyUpload = (CheckBox) view.findViewById(R.id.create_key_upload);
        mEmailsText = (TextView) view.findViewById(R.id.email);
        mNameText = (TextView) view.findViewById(R.id.name);

        mEmailsText.setText(generateAdditionalEmails(mWizardFragmentListener.getEmail(),
                mWizardFragmentListener.getAdditionalEmails()));

        mNameText.setText(mWizardFragmentListener.getName());

        if (Constants.DEBUG) {
            mCreateKeyUpload.setChecked(false);
        }
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
                    mCreateKeyEditText.setText(getActivity().getString(R.string.create_key_custom));
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

        mEmail = mWizardFragmentListener.getEmail().toString();
        mName = mWizardFragmentListener.getName().toString();
        mAdditionalEmails = mWizardFragmentListener.getAdditionalEmails();
        mPassphrase = mWizardFragmentListener.getPassphrase();

        prepareKeyRingData();

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

    @Override
    public boolean onNextClicked() {
        createKey();
        return false;
    }

    public void prepareKeyRingData() {
        if (mSaveKeyringParcel == null) {
            mSaveKeyringParcel = new SaveKeyringParcel();
            if (mWizardFragmentListener.createYubiKey()) {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.AUTHENTICATION, 0L));
                mCreateKeyEditText.setText(R.string.create_key_custom);
                mEditButton.setEnabled(false);

                // use empty passphrase
                mSaveKeyringParcel.mNewUnlock = new SaveKeyringParcel.ChangeUnlockParcel(new Passphrase(), null);
            } else {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.SIGN_DATA, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));

                mSaveKeyringParcel.mNewUnlock = mPassphrase != null
                        ? new SaveKeyringParcel.ChangeUnlockParcel(mPassphrase)
                        : null;
            }
            String userId = KeyRing.createUserId(
                    new KeyRing.UserId(mName, mEmail, null)
            );
            mSaveKeyringParcel.mAddUserIds.add(userId);
            mSaveKeyringParcel.mChangePrimaryUserId = userId;
            if (mAdditionalEmails != null && mAdditionalEmails.size() > 0) {
                for (String email : mAdditionalEmails) {
                    String thisUserId = KeyRing.createUserId(new KeyRing.UserId(mName, email, null)
                    );
                    mSaveKeyringParcel.mAddUserIds.add(thisUserId);
                }
            }
        }
    }

    /**
     * Generates a string of the user's emails.
     *
     * @param mainEmail
     * @param additionalEmails
     * @return
     */
    CharSequence generateAdditionalEmails(CharSequence mainEmail, ArrayList<String> additionalEmails) {
        if (additionalEmails == null) {
            return mainEmail;
        }

        StringBuffer emails = new StringBuffer();
        emails.append(mainEmail);
        emails.append(", ");
        Iterator<?> it = additionalEmails.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            emails.append(next);
            if (it.hasNext()) {
                emails.append(", ");
            }
        }
        return emails;
    }

    private void createKey() {
        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> createKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {
            @Override
            public SaveKeyringParcel createOperationInput() {
                return mSaveKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                if (mWizardFragmentListener.createYubiKey()) {
                    moveToCard(result);
                    return;
                }

                if (result.mMasterKeyId != null && mCreateKeyUpload.isChecked()) {
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

        mCreateOpHelper = new CryptoOperationHelper<>(1, getActivity(), createKeyCallback, R.string.progress_building_key);
        mCreateOpHelper.cryptoOperation();
    }

    private void displayResult(EditKeyResult result) {
        final Activity activity = getActivity();
        if (activity == null) {
            mQueuedDisplayResult = result;
            return;
        }
        result.createNotify(activity).show();
    }

    private void moveToCard(final EditKeyResult saveKeyResult) {
        final SaveKeyringParcel changeKeyringParcel;
        final Activity activity = getActivity();
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
                new String[]{KeychainContract.Keys.KEY_ID,}, null, null, null
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
        changeKeyringParcel.mCardPin = mWizardFragmentListener.getYubiKeyPin();
        changeKeyringParcel.mCardAdminPin = mWizardFragmentListener.getYubiKeyAdminPin();

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

                if (result.mMasterKeyId != null && mCreateKeyUpload.isChecked()) {
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


        mMoveToCardOpHelper = new CryptoOperationHelper<>(2, getActivity(), callback, R.string.progress_modify);
        mMoveToCardOpHelper.cryptoOperation();
    }

    private void uploadKey(final EditKeyResult saveKeyResult) {
        // if the activity is gone at this point, there is nothing we can do!
        final Activity activity = getActivity();
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

        mUploadOpHelper = new CryptoOperationHelper<>(3, getActivity(), callback, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

    public void finishWithResult(OperationResult result) {
        final Activity activity = getActivity();
        if (activity == null) {
            mQueuedFinishResult = result;
            return;
        }

        Intent data = new Intent();
        data.putExtra(OperationResult.EXTRA_RESULT, result);
        activity.setResult(Activity.RESULT_OK, data);
        activity.finish();
    }

    public void onCreateKeyClicked() {
        Intent edit = new Intent(getActivity(), EditKeyActivity.class);
        edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
        startActivityForResult(edit, REQUEST_EDIT_KEY);
    }
}
