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

package org.thialfihar.android.apg;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class SecretKeyListActivity extends KeyListActivity implements OnChildClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mExportFilename = Constants.path.app_dir + "/secexport.asc";
        mKeyType = Id.type.secret_key;
        super.onCreate(savedInstanceState);
        mList.setOnChildClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.import_keys, 0, R.string.menu_importKeys)
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, Id.menu.option.export_keys, 1, R.string.menu_exportKeys)
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(1, Id.menu.option.create, 2, R.string.menu_createKey)
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(3, Id.menu.option.search, 3, R.string.menu_search)
                .setIcon(android.R.drawable.ic_menu_search);
        menu.add(3, Id.menu.option.preferences, 4, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(3, Id.menu.option.about, 5, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
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
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // TODO: user id? menu.setHeaderTitle("Key");
            menu.add(0, Id.menu.edit, 0, R.string.menu_editKey);
            menu.add(0, Id.menu.export, 1, R.string.menu_exportKey);
            menu.add(0, Id.menu.delete, 2, R.string.menu_deleteKey);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.pass_phrase: {
                long keyId = ((KeyListAdapter) mList.getExpandableListAdapter()).getGroupId(mSelectedItem);
                return AskForSecretKeyPassPhrase.createDialog(this, keyId, this);
            }

            default: {
                return super.onCreateDialog(id);
            }
        }
    }

    public void checkPassPhraseAndEdit() {
        long keyId = ((KeyListAdapter) mList.getExpandableListAdapter()).getGroupId(mSelectedItem);
        String passPhrase = Apg.getCachedPassPhrase(keyId);
        if (passPhrase == null) {
            showDialog(Id.dialog.pass_phrase);
        } else {
            Apg.setEditPassPhrase(passPhrase);
            editKey();
        }
    }

    @Override
    public void passPhraseCallback(long keyId, String passPhrase) {
        super.passPhraseCallback(keyId, passPhrase);
        Apg.setEditPassPhrase(passPhrase);
        editKey();
    }

    private void createKey() {
        Apg.setEditPassPhrase("");
        Intent intent = new Intent(this, EditKeyActivity.class);
        startActivityForResult(intent, Id.message.create_key);
    }

    private void editKey() {
        long keyId = ((KeyListAdapter) mList.getExpandableListAdapter()).getGroupId(mSelectedItem);
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.putExtra(Apg.EXTRA_KEY_ID, keyId);
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
