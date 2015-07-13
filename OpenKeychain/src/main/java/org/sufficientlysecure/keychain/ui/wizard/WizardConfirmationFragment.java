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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
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
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * Confirmation fragment before creating the key
 */
public class WizardConfirmationFragment extends WizardFragment {
    public static final int REQUEST_EDIT_KEY = 0x00008007;

    private CheckBox mCreateKeyUpload;
    private TextView mCreateKeyEditText;
    private CryptoOperationHelper<ExportKeyringParcel, ExportResult> mUploadOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mCreateOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mMoveToCardOpHelper;

    // queued results which may trigger delayed actions
    private EditKeyResult mQueuedSaveKeyResult;
    private OperationResult mQueuedFinishResult;
    private EditKeyResult mQueuedDisplayResult;

    private WizardConfirmationFragmentViewModel mWizardConfirmationFragmentViewModel;

    public static WizardConfirmationFragment newInstance() {
        return new WizardConfirmationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWizardConfirmationFragmentViewModel = new WizardConfirmationFragmentViewModel();
        mWizardConfirmationFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView;

        View view = inflater.inflate(R.layout.wizard_confirmation_fragment_, container, false);
        textView = (TextView) view.findViewById(R.id.create_key_edit_button);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent edit = new Intent(getActivity(), EditKeyActivity.class);
                edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL,
                        mWizardConfirmationFragmentViewModel.getSaveKeyringParcel());
                startActivityForResult(edit, REQUEST_EDIT_KEY);
            }
        });

        mCreateKeyEditText = (TextView) view.findViewById(R.id.create_key_edit_text);
        mCreateKeyUpload = (CheckBox) view.findViewById(R.id.create_key_upload);
        textView = (TextView) view.findViewById(R.id.email);

        textView.setText(mWizardConfirmationFragmentViewModel.
                generateAdditionalEmails(mWizardFragmentListener.getEmail(),
                        mWizardFragmentListener.getAdditionalEmails()));

        textView = (TextView) view.findViewById(R.id.name);
        textView.setText(mWizardFragmentListener.getName());

        // If this is a debug build, don't upload by default
        if (Constants.DEBUG) {
            mCreateKeyUpload.setChecked(false);
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
                    mWizardConfirmationFragmentViewModel.setSaveKeyringParcel((SaveKeyringParcel)
                            data.getParcelableExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL));
                    mCreateKeyEditText.setText(R.string.create_key_custom);
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

        mWizardConfirmationFragmentViewModel.setEmail(mWizardFragmentListener.getEmail().toString());
        mWizardConfirmationFragmentViewModel.setName(mWizardFragmentListener.getName().toString());
        mWizardConfirmationFragmentViewModel.setAdditionalEmails(mWizardFragmentListener.getAdditionalEmails());
        mWizardConfirmationFragmentViewModel.setPassphrase(mWizardFragmentListener.getPassphrase());

        mWizardConfirmationFragmentViewModel.prepareKeyRingData();
        if (mWizardConfirmationFragmentViewModel.isUseSmartCardSettings()) {
            mCreateKeyEditText.setText(R.string.create_key_custom);
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
        if (getActivity() == null) {
            // this is a ui-triggered action, nvm if it fails while detached!
            return;
        }

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> createKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {
            @Override
            public SaveKeyringParcel createOperationInput() {
                return mWizardConfirmationFragmentViewModel.getSaveKeyringParcel();
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

        mCreateOpHelper = new CryptoOperationHelper<>(this, createKeyCallback, R.string.progress_building_key);
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
        Activity activity = getActivity();

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


        mMoveToCardOpHelper = new CryptoOperationHelper<>(this, callback, R.string.progress_modify);
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

        mUploadOpHelper = new CryptoOperationHelper<>(this, callback, R.string.progress_uploading);
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


    @Override
    public boolean onNextClicked() {
        createKey();
        return false;
    }
}
