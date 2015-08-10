package org.sufficientlysecure.keychain.service;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyserverSyncAdapterService extends Service {

    private static final String ACTION_IGNORE_TOR = "ignore_tor";
    private static final String ACTION_UPDATE_ALL = "update_all";
    private static final String ACTION_SYNC_NOW = "sync_now";
    private static final String ACTION_DISMISS_NOTIFICATION = "cancel_sync";
    private static final String ACTION_START_ORBOT = "start_orbot";
    private static final String ACTION_CANCEL = "cancel";

    private AtomicBoolean mCancelled = new AtomicBoolean(false);

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        Log.e("PHILIP", "Sync adapter service starting" + intent.getAction());

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(Constants.Notification.KEYSERVER_SYNC_FAIL_ORBOT);
        switch (intent.getAction()) {
            case ACTION_CANCEL: {
                mCancelled.set(true);
                break;
            }
            // the reason for the separation betweyeen SYNC_NOW and UPDATE_ALL is so that starting
            // the sync directly from the notification is possible while the screen is on with
            // UPDATE_ALL, but a postponed sync is only started if screen is off
            case ACTION_SYNC_NOW: {
                // this checks for screen on/off before sync, and postpones the sync if on
                ContentResolver.requestSync(
                        new Account(Constants.ACCOUNT_NAME, Constants.ACCOUNT_TYPE),
                        Constants.PROVIDER_AUTHORITY,
                        new Bundle()
                );
                break;
            }
            case ACTION_UPDATE_ALL: {
                // does not check for screen on/off
                asyncKeyUpdate(this, new CryptoInputParcel());
                break;
            }
            case ACTION_IGNORE_TOR: {
                asyncKeyUpdate(this, new CryptoInputParcel(ParcelableProxy.getForNoProxy()));
                break;
            }
            case ACTION_START_ORBOT: {
                Intent startOrbot = new Intent(this, OrbotRequiredDialogActivity.class);
                startOrbot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startOrbot.putExtra(OrbotRequiredDialogActivity.EXTRA_START_ORBOT, true);
                Messenger messenger = new Messenger(
                        new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                switch (msg.what) {
                                    case OrbotRequiredDialogActivity.MESSAGE_ORBOT_STARTED: {
                                        Log.e("PHILIP", "orbot activity returned");
                                        asyncKeyUpdate(KeyserverSyncAdapterService.this,
                                                new CryptoInputParcel());
                                        break;
                                    }
                                    case OrbotRequiredDialogActivity.MESSAGE_ORBOT_IGNORE: {
                                        asyncKeyUpdate(KeyserverSyncAdapterService.this,
                                                new CryptoInputParcel(
                                                        ParcelableProxy.getForNoProxy()));
                                        break;
                                    }
                                    case OrbotRequiredDialogActivity.MESSAGE_DIALOG_CANCEL: {
                                        // just stop service
                                        stopSelf();
                                        break;
                                    }
                                }
                            }
                        }
                );
                startOrbot.putExtra(OrbotRequiredDialogActivity.EXTRA_MESSENGER, messenger);
                startActivity(startOrbot);
                break;
            }
            case ACTION_DISMISS_NOTIFICATION: {
                // notification is dismissed at the beginning
                stopSelf(startId);
                break;
            }
        }
        return START_NOT_STICKY;
    }

    private class KeyserverSyncAdapter extends AbstractThreadedSyncAdapter {

        public KeyserverSyncAdapter() {
            super(KeyserverSyncAdapterService.this, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {
            Log.d(Constants.TAG, "Performing a keyserver sync!");

            PowerManager pm = (PowerManager) KeyserverSyncAdapterService.this
                    .getSystemService(Context.POWER_SERVICE);
            @SuppressWarnings("deprecation") // our min is API 15, deprecated only in 20
                    boolean isScreenOn = pm.isScreenOn();

            if (!isScreenOn) {
                Intent serviceIntent = new Intent(KeyserverSyncAdapterService.this,
                        KeyserverSyncAdapterService.class);
                serviceIntent.setAction(ACTION_UPDATE_ALL);
                startService(serviceIntent);
            } else {
                postponeSync();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new KeyserverSyncAdapter().getSyncAdapterBinder();
    }

    private void handleUpdateResult(ImportKeyResult result) {
        if (result.isPending()) {
            Log.e(Constants.TAG, "Keyserver sync pending result: "
                    + result.getRequiredInputParcel().mType);
            // result is pending due to Orbot not being started
            // try to start it silently, if disabled show notificationaa
            new OrbotHelper.SilentStartManager() {
                @Override
                protected void onOrbotStarted() {
                    // retry the update
                    asyncKeyUpdate(KeyserverSyncAdapterService.this,
                            new CryptoInputParcel());
                }

                @Override
                protected void onSilentStartDisabled() {
                    // show notification
                    NotificationManager manager =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(Constants.Notification.KEYSERVER_SYNC_FAIL_ORBOT,
                            getOrbotNoification(KeyserverSyncAdapterService.this));
                }
            }.startOrbotAndListen(this, false);
        } else if (isUpdateCancelled()) {
            Log.d(Constants.TAG, "Keyserver sync cancelled");
            postponeSync();
        } else {
            Log.d(Constants.TAG, "Keyserver sync completed: Updated: " + result.mUpdatedKeys
                    + " Failed: " + result.mBadKeys);
            stopSelf();
        }
    }

    private void postponeSync() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(this, KeyserverSyncAdapterService.class);
        serviceIntent.setAction(ACTION_SYNC_NOW);
        PendingIntent pi = PendingIntent.getService(this, 0, serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 30 * 1000,
                pi
        );
    }

    private void asyncKeyUpdate(final Context context,
                                final CryptoInputParcel cryptoInputParcel) {
        Log.e("PHILIP", "async key update starting");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ImportKeyResult result = updateKeysFromKeyserver(context, cryptoInputParcel);
                handleUpdateResult(result);
            }
        }).start();
    }

    private synchronized ImportKeyResult updateKeysFromKeyserver(final Context context,
                                                    final CryptoInputParcel cryptoInputParcel) {
        mCancelled.set(false);

        ArrayList<ParcelableKeyRing> keyList = getKeysToUpdate(context);

        if (isUpdateCancelled()) { // if we've already been cancelled
            return new ImportKeyResult(OperationResult.RESULT_CANCELLED,
                    new OperationResult.OperationLog());
        }

        if (cryptoInputParcel.getParcelableProxy() == null) {
            // no explicit proxy, retrieve from preferences. Check if we should do a staggered sync
            if (Preferences.getPreferences(context).getProxyPrefs().torEnabled) {
                return staggeredUpdate(context, keyList, cryptoInputParcel);
            } else {
                return directUpdate(context, keyList, cryptoInputParcel);
            }
        } else {
            return directUpdate(context, keyList, cryptoInputParcel);
        }
    }

    private ImportKeyResult directUpdate(Context context, ArrayList<ParcelableKeyRing> keyList,
                                         CryptoInputParcel cryptoInputParcel) {
        Log.d(Constants.TAG, "Starting normal update");
        ImportOperation importOp = new ImportOperation(context, new ProviderHelper(context), null);
        return importOp.execute(
                new ImportKeyringParcel(keyList,
                        Preferences.getPreferences(context).getPreferredKeyserver()),
                cryptoInputParcel
        );
    }


    /**
     * will perform a staggered update of user's keys using delays to ensure new Tor circuits, as
     * performed by parcimonie. Relevant issue and method at:
     * https://github.com/open-keychain/open-keychain/issues/1337
     *
     * @return result of the sync
     */
    private ImportKeyResult staggeredUpdate(Context context, ArrayList<ParcelableKeyRing> keyList,
                                            CryptoInputParcel cryptoInputParcel) {
        Log.d(Constants.TAG, "Starting staggered update");
        // assuming maxCircuitDirtiness is 10min in Tor
        // final int MAX_CIRCUIT_DIRTINESS = (int) TimeUnit.MINUTES.toSeconds(10);
        // TODO: PHILIP remove after testing
        final int MAX_CIRCUIT_DIRTINESS = (int) TimeUnit.MINUTES.toSeconds(1);
        // final int WEEK_IN_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
        final int WEEK_IN_SECONDS = 0;
        ImportOperation.KeyImportAccumulator accumulator
                = new ImportOperation.KeyImportAccumulator(keyList.size(), null);
        for (ParcelableKeyRing keyRing : keyList) {
            int waitTime;
            int staggeredTime = new Random().nextInt(1 + 2 * (WEEK_IN_SECONDS / keyList.size()));
            if (staggeredTime >= MAX_CIRCUIT_DIRTINESS) {
                waitTime = staggeredTime;
            } else {
                waitTime = MAX_CIRCUIT_DIRTINESS + new Random().nextInt(MAX_CIRCUIT_DIRTINESS);
            }
            Log.d(Constants.TAG, "Updating key with fingerprint " + keyRing.mExpectedFingerprint +
                    " with a wait time of " + waitTime + "s");
            try {
                Thread.sleep(waitTime * 1000);
            } catch (InterruptedException e) {
                Log.e(Constants.TAG, "Exception during sleep between key updates", e);
                // skip this one
                continue;
            }
            ArrayList<ParcelableKeyRing> keyWrapper = new ArrayList<>();
            keyWrapper.add(keyRing);
            if (isUpdateCancelled()) {
                return new ImportKeyResult(ImportKeyResult.RESULT_CANCELLED,
                        new OperationResult.OperationLog());
            }
            ImportKeyResult result =
                    new ImportOperation(context, new ProviderHelper(context), null, mCancelled)
                            .execute(
                                    new ImportKeyringParcel(
                                            keyWrapper,
                                            Preferences.getPreferences(context)
                                                    .getPreferredKeyserver()
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

    /**
     * 1. Get keys which have been updated recently and therefore do not need to
     * be updated now
     * 2. Get list of all keys and filter out ones that don't need to be updated
     * 3. Return keys to be updated
     *
     * @return list of keys that require update
     */
    private ArrayList<ParcelableKeyRing> getKeysToUpdate(Context context) {

        // 1. Get keys which have been updated recently and don't need to updated now
        final int INDEX_UPDATED_KEYS_MASTER_KEY_ID = 0;
        final int INDEX_LAST_UPDATED = 1;

        // all time in seconds not milliseconds
        // TODO: PHILIP correct TIME_MAX after testing
        // final long TIME_MAX = TimeUnit.DAYS.toSeconds(7);
        final long TIME_MAX = 1;
        final long CURRENT_TIME = GregorianCalendar.getInstance().getTimeInMillis() / 1000;
        Log.e("PHILIP", "week: " + TIME_MAX + " current: " + CURRENT_TIME);
        Cursor updatedKeysCursor = context.getContentResolver().query(
                KeychainContract.UpdatedKeys.CONTENT_URI,
                new String[]{
                        KeychainContract.UpdatedKeys.MASTER_KEY_ID,
                        KeychainContract.UpdatedKeys.LAST_UPDATED
                },
                "? - " + KeychainContract.UpdatedKeys.LAST_UPDATED + " < " + TIME_MAX,
                new String[]{"" + CURRENT_TIME},
                null
        );

        ArrayList<Long> ignoreMasterKeyIds = new ArrayList<>();
        while (updatedKeysCursor.moveToNext()) {
            long masterKeyId = updatedKeysCursor.getLong(INDEX_UPDATED_KEYS_MASTER_KEY_ID);
            Log.d(Constants.TAG, "Keyserver sync: {" + masterKeyId + "} last updated at {"
                    + updatedKeysCursor.getLong(INDEX_LAST_UPDATED) + "}s");
            ignoreMasterKeyIds.add(masterKeyId);
        }
        updatedKeysCursor.close();

        // 2. Make a list of public keys which should be updated
        final int INDEX_MASTER_KEY_ID = 0;
        final int INDEX_FINGERPRINT = 1;
        Cursor keyCursor = context.getContentResolver().query(
                KeychainContract.KeyRings.buildUnifiedKeyRingsUri(),
                new String[]{
                        KeychainContract.KeyRings.MASTER_KEY_ID,
                        KeychainContract.KeyRings.FINGERPRINT
                },
                null,
                null,
                null
        );

        if (keyCursor == null) {
            return new ArrayList<>();
        }

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        while (keyCursor.moveToNext()) {
            long keyId = keyCursor.getLong(INDEX_MASTER_KEY_ID);
            if (ignoreMasterKeyIds.contains(keyId)) {
                continue;
            }
            String fingerprint = KeyFormattingUtils
                    .convertFingerprintToHex(keyCursor.getBlob(INDEX_FINGERPRINT));
            String hexKeyId = KeyFormattingUtils
                    .convertKeyIdToHex(keyId);
            // we aren't updating from keybase as of now
            keyList.add(new ParcelableKeyRing(fingerprint, hexKeyId, null));
        }
        keyCursor.close();

        return keyList;
    }

    private boolean isUpdateCancelled() {
        return mCancelled.get();
    }

    /**
     * will cancel an update already in progress
     *
     * @param context used to send an Intent to the service requesting cancellation.
     */
    public static void cancelUpdates(Context context) {
        Intent intent = new Intent(context, KeyserverSyncAdapterService.class);
        intent.setAction(ACTION_CANCEL);
        context.startService(intent);
    }

    // TODO: PHILIP remove!
    @Override
    public void onDestroy() {
        Log.e("PHILIP", "onDestroy");
        super.onDestroy();
    }

    private Notification getOrbotNoification(Context context) {
        // TODO: PHILIP work in progress
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setLargeIcon(getBitmap(R.drawable.ic_launcher, context))
                .setContentTitle(context.getString(R.string.keyserver_sync_orbot_notif_title))
                .setContentText(context.getString(R.string.keyserver_sync_orbot_notif_msg))
                .setAutoCancel(true);

        // In case the user decides to not use tor
        Intent ignoreTorIntent = new Intent(context, KeyserverSyncAdapterService.class);
        ignoreTorIntent.setAction(ACTION_IGNORE_TOR);
        PendingIntent ignoreTorPi = PendingIntent.getService(
                context,
                0, // security not issue since we're giving this pending intent to Notification Manager
                ignoreTorIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        builder.addAction(R.drawable.abc_ic_clear_mtrl_alpha,
                context.getString(R.string.keyserver_sync_orbot_notif_ignore),
                ignoreTorPi);

        Intent startOrbotIntent = new Intent(context, KeyserverSyncAdapterService.class);
        startOrbotIntent.setAction(ACTION_START_ORBOT);
        PendingIntent startOrbotPi = PendingIntent.getService(
                context,
                0, // security not issue since we're giving this pending intent to Notification Manager
                startOrbotIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        builder.addAction(R.drawable.abc_ic_commit_search_api_mtrl_alpha,
                context.getString(R.string.keyserver_sync_orbot_notif_start),
                startOrbotPi
        );
        builder.setContentIntent(startOrbotPi);

        return builder.build();
    }

    // from de.azapps.mirakel.helper.Helpers from https://github.com/MirakelX/mirakel-android
    private Bitmap getBitmap(int resId, Context context) {
        int mLargeIconWidth = (int) context.getResources().getDimension(
                android.R.dimen.notification_large_icon_width);
        int mLargeIconHeight = (int) context.getResources().getDimension(
                android.R.dimen.notification_large_icon_height);
        Drawable d;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // noinspection deprecation (can't help it at this api level)
            d = context.getResources().getDrawable(resId);
        } else {
            d = context.getDrawable(resId);
        }
        if (d == null) {
            return null;
        }
        Bitmap b = Bitmap.createBitmap(mLargeIconWidth, mLargeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, mLargeIconWidth, mLargeIconHeight);
        d.draw(c);
        return b;
    }
}