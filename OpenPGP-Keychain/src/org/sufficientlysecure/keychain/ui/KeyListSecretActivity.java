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

package org.sufficientlysecure.keychain.ui;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.pgp.PgpMain;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

public class KeyListSecretActivity extends KeyListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyType = Id.type.secret_key;

        setContentView(R.layout.key_list_secret_activity);

        mExportFilename = Constants.path.APP_DIR + "/secexport.asc";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.option.create, 1, R.string.menu_createKey).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.createExpert, 2, R.string.menu_createKeyExpert).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.option.create: {
            createKey();
            return true;
        }

        case Id.menu.option.createExpert: {
            createKeyExpert();
            return true;
        }

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    public void checkPassPhraseAndEdit(long masterKeyId, boolean masterCanSign) {
        String passPhrase = PassphraseCacheService.getCachedPassphrase(this, masterKeyId);
        if (passPhrase == null) {
            showPassphraseDialog(masterKeyId, masterCanSign);
        } else {
            PgpMain.setEditPassPhrase(passPhrase);
            editKey(masterKeyId, masterCanSign);
        }
    }

    private void showPassphraseDialog(final long masterKeyId, boolean masterCanSign) {
        // Message is received after passphrase is cached
        final boolean mCanSign = masterCanSign;
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passPhrase = PassphraseCacheService.getCachedPassphrase(
                            KeyListSecretActivity.this, masterKeyId);
                    PgpMain.setEditPassPhrase(passPhrase);
                    editKey(masterKeyId, mCanSign);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    KeyListSecretActivity.this, messenger, masterKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpMain.PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    private void createKey() {
        PgpMain.setEditPassPhrase("");
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
        intent.putExtra(EditKeyActivity.EXTRA_USER_IDS, ""); // show user id view
        startActivityForResult(intent, 0);
    }

    private void createKeyExpert() {
        PgpMain.setEditPassPhrase("");
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
        startActivityForResult(intent, 0);
    }

    private void editKey(long masterKeyId, boolean masterCanSign) {
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
        intent.putExtra(EditKeyActivity.EXTRA_MASTER_CAN_SIGN, masterCanSign);
        startActivityForResult(intent, 0);
    }

}
