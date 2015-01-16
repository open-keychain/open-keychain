/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;

public class KeyListActivity extends DrawerActivity {

    public static final int REQUEST_CODE_RESULT_TO_LIST = 1;

    ExportHelper mExportHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.nav_keys);

        // if this is the first time show first time activity
        Preferences prefs = Preferences.getPreferences(this);
        if (prefs.isFirstTime()) {
            startActivity(new Intent(this, FirstTimeActivity.class));
            finish();
            return;
        }

        mExportHelper = new ExportHelper(this);

        setContentView(R.layout.key_list_activity);

        Intent data = getIntent();
        // If we got an EXTRA_RESULT in the intent, show the notification
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        }

        // now setup navigation drawer in DrawerActivity...
        activateDrawerNavigation(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_list, menu);

        if (Constants.DEBUG) {
            menu.findItem(R.id.menu_key_list_debug_cons).setVisible(true);
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
                Intent scanQrCode = new Intent(this, QrCodeScanActivity.class);
                scanQrCode.setAction(QrCodeScanActivity.ACTION_SCAN_WITH_RESULT);
                startActivityForResult(scanQrCode, 0);
                return true;

            case R.id.menu_key_list_search_cloud:
                searchCloud();
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

            case R.id.menu_key_list_debug_cons:
                consolidate();
                return true;

            case R.id.menu_key_list_debug_read:
                try {
                    KeychainDatabase.debugBackup(this, true);
                    Notify.showNotify(this, "Restored debug_backup.db", Notify.Style.INFO);
                    getContentResolver().notifyChange(KeychainContract.KeyRings.CONTENT_URI, null);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.showNotify(this, "IO Error " + e.getMessage(), Notify.Style.ERROR);
                }
                return true;

            case R.id.menu_key_list_debug_write:
                try {
                    KeychainDatabase.debugBackup(this, false);
                    Notify.showNotify(this, "Backup to debug_backup.db completed", Notify.Style.INFO);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    Notify.showNotify(this, "IO Error: " + e.getMessage(), Notify.Style.ERROR);
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

    private void searchCloud() {
        Intent importIntent = new Intent(this, ImportKeysActivity.class);
        importIntent.putExtra(ImportKeysActivity.EXTRA_QUERY, (String) null); // hack to show only cloud tab
        startActivity(importIntent);
    }

    private void createKey() {
        Intent intent = new Intent(this, CreateKeyActivity.class);
        startActivityForResult(intent, 0);
    }

    private void consolidate() {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }
                    final ConsolidateResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);
                    if (result == null) {
                        return;
                    }

                    result.createNotify(KeyListActivity.this).show();
                }
            }
        };

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_CONSOLIDATE);

        // fill values for this action
        Bundle data = new Bundle();

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
