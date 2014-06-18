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

package org.sufficientlysecure.keychain.service;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.SyncResult;
import android.os.*;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.helper.EmailKeyHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class ContactSyncAdapterService extends Service {

    private class ContactSyncAdapter extends AbstractThreadedSyncAdapter {

        private final AtomicBoolean importDone = new AtomicBoolean(false);

        public ContactSyncAdapter() {
            super(ContactSyncAdapterService.this, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
                                  final SyncResult syncResult) {
            importDone.set(false);
            KeychainApplication.setupAccountAsNeeded(ContactSyncAdapterService.this);
            EmailKeyHelper.importContacts(getContext(), new Messenger(new Handler(Looper.getMainLooper(),
                    new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            Bundle data = msg.getData();
                            switch (msg.arg1) {
                                case KeychainIntentServiceHandler.MESSAGE_OKAY:
                                    Log.d(Constants.TAG, "Syncing... Done.");
                                    synchronized (importDone) {
                                        importDone.set(true);
                                        importDone.notifyAll();
                                    }
                                    return true;
                                case KeychainIntentServiceHandler.MESSAGE_UPDATE_PROGRESS:
                                    if (data.containsKey(KeychainIntentServiceHandler.DATA_PROGRESS) &&
                                            data.containsKey(KeychainIntentServiceHandler.DATA_PROGRESS_MAX)) {
                                        Log.d(Constants.TAG, "Syncing... Progress: " +
                                                data.getInt(KeychainIntentServiceHandler.DATA_PROGRESS) + "/" +
                                                data.getInt(KeychainIntentServiceHandler.DATA_PROGRESS_MAX));
                                        return false;
                                    }
                                default:
                                    Log.d(Constants.TAG, "Syncing... " + msg.toString());
                                    return false;
                            }
                        }
                    })));
            synchronized (importDone) {
                try {
                    if (!importDone.get()) importDone.wait();
                } catch (InterruptedException e) {
                    Log.w(Constants.TAG, e);
                    return;
                }
            }
            ContactHelper.writeKeysToContacts(ContactSyncAdapterService.this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ContactSyncAdapter().getSyncAdapterBinder();
    }
}
