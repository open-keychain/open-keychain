package org.sufficientlysecure.keychain.service;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyserverSyncAdapterService extends Service {

    private static final String ACTION_IGNORE_TOR = "ignore_tor";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("PHILIP", "Sync adapter service starting");
        switch (intent.getAction()) {
            case ACTION_IGNORE_TOR: {
                updateKeysFromKeyserver(this,
                        new CryptoInputParcel(ParcelableProxy.getForNoProxy()));
                break;
            }
        }
        // TODO: correct flag?
        return START_NOT_STICKY;
    }

    private static AtomicBoolean sCancelled = new AtomicBoolean(false);

    private class KeyserverSyncAdapter extends AbstractThreadedSyncAdapter {

        public KeyserverSyncAdapter() {
            super(KeyserverSyncAdapterService.this, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {
            Log.d(Constants.TAG, "Performing a keyserver sync!");

            updateKeysFromKeyserver(KeyserverSyncAdapterService.this, new CryptoInputParcel());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new KeyserverSyncAdapter().getSyncAdapterBinder();
    }

    public static void updateKeysFromKeyserver(Context context,
                                               CryptoInputParcel cryptoInputParcel) {
        /**
         * 1. Get keys which have been updated recently and therefore do not need to
         * be updated now
         * 2. Get list of all keys and filter out ones that don't need to be updated
         * 3. Update the remaining keys.
         * At any time, if the operation is to be cancelled, the sCancelled AtomicBoolean may be set
         */

        // 1. Get keys which have been updated recently and don't need to updated now
        final int INDEX_UPDATED_KEYS_MASTER_KEY_ID = 0;
        final int INDEX_LAST_UPDATED = 1;

        final long TIME_DAY = TimeUnit.DAYS.toSeconds(1);
        final long CURRENT_TIME = GregorianCalendar.getInstance().get(GregorianCalendar.SECOND);
        Log.e("PHILIP", "day: " + TIME_DAY);
        Cursor updatedKeysCursor = context.getContentResolver().query(
                KeychainContract.UpdatedKeys.CONTENT_URI,
                new String[]{
                        KeychainContract.UpdatedKeys.MASTER_KEY_ID,
                        KeychainContract.UpdatedKeys.LAST_UPDATED
                },
                "? - " + KeychainContract.UpdatedKeys.LAST_UPDATED + " < " + TIME_DAY,
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
            return;
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

        if (sCancelled.get()) {
            // if we've already been cancelled
            return;
        }

        // 3. Actually update the keys
        Log.e("PHILIP", keyList.toString());
        ImportOperation importOp = new ImportOperation(context, new ProviderHelper(context), null);
        ImportKeyResult result = importOp.execute(
                new ImportKeyringParcel(keyList,
                        Preferences.getPreferences(context).getPreferredKeyserver()),
                cryptoInputParcel
        );
        if (result.isPending()) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            manager.notify(Constants.Notification.KEYSERVER_SYNC_FAIL_ORBOT,
                    getOrbotNoification(context));

            Log.d(Constants.TAG, "Keyserver sync failed due to pending result " +
                    result.getRequiredInputParcel().mType);
        } else {
            Log.d(Constants.TAG, "Background keyserver sync completed: new " + result.mNewKeys
                    + " updated " + result.mUpdatedKeys + " bad " + result.mBadKeys);
        }
    }

    public static void preventAndCancelUpdates() {
        // TODO: PHILIP uncomment!
        // sCancelled.set(true);
    }

    public static void allowUpdates() {
        sCancelled.set(false);
    }

    private static Notification getOrbotNoification(Context context) {
        // TODO: PHILIP work in progress
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setLargeIcon(getBitmap(R.drawable.ic_launcher, context))
                .setContentTitle(context.getString(R.string.keyserver_sync_orbot_notif_title))
                .setContentText(context.getString(R.string.keyserver_sync_orbot_notif_msg));

        // In case the user decides to not use tor
        Intent ignoreTorIntent = new Intent(context, KeyserverSyncAdapterService.class);
        ignoreTorIntent.setAction(ACTION_IGNORE_TOR);
        PendingIntent ignoreTorPi = PendingIntent.getService(
                context,
                Constants.Notification.KEYSERVER_SYNC_FAIL_ORBOT,
                ignoreTorIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        builder.addAction(R.drawable.abc_ic_clear_mtrl_alpha,
                context.getString(R.string.keyserver_sync_orbot_notif_ignore),
                ignoreTorPi);

        return builder.build();
    }

    // from de.azapps.mirakel.helper.Helpers from https://github.com/MirakelX/mirakel-android
    private static Bitmap getBitmap(int resId, Context context) {
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
