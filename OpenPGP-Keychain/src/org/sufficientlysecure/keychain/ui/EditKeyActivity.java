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
import java.util.Iterator;
import java.util.Vector;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.KeyEditor;
import org.sufficientlysecure.keychain.ui.widget.SectionView;
import org.sufficientlysecure.keychain.ui.widget.UserIdEditor;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class EditKeyActivity extends SherlockFragmentActivity {

    // Actions for internal use only:
    public static final String ACTION_CREATE_KEY = Constants.INTENT_PREFIX + "CREATE_KEY";
    public static final String ACTION_EDIT_KEY = Constants.INTENT_PREFIX + "EDIT_KEY";

    // possible extra keys
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_NO_PASSPHRASE = "no_passphrase";
    public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generate_default_keys";
    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String EXTRA_MASTER_CAN_SIGN = "master_can_sign";

    // results when saving key
    public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String RESULT_EXTRA_USER_ID = "user_id";

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIdsView;
    private SectionView mKeysView;

    private String mCurrentPassPhrase = null;
    private String mNewPassPhrase = null;

    private Button mChangePassPhrase;

    private CheckBox mNoPassphrase;

    Vector<String> mUserIds;
    Vector<PGPSecretKey> mKeys;
    Vector<Integer> mKeysUsages;
    boolean masterCanSign = true;

    // will be set to false to build layout later in handler
    private boolean mBuildLayout = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar
        ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.btn_save,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // save
                        saveClicked();
                    }
                }, R.string.btn_do_not_save, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                });

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

        if (mBuildLayout) {
            buildLayout();
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

                    // build layout in handler after generating keys not directly in onCreate
                    mBuildLayout = false;

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
                                PGPSecretKeyRing masterKeyRing = (PGPSecretKeyRing) PgpConversionHelper
                                        .BytesToPGPKeyRing(data
                                                .getByteArray(KeychainIntentService.RESULT_NEW_KEY));
                                PGPSecretKeyRing subKeyRing = (PGPSecretKeyRing) PgpConversionHelper
                                        .BytesToPGPKeyRing(data
                                                .getByteArray(KeychainIntentService.RESULT_NEW_KEY2));

                                // add master key
                                @SuppressWarnings("unchecked")
                                Iterator<PGPSecretKey> masterIt = masterKeyRing.getSecretKeys();
                                mKeys.add(masterIt.next());
                                mKeysUsages.add(Id.choice.usage.sign_only);

                                // add sub key
                                @SuppressWarnings("unchecked")
                                Iterator<PGPSecretKey> subIt = subKeyRing.getSecretKeys();
                                subIt.next(); // masterkey
                                mKeys.add(subIt.next());
                                mKeysUsages.add(Id.choice.usage.encrypt_only);

                                buildLayout();
                            }
                        };
                    };

                    // Create a new Messenger for the communication back
                    Messenger messenger = new Messenger(saveHandler);
                    serviceIntent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                    saveHandler.showProgressDialog(this);

                    // start service with intent
                    startService(serviceIntent);
                }
            }
        }
    }

    private void showPassphraseDialog(final long masterKeyId, final boolean masterCanSign) {
        // Message is received after passphrase is cached
        final boolean mCanSign = masterCanSign;
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passPhrase = PassphraseCacheService.getCachedPassphrase(
                            EditKeyActivity.this, masterKeyId);
                    mCurrentPassPhrase = passPhrase;
                    finallyEdit(masterKeyId, masterCanSign);
                } else {
                    finish();
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

    /**
     * Handle intent action to edit existing key
     * 
     * @param intent
     */
    private void handleActionEditKey(Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null) {
            if (extras.containsKey(EXTRA_MASTER_CAN_SIGN)) {
                masterCanSign = extras.getBoolean(EXTRA_MASTER_CAN_SIGN);
            }
            if (extras.containsKey(EXTRA_MASTER_KEY_ID)) {
                long masterKeyId = extras.getLong(EXTRA_MASTER_KEY_ID);

                // build layout in edit()
                mBuildLayout = false;

                String passPhrase = PassphraseCacheService.getCachedPassphrase(this, masterKeyId);
                if (passPhrase == null) {
                    showPassphraseDialog(masterKeyId, masterCanSign);
                } else {
                    // PgpMain.setEditPassPhrase(passPhrase);
                    mCurrentPassPhrase = passPhrase;

                    finallyEdit(masterKeyId, masterCanSign);
                }
            }
        }
    }

    private void finallyEdit(final long masterKeyId, final boolean masterCanSign) {
        // TODO: ???
        if (mCurrentPassPhrase == null) {
            mCurrentPassPhrase = "";
        }

        if (mCurrentPassPhrase.equals("")) {
            // check "no passphrase" checkbox and remove button
            mNoPassphrase.setChecked(true);
            mChangePassPhrase.setVisibility(View.GONE);
        }

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
                for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                    Log.d(Constants.TAG, "Added userId " + userId);
                    mUserIds.add(userId);
                }
            }
        }

        buildLayout();
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
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        // set title based on isPassphraseSet()
        int title = -1;
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
     */
    private void buildLayout() {
        setContentView(R.layout.edit_key);

        // find views
        mChangePassPhrase = (Button) findViewById(R.id.edit_key_btn_change_pass_phrase);
        mNoPassphrase = (CheckBox) findViewById(R.id.edit_key_no_passphrase);

        // Build layout based on given userIds and keys
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout container = (LinearLayout) findViewById(R.id.edit_key_container);
        mUserIdsView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mUserIdsView.setType(Id.type.user_id);
        mUserIdsView.setCanEdit(masterCanSign);
        mUserIdsView.setUserIds(mUserIds);
        container.addView(mUserIdsView);
        mKeysView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mKeysView.setType(Id.type.key);
        mKeysView.setCanEdit(masterCanSign);
        mKeysView.setKeys(mKeys, mKeysUsages);
        container.addView(mKeysView);

        updatePassPhraseButtonText();

        mChangePassPhrase.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showSetPassphraseDialog();
            }
        });

        // disable passphrase when no passphrase checkobox is checked!
        mNoPassphrase.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // remove passphrase
                    mNewPassPhrase = null;

                    mChangePassPhrase.setVisibility(View.GONE);
                } else {
                    mChangePassPhrase.setVisibility(View.VISIBLE);
                }
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
        } else if ((!mCurrentPassPhrase.equals(""))
                || (mNewPassPhrase != null && !mNewPassPhrase.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    private void saveClicked() {
        try {
            if (!isPassphraseSet()) {
                throw new PgpGeneralException(this.getString(R.string.set_a_passphrase));
            }

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
            data.putLong(KeychainIntentService.SAVE_KEYRING_MASTER_KEY_ID, getMasterKeyId());
            data.putBoolean(KeychainIntentService.SAVE_KEYRING_CAN_SIGN, masterCanSign);

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
                };
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
        setResult(RESULT_CANCELED);
        finish();
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
            String userId = null;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.NoNameException e) {
                throw new PgpGeneralException(this.getString(R.string.error_user_id_needs_a_name));
            } catch (UserIdEditor.NoEmailException e) {
                throw new PgpGeneralException(
                        this.getString(R.string.error_user_id_needs_an_email_address));
            } catch (UserIdEditor.InvalidEmailException e) {
                throw new PgpGeneralException(e.getMessage());
            }

            if (userId.equals("")) {
                continue;
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
        ArrayList<Integer> getKeysUsages = new ArrayList<Integer>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            getKeysUsages.add(editor.getUsage());
        }

        return getKeysUsages;
    }

    private void updatePassPhraseButtonText() {
        mChangePassPhrase.setText(isPassphraseSet() ? R.string.btn_change_passphrase
                : R.string.btn_set_passphrase);
    }
}
