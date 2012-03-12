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

package org.apg.ui;

import org.apg.Apg;
import org.apg.Constants;
import org.apg.Id;
import org.apg.Constants.path;
import org.apg.Id.menu;
import org.apg.Id.request;
import org.apg.Id.type;
import org.apg.Id.menu.option;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.apg.R;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class PublicKeyListActivity extends KeyListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mExportFilename = Constants.path.APP_DIR + "/pubexport.asc";
        mKeyType = Id.type.public_key;
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, Id.menu.option.search, 0, R.string.menu_search)
                .setIcon(R.drawable.ic_menu_search_holo_light)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(1, Id.menu.option.scanQRCode, 1, R.string.menu_scanQRCode)
        // .setIcon(R.drawable.ic_suggestions_add)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(1, Id.menu.option.key_server, 2, R.string.menu_keyServer)
                .setIcon(R.drawable.ic_menu_find_holo_light)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, Id.menu.option.import_keys, 3, R.string.menu_importKeys)
        // .setIcon(R.drawable.ic_suggestions_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, Id.menu.option.export_keys, 4, R.string.menu_exportKeys)
        // .setIcon(R.drawable.ic_menu_share_holo_light)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.option.key_server: {
            startActivity(new Intent(this, KeyServerQueryActivity.class));

            return true;
        }
        case Id.menu.option.scanQRCode: {
            Intent intent = new Intent(this, ImportFromQRCodeActivity.class);
            intent.setAction(Apg.Intent.IMPORT_FROM_QR_CODE);
            startActivityForResult(intent, Id.request.import_from_qr_code);

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
            menu.add(0, Id.menu.export, 0, R.string.menu_exportKey);
            menu.add(0, Id.menu.delete, 1, R.string.menu_deleteKey);
            menu.add(0, Id.menu.update, 1, R.string.menu_updateKey);
            menu.add(0, Id.menu.exportToServer, 1, R.string.menu_exportKeyToServer);
            menu.add(0, Id.menu.signKey, 1, R.string.menu_signKey);
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

        case Id.menu.exportToServer: {
            mSelectedItem = groupPosition;
            final int keyRingId = mListAdapter.getKeyRingId(groupPosition);

            Intent intent = new Intent(this, SendKeyActivity.class);
            intent.setAction(Apg.Intent.EXPORT_KEY_TO_SERVER);
            intent.putExtra(Apg.EXTRA_KEY_ID, keyRingId);
            startActivityForResult(intent, Id.request.export_to_server);

            return true;
        }

        case Id.menu.signKey: {
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

            Intent intent = new Intent(this, SignKeyActivity.class);
            intent.putExtra(Apg.EXTRA_KEY_ID, keyId);
            startActivity(intent);

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
            if (resultCode == RESULT_CANCELED || data == null
                    || data.getStringExtra(Apg.EXTRA_TEXT) == null) {
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
