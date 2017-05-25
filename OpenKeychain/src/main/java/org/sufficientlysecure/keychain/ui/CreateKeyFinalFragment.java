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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.bouncycastle.bcpg.sig.KeyFlags;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

public class CreateKeyFinalFragment extends Fragment {

    public static final int REQUEST_EDIT_KEY = 0x00008007;
    public static final String STATE_CUSTOM_CONFIGURATION = "state_save_keyring_parcel";

    TextView mNameEdit;
    TextView mEmailEdit;
    CheckBox mUploadCheckbox;
    View mBackButton;
    View mCreateButton;
    View mCustomKeyLayout;
    Button mCustomKeyRevertButton;

    private SaveKeyringParcel mSaveKeyringParcel;
    private boolean isCustomConfiguration;

    private CryptoOperationHelper<UploadKeyringParcel, UploadResult> mUploadOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mCreateOpHelper;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mMoveToCardOpHelper;

    // queued results which may trigger delayed actions
    private EditKeyResult mQueuedSaveKeyResult;
    private OperationResult mQueuedFinishResult;
    private EditKeyResult mQueuedDisplayResult;

    // NOTE: Do not use more complicated pattern like defined in android.util.Patterns.EMAIL_ADDRESS
    // EMAIL_ADDRESS fails for mails with umlauts for example
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\S]+@[\\S]+\\.[a-z]+$");

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
        mCustomKeyLayout = view.findViewById(R.id.custom_key_layout);
        mCustomKeyRevertButton = (Button) view.findViewById(R.id.revert_key_configuration);

        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        // set values
        if (createKeyActivity.mName != null) {
            mNameEdit.setText(createKeyActivity.mName);
        } else {
            mNameEdit.setText(getString(R.string.user_id_no_name));
        }
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

        checkEmailValidity();

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });

        mCustomKeyRevertButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                keyConfigRevertToDefault();
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

        // If this is a debug build, don't upload by default
        if (Constants.DEBUG) {
            mUploadCheckbox.setChecked(false);
        }

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        MenuItem editItem = menu.findItem(R.id.menu_create_key_edit);
        editItem.setEnabled(!createKeyActivity.mCreateSecurityToken);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.create_key_final, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_create_key_edit:
                Intent edit = new Intent(getActivity(), EditKeyActivity.class);
                edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL, mSaveKeyringParcel);
                startActivityForResult(edit, REQUEST_EDIT_KEY);
                return true;

            default:
                return super.onOptionsItemSelected(item);
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
                    SaveKeyringParcel customKeyConfiguration =
                            data.getParcelableExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL);
                    keyConfigUseCustom(customKeyConfiguration);
                }
                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void keyConfigUseCustom(SaveKeyringParcel customKeyConfiguration) {
        mSaveKeyringParcel = customKeyConfiguration;
        mCustomKeyLayout.setVisibility(View.VISIBLE);
        isCustomConfiguration = true;
    }

    public void keyConfigRevertToDefault() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mSaveKeyringParcel = createDefaultSaveKeyringParcel((CreateKeyActivity) activity);
        mCustomKeyLayout.setVisibility(View.GONE);
        isCustomConfiguration = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (isCustomConfiguration) {
            outState.putParcelable(STATE_CUSTOM_CONFIGURATION, mSaveKeyringParcel);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_CUSTOM_CONFIGURATION)) {
            keyConfigUseCustom(savedInstanceState.<SaveKeyringParcel>getParcelable(STATE_CUSTOM_CONFIGURATION));
        } else {
            keyConfigRevertToDefault();
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

    private static SaveKeyringParcel createDefaultSaveKeyringParcel(CreateKeyActivity createKeyActivity) {
        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();

        if (createKeyActivity.mCreateSecurityToken) {
            if (createKeyActivity.mSecurityTokenSign == null) {
                createKeyActivity.mSecurityTokenSign = Constants.SECURITY_TOKEN_V2_SIGN;
                createKeyActivity.mSecurityTokenDec = Constants.SECURITY_TOKEN_V2_DEC;
                createKeyActivity.mSecurityTokenAuth = Constants.SECURITY_TOKEN_V2_AUTH;
            }
            createKeyActivity.mSecurityTokenSign.addToSaveKeyringParcel(
                    builder, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
            createKeyActivity.mSecurityTokenDec.addToSaveKeyringParcel(
                    builder, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
            createKeyActivity.mSecurityTokenAuth.addToSaveKeyringParcel(builder, KeyFlags.AUTHENTICATION);

            // use empty passphrase
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(new Passphrase()));
        } else {
            Constants.addDefaultSubkeys(builder);

            if (createKeyActivity.mPassphrase != null) {
                builder.setNewUnlock(
                        ChangeUnlockParcel.createUnLockParcelForNewKey(createKeyActivity.mPassphrase));
            } else {
                builder.setNewUnlock(null);
            }
        }
        String userId = KeyRing.createUserId(
                new OpenPgpUtils.UserId(createKeyActivity.mName, createKeyActivity.mEmail, null)
        );
        builder.addUserId(userId);
        builder.setChangePrimaryUserId(userId);
        if (createKeyActivity.mAdditionalEmails != null
                && createKeyActivity.mAdditionalEmails.size() > 0) {
            for (String email : createKeyActivity.mAdditionalEmails) {
                String thisUserId = KeyRing.createUserId(
                        new OpenPgpUtils.UserId(createKeyActivity.mName, email, null)
                );
                builder.addUserId(thisUserId);
            }
        }

        return builder.build();
    }

    private void checkEmailValidity() {
        CreateKeyActivity createKeyActivity = (CreateKeyActivity) getActivity();

        boolean emailsValid = true;
        if (!EMAIL_PATTERN.matcher(createKeyActivity.mEmail).matches()) {
            emailsValid = false;
        }
        if (createKeyActivity.mAdditionalEmails != null && createKeyActivity.mAdditionalEmails.size() > 0) {
            for (Iterator<?> it = createKeyActivity.mAdditionalEmails.iterator(); it.hasNext(); ) {
                if (!EMAIL_PATTERN.matcher(it.next().toString()).matches()) {
                    emailsValid = false;
                }
            }
        }
        if (!emailsValid) {
            mEmailEdit.setError(getString(R.string.create_key_final_email_valid_warning));
            mEmailEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNameEdit.requestFocus(); // Workaround to remove focus from email
                }
            });
        }
    }

    private void createKey() {
        CreateKeyActivity activity = (CreateKeyActivity) getActivity();
        if (activity == null) {
            // this is a ui-triggered action, nvm if it fails while detached!
            return;
        }

        final boolean createSecurityToken = activity.mCreateSecurityToken;

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> createKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {
            @Override
            public SaveKeyringParcel createOperationInput() {
                return mSaveKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {

                if (createSecurityToken) {
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

        SaveKeyringParcel.Builder builder;
        CachedPublicKeyRing key = (KeyRepository.createDatabaseInteractor(getContext()))
                .getCachedPublicKeyRing(saveKeyResult.mMasterKeyId);
        try {
            builder = SaveKeyringParcel.buildChangeKeyringParcel(key.getMasterKeyId(), key.getFingerprint());
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "Key that should be moved to Security Token not found in database!");
            return;
        }

        // define subkeys that should be moved to the card
        Cursor cursor = activity.getContentResolver().query(
                KeychainContract.Keys.buildKeysUri(builder.getMasterKeyId()),
                new String[]{KeychainContract.Keys.KEY_ID,}, null, null, null
        );
        try {
            while (cursor != null && cursor.moveToNext()) {
                long subkeyId = cursor.getLong(0);
                builder.addOrReplaceSubkeyChange(SubkeyChange.createMoveToSecurityTokenChange(subkeyId));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // define new PIN and Admin PIN for the card
        builder.setSecurityTokenPin(activity.mSecurityTokenPin);
        builder.setSecurityTokenAdminPin(activity.mSecurityTokenAdminPin);

        final SaveKeyringParcel saveKeyringParcel = builder.build();

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> callback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return saveKeyringParcel;
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
        mMoveToCardOpHelper.cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
    }

    private void uploadKey(final EditKeyResult saveKeyResult) {
        Activity activity = getActivity();
        // if the activity is gone at this point, there is nothing we can do!
        if (activity == null) {
            mQueuedSaveKeyResult = saveKeyResult;
            return;
        }

        // set data uri as path to keyring
        final long masterKeyId = saveKeyResult.mMasterKeyId;
        // upload to favorite keyserver
        final ParcelableHkpKeyserver keyserver = Preferences.getPreferences(activity).getPreferredKeyserver();

        CryptoOperationHelper.Callback<UploadKeyringParcel, UploadResult> callback
                = new CryptoOperationHelper.Callback<UploadKeyringParcel, UploadResult>() {

            @Override
            public UploadKeyringParcel createOperationInput() {
                return UploadKeyringParcel.createWithKeyId(keyserver, masterKeyId);
            }

            @Override
            public void onCryptoOperationSuccess(UploadResult result) {
                handleResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(UploadResult result) {
                handleResult(result);
            }

            public void handleResult(UploadResult result) {
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
