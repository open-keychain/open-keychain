/*
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

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.PGPHelper;
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
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

import com.google.zxing.integration.android.IntentIntegrator;

public class SecretKeyListActivity extends KeyListActivity implements OnChildClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mExportFilename = Constants.path.APP_DIR + "/secexport.asc";
        mKeyType = Id.type.secret_key;
        super.onCreate(savedInstanceState);
        mList.setOnChildClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(3, Id.menu.option.search, 0, R.string.menu_search)
                .setIcon(R.drawable.ic_menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(1, Id.menu.option.create, 1, R.string.menu_createKey).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, Id.menu.option.import_keys, 2, R.string.menu_importKeys).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, Id.menu.option.export_keys, 3, R.string.menu_exportKeys).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // TODO: user id? menu.setHeaderTitle("Key");
            menu.add(0, Id.menu.edit, 0, R.string.menu_editKey);
            menu.add(0, Id.menu.export, 1, R.string.menu_exportKey);
            menu.add(0, Id.menu.delete, 2, R.string.menu_deleteKey);
            menu.add(0, Id.menu.share_qr_code, 2, R.string.menu_share);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuItem.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        if (type != ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return super.onContextItemSelected(menuItem);
        }

        switch (menuItem.getItemId()) {
        case Id.menu.edit: {
            mSelectedItem = groupPosition;
            checkPassPhraseAndEdit();
            return true;
        }

        case Id.menu.share_qr_code: {
            mSelectedItem = groupPosition;

            long keyId = ((KeyListAdapter) mList.getExpandableListAdapter())
                    .getGroupId(mSelectedItem);
            // String msg = keyId + "," + PGPHelper.getFingerPrint(keyId);
            String msg = PGPHelper.getPubkeyAsArmoredString(this, keyId);

            new IntentIntegrator(this).shareText(msg);
        }

        default: {
            return super.onContextItemSelected(menuItem);
        }
        }
    }

    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
            int childPosition, long id) {
        mSelectedItem = groupPosition;
        checkPassPhraseAndEdit();
        return true;
    }

    public void checkPassPhraseAndEdit() {
        long keyId = ((KeyListAdapter) mList.getExpandableListAdapter()).getGroupId(mSelectedItem);
        String passPhrase = PassphraseCacheService.getCachedPassphrase(this, keyId);
        if (passPhrase == null) {
            showPassphraseDialog(keyId);
        } else {
            PGPMain.setEditPassPhrase(passPhrase);
            editKey();
        }
    }

    private void showPassphraseDialog(final long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passPhrase = PassphraseCacheService.getCachedPassphrase(
                            SecretKeyListActivity.this, secretKeyId);
                    PGPMain.setEditPassPhrase(passPhrase);
                    editKey();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    SecretKeyListActivity.this, messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PGPMain.GeneralException e) {
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

    private void editKey() {
        long keyId = ((KeyListAdapter) mList.getExpandableListAdapter()).getGroupId(mSelectedItem);
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
                refreshList();
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
