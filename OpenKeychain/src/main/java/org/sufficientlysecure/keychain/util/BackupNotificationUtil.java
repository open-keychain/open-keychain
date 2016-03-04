package org.sufficientlysecure.keychain.util;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.receiver.BackupNotificationAlarmReceiver;

import java.util.Arrays;
import java.util.List;

/**
 * This class is used to setup a backup reminder for every new key created by user
 */
public class BackupNotificationUtil {

    public static void setBackupReminder(@NonNull Context context, long masterKeyId, String name, String email) {
        Preferences.getPreferences(context).setBackupReminder(masterKeyId);
        Intent intentAlarm = new Intent(context, BackupNotificationAlarmReceiver.class);

        intentAlarm.putExtra(BackupNotificationAlarmReceiver.MASTER_KEY_ID, masterKeyId);
        intentAlarm.putExtra(BackupNotificationAlarmReceiver.NAME, name);
        intentAlarm.putExtra(BackupNotificationAlarmReceiver.EMAIL, email);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                AlarmManager.INTERVAL_DAY * 5, pendingIntent);
    }

    public static void removeBackupReminder(@NonNull Context context, long masterKeyId) {
        Preferences.getPreferences(context).removeBackupReminder(masterKeyId);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Long.valueOf(masterKeyId).hashCode());
    }

    public static void removeAllBackupReminders(@NonNull Context context) {
        Preferences preferences = Preferences.getPreferences(context);
        List<String> masterKeyIds = Arrays.asList(preferences.getBackupReminders().split(","));
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        for (String masterKeyId : masterKeyIds) {
            notificationManager.cancel(Long.valueOf(masterKeyId).hashCode());
        }
        Preferences.getPreferences(context).removeAllBackupReminders();
    }
}
