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

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends SherlockActivity {

    public void manageKeysOnClick(View view) {
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(new Intent(this, KeyListPublicActivity.class), 0);
    }

    public void myKeysOnClick(View view) {
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(new Intent(this, KeyListSecretActivity.class), 0);
    }

    public void encryptOnClick(View view) {
        Intent intent = new Intent(MainActivity.this, EncryptActivity.class);
        intent.setAction(EncryptActivity.ACTION_ENCRYPT);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, 0);
    }

    public void decryptOnClick(View view) {
        Intent intent = new Intent(MainActivity.this, DecryptActivity.class);
        intent.setAction(DecryptActivity.ACTION_DECRYPT);
        // used instead of startActivity set actionbar based on callingPackage
        startActivityForResult(intent, 0);
    }

    public void scanQrcodeOnClick(View view) {
        Intent intent = new Intent(this, ImportFromQRCodeActivity.class);
        intent.setAction(ImportFromQRCodeActivity.IMPORT_FROM_QR_CODE);
        startActivityForResult(intent, Id.request.import_from_qr_code);
    }

    public void helpOnClick(View view) {
        startActivity(new Intent(this, HelpActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.preferences, 0, R.string.menu_preferences)
                .setIcon(R.drawable.ic_menu_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case Id.menu.option.preferences:
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;

        default:
            break;

        }
        return false;
    }

}