/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.Editor;
import org.sufficientlysecure.keychain.ui.widget.KeyEditor;
import org.sufficientlysecure.keychain.ui.widget.SectionView;
import org.sufficientlysecure.keychain.ui.widget.UserIdEditor;
import org.sufficientlysecure.keychain.ui.widget.Editor.EditorListener;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class EditKeyActivity extends ActionBarActivity implements EditorListener {

    // Actions for internal use only:
    public static final String ACTION_CREATE_KEY = Constants.INTENT_PREFIX + "CREATE_KEY";
    public static final String ACTION_EDIT_KEY = Constants.INTENT_PREFIX + "EDIT_KEY";

    // possible extra keys
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_NO_PASSPHRASE = "no_passphrase";
    public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generate_default_keys";

    // results when saving key
    public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String RESULT_EXTRA_USER_ID = "user_id";

    // EDIT
    private Uri mDataUri;

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIdsView;
    private SectionView mKeysView;

    private String mCurrentPassPhrase = null;
    private String mNewPassPhrase = null;
    private String mSavedNewPassPhrase = null;
    private boolean mIsPassPhraseSet;
    private boolean mNeedsSaving;
    private boolean mIsBrandNewKeyring = false;
    private MenuItem mSaveButton;

    private BootstrapButton mChangePassPhrase;

    private CheckBox mNoPassphrase;

    Vector<String> mUserIds;
    Vector<PGPSecretKey> mKeys;
    Vector<Integer> mKeysUsages;
    boolean masterCanSign = true;

    ExportHelper mExportHelper;

    public boolean needsSaving()
    {
        mNeedsSaving = (mUserIdsView == null) ? false : mUserIdsView.needsSaving();
        mNeedsSaving |= (mKeysView == null) ? false : mKeysView.needsSaving();
        mNeedsSaving |= hasPassphraseChanged();
        mNeedsSaving |= mIsBrandNewKeyring;
        return mNeedsSaving;
    }


    public void somethingChanged()
    {
        ActivityCompat.invalidateOptionsMenu(this);
        //Toast.makeText(this, "Needs saving: " + Boolean.toString(mNeedsSaving) + "(" + Boolean.toString(mUserIdsView.needsSaving()) + ", " + Boolean.toString(mKeysView.needsSaving()) + ")", Toast.LENGTH_LONG).show();
    }

    public void onDeleted(Editor e, boolean wasNewItem)
    {
        somethingChanged();
    }

    public void onEdited()
    {
        somethingChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(android.R.color.transparent);
        getSupportActionBar().setHomeButtonEnabled(true);

        mUserIds = new Vector<String>();
        mKeys = new Vector<PGPSecretKey>();
        mKeysUsages = new Vector<Integer>();

        // Catch Intents opened from other apps
        Intent intent = getIntent();
        String action = intent.getAction();
        if (ACTION_CREATE_KEY.equals(action)) {
            handleActionCreateKey(intent);
        } else if (ACTION_EDIT_KEY.equals(action)) {
            handleActionEditKey(intent);
        }
    }

    /**
     * Handle intent action to create new key
     * 
     * @param intent
     */
    private void handleActionCreateKey(Intent intent) {
        Bundle extras = intent.getExtras();

        mCurrentPassPhrase = "";
        mIsBrandNewKeyring = true;

        if (extras != null) {
            // if userId is given, prefill the fields
            if (extras.containsKey(EXTRA_USER_IDS)) {
                Log.d(Constants.TAG, "UserIds are given!");
                mUserIds.add(extras.getString(EXTRA_USER_IDS));
            }

            // if no passphrase is given
            if (extras.containsKey(EXTRA_NO_PASSPHRASE)) {
                boolean noPassphrase = extras.getBoolean(EXTRA_NO_PASSPHRASE);
                if (noPassphrase) {
                    // check "no passphrase" checkbox and remove button
                    mNoPassphrase.setChecked(true);
                    mChangePassPhrase.setVisibility(View.GONE);
                }
            }

            // generate key
            if (extras.containsKey(EXTRA_GENERATE_DEFAULT_KEYS)) {
                boolean generateDefaultKeys = extras.getBoolean(EXTRA_GENERATE_DEFAULT_KEYS);
                if (generateDefaultKeys) {

                    // Send all information needed to service generate keys in other thread
                    Intent serviceIntent = new Intent(this, KeychainIntentService.class);
                    serviceIntent.setAction(KeychainIntentService.ACTION_GENERATE_DEFAULT_RSA_KEYS);

                    // fill values for this action
                    Bundle data = new Bundle();
                    data.putString(KeychainIntentService.GENERATE_KEY_SYMMETRIC_PASSPHRASE,
                            mCurrentPassPhrase);

                    serviceIntent.putExtra(KeychainIntentService.EXTRA_DATA, data);

                    // Message is received after generating is done in ApgService
                    KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                            this, R.string.progress_generating, ProgressDialog.STYLE_SPINNER) {
                        public void handleMessage(Message message) {
                            // handle messages by standard ApgHandler first
                            super.handleMessage(message);

                            if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                                // get new key from data bundle returned from service
                                Bundle data = message.getData();
                                PGPSecretKey masterKey = PgpConversionHelper
                                        .BytesToPGPSecretKey(data
                                                .getByteArray(KeychainIntentService.RESULT_NEW_KEY));
                                PGPSecretKey subKey = PgpConversionHelper
                                        .BytesToPGPSecretKey(data
                                                .getByteArray(KeychainIntentService.RESULT_NEW_KEY2));

                                //We must set the key flags here as they are not set when we make the
                                //key pair. Because we are not generating hashed packets there...
                                // add master key
                                mKeys.add(masterKey);
                                mKeysUsages.add(KeyFlags.CERTIFY_OTHER);

                                // add sub key
                                mKeys.add(subKey);
                                mKeysUsages.add(KeyFlags.ENCRYPT_COMMS + KeyFlags.ENCRYPT_STORAGE);

                                buildLayout(true);
                            }
                        }
                    };

                    // Create a new Messenger for the communication back
                    Messenger messenger = new Messenger(saveHandler);
                    serviceIntent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                    saveHandler.showProgressDialog(this);

                    // start service with intent
                    startService(serviceIntent);
                }
            }
        } else {
            buildLayout(false);
        }
    }

    /**
     * Handle intent action to edit existing key
     * 
     * @param intent
     */
    private void handleActionEditKey(Intent intent) {
        mDataUri = intent.getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
        } else {
            Log.d(Constants.TAG, "uri: " + mDataUri);

            long keyRingRowId = Long.valueOf(mDataUri.getLastPathSegment());

            // get master key id using row id
            long masterKeyId = ProviderHelper.getSecretMasterKeyId(this, keyRingRowId);

            masterCanSign = ProviderHelper.getSecretMasterKeyCanCertify(this, keyRingRowId);
            finallyEdit(masterKeyId);
        }
    }

    private void showPassphraseDialog(final long masterKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    mCurrentPassPhrase = PassphraseCacheService.getCachedPassphrase(
                            EditKeyActivity.this, masterKeyId);
                    finallySaveClicked();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    EditKeyActivity.this, messenger, masterKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_edit, menu);
        mSaveButton = menu.findItem(R.id.menu_key_edit_save);
        mSaveButton.setEnabled(needsSaving());
        //totally get rid of some actions for new keys
        if (mDataUri == null) {
            MenuItem mButton = menu.findItem(R.id.menu_key_edit_export_file);
            mButton.setVisible(false);
            mButton = menu.findItem(R.id.menu_key_edit_delete);
            mButton.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            cancelClicked(); //TODO: why isn't this triggered on my tablet - one of many ui problems I've had with this device. A code compatibility issue or a Samsung fail?
            return true;
        case R.id.menu_key_edit_cancel:
            cancelClicked();
            return true;
        case R.id.menu_key_edit_export_file:
            if (needsSaving()) {
                Toast.makeText(this, R.string.error_save_first, Toast.LENGTH_LONG).show();
            } else {
                mExportHelper.showExportKeysDialog(mDataUri, Id.type.secret_key, Constants.path.APP_DIR
                        + "/secexport.asc");
            }
            return true;
        case R.id.menu_key_edit_delete: {
            // Message is received after key is deleted
            Handler returnHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == DeleteKeyDialogFragment.MESSAGE_OKAY) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            };

            mExportHelper.deleteKey(mDataUri, Id.type.secret_key, returnHandler);
            return true;
        }
        case R.id.menu_key_edit_save:
            saveClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unchecked")
    private void finallyEdit(final long masterKeyId) {
        if (masterKeyId != 0) {
            PGPSecretKey masterKey = null;
            mKeyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this, masterKeyId);
            if (mKeyRing != null) {
                masterKey = PgpKeyHelper.getMasterKey(mKeyRing);
                for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(mKeyRing.getSecretKeys())) {
                    mKeys.add(key);
                    mKeysUsages.add(-1); // get usage when view is created
                }
            } else {
                Log.e(Constants.TAG, "Keyring not found with masterKeyId: " + masterKeyId);
                Toast.makeText(this, R.string.error_no_secret_key_found, Toast.LENGTH_LONG).show();
            }
            if (masterKey != null) {
                boolean isSet = false;
                for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                    Log.d(Constants.TAG, "Added userId " + userId);
                    if (!isSet) {
                        isSet = true;
                        String[] parts = PgpKeyHelper.splitUserId(userId);
                        if (parts[0] != null)
                            setTitle(parts[0]);
                    }
                    mUserIds.add(userId);
                }
            }
        }

        mCurrentPassPhrase = "";

        buildLayout(false);
        mIsPassPhraseSet = PassphraseCacheService.hasPassphrase(this, masterKeyId);
        if (!mIsPassPhraseSet) {
            // check "no passphrase" checkbox and remove button
            mNoPassphrase.setChecked(true);
            mChangePassPhrase.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the dialog to set a new passphrase
     */
    private void showSetPassphraseDialog() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // set new returned passphrase!
                    mNewPassPhrase = data
                            .getString(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE);

                    updatePassPhraseButtonText();
                    somethingChanged();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        // set title based on isPassphraseSet()
        int title;
        if (isPassphraseSet()) {
            title = R.string.title_change_pass_phrase;
        } else {
            title = R.string.title_set_passphrase;
        }

        SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(
                messenger, title);

        setPassphraseDialog.show(getSupportFragmentManager(), "setPassphraseDialog");
    }

    /**
     * Build layout based on mUserId, mKeys and mKeysUsages Vectors. It creates Views for every user
     * id and key.
     * @param newKeys
     */
    private void buildLayout(boolean newKeys) {
        setContentView(R.layout.edit_key_activity);

        // find views
        mChangePassPhrase = (BootstrapButton) findViewById(R.id.edit_key_btn_change_pass_phrase);
        mNoPassphrase = (CheckBox) findViewById(R.id.edit_key_no_passphrase);

        // Build layout based on given userIds and keys
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout container = (LinearLayout) findViewById(R.id.edit_key_container);
        mUserIdsView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mUserIdsView.setType(Id.type.user_id);
        mUserIdsView.setCanEdit(masterCanSign);
        mUserIdsView.setUserIds(mUserIds);
        mUserIdsView.setEditorListener(this);
        container.addView(mUserIdsView);
        mKeysView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mKeysView.setType(Id.type.key);
        mKeysView.setCanEdit(masterCanSign);
        mKeysView.setKeys(mKeys, mKeysUsages, newKeys);
        mKeysView.setEditorListener(this);
        container.addView(mKeysView);

        updatePassPhraseButtonText();

        mChangePassPhrase.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showSetPassphraseDialog();
            }
        });

        // disable passphrase when no passphrase checkbox is checked!
        mNoPassphrase.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // remove passphrase
                    mSavedNewPassPhrase = mNewPassPhrase;
                    mNewPassPhrase = "";
                    mChangePassPhrase.setVisibility(View.GONE);
                } else {
                    mNewPassPhrase = mSavedNewPassPhrase;
                    mChangePassPhrase.setVisibility(View.VISIBLE);
                }
                somethingChanged();
            }
        });
    }

    private long getMasterKeyId() {
        if (mKeysView.getEditors().getChildCount() == 0) {
            return 0;
        }
        return ((KeyEditor) mKeysView.getEditors().getChildAt(0)).getValue().getKeyID();
    }

    public boolean isPassphraseSet() {
        if (mNoPassphrase.isChecked()) {
            return true;
        } else if ((mIsPassPhraseSet)
                || (mNewPassPhrase != null && !mNewPassPhrase.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasPassphraseChanged()
    {
        if (mNoPassphrase != null) {
            if (mNoPassphrase.isChecked()) {
                return mIsPassPhraseSet;
            } else {
                return (mNewPassPhrase != null && !mNewPassPhrase.equals(""));
            }
        }else {
            return false;
        }
    }

    private void saveClicked() {
        long masterKeyId = getMasterKeyId();
        if (needsSaving()) { //make sure, as some versions don't support invalidateOptionsMenu
            try {
                if (!isPassphraseSet()) {
                    throw new PgpGeneralException(this.getString(R.string.set_a_passphrase));
                }

                String passphrase;
                if (mIsPassPhraseSet)
                    passphrase = PassphraseCacheService.getCachedPassphrase(this, masterKeyId);
                else
                    passphrase = "";
                if (passphrase == null) {
                    showPassphraseDialog(masterKeyId);
                } else {
                    mCurrentPassPhrase = passphrase;
                    finallySaveClicked();
                }
            } catch (PgpGeneralException e) {
                Toast.makeText(this, getString(R.string.error_message, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean[] toPrimitiveArray(final List<Boolean> booleanList) {
        final boolean[] primitives = new boolean[booleanList.size()];
        int index = 0;
        for (Boolean object : booleanList) {
            primitives[index++] = object;
        }
        return primitives;
    }

    private void finallySaveClicked() {
        try {
            // Send all information needed to service to edit key in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_SAVE_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();
            data.putString(KeychainIntentService.SAVE_KEYRING_CURRENT_PASSPHRASE,
                    mCurrentPassPhrase);
            data.putString(KeychainIntentService.SAVE_KEYRING_NEW_PASSPHRASE, mNewPassPhrase);
            data.putStringArrayList(KeychainIntentService.SAVE_KEYRING_USER_IDS,
                    getUserIds(mUserIdsView));
            ArrayList<PGPSecretKey> keys = getKeys(mKeysView);
            data.putByteArray(KeychainIntentService.SAVE_KEYRING_KEYS,
                    PgpConversionHelper.PGPSecretKeyArrayListToBytes(keys));
            data.putIntegerArrayList(KeychainIntentService.SAVE_KEYRING_KEYS_USAGES,
                    getKeysUsages(mKeysView));
            data.putSerializable(KeychainIntentService.SAVE_KEYRING_KEYS_EXPIRY_DATES,
                    getKeysExpiryDates(mKeysView));
            data.putLong(KeychainIntentService.SAVE_KEYRING_MASTER_KEY_ID, getMasterKeyId());
            data.putBoolean(KeychainIntentService.SAVE_KEYRING_CAN_SIGN, masterCanSign);
            data.putStringArrayList(KeychainIntentService.SAVE_KEYRING_ORIGINAL_IDS, );
            data.putBooleanArray(KeychainIntentService.SAVE_KEYRING_ORIGINAL_IDS,
                    toPrimitiveArray(mUserIdsView.getNeedsSavingArray()));
            data.putBooleanArray(KeychainIntentService.SAVE_KEYRING_MODDED_KEYS,
                    toPrimitiveArray(mKeysView.getNeedsSavingArray()));

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Message is received after saving is done in ApgService
            KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                    R.string.progress_saving, ProgressDialog.STYLE_HORIZONTAL) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                        Intent data = new Intent();
                        data.putExtra(RESULT_EXTRA_MASTER_KEY_ID, getMasterKeyId());
                        ArrayList<String> userIds = null;
                        try {
                            userIds = getUserIds(mUserIdsView);
                        } catch (PgpGeneralException e) {
                            Log.e(Constants.TAG, "exception while getting user ids", e);
                        }
                        data.putExtra(RESULT_EXTRA_USER_ID, userIds.get(0));
                        setResult(RESULT_OK, data);
                        finish();
                    }
                }
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } catch (PgpGeneralException e) {
            Toast.makeText(this, getString(R.string.error_message, e.getMessage()),
                   Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelClicked() {
        if (needsSaving()) { //ask if we want to save
            AlertDialog.Builder alert = new AlertDialog.Builder(
                    EditKeyActivity.this);

            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setTitle(R.string.warning);
            alert.setMessage(EditKeyActivity.this.getString(R.string.ask_save_changed_key));

            alert.setPositiveButton(EditKeyActivity.this.getString(android.R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            saveClicked();
                        }
                    });
            alert.setNegativeButton(this.getString(android.R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    });
            alert.setCancelable(false);
            alert.create().show();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Returns user ids from the SectionView
     * 
     * @param userIdsView
     * @return
     */
    private ArrayList<String> getUserIds(SectionView userIdsView) throws PgpGeneralException {
        ArrayList<String> userIds = new ArrayList<String>();

        ViewGroup userIdEditors = userIdsView.getEditors();

        boolean gotMainUserId = false;
        for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
            UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
            String userId;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.InvalidEmailException e) {
                throw new PgpGeneralException(e.getMessage());
            }

            if (editor.isMainUserId()) {
                userIds.add(0, userId);
                gotMainUserId = true;
            } else {
                userIds.add(userId);
            }
        }

        if (userIds.size() == 0) {
            throw new PgpGeneralException(getString(R.string.error_key_needs_a_user_id));
        }

        if (!gotMainUserId) {
            throw new PgpGeneralException(getString(R.string.error_main_user_id_must_not_be_empty));
        }

        return userIds;
    }

    /**
     * Returns keys from the SectionView
     * 
     * @param keysView
     * @return
     */
    private ArrayList<PGPSecretKey> getKeys(SectionView keysView) throws PgpGeneralException {
        ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            keys.add(editor.getValue());
        }

        return keys;
    }

    /**
     * Returns usage selections of keys from the SectionView
     * 
     * @param keysView
     * @return
     */
    private ArrayList<Integer> getKeysUsages(SectionView keysView) throws PgpGeneralException {
        ArrayList<Integer> keysUsages = new ArrayList<Integer>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            keysUsages.add(editor.getUsage());
        }

        return keysUsages;
    }

    private ArrayList<GregorianCalendar> getKeysExpiryDates(SectionView keysView) throws PgpGeneralException {
        ArrayList<GregorianCalendar> keysExpiryDates = new ArrayList<GregorianCalendar>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            keysExpiryDates.add(editor.getExpiryDate());
        }

        return keysExpiryDates;
    }

    private void updatePassPhraseButtonText() {
        mChangePassPhrase.setText(isPassphraseSet() ? getString(R.string.btn_change_passphrase)
                : getString(R.string.btn_set_passphrase));
    }

}
