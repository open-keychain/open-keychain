package org.sufficientlysecure.keychain.keysync;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.os.CancellationSignal;

import androidx.work.Worker;
import org.sufficientlysecure.keychain.Constants.NotificationChannels;
import org.sufficientlysecure.keychain.Constants.NotificationIds;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import org.sufficientlysecure.keychain.operations.KeySyncOperation;
import org.sufficientlysecure.keychain.operations.KeySyncParcel;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
import org.sufficientlysecure.keychain.util.ResourceUtils;
import timber.log.Timber;


public class KeyserverSyncWorker extends Worker {
    private CancellationSignal cancellationSignal = new CancellationSignal();

    @NonNull
    @Override
    public WorkerResult doWork() {
        KeyWritableRepository keyWritableRepository = KeyWritableRepository.create(getApplicationContext());

        Timber.d("Starting key sync…");
        Progressable notificationProgressable = notificationShowForProgress();
        KeySyncOperation keySync = new KeySyncOperation(getApplicationContext(), keyWritableRepository, notificationProgressable, cancellationSignal);
        ImportKeyResult result = keySync.execute(KeySyncParcel.createRefreshOutdated(), CryptoInputParcel.createCryptoInputParcel());
        return handleUpdateResult(result);
    }

    /**
     * Since we're returning START_REDELIVER_INTENT in onStartCommand, we need to remember to call
     * stopSelf(int) to prevent the Intent from being redelivered if our work is already done
     *
     * @param result
     *         result of keyserver sync
     */
    private WorkerResult handleUpdateResult(ImportKeyResult result) {
        if (result.isPending()) {
            Timber.d("Orbot required for sync but not running, attempting to start");
            // result is pending due to Orbot not being started
            // try to start it silently, if disabled show notifications
            new OrbotHelper.SilentStartManager() {
                @Override
                protected void onOrbotStarted() {
                }

                @Override
                protected void onSilentStartDisabled() {
                    OrbotRequiredDialogActivity.showOrbotRequiredNotification(getApplicationContext());
                }
            }.startOrbotAndListen(getApplicationContext(), false);
            return WorkerResult.RETRY;
        } else if (isStopped()) {
            Timber.d("Keyserver sync cancelled");
            return WorkerResult.FAILURE;
        } else {
            Timber.d("Keyserver sync completed: Updated: %d, Failed: %d", result.mUpdatedKeys, result.mBadKeys);
            return WorkerResult.SUCCESS;
        }
    }

    private Progressable notificationShowForProgress() {
        final Context context = getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return null;
        }

        createNotificationChannelsIfNecessary(context, notificationManager);

        NotificationCompat.Builder builder = new Builder(context, NotificationChannels.KEYSERVER_SYNC)
                .setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setLargeIcon(ResourceUtils.getDrawableAsNotificationBitmap(context, R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.notify_title_keysync))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setTimeoutAfter(5000)
                .setVibrate(null)
                .setSound(null)
                .setProgress(0, 0, true);

        return new Progressable() {
            @Override
            public void setProgress(String message, int current, int total) {
                setProgress(current, total);
            }

            @Override
            public void setProgress(int resourceId, int current, int total) {
                setProgress(current, total);
            }

            @Override
            public void setProgress(int current, int total) {
                if (total == 0) {
                    notificationManager.cancel(NotificationIds.KEYSERVER_SYNC);
                    return;
                }

                builder.setProgress(total, current, false);
                if (current == total) {
                    builder.setContentTitle(context.getString(R.string.notify_title_keysync_finished, total));
                    builder.setContentText(null);
                } else {
                    builder.setContentText(context.getString(R.string.notify_content_keysync, current, total));
                }
                notificationManager.notify(NotificationIds.KEYSERVER_SYNC, builder.build());
            }

            @Override
            public void setPreventCancel() {
            }
        };
    }

    private void createNotificationChannelsIfNecessary(Context context,
            NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notify_channel_keysync);
            NotificationChannel channel = new NotificationChannel(
                    NotificationChannels.KEYSERVER_SYNC, name, NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        cancellationSignal.cancel();
    }
}
