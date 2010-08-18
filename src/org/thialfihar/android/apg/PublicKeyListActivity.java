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

import org.bouncycastle2.openpgp.PGPPublicKeyRing;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class PublicKeyListActivity extends KeyListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mExportFilename = Constants.path.app_dir + "/pubexport.asc";
        mKeyType = Id.type.public_key;
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.import_keys, 0, R.string.menu_importKeys)
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, Id.menu.option.export_keys, 1, R.string.menu_exportKeys)
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(1, Id.menu.option.search, 2, R.string.menu_search)
                .setIcon(android.R.drawable.ic_menu_search);
        menu.add(1, Id.menu.option.preferences, 3, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(1, Id.menu.option.about, 4, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // TODO: user id? menu.setHeaderTitle("Key");
            menu.add(0, Id.menu.export, 0, R.string.menu_exportKey);
            menu.add(0, Id.menu.delete, 1, R.string.menu_deleteKey);
            menu.add(0, Id.menu.update, 1, R.string.menu_updateKey);
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
            case Id.menu.update: {
                mSelectedItem = groupPosition;
                final int keyRingId = mListAdapter.getKeyRingId(groupPosition);
                long keyId = 0;
                Object keyRing = Apg.getKeyRing(keyRingId);
                if (keyRing != null && keyRing instanceof PGPPublicKeyRing) {
                    keyId = Apg.getMasterKey((PGPPublicKeyRing) keyRing).getKeyID();
                }
                if (keyId == 0) {
                    // this shouldn't happen
                    return true;
                }

                Intent intent = new Intent(this, KeyServerQueryActivity.class);
                intent.setAction(Apg.Intent.LOOK_UP_KEY_ID_AND_RETURN);
                intent.putExtra(Apg.EXTRA_KEY_ID, keyId);
                startActivityForResult(intent, Id.request.look_up_key_id);
                return true;
            }

            default: {
                return super.onContextItemSelected(menuItem);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.look_up_key_id: {
                if (resultCode == RESULT_CANCELED || data == null ||
                    data.getStringExtra(Apg.EXTRA_TEXT) == null) {
                    return;
                }

                Intent intent = new Intent(this, PublicKeyListActivity.class);
                intent.setAction(Apg.Intent.IMPORT);
                intent.putExtra(Apg.EXTRA_TEXT, data.getStringExtra(Apg.EXTRA_TEXT));
                handleIntent(intent);
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }
}
