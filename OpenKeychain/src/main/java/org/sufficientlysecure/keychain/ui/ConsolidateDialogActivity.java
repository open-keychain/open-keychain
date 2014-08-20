/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;

/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 */
public class ConsolidateDialogActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        consolidateRecovery();

    }

    private void consolidateRecovery() {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    /* don't care about the results (for now?)

                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }
                    final ConsolidateResult result =
                            returnData.getParcelable(KeychainIntentService.RESULT_CONSOLIDATE);
                    if (result == null) {
                        return;
                    }
                    result.createNotify(ConsolidateDialogActivity.this).show();
                    */

                    ConsolidateDialogActivity.this.finish();
                }
            }
        };

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_CONSOLIDATE);

        // fill values for this action
        Bundle data = new Bundle();
        data.putBoolean(KeychainIntentService.CONSOLIDATE_RECOVERY, true);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);

    }

}
