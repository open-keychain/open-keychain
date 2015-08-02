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
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.Iterator;

public class WizardConfirmationFragmentViewModel implements BaseViewModel {
    public static final int REQUEST_EDIT_KEY = 0x00008007;
    private Activity mActivity;
    private SaveKeyringParcel mSaveKeyringParcel;
    private boolean mUseSmartCardSettings = false;
    private String mName;
    private String mEmail;
    private ArrayList<String> mAdditionalEmails;
    private Passphrase mPassphrase;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;
    private CryptoOperationHelper<ExportKeyringParcel, ExportResult> mUploadOpHelper;
    private CryptoOperationHelper mCreateOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mMoveToCardOpHelper;

    // queued results which may trigger delayed actions
    private EditKeyResult mQueuedSaveKeyResult;
    private OperationResult mQueuedFinishResult;
    private EditKeyResult mQueuedDisplayResult;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void setEmails(CharSequence emails);

        void setName(CharSequence name);

        void setCreateKeyEditText(CharSequence text);

        boolean isUploadOptionChecked();

        void setUploadOption(boolean checked);
    }

    public WizardConfirmationFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
                                               WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = onViewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
        mOnViewModelEventBind.setEmails(generateAdditionalEmails(mWizardFragmentListener.getEmail(),
                mWizardFragmentListener.getAdditionalEmails()));

        mOnViewModelEventBind.setName(mWizardFragmentListener.getName());

        if(Constants.DEBUG) {
            mOnViewModelEventBind.setUploadOption(true);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    public void prepareKeyRingData() {
        if (mSaveKeyringParcel == null) {
            mSaveKeyringParcel = new SaveKeyringParcel();
            if (mUseSmartCardSettings) {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.AUTHENTICATION, 0L));
            } else {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.SIGN_DATA, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
            }
            String userId = KeyRing.createUserId(
                    new KeyRing.UserId(mName, mEmail, null)
            );
            mSaveKeyringParcel.mAddUserIds.add(userId);
            mSaveKeyringParcel.mChangePrimaryUserId = userId;
            if (mAdditionalEmails != null && mAdditionalEmails.size() > 0) {
                for (String email : mAdditionalEmails) {
                    String thisUserId = KeyRing.createUserId(
                            new KeyRing.UserId(mName, email, null)
                    );
                    mSaveKeyringParcel.mAddUserIds.add(thisUserId);
                }
            }
            mSaveKeyringParcel.mNewUnlock = new SaveKeyringParcel.ChangeUnlockParcel(mPassphrase);
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
                    mSaveKeyringParcel = ((SaveKeyringParcel)
                            data.getParcelableExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL));
                    mOnViewModelEventBind.setCreateKeyEditText(mActivity.
                            getString(R.string.create_key_custom));
                }
            }
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        mEmail = mWizardFragmentListener.getEmail().toString();
        mName = mWizardFragmentListener.getName().toString();
        mAdditionalEmails = mWizardFragmentListener.getAdditionalEmails();
        mPassphrase = mWizardFragmentListener.getPassphrase();

        prepareKeyRingData();
        if (mUseSmartCardSettings) {
            mOnViewModelEventBind.setCreateKeyEditText(mActivity.getString(R.string.create_key_custom));
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

                if (result.mMasterKeyId != null && mOnViewModelEventBind.isUploadOptionChecked()) {
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

        mCreateOpHelper = new CryptoOperationHelper<>(1, (WizardFragment) mOnViewModelEventBind,
                createKeyCallback, R.string.progress_building_key);
        mCreateOpHelper.cryptoOperation();
    }

    private void displayResult(EditKeyResult result) {
        if (mActivity == null) {
            mQueuedDisplayResult = result;
            return;
        }
        result.createNotify(mActivity).show();
    }

    private void moveToCard(final EditKeyResult saveKeyResult) {
        final SaveKeyringParcel changeKeyringParcel;
        CachedPublicKeyRing key = (new ProviderHelper(mActivity))
                .getCachedPublicKeyRing(saveKeyResult.mMasterKeyId);
        try {
            changeKeyringParcel = new SaveKeyringParcel(key.getMasterKeyId(), key.getFingerprint());
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "Key that should be moved to YubiKey not found in database!");
            return;
        }

        // define subkeys that should be moved to the card
        Cursor cursor = mActivity.getContentResolver().query(
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

                if (result.mMasterKeyId != null && mOnViewModelEventBind.isUploadOptionChecked()) {
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


        mMoveToCardOpHelper = new CryptoOperationHelper<>(2, (WizardFragment) mOnViewModelEventBind,
                callback, R.string.progress_modify);
        mMoveToCardOpHelper.cryptoOperation();
    }

    private void uploadKey(final EditKeyResult saveKeyResult) {
        // if the activity is gone at this point, there is nothing we can do!
        if (mActivity == null) {
            mQueuedSaveKeyResult = saveKeyResult;
            return;
        }

        // set data uri as path to keyring
        final Uri blobUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(saveKeyResult.mMasterKeyId);
        // upload to favorite keyserver
        final String keyserver = Preferences.getPreferences(mActivity).getPreferredKeyserver();

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

        mUploadOpHelper = new CryptoOperationHelper<>(3, (WizardFragment) mOnViewModelEventBind,
                callback, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

    public void finishWithResult(OperationResult result) {
        if (mActivity == null) {
            mQueuedFinishResult = result;
            return;
        }

        Intent data = new Intent();
        data.putExtra(OperationResult.EXTRA_RESULT, result);
        mActivity.setResult(Activity.RESULT_OK, data);
        mActivity.finish();
    }


    public void onCreateKeyClicked() {
        Intent edit = new Intent(mActivity, EditKeyActivity.class);
        edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
        mActivity.startActivityForResult(edit, REQUEST_EDIT_KEY);
    }

    public boolean onNextClicked() {
        createKey();
        return false;
    }
}
