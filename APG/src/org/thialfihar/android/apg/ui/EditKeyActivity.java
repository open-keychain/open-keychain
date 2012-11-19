/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.provider.ProviderHelper;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.ui.dialog.SetPassphraseDialogFragment;
import org.thialfihar.android.apg.ui.widget.KeyEditor;
import org.thialfihar.android.apg.ui.widget.SectionView;
import org.thialfihar.android.apg.ui.widget.UserIdEditor;
import org.thialfihar.android.apg.util.IterableIterator;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import org.thialfihar.android.apg.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class EditKeyActivity extends SherlockFragmentActivity {

    // possible intent actions for this activity
    public static final String ACTION_CREATE_KEY = Constants.INTENT_PREFIX + "CREATE_KEY";
    public static final String ACTION_EDIT_KEY = Constants.INTENT_PREFIX + "EDIT_KEY";

    // possible extra keys
    public static final String EXTRA_USER_IDS = "userIds";
    public static final String EXTRA_NO_PASSPHRASE = "noPassphrase";
    public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generateDefaultKeys";
    public static final String EXTRA_KEY_ID = "keyId";

    private ActionBar mActionBar;

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

    // will be set to false to build layout later in handler
    private boolean mBuildLayout = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, Id.menu.option.cancel, 0, R.string.btn_doNotSave).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.save, 1, R.string.btn_save).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, KeyListSecretActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        case Id.menu.option.save:
            saveClicked();
            return true;

        case Id.menu.option.cancel:
            finish();
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check permissions for intent actions without user interaction
        String[] restrictedActions = new String[] { ACTION_CREATE_KEY };
        OtherHelper.checkPackagePermissionForActions(this, this.getCallingPackage(),
                Constants.PERMISSION_ACCESS_API, getIntent().getAction(), restrictedActions);

        setContentView(R.layout.edit_key);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);

        // set actionbar without home button if called from another app
        OtherHelper.setActionBarBackButton(this);

        // find views
        mChangePassPhrase = (Button) findViewById(R.id.edit_key_btn_change_pass_phrase);
        mNoPassphrase = (CheckBox) findViewById(R.id.edit_key_no_passphrase);

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

        mActionBar.setTitle(R.string.title_createKey);

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
                    Intent serviceIntent = new Intent(this, ApgIntentService.class);
                    serviceIntent.putExtra(ApgIntentService.EXTRA_ACTION,
                            ApgIntentService.ACTION_GENERATE_DEFAULT_RSA_KEYS);

                    // fill values for this action
                    Bundle data = new Bundle();
                    data.putString(ApgIntentService.SYMMETRIC_PASSPHRASE, mCurrentPassPhrase);

                    serviceIntent.putExtra(ApgIntentService.EXTRA_DATA, data);

                    // Message is received after generating is done in ApgService
                    ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(this,
                            R.string.progress_generating, ProgressDialog.STYLE_SPINNER) {
                        public void handleMessage(Message message) {
                            // handle messages by standard ApgHandler first
                            super.handleMessage(message);

                            if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                                // get new key from data bundle returned from service
                                Bundle data = message.getData();
                                PGPSecretKeyRing masterKeyRing = (PGPSecretKeyRing) PGPConversionHelper
                                        .BytesToPGPKeyRing(data
                                                .getByteArray(ApgIntentService.RESULT_NEW_KEY));
                                PGPSecretKeyRing subKeyRing = (PGPSecretKeyRing) PGPConversionHelper
                                        .BytesToPGPKeyRing(data
                                                .getByteArray(ApgIntentService.RESULT_NEW_KEY2));

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
                    serviceIntent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

                    saveHandler.showProgressDialog(this);

                    // start service with intent
                    startService(serviceIntent);
                }
            }
        }
    }

    /**
     * Handle intent action to edit existing key
     * 
     * @param intent
     */
    private void handleActionEditKey(Intent intent) {
        Bundle extras = intent.getExtras();

        mActionBar.setTitle(R.string.title_editKey);

        mCurrentPassPhrase = PGPMain.getEditPassPhrase();
        if (mCurrentPassPhrase == null) {
            mCurrentPassPhrase = "";
        }

        if (mCurrentPassPhrase.equals("")) {
            // check "no passphrase" checkbox and remove button
            mNoPassphrase.setChecked(true);
            mChangePassPhrase.setVisibility(View.GONE);
        }

        if (extras != null) {
            if (extras.containsKey(EXTRA_KEY_ID)) {
                long keyId = extras.getLong(EXTRA_KEY_ID);

                if (keyId != 0) {
                    PGPSecretKey masterKey = null;
                    mKeyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this, keyId);
                    if (mKeyRing != null) {
                        masterKey = PGPHelper.getMasterKey(mKeyRing);
                        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(
                                mKeyRing.getSecretKeys())) {
                            mKeys.add(key);
                            mKeysUsages.add(-1); // get usage when view is created
                        }
                    }
                    if (masterKey != null) {
                        for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                            Log.d(Constants.TAG, "Added userId " + userId);
                            mUserIds.add(userId);
                        }
                    }
                }
            }
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
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        // set title based on isPassphraseSet()
        int title = -1;
        if (isPassphraseSet()) {
            title = R.string.title_changePassPhrase;
        } else {
            title = R.string.title_setPassPhrase;
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
        // Build layout based on given userIds and keys
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout container = (LinearLayout) findViewById(R.id.edit_key_container);
        mUserIdsView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mUserIdsView.setType(Id.type.user_id);
        mUserIdsView.setUserIds(mUserIds);
        container.addView(mUserIdsView);
        mKeysView = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mKeysView.setType(Id.type.key);
        mKeysView.setKeys(mKeys, mKeysUsages);
        container.addView(mKeysView);

        updatePassPhraseButtonText();
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
                throw new PGPMain.ApgGeneralException(this.getString(R.string.setAPassPhrase));
            }

            // Send all information needed to service to edit key in other thread
            Intent intent = new Intent(this, ApgIntentService.class);

            intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_SAVE_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();
            data.putString(ApgIntentService.CURRENT_PASSPHRASE, mCurrentPassPhrase);
            data.putString(ApgIntentService.NEW_PASSPHRASE, mNewPassPhrase);
            data.putStringArrayList(ApgIntentService.USER_IDS, getUserIds(mUserIdsView));
            ArrayList<PGPSecretKey> keys = getKeys(mKeysView);
            data.putByteArray(ApgIntentService.KEYS,
                    PGPConversionHelper.PGPSecretKeyArrayListToBytes(keys));
            data.putIntegerArrayList(ApgIntentService.KEYS_USAGES, getKeysUsages(mKeysView));
            data.putLong(ApgIntentService.MASTER_KEY_ID, getMasterKeyId());

            intent.putExtra(ApgIntentService.EXTRA_DATA, data);

            // Message is received after saving is done in ApgService
            ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(this,
                    R.string.progress_saving, ProgressDialog.STYLE_HORIZONTAL) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                        finish();
                    }
                };
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } catch (PGPMain.ApgGeneralException e) {
            Toast.makeText(this, getString(R.string.errorMessage, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns user ids from the SectionView
     * 
     * @param userIdsView
     * @return
     */
    private ArrayList<String> getUserIds(SectionView userIdsView)
            throws PGPMain.ApgGeneralException {
        ArrayList<String> userIds = new ArrayList<String>();

        ViewGroup userIdEditors = userIdsView.getEditors();

        boolean gotMainUserId = false;
        for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
            UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
            String userId = null;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.NoNameException e) {
                throw new PGPMain.ApgGeneralException(
                        this.getString(R.string.error_userIdNeedsAName));
            } catch (UserIdEditor.NoEmailException e) {
                throw new PGPMain.ApgGeneralException(
                        this.getString(R.string.error_userIdNeedsAnEmailAddress));
            } catch (UserIdEditor.InvalidEmailException e) {
                throw new PGPMain.ApgGeneralException(e.getMessage());
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
            throw new PGPMain.ApgGeneralException(getString(R.string.error_keyNeedsAUserId));
        }

        if (!gotMainUserId) {
            throw new PGPMain.ApgGeneralException(
                    getString(R.string.error_mainUserIdMustNotBeEmpty));
        }

        return userIds;
    }

    /**
     * Returns keys from the SectionView
     * 
     * @param keysView
     * @return
     */
    private ArrayList<PGPSecretKey> getKeys(SectionView keysView)
            throws PGPMain.ApgGeneralException {
        ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PGPMain.ApgGeneralException(getString(R.string.error_keyNeedsMasterKey));
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
    private ArrayList<Integer> getKeysUsages(SectionView keysView)
            throws PGPMain.ApgGeneralException {
        ArrayList<Integer> getKeysUsages = new ArrayList<Integer>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PGPMain.ApgGeneralException(getString(R.string.error_keyNeedsMasterKey));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            getKeysUsages.add(editor.getUsage());
        }

        return getKeysUsages;
    }

    private void updatePassPhraseButtonText() {
        mChangePassPhrase.setText(isPassphraseSet() ? R.string.btn_changePassPhrase
                : R.string.btn_setPassPhrase);
    }
}
