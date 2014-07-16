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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.Menu;
import android.view.MenuItem;

import com.devspark.appmsg.AppMsg;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.choice.algorithm;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;

public class KeyListActivity extends DrawerActivity {

    ExportHelper mExportHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preferences prefs = Preferences.getPreferences(this);
        if (prefs.getFirstTime()) {
            prefs.setFirstTime(false);
            Intent intent = new Intent(this, FirstTimeActivity.class);
            startActivity(intent);
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
            case R.id.menu_key_list_import:
                importKeys();
                return true;

            case R.id.menu_key_list_create:
                createKey();
                return true;

            case R.id.menu_key_list_create_expert:
                createKeyExpert();
                return true;

            case R.id.menu_key_list_export:
                mExportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, true);
                return true;

            case R.id.menu_key_list_debug_read:
                try {
                    KeychainDatabase.debugRead(this);
                    AppMsg.makeText(this, "Restored from backup", AppMsg.STYLE_CONFIRM).show();
                    getContentResolver().notifyChange(KeychainContract.KeyRings.CONTENT_URI, null);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    AppMsg.makeText(this, "IO Error: " + e.getMessage(), AppMsg.STYLE_ALERT).show();
                }
                return true;

            case R.id.menu_key_list_debug_write:
                try {
                    KeychainDatabase.debugWrite(this);
                    AppMsg.makeText(this, "Backup successful", AppMsg.STYLE_CONFIRM).show();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IO Error", e);
                    AppMsg.makeText(this, "IO Error: " + e.getMessage(), AppMsg.STYLE_ALERT).show();
                }
                return true;

            case R.id.menu_key_list_debug_first_time:
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

    private void createKeyExpert() {
        Intent intent = new Intent(this, KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SAVE_KEYRING);

        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);
                Bundle data = message.getData();
                // OtherHelper.logDebugBundle(data, "message reply");
            }
        };

        // fill values for this action
        Bundle data = new Bundle();

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));
        parcel.mAddSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));
        parcel.mAddUserIds.add("swagerinho");
        parcel.mNewPassphrase = "swag";

        // get selected key entries
        data.putParcelable(KeychainIntentService.SAVE_KEYRING_PARCEL, parcel);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(this);

        startService(intent);
    }
}