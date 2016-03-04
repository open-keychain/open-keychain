package org.sufficientlysecure.keychain.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.util.Preferences;


public class BackupNotificationAlarmReceiver extends BroadcastReceiver {
    public static final String MASTER_KEY_ID = "masterKeyId";
    public static final String NAME = "name";
    public static final String EMAIL = "email";

    public static final String PERFORM_BACKUP = "performBackup";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Constants.TAG, "Inside backup notification alarm receiver");

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        long masterKeyId = intent.getLongExtra(MASTER_KEY_ID, 0);
        String name = intent.getStringExtra(NAME);
        String email = intent.getStringExtra(EMAIL);

        // check if backup is already created
        String masterKeyIds = Preferences.getPreferences(context).getBackupReminders();
        if (masterKeyIds == null || !masterKeyIds.contains(Long.toString(masterKeyId)) || masterKeyId == 0) {
            // cancel the repeating alarm by same PendingIntent
            Intent intentAlarm = new Intent(context, BackupNotificationAlarmReceiver.class);

            intentAlarm.putExtra(MASTER_KEY_ID, masterKeyId);
            intentAlarm.putExtra(NAME, name);
            intentAlarm.putExtra(EMAIL, email);

            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAlarm,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(pendingIntent);

            return;
        }

        notificationManager.notify(Long.valueOf(masterKeyId).hashCode(), getNotification(context,
                masterKeyId, name, email));
    }

    private Notification getNotification(Context context, long masterKeyId, String name, String email) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setColor(context.getResources().getColor(R.color.primary))
                .setContentTitle(context.getString(R.string.backup_reminder_notif_title))
                .setContentText(context.getString(R.string.backup_reminder_notif_touch_to_backup));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(context.getString(R.string.backup_reminder_notif_title));

        inboxStyle.addLine(name+ " <" +email+ ">");

        // Moves the big view style object into the notification object.
        builder.setStyle(inboxStyle);

        Intent resultIntent = new Intent(context, ViewKeyActivity.class);
        resultIntent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(masterKeyId));
        resultIntent.setAction(PERFORM_BACKUP);

        PendingIntent createBackupPi = PendingIntent.getActivity(
                context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Add backup PI to normal touch
        builder.setContentIntent(createBackupPi);

        // Add backup PI action below text
        builder.addAction(
                R.drawable.ic_view_list_grey_24dp,
                context.getString(R.string.backup_reminder_notif_backup_now),
                createBackupPi
        );

        return builder.build();
    }
}
