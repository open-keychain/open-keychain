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
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.service.ApgHandler;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.ui.dialog.ProgressDialogFragment;
import org.thialfihar.android.apg.ui.widget.KeyEditor;
import org.thialfihar.android.apg.ui.widget.SectionView;
import org.thialfihar.android.apg.ui.widget.UserIdEditor;
import org.thialfihar.android.apg.util.IterableIterator;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.Iterator;
import java.util.Vector;

public class EditKeyActivity extends SherlockFragmentActivity {

    // possible intent actions for this activity
    public static final String ACTION_CREATE_KEY = Constants.INTENT_PREFIX + "CREATE_KEY";
    public static final String ACTION_EDIT_KEY = Constants.INTENT_PREFIX + "EDIT_KEY";

    private Intent mIntent = null;
    private ActionBar mActionBar;

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIdsView;
    private SectionView mKeysView;

    private String mCurrentPassPhrase = null;
    private String mNewPassPhrase = null;

    private Button mChangePassPhrase;

    private CheckBox mNoPassphrase;

    private ProgressDialogFragment mSavingDialog;
    private ProgressDialogFragment mGeneratingDialog;

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
            Intent intent = new Intent(this, SecretKeyListActivity.class);
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
        setContentView(R.layout.edit_key);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);

        // set actionbar without home button if called from another app
        if (getCallingPackage() != null && getCallingPackage().equals(Constants.PACKAGE_NAME)) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        } else {
            mActionBar.setDisplayHomeAsUpEnabled(false);
            mActionBar.setHomeButtonEnabled(false);
        }

        // find views
        mChangePassPhrase = (Button) findViewById(R.id.edit_key_btn_change_pass_phrase);
        mNoPassphrase = (CheckBox) findViewById(R.id.edit_key_no_passphrase);

        mUserIds = new Vector<String>();
        mKeys = new Vector<PGPSecretKey>();
        mKeysUsages = new Vector<Integer>();

        // Catch Intents opened from other apps
        mIntent = getIntent();

        // Handle intents
        Bundle extras = mIntent.getExtras();
        if (ACTION_CREATE_KEY.equals(mIntent.getAction())) {
            mActionBar.setTitle(R.string.title_createKey);

            mCurrentPassPhrase = "";

            if (extras != null) {
                // if userId is given, prefill the fields
                if (extras.containsKey(PGPHelper.EXTRA_USER_IDS)) {
                    Log.d(Constants.TAG, "UserIds are given!");
                    mUserIds.add(extras.getString(PGPHelper.EXTRA_USER_IDS));
                }

                // if no passphrase is given
                if (extras.containsKey(PGPHelper.EXTRA_NO_PASSPHRASE)) {
                    boolean noPassphrase = extras.getBoolean(PGPHelper.EXTRA_NO_PASSPHRASE);
                    if (noPassphrase) {
                        // check "no passphrase" checkbox and remove button
                        mNoPassphrase.setChecked(true);
                        mChangePassPhrase.setVisibility(View.GONE);
                    }
                }

                // generate key
                if (extras.containsKey(PGPHelper.EXTRA_GENERATE_DEFAULT_KEYS)) {
                    boolean generateDefaultKeys = extras
                            .getBoolean(PGPHelper.EXTRA_GENERATE_DEFAULT_KEYS);
                    if (generateDefaultKeys) {

                        // build layout in handler after generating keys not directly in onCreate
                        mBuildLayout = false;

                        // Send all information needed to service generate keys in other thread
                        Intent intent = new Intent(this, ApgService.class);
                        intent.putExtra(ApgService.EXTRA_ACTION,
                                ApgService.ACTION_GENERATE_DEFAULT_RSA_KEYS);

                        // fill values for this action
                        Bundle data = new Bundle();
                        data.putString(ApgService.SYMMETRIC_PASSPHRASE, mCurrentPassPhrase);

                        intent.putExtra(ApgService.EXTRA_DATA, data);

                        // show progress dialog
                        mGeneratingDialog = ProgressDialogFragment.newInstance(
                                R.string.progress_generating, ProgressDialog.STYLE_SPINNER);

                        // Message is received after generating is done in ApgService
                        ApgHandler saveHandler = new ApgHandler(this, mGeneratingDialog) {
                            public void handleMessage(Message message) {
                                // handle messages by standard ApgHandler first
                                super.handleMessage(message);

                                if (message.arg1 == ApgHandler.MESSAGE_OKAY) {
                                    // get new key from data bundle returned from service
                                    Bundle data = message.getData();
                                    PGPSecretKeyRing masterKeyRing = PGPConversionHelper
                                            .BytesToPGPSecretKeyRing(data
                                                    .getByteArray(ApgService.RESULT_NEW_KEY));
                                    PGPSecretKeyRing subKeyRing = PGPConversionHelper
                                            .BytesToPGPSecretKeyRing(data
                                                    .getByteArray(ApgService.RESULT_NEW_KEY2));

                                    // add master key
                                    Iterator<PGPSecretKey> masterIt = masterKeyRing.getSecretKeys();
                                    mKeys.add(masterIt.next());
                                    mKeysUsages.add(Id.choice.usage.sign_only);

                                    // add sub key
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
                        intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

                        mGeneratingDialog.show(getSupportFragmentManager(), "dialog");

                        // start service with intent
                        startService(intent);
                    }
                }
            }
        } else if (ACTION_EDIT_KEY.equals(mIntent.getAction())) {
            mActionBar.setTitle(R.string.title_editKey);

            mCurrentPassPhrase = PGPHelper.getEditPassPhrase();
            if (mCurrentPassPhrase == null) {
                mCurrentPassPhrase = "";
            }

            if (mCurrentPassPhrase.equals("")) {
                // check "no passphrase" checkbox and remove button
                mNoPassphrase.setChecked(true);
                mChangePassPhrase.setVisibility(View.GONE);
            }

            if (extras != null) {

                if (extras.containsKey(PGPHelper.EXTRA_KEY_ID)) {
                    long keyId = mIntent.getExtras().getLong(PGPHelper.EXTRA_KEY_ID);

                    if (keyId != 0) {
                        PGPSecretKey masterKey = null;
                        mKeyRing = PGPHelper.getSecretKeyRing(keyId);
                        if (mKeyRing != null) {
                            masterKey = PGPHelper.getMasterKey(mKeyRing);
                            for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(
                                    mKeyRing.getSecretKeys())) {
                                mKeys.add(key);
                                mKeysUsages.add(-1); // get usage when view is created
                            }
                        }
                        if (masterKey != null) {
                            for (String userId : new IterableIterator<String>(
                                    masterKey.getUserIDs())) {
                                mUserIds.add(userId);
                            }
                        }
                    }
                }
            }
        }

        mChangePassPhrase.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(Id.dialog.new_pass_phrase);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case Id.dialog.new_pass_phrase: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            if (isPassphraseSet()) {
                alert.setTitle(R.string.title_changePassPhrase);
            } else {
                alert.setTitle(R.string.title_setPassPhrase);
            }
            alert.setMessage(R.string.enterPassPhraseTwice);

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.passphrase, null);
            final EditText input1 = (EditText) view.findViewById(R.id.passphrase_passphrase);
            final EditText input2 = (EditText) view.findViewById(R.id.passphrase_passphrase_again);

            alert.setView(view);

            alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    removeDialog(Id.dialog.new_pass_phrase);

                    String passPhrase1 = "" + input1.getText();
                    String passPhrase2 = "" + input2.getText();
                    if (!passPhrase1.equals(passPhrase2)) {
                        showDialog(Id.dialog.pass_phrases_do_not_match);
                        return;
                    }

                    if (passPhrase1.equals("")) {
                        showDialog(Id.dialog.no_pass_phrase);
                        return;
                    }

                    mNewPassPhrase = passPhrase1;
                    updatePassPhraseButtonText();
                }
            });

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    removeDialog(Id.dialog.new_pass_phrase);
                }
            });

            return alert.create();
        }

        default: {
            return super.onCreateDialog(id);
        }
        }
    }

    private void saveClicked() {

        try {
            if (!isPassphraseSet()) {
                throw new PGPHelper.GeneralException(this.getString(R.string.setAPassPhrase));
            }

            // Send all information needed to service to edit key in other thread
            Intent intent = new Intent(this, ApgService.class);

            intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_SAVE_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();
            data.putString(ApgService.CURRENT_PASSPHRASE, mCurrentPassPhrase);
            data.putString(ApgService.NEW_PASSPHRASE, mNewPassPhrase);
            data.putSerializable(ApgService.USER_IDS, getUserIds(mUserIdsView));
            Vector<PGPSecretKey> keys = getKeys(mKeysView);
            data.putByteArray(ApgService.KEYS, PGPConversionHelper.PGPSecretKeyListToBytes(keys));
            data.putSerializable(ApgService.KEYS_USAGES, getKeysUsages(mKeysView));
            data.putLong(ApgService.MASTER_KEY_ID, getMasterKeyId());

            intent.putExtra(ApgService.EXTRA_DATA, data);

            // show progress dialog
            mSavingDialog = ProgressDialogFragment.newInstance(R.string.progress_saving,
                    ProgressDialog.STYLE_HORIZONTAL);

            // Message is received after saving is done in ApgService
            ApgHandler saveHandler = new ApgHandler(this, mSavingDialog) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == ApgHandler.MESSAGE_OKAY) {
                        finish();
                    }
                };
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

            mSavingDialog.show(getSupportFragmentManager(), "dialog");

            // start service with intent
            startService(intent);
        } catch (PGPHelper.GeneralException e) {
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
    private Vector<String> getUserIds(SectionView userIdsView) throws PGPHelper.GeneralException {
        Vector<String> userIds = new Vector<String>();

        ViewGroup userIdEditors = userIdsView.getEditors();

        boolean gotMainUserId = false;
        for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
            UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
            String userId = null;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.NoNameException e) {
                throw new PGPHelper.GeneralException(
                        this.getString(R.string.error_userIdNeedsAName));
            } catch (UserIdEditor.NoEmailException e) {
                throw new PGPHelper.GeneralException(
                        this.getString(R.string.error_userIdNeedsAnEmailAddress));
            } catch (UserIdEditor.InvalidEmailException e) {
                throw new PGPHelper.GeneralException(e.getMessage());
            }

            if (userId.equals("")) {
                continue;
            }

            if (editor.isMainUserId()) {
                userIds.insertElementAt(userId, 0);
                gotMainUserId = true;
            } else {
                userIds.add(userId);
            }
        }

        if (userIds.size() == 0) {
            throw new PGPHelper.GeneralException(getString(R.string.error_keyNeedsAUserId));
        }

        if (!gotMainUserId) {
            throw new PGPHelper.GeneralException(getString(R.string.error_mainUserIdMustNotBeEmpty));
        }

        return userIds;
    }

    /**
     * Returns keys from the SectionView
     * 
     * @param keysView
     * @return
     */
    private Vector<PGPSecretKey> getKeys(SectionView keysView) throws PGPHelper.GeneralException {
        Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PGPHelper.GeneralException(getString(R.string.error_keyNeedsMasterKey));
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
    private Vector<Integer> getKeysUsages(SectionView keysView) throws PGPHelper.GeneralException {
        Vector<Integer> getKeysUsages = new Vector<Integer>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            throw new PGPHelper.GeneralException(getString(R.string.error_keyNeedsMasterKey));
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
