/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.NotificationChannelManager;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.SettingsActivity;
import org.sufficientlysecure.keychain.util.ContactHelper;
import timber.log.Timber;


public class ContactSyncAdapterService extends Service {

    private static final int NOTIFICATION_ID_SYNC_SETTINGS = 13;

    private class ContactSyncAdapter extends AbstractThreadedSyncAdapter {

//        private final AtomicBoolean importDone = new AtomicBoolean(false);

        public ContactSyncAdapter() {
            super(ContactSyncAdapterService.this, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
                                  final SyncResult syncResult) {
            Timber.d("Performing a contact sync!");

            new ContactHelper(ContactSyncAdapterService.this).writeKeysToContacts();

//            importKeys();
        }

        @Override
        public void onSecurityException(Account account, Bundle extras, String authority, SyncResult syncResult) {
            super.onSecurityException(account, extras, authority, syncResult);

            // deactivate sync
            ContentResolver.setSyncAutomatically(account, authority, false);

            NotificationChannelManager.getInstance(getContext()).createNotificationChannelsIfNecessary();

            // show notification linking to sync settings
            Intent resultIntent = new Intent(ContactSyncAdapterService.this, SettingsActivity.class);
            resultIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    SettingsActivity.SyncPrefsFragment.class.getName());
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            ContactSyncAdapterService.this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(ContactSyncAdapterService.this, NotificationChannelManager.PERMISSION_REQUESTS)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_notify_24dp)
                            .setColor(getResources().getColor(R.color.primary))
                            .setContentTitle(getString(R.string.sync_notification_permission_required_title))
                            .setContentText(getString(R.string.sync_notification_permission_required_text))
                            .setContentIntent(resultPendingIntent);
            NotificationManagerCompat.from(ContactSyncAdapterService.this)
                    .notify(NOTIFICATION_ID_SYNC_SETTINGS, mBuilder.build());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ContactSyncAdapter().getSyncAdapterBinder();
    }

    public static void requestContactsSync() {
        // if user has disabled automatic sync, do nothing
        boolean isSyncEnabled = ContentResolver.getSyncAutomatically(new Account
                (Constants.ACCOUNT_NAME, Constants.ACCOUNT_TYPE), ContactsContract.AUTHORITY);

        if (!isSyncEnabled) {
            return;
        }

        Bundle extras = new Bundle();
        // no need to wait, do it immediately
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                new Account(Constants.ACCOUNT_NAME, Constants.ACCOUNT_TYPE),
                ContactsContract.AUTHORITY,
                extras);
    }

    public static void enableContactsSync(Context context) {
        Account account = KeychainApplication.createAccountIfNecessary(context);
        if (account == null) {
            return;
        }

        ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
    }

// TODO: Import is currently disabled, until we implement proper origin management
//    private static void importKeys() {
//            importDone.set(false);
//            KeychainApplication.setupAccountAsNeeded(ContactSyncAdapterService.this);
//            EmailKeyHelper.importContacts(getContext(), new Messenger(new Handler(Looper.getMainLooper(),
//                    new Handler.Callback() {
//                        @Override
//                        public boolean handleMessage(Message msg) {
//                            Bundle data = msg.getInputData();
//                            switch (msg.arg1) {
//                                case KeychainIntentServiceHandler.MESSAGE_OKAY:
//                                    Log.d(Constants.TAG, "Syncing... Done.");
//                                    synchronized (importDone) {
//                                        importDone.set(true);
//                                        importDone.notifyAll();
//                                    }
//                                    return true;
//                                case KeychainIntentServiceHandler.MESSAGE_UPDATE_PROGRESS:
//                                    if (data.containsKey(KeychainIntentServiceHandler.DATA_PROGRESS) &&
//                                            data.containsKey(KeychainIntentServiceHandler.DATA_PROGRESS_MAX)) {
//                                        Log.d(Constants.TAG, "Syncing... Progress: " +
//                                                data.getInt(KeychainIntentServiceHandler.DATA_PROGRESS) + "/" +
//                                                data.getInt(KeychainIntentServiceHandler.DATA_PROGRESS_MAX));
//                                        return false;
//                                    }
//                                default:
//                                    Log.d(Constants.TAG, "Syncing... " + msg.toString());
//                                    return false;
//                            }
//                        }
//                    })));
//            synchronized (importDone) {
//                try {
//                    if (!importDone.get()) importDone.wait();
//                } catch (InterruptedException e) {
//                    Log.w(Constants.TAG, e);
//                    return;
//                }
//            }
//    }

}
