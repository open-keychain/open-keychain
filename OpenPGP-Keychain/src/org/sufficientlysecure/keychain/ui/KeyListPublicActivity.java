/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;

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
        menu.add(1, Id.menu.option.key_server, 1, R.string.menu_key_server)
                .setIcon(R.drawable.ic_menu_search_list)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(1, Id.menu.option.import_from_qr_code, 2, R.string.menu_import_from_qr_code)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.import_from_nfc, 3, R.string.menu_import_from_nfc)
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
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE);
            startActivityForResult(intentImportFromFile, 0);

            return true;
        }

        case Id.menu.option.import_from_qr_code: {
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_QR_CODE);
            startActivityForResult(intentImportFromFile, Id.request.import_from_qr_code);

            return true;
        }

        case Id.menu.option.import_from_nfc: {
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            intentImportFromFile.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_NFC);
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
