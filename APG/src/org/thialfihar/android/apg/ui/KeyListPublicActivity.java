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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class KeyListPublicActivity extends KeyListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyType = Id.type.public_key;

        setContentView(R.layout.key_list_public_activity);

        mExportFilename = Constants.path.APP_DIR + "/pubexport.asc";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.option.key_server, 1, R.string.menu_keyServer)
                .setIcon(R.drawable.ic_menu_search_list)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(1, Id.menu.option.import_from_qr_code, 2, R.string.menu_importFromQrCode)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.import_from_nfc, 3, R.string.menu_importFromNfc)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.option.key_server: {
            startActivityForResult(new Intent(this, KeyServerQueryActivity.class), 0);

            return true;
        }
        case Id.menu.option.import_from_file: {
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_FROM_FILE);
            startActivityForResult(intentImportFromFile, 0);

            return true;
        }

        case Id.menu.option.import_from_qr_code: {
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_FROM_QR_CODE);
            startActivityForResult(intentImportFromFile, Id.request.import_from_qr_code);

            return true;
        }

        case Id.menu.option.import_from_nfc: {
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_FROM_NFC);
            startActivityForResult(intentImportFromFile, 0);

            return true;
        }

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    // @Override
    // protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // switch (requestCode) {
    // case Id.request.look_up_key_id: {
    // if (resultCode == RESULT_CANCELED || data == null
    // || data.getStringExtra(KeyServerQueryActivity.RESULT_EXTRA_TEXT) == null) {
    // return;
    // }
    //
    // Intent intent = new Intent(this, KeyListPublicActivity.class);
    // intent.setAction(KeyListPublicActivity.ACTION_IMPORT);
    // intent.putExtra(KeyListPublicActivity.EXTRA_TEXT,
    // data.getStringExtra(KeyListActivity.EXTRA_TEXT));
    // handleActions(intent);
    // break;
    // }
    //
    // default: {
    // super.onActivityResult(requestCode, resultCode, data);
    // break;
    // }
    // }
    // }
}
