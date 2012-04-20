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
import org.thialfihar.android.apg.Apg;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.ui.widget.KeyEditor;
import org.thialfihar.android.apg.ui.widget.SectionView;
import org.thialfihar.android.apg.ui.widget.UserIdEditor;
import org.thialfihar.android.apg.util.IterableIterator;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

public class EditKeyActivity extends SherlockActivity { // extends BaseActivity {
    private Intent mIntent = null;
    private ActionBar mActionBar;

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIds;
    private SectionView mKeys;

    private String mCurrentPassPhrase = null;
    private String mNewPassPhrase = null;

    private Button mChangePassPhrase;

    private CheckBox mNoPassphrase;

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
            startActivity(new Intent(this, SecretKeyListActivity.class));
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

        // find views
        mChangePassPhrase = (Button) findViewById(R.id.edit_key_btn_change_pass_phrase);
        mNoPassphrase = (CheckBox) findViewById(R.id.edit_key_no_passphrase);

        Vector<String> userIds = new Vector<String>();
        Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();
        Vector<Integer> keysUsages = new Vector<Integer>();

        // Catch Intents opened from other apps
        mIntent = getIntent();
        Bundle extras = mIntent.getExtras();
        if (Apg.Intent.CREATE_KEY.equals(mIntent.getAction())) {

            mActionBar.setTitle(R.string.title_createKey);

            mCurrentPassPhrase = "";

            if (extras != null) {

                // disable home button on actionbar because this activity is run from another app
                mActionBar.setDisplayShowTitleEnabled(true);
                mActionBar.setDisplayHomeAsUpEnabled(false);
                mActionBar.setHomeButtonEnabled(false);

                // if userId is given, prefill the fields
                if (extras.containsKey(Apg.EXTRA_USER_IDS)) {
                    Log.d(Constants.TAG, "UserIds are given!");
                    userIds.add(extras.getString(Apg.EXTRA_USER_IDS));
                }

                // if no passphrase is given
                if (extras.containsKey(Apg.EXTRA_NO_PASSPHRASE)) {
                    boolean noPassphrase = extras.getBoolean(Apg.EXTRA_NO_PASSPHRASE);
                    if (noPassphrase) {
                        // check "no passphrase" checkbox and remove button
                        mNoPassphrase.setChecked(true);
                        mChangePassPhrase.setVisibility(View.GONE);
                    }
                }

                // generate key
                if (extras.containsKey(Apg.EXTRA_GENERATE_DEFAULT_KEYS)) {
                    boolean generateDefaultKeys = extras
                            .getBoolean(Apg.EXTRA_GENERATE_DEFAULT_KEYS);
                    if (generateDefaultKeys) {

                        // generate a RSA 2048 key for encryption and signing!
                        try {
                            PGPSecretKey masterKey = Apg.createKey(this, Id.choice.algorithm.rsa,
                                    2048, mCurrentPassPhrase, null);

                            // add new masterKey to keys array, which is then added to view
                            keys.add(masterKey);
                            keysUsages.add(Id.choice.usage.sign_only);

                            PGPSecretKey subKey = Apg.createKey(this, Id.choice.algorithm.rsa,
                                    2048, mCurrentPassPhrase, masterKey);

                            keys.add(subKey);
                            keysUsages.add(Id.choice.usage.encrypt_only);
                        } catch (Exception e) {
                            Log.e(Constants.TAG, "Creating initial key failed: +" + e);
                        }
                    }

                }
            }
        } else if (Apg.Intent.EDIT_KEY.equals(mIntent.getAction())) {

            mActionBar.setTitle(R.string.title_editKey);

            mCurrentPassPhrase = Apg.getEditPassPhrase();
            if (mCurrentPassPhrase == null) {
                mCurrentPassPhrase = "";
            }

            if (mCurrentPassPhrase.equals("")) {
                // check "no passphrase" checkbox and remove button
                mNoPassphrase.setChecked(true);
                mChangePassPhrase.setVisibility(View.GONE);
            }

            if (extras != null) {

                if (extras.containsKey(Apg.EXTRA_KEY_ID)) {
                    long keyId = mIntent.getExtras().getLong(Apg.EXTRA_KEY_ID);

                    if (keyId != 0) {
                        PGPSecretKey masterKey = null;
                        mKeyRing = Apg.getSecretKeyRing(keyId);
                        if (mKeyRing != null) {
                            masterKey = Apg.getMasterKey(mKeyRing);
                            for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(
                                    mKeyRing.getSecretKeys())) {
                                keys.add(key);
                                keysUsages.add(-1); // get usage when view is created
                            }
                        }
                        if (masterKey != null) {
                            for (String userId : new IterableIterator<String>(
                                    masterKey.getUserIDs())) {
                                userIds.add(userId);
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

        // Build layout based on given userIds and keys
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout container = (LinearLayout) findViewById(R.id.edit_key_container);
        mUserIds = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mUserIds.setType(Id.type.user_id);
        mUserIds.setUserIds(userIds);
        container.addView(mUserIds);
        mKeys = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mKeys.setType(Id.type.key);
        mKeys.setKeys(keys, keysUsages);
        container.addView(mKeys);

        updatePassPhraseButtonText();
    }

    private long getMasterKeyId() {
        if (mKeys.getEditors().getChildCount() == 0) {
            return 0;
        }
        return ((KeyEditor) mKeys.getEditors().getChildAt(0)).getValue().getKeyID();
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
        if (!isPassphraseSet()) {
            Toast.makeText(this, R.string.setAPassPhrase, Toast.LENGTH_SHORT).show();
            return;
        }
//        showDialog(Id.dialog.saving);
        
        ProgressDialogFragment newFragment = ProgressDialogFragment.newInstance(
                ProgressDialogFragment.ID_SAVING);
        newFragment.show(getSupportFragmentManager(), "saving");
//        ((ProgressDialog) newFragment.getDialog()).setProgress(value)

        // startThread();

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(this, ApgService.class);
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(handler);
        intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);
        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_SAVE_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();
        data.putString(ApgService.DATA_CURRENT_PASSPHRASE, mCurrentPassPhrase);
        data.putString(ApgService.DATA_NEW_PASSPHRASE, mNewPassPhrase);
        data.putSerializable(ApgService.DATA_USER_IDS, getUserIds(mUserIds));

        Vector<PGPSecretKey> keys = getKeys(mKeys);

        // convert to byte[]
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (PGPSecretKey key : keys) {
            try {
                key.encode(os);
            } catch (IOException e) {
                Log.e(Constants.TAG,
                        "Error while converting PGPSecretKey to byte[]: " + e.getMessage());
                e.printStackTrace();
            }
        }

        byte[] keysBytes = os.toByteArray();

        data.putByteArray(ApgService.DATA_KEYS, keysBytes);

        data.putSerializable(ApgService.DATA_KEYS_USAGES, getKeysUsages(mKeys).toArray());
        data.putLong(ApgService.DATA_MASTER_KEY_ID, getMasterKeyId());

        intent.putExtra(ApgService.EXTRA_DATA, data);

        startService(intent);
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            Object path = message.obj;
            if (message.arg1 == ApgService.MESSAGE_OKAY) {
                Toast.makeText(EditKeyActivity.this, "okay", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(EditKeyActivity.this, "nope", Toast.LENGTH_LONG).show();
            }

        };
    };

    // TODO: put in other class
    private Vector<String> getUserIds(SectionView userIdsView) {
        Vector<String> userIds = new Vector<String>();

        ViewGroup userIdEditors = userIdsView.getEditors();

        boolean gotMainUserId = false;
        for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
            UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
            String userId = null;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.NoNameException e) {
                // throw new Apg.GeneralException(this.getString(R.string.error_userIdNeedsAName));
            } catch (UserIdEditor.NoEmailException e) {
                // throw new Apg.GeneralException(
                // this.getString(R.string.error_userIdNeedsAnEmailAddress));
            } catch (UserIdEditor.InvalidEmailException e) {
                // throw new Apg.GeneralException("" + e);
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
            // throw new Apg.GeneralException(context.getString(R.string.error_keyNeedsAUserId));
        }

        if (!gotMainUserId) {
            // throw new Apg.GeneralException(
            // context.getString(R.string.error_mainUserIdMustNotBeEmpty));
        }

        return userIds;
    }

    private Vector<PGPSecretKey> getKeys(SectionView keysView) {
        Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            // throw new Apg.GeneralException(getString(R.string.error_keyNeedsMasterKey));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            keys.add(editor.getValue());
        }

        return keys;
    }

    private Vector<Integer> getKeysUsages(SectionView keysView) {
        Vector<Integer> getKeysUsages = new Vector<Integer>();

        ViewGroup keyEditors = keysView.getEditors();

        if (keyEditors.getChildCount() == 0) {
            // throw new Apg.GeneralException(getString(R.string.error_keyNeedsMasterKey));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
            getKeysUsages.add(editor.getUsage());
        }

        return getKeysUsages;
    }

    // @Override
    // public void run() {
    // String error = null;
    // Bundle data = new Bundle();
    // Message msg = new Message();
    //
    // data.putParcelable("ts", (Parcelable) mUserIds);
    //
    // try {
    // String oldPassPhrase = mCurrentPassPhrase;
    // String newPassPhrase = mNewPassPhrase;
    // if (newPassPhrase == null) {
    // newPassPhrase = oldPassPhrase;
    // }
    // Apg.buildSecretKey(this, mUserIds, mKeys, oldPassPhrase, newPassPhrase, this);
    // Apg.setCachedPassPhrase(getMasterKeyId(), newPassPhrase);
    // } catch (NoSuchProviderException e) {
    // error = "" + e;
    // } catch (NoSuchAlgorithmException e) {
    // error = "" + e;
    // } catch (PGPException e) {
    // error = "" + e;
    // } catch (SignatureException e) {
    // error = "" + e;
    // } catch (Apg.GeneralException e) {
    // error = "" + e;
    // } catch (Database.GeneralException e) {
    // error = "" + e;
    // } catch (IOException e) {
    // error = "" + e;
    // }
    //
    // data.putInt(Constants.extras.STATUS, Id.message.done);
    //
    // if (error != null) {
    // data.putString(Apg.EXTRA_ERROR, error);
    // }
    //
    // msg.setData(data);
    // sendMessage(msg);
    // }

    // @Override
    // public void doneCallback(Message msg) {
    // super.doneCallback(msg);
    //
    // Bundle data = msg.getData();
    // removeDialog(Id.dialog.saving);
    //
    // String error = data.getString(Apg.EXTRA_ERROR);
    // if (error != null) {
    // Toast.makeText(EditKeyActivity.this, getString(R.string.errorMessage, error),
    // Toast.LENGTH_SHORT).show();
    // } else {
    // Toast.makeText(EditKeyActivity.this, R.string.keySaved, Toast.LENGTH_SHORT).show();
    // setResult(RESULT_OK);
    // finish();
    // }
    // }

    private void updatePassPhraseButtonText() {
        mChangePassPhrase.setText(isPassphraseSet() ? R.string.btn_changePassPhrase
                : R.string.btn_setPassPhrase);
    }
}
