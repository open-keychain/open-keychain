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

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class KeyListPublicActivity extends KeyActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_list_public_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.key_list_public, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            return true;
        case R.id.menu_key_list_public_import:
            Intent intentImportFromFile = new Intent(this, ImportKeysActivity.class);
            startActivityForResult(intentImportFromFile, Id.request.import_from_qr_code);

            return true;
        case R.id.menu_key_list_public_export:
            showExportKeysDialog(null, Id.type.public_key, Constants.path.APP_DIR
                    + "/pubexport.asc");

            return true;
        default:
            return super.onOptionsItemSelected(item);
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
