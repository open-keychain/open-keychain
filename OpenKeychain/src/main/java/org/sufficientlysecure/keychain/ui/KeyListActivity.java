/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.io.IOException;

public class KeyListActivity extends DrawerActivity {

    ExportHelper mExportHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if this is the first time show first time activity
        Preferences prefs = Preferences.getPreferences(this);
        if (prefs.isFirstTime()) {
            startActivity(new Intent(this, FirstTimeActivity.class));
            finish();
            return;
        }

        mExportHelper = new ExportHelper(this);

        setContentView(R.layout.key_list_activity);

        // now setup navigation drawer in DrawerActivity...
        setupDrawerNavigation(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_list, menu);

        if (Constants.DEBUG) {
            menu.findItem(R.id.menu_key_list_debug_read).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_write).setVisible(true);
            menu.findItem(R.id.menu_key_list_debug_first_time).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_key_list_add:
                importKeys();
                return true;

            case R.id.menu_key_list_create:
                createKey();
                return true;

            case R.id.menu_key_list_import_existing_key:
                Intent intentImportExisting = new Intent(this, ImportKeysActivity.class);
                intentImportExisting.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
                startActivityForResult(intentImportExisting, 0);
                return true;

            case R.id.menu_key_list_export:
                mExportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, true);
                return true;

            case R.id.menu_key_list_debug_read:
                try {
                    KeychainDatabase.debugRead(this);
                    Notify.showNotify(this, "Restored Notify.Style backup", Notify.Style.INFO);
                    getContentResolver().notifyChange(KeychainContract.KeyRings.CONTENT_URI, null);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.showNotify(this, "IO Notify.Style: " + e.getMessage(), Notify.Style.ERROR);
                }
                return true;

            case R.id.menu_key_list_debug_write:
                try {
                    KeychainDatabase.debugWrite(this);
                    Notify.showNotify(this, "Backup Notify.Style", Notify.Style.INFO);
                } catch(IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.showNotify(this, "IO Notify.Style: " + e.getMessage(), Notify.Style.ERROR);
                }
                return true;

            case R.id.menu_key_list_debug_first_time:
                Preferences prefs = Preferences.getPreferences(this);
                prefs.setFirstTime(true);
                Intent intent = new Intent(this, FirstTimeActivity.class);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void importKeys() {
        Intent intent = new Intent(this, ImportKeysActivity.class);
        startActivityForResult(intent, 0);
    }

    private void createKey() {
        Intent intent = new Intent(this, CreateKeyActivity.class);
        startActivity(intent);
    }

}
