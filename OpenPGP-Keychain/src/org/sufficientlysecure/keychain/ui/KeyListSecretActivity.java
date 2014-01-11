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

package org.sufficientlysecure.keychain.ui;

import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.app.ProgressDialog;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class KeyListSecretActivity extends KeyListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyType = Id.type.secret_key;

        setContentView(R.layout.key_list_secret_activity);

        mExportFilename = Constants.path.APP_DIR + "/secexport.asc";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.option.create, 1, R.string.menu_createKey).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(1, Id.menu.option.createExpert, 2, R.string.menu_createKeyExpert).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.option.create: {
            createKey();
            return true;
        }

        case Id.menu.option.createExpert: {
            createKeyExpert();
            return true;
        }

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    private void createKey() {
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
        intent.putExtra(EditKeyActivity.EXTRA_USER_IDS, ""); // show user id view
        startActivityForResult(intent, 0);
    }

    private void createKeyExpert() {
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_CREATE_KEY);
        startActivityForResult(intent, 0);
    }

    void editKey(long masterKeyId, boolean masterCanSign) {
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.setAction(EditKeyActivity.ACTION_EDIT_KEY);
        intent.putExtra(EditKeyActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
        intent.putExtra(EditKeyActivity.EXTRA_MASTER_CAN_SIGN, masterCanSign);
        startActivityForResult(intent, 0);
    }

    private void showPassphraseDialog(final long masterKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passPhrase = PassphraseCacheService.getCachedPassphrase(
                            KeyListSecretActivity.this, masterKeyId);
                    finallyCrossCertKey(masterKeyId, passPhrase);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    KeyListSecretActivity.this, messenger, masterKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    void crossCertKey(long masterKeyId){
        showPassphraseDialog(masterKeyId);
    }

    void finallyCrossCertKey(long masterKeyId, String passphrase){
	    // Send all information needed to service to edit key in other thread
	    Intent intent = new Intent(this, KeychainIntentService.class);

	    intent.setAction(KeychainIntentService.ACTION_CROSSCERTIFY_KEYRING);

	    // fill values for this action
	    Bundle data = new Bundle();

	    data.putLong(KeychainIntentService.CROSSCERTIFY_KEYRING_MASTER_KEY_ID, masterKeyId);
            data.putString(KeychainIntentService.CROSSCERTIFY_KEYRING_CURRENT_PASSPHRASE, passphrase);

	    intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

	    // Message is received after saving is done in ApgService
	    KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
		    R.string.progress_saving, ProgressDialog.STYLE_HORIZONTAL) {
		public void handleMessage(Message message) {
		    // handle messages by standard ApgHandler first
		    super.handleMessage(message);

		    if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
		        //Intent data = new Intent();
		        //setResult(mContext.RESULT_OK, data);
		        //mContext.finish();

		        //remove option to cross certify
		    }
		};
	    };

	    // Create a new Messenger for the communication back
	    Messenger messenger = new Messenger(saveHandler);
	    intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

	    saveHandler.showProgressDialog(this);

	    // start service with intent
	    startService(intent);
		
    }

}
