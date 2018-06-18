package org.sufficientlysecure.keychain.keysync;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import androidx.work.Worker;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.NotificationChannels;
import org.sufficientlysecure.keychain.Constants.NotificationIds;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.KeyMetadataDao;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.ResourceUtils;
import timber.log.Timber;


public class KeyserverSyncWorker extends Worker {
    public static final String DATA_IS_FOREGROUND = "foreground";
    public static final String DATA_IS_FORCE = "force";

    // time since last update after which a key should be updated again, in s
    private static final long KEY_STALE_THRESHOLD_MILLIS =
            Constants.DEBUG_KEYSERVER_SYNC ? 1 : TimeUnit.DAYS.toMillis(7);
    // Time taken by Orbot before a new circuit is created
    private static final int ORBOT_CIRCUIT_TIMEOUT_SECONDS =
            Constants.DEBUG_KEYSERVER_SYNC ? 2 : (int) TimeUnit.MINUTES.toSeconds(10);

    private AtomicBoolean cancellationSignal = new AtomicBoolean(false);
    private KeyMetadataDao keyMetadataDao;
    private KeyWritableRepository keyWritableRepository;
    private Preferences preferences;

    @NonNull
    @Override
    public WorkerResult doWork() {
        keyMetadataDao = KeyMetadataDao.create(getApplicationContext());
        keyWritableRepository = KeyWritableRepository.create(getApplicationContext());
        preferences = Preferences.getPreferences(getApplicationContext());

        boolean isForeground = getInputData().getBoolean(DATA_IS_FOREGROUND, false);
        boolean isForceUpdate = getInputData().getBoolean(DATA_IS_FORCE, false);

        Timber.d("Starting key sync…");
        Progressable notificationProgressable = notificationShowForProgress(isForeground);
        ImportKeyResult result = updateKeysFromKeyserver(getApplicationContext(), isForceUpdate, notificationProgressable);
        return handleUpdateResult(result);
    }

    private ImportKeyResult updateKeysFromKeyserver(Context context, boolean isForceUpdate,
            Progressable notificationProgressable) {
        long staleKeyThreshold = System.currentTimeMillis() - (isForceUpdate ? 0 : KEY_STALE_THRESHOLD_MILLIS);
        List<byte[]> staleKeyFingerprints =
                keyMetadataDao.getFingerprintsForKeysOlderThan(staleKeyThreshold, TimeUnit.MILLISECONDS);
        List<ParcelableKeyRing> staleKeyParcelableKeyRings = fingerprintListToParcelableKeyRings(staleKeyFingerprints);

        if (isStopped()) { // if we've already been cancelled
            return new ImportKeyResult(OperationResult.RESULT_CANCELLED, new OperationResult.OperationLog());
        }

        // no explicit proxy, retrieve from preferences. Check if we should do a staggered sync
        CryptoInputParcel cryptoInputParcel = CryptoInputParcel.createCryptoInputParcel();

        ImportKeyResult importKeyResult;
        if (preferences.getParcelableProxy().isTorEnabled()) {
            importKeyResult = staggeredUpdate(context, staleKeyParcelableKeyRings, cryptoInputParcel);
        } else {
            importKeyResult =
                    directUpdate(context, staleKeyParcelableKeyRings, cryptoInputParcel, notificationProgressable);
        }
        return importKeyResult;
    }

    private List<ParcelableKeyRing> fingerprintListToParcelableKeyRings(List<byte[]> staleKeyFingerprints) {
        ArrayList<ParcelableKeyRing> result = new ArrayList<>(staleKeyFingerprints.size());
        for (byte[] fingerprint : staleKeyFingerprints) {
            Timber.d("Keyserver sync: Updating %s", KeyFormattingUtils.beautifyKeyId(fingerprint));
            result.add(ParcelableKeyRing.createFromReference(fingerprint, null, null, null));
        }
        return result;
    }

    private ImportKeyResult directUpdate(Context context, List<ParcelableKeyRing> keyList,
            CryptoInputParcel cryptoInputParcel, Progressable notificationProgressable) {
        Timber.d("Starting normal update");
        ImportOperation importOp = new ImportOperation(context, keyWritableRepository, notificationProgressable);
        return importOp.execute(
                ImportKeyringParcel.createImportKeyringParcel(keyList, preferences.getPreferredKeyserver()),
                cryptoInputParcel
        );
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

    /**
     * will perform a staggered update of user's keys using delays to ensure new Tor circuits, as
     * performed by parcimonie. Relevant issue and method at:
     * https://github.com/open-keychain/open-keychain/issues/1337
     *
     * @return result of the sync
     */
    private ImportKeyResult staggeredUpdate(Context context, List<ParcelableKeyRing> keyList,
            CryptoInputParcel cryptoInputParcel) {
        Timber.d("Starting staggered update");
        // final int WEEK_IN_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
        // we are limiting our randomness to ORBOT_CIRCUIT_TIMEOUT_SECONDS for now
        final int WEEK_IN_SECONDS = 0;

        ImportOperation.KeyImportAccumulator accumulator
                = new ImportOperation.KeyImportAccumulator(keyList.size(), null);

        // so that the first key can be updated without waiting. This is so that there isn't a
        // large gap between a "Start Orbot" notification and the next key update
        boolean first = true;

        for (ParcelableKeyRing keyRing : keyList) {
            int waitTime;
            int staggeredTime = new Random().nextInt(1 + 2 * (WEEK_IN_SECONDS / keyList.size()));
            if (staggeredTime >= ORBOT_CIRCUIT_TIMEOUT_SECONDS) {
                waitTime = staggeredTime;
            } else {
                waitTime = ORBOT_CIRCUIT_TIMEOUT_SECONDS
                        + new Random().nextInt(1 + ORBOT_CIRCUIT_TIMEOUT_SECONDS);
            }

            if (first) {
                waitTime = 0;
                first = false;
            }

            Timber.d("Updating key with a wait time of %d seconds", waitTime);
            try {
                Thread.sleep(waitTime * 1000);
            } catch (InterruptedException e) {
                Timber.e(e, "Exception during sleep between key updates");
                // skip this one
                continue;
            }
            ArrayList<ParcelableKeyRing> keyWrapper = new ArrayList<>();
            keyWrapper.add(keyRing);
            if (isStopped()) {
                return new ImportKeyResult(ImportKeyResult.RESULT_CANCELLED,
                        new OperationResult.OperationLog());
            }
            ImportKeyResult result =
                    new ImportOperation(context, keyWritableRepository, null, cancellationSignal)
                            .execute(
                                    ImportKeyringParcel.createImportKeyringParcel(
                                            keyWrapper,
                                            preferences.getPreferredKeyserver()
                                    ),
                                    cryptoInputParcel
                            );
            if (result.isPending()) {
                return result;
            }
            accumulator.accumulateKeyImport(result);
        }
        return accumulator.getConsolidatedResult();
    }

    private Progressable notificationShowForProgress(boolean isForeground) {
        final Context context = getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return null;
        }

        createNotificationChannelsIfNecessary(context, notificationManager);

        NotificationCompat.Builder builder = new Builder(context, isForeground ?
                NotificationChannels.KEYSERVER_SYNC_FOREGROUND : NotificationChannels.KEYSERVER_SYNC)
                .setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setLargeIcon(ResourceUtils.getDrawableAsNotificationBitmap(context, R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.notify_title_keysync))
                .setPriority(isForeground ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_MIN)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notify_channel_keysync_foreground);
            NotificationChannel channel = new NotificationChannel(
                    NotificationChannels.KEYSERVER_SYNC_FOREGROUND, name, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        cancellationSignal.set(true);
    }
}
