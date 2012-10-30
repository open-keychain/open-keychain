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

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.service.PassphraseCacheService;
import org.thialfihar.android.apg.ui.dialog.PassphraseDialogFragment;
import org.thialfihar.android.apg.util.Log;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ExpandableListView;

public class KeyListSecretActivity extends KeyListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyType = Id.type.secret_key;

        setContentView(R.layout.key_list_secret_activity);

        mExportFilename = Constants.path.APP_DIR + "/secexport.asc";

        // mList.setOnChildClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.option.create, 1, R.string.menu_createKey).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.option.create: {
            createKey();
            return true;
        }

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    //
    // public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
    // int childPosition, long id) {
    // mSelectedItem = groupPosition;
    // checkPassPhraseAndEdit();
    // return true;
    // }
    //
    public void checkPassPhraseAndEdit(long keyId) {
        String passPhrase = PassphraseCacheService.getCachedPassphrase(this, keyId);
        if (passPhrase == null) {
            showPassphraseDialog(keyId);
        } else {
            PGPMain.setEditPassPhrase(passPhrase);
            editKey(keyId);
        }
    }

    private void showPassphraseDialog(final long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passPhrase = PassphraseCacheService.getCachedPassphrase(
                            KeyListSecretActivity.this, secretKeyId);
                    PGPMain.setEditPassPhrase(passPhrase);
                    editKey(secretKeyId);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    KeyListSecretActivity.this, messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PGPMain.ApgGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    private void createKey() {
        PGPMain.setEditPassPhrase("");
        Intent intent = new Intent(EditKeyActivity.ACTION_CREATE_KEY);
        startActivityForResult(intent, Id.message.create_key);
    }

    private void editKey(long keyId) {
        Intent intent = new Intent(EditKeyActivity.ACTION_EDIT_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_KEY_ID, keyId);
        startActivityForResult(intent, Id.message.edit_key);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.message.create_key: // intentionally no break
        case Id.message.edit_key: {
            if (resultCode == RESULT_OK) {
                // refreshList();
            }
            break;
        }

        default: {
            break;
        }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
