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

import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.remote.RegisteredAppsListActivity;

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
        Intent intent = new Intent(this, ImportKeysActivity.class);
        startActivityForResult(intent, 0);
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
        menu.add(0, Id.menu.option.crypto_consumers, 0, R.string.menu_api_app_settings)
                .setIcon(R.drawable.ic_menu_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case Id.menu.option.preferences:
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;

        case Id.menu.option.crypto_consumers:
            startActivity(new Intent(this, RegisteredAppsListActivity.class));
            return true;

        default:
            break;

        }
        return false;
    }

}