/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.Date;

/**
 * This service runs in its own process, but is available to all other processes as the main
 * passphrase cache. Use the static methods addCachedPassphrase, addCachedSubkeyPassphrase
 * getCachedPassphrase and getCachedSubkeyPassphrase for convenience.
 *
 * The passphrase cache service works with master key ids. The master key id is used to
 * retrieve relevant keyring info from the database, including passphrases for subkeys.
 * It is assumed that all subkeys from the same master key will use the same passphrase.
 */
public class PassphraseCacheService extends Service {

    public static final String ACTION_PASSPHRASE_CACHE_ADD_MAIN = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_ADD_MAIN";
    public static final String ACTION_PASSPHRASE_CACHE_ADD_SUB = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_ADD_SUB";
    public static final String ACTION_PASSPHRASE_CACHE_GET = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_GET";
    public static final String ACTION_PASSPHRASE_CACHE_CLEAR = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_CLEAR";

    public static final String BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_BROADCAST";

    public static final String EXTRA_TTL = "ttl";
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String EXTRA_SUBKEY_ID = "subkey_id";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_USER_ID = "user_id";

    private static final int DEFAULT_TTL = 0;

    private static final int MSG_PASSPHRASE_CACHE_GET_OKAY = 1;
    private static final int MSG_PASSPHRASE_CACHE_GET_KEY_NOT_FOUND = 2;

    private BroadcastReceiver mIntentReceiver;

    private LongSparseArray<CachedPassphrases> mPassphraseCache = new LongSparseArray<>();

    Context mContext;

    public static class KeyNotFoundException extends Exception {
        public KeyNotFoundException() {
        }

        public KeyNotFoundException(String name) {
            super(name);
        }
    }

    /**
     * This caches a new passphrase in memory by sending a new command to the service. An android
     * service is only run once. Thus, when the service is already started, new commands just add
     * new events to the alarm manager for new passphrases to let them timeout in the future.
     */
    public static void addCachedPassphrase(Context context, long masterKeyId,
                                           Passphrase passphrase,
                                           String primaryUserId,
                                           int timeToLiveSeconds) {
        Log.d(Constants.TAG, "PassphraseCacheService.addCachedPassphrase() for " + masterKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_ADD_MAIN);

        intent.putExtra(EXTRA_TTL, timeToLiveSeconds);
        intent.putExtra(EXTRA_PASSPHRASE, passphrase);
        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_USER_ID, primaryUserId);

        context.startService(intent);
    }

    public static void addCachedSubKeyPassphrase(Context context, long masterKeyId,
                                                 long subKeyId,
                                           Passphrase passphrase,
                                           String primaryUserId,
                                           int timeToLiveSeconds) {
        Log.d(Constants.TAG, "PassphraseCacheService.addCachedSubkeyPassphrase() for " + subKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_ADD_SUB);

        intent.putExtra(EXTRA_TTL, timeToLiveSeconds);
        intent.putExtra(EXTRA_PASSPHRASE, passphrase);
        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_SUBKEY_ID, subKeyId);
        intent.putExtra(EXTRA_USER_ID, primaryUserId);

        context.startService(intent);
    }

    public static void clearCachedPassphrase(Context context, long masterKeyId) {
        Log.d(Constants.TAG, "PassphraseCacheService.clearCachedPassphrase() for " + masterKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);

        intent.putExtra(EXTRA_KEY_ID, masterKeyId);

        context.startService(intent);
    }

    public static void clearCachedSubkeyPassphrase(Context context, long masterKeyId, long subkeyId) {
        Log.d(Constants.TAG, "PassphraseCacheService.clearCachedPassphrase() for " + masterKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);

        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_KEY_ID, subkeyId);
        context.startService(intent);
    }

    public static void clearAllCachedPassphrases(Context context) {
        Log.d(Constants.TAG, "PassphraseCacheService.clearAllCachedPassphrases()");

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);

        context.startService(intent);
    }

    /**
     * Gets a cached passphrase from memory by sending an intent to the service. This method is
     * designed to wait until the service returns the passphrase.
     *
     * @return passphrase or null (if no passphrase is cached for this keyId)
     */
    public static Passphrase getCachedPassphrase(Context context, long masterKeyId) throws KeyNotFoundException {
        return getCachedPassphrase(context, masterKeyId, Constants.key.none);
    }

    public static Passphrase getCachedSubkeyPassphrase(Context context, long masterKeyId, long subkeyId) throws KeyNotFoundException {
        return getCachedPassphrase(context, masterKeyId, subkeyId);
    }

    private static Passphrase getCachedPassphrase(Context context, long masterKeyId, long subKeyId) throws KeyNotFoundException {
        Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphraseImpl() for masterKeyId "
                + masterKeyId + ", subKeyId " + subKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_GET);

        final Object mutex = new Object();
        final Message returnMessage = Message.obtain();

        HandlerThread handlerThread = new HandlerThread("getPassphraseThread");
        handlerThread.start();
        Handler returnHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                // copy over result to handle after mutex.wait
                returnMessage.what = message.what;
                returnMessage.copyFrom(message);
                synchronized (mutex) {
                    mutex.notify();
                }
                // quit handlerThread
                getLooper().quit();
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_SUBKEY_ID, subKeyId);
        intent.putExtra(EXTRA_MESSENGER, messenger);
        // send intent to this service
        context.startService(intent);

        // Wait on mutex until passphrase is returned to handlerThread. Note that this local
        // variable is used in the handler closure above, so it does make sense here!
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            try {
                mutex.wait(3000);
            } catch (InterruptedException e) {
                // don't care
            }
        }

        switch (returnMessage.what) {
            case MSG_PASSPHRASE_CACHE_GET_OKAY:
                Bundle returnData = returnMessage.getData();
                returnData.setClassLoader(context.getClassLoader());
                return returnData.getParcelable(EXTRA_PASSPHRASE);
            case MSG_PASSPHRASE_CACHE_GET_KEY_NOT_FOUND:
                throw new KeyNotFoundException();
            default:
                Log.e(Constants.TAG, "timeout case!");
                throw new KeyNotFoundException("should not happen!");
        }
    }

    /**
     * Internal implementation to get cached passphrase.
     */
    private Passphrase getCachedPassphraseImpl(long masterKeyId, long subKeyId) throws ProviderHelper.NotFoundException {
        // on "none" key, just do nothing
        if (masterKeyId == Constants.key.none) {
            return null;
        }

        // passphrase for symmetric encryption?
        if (masterKeyId == Constants.key.symmetric) {
            Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphraseImpl() for symmetric encryption");
            CachedPassphrases cachedPassphrases = mPassphraseCache.get(Constants.key.symmetric);
            if (cachedPassphrases == null) {
                return null;
            }
            return cachedPassphrases.mKeyringPassphrase;
        }

        // try to get master key id which is used as an identifier for cached passphrases
        Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphraseImpl() for masterKeyId " + masterKeyId);

        // get the type of key (from the database)
        CachedPublicKeyRing keyRing = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);


        // get cached passphrases
        CachedPassphrases cachedPassphrases = mPassphraseCache.get(masterKeyId);
        if (cachedPassphrases == null) {
            Log.d(Constants.TAG, "PassphraseCacheService: keyring passphrase not (yet) cached, returning null");
            // not really an error, just means the passphrase is not cached but not empty either
            return null;
        }

        // return keyring passphrase || subkey passphrase
        if (subKeyId == Constants.key.none) {
            // TODO: wip, implement after adding col to db
            SecretKeyRingType keyType = keyRing.getSecretKeyringType();
            switch (keyType) {
                case PASSPHRASE_EMPTY:
                    return new Passphrase();
                case UNAVAILABLE:
                    throw new ProviderHelper.NotFoundException("secret key data for this keyring is not available");
            }
            return cachedPassphrases.mKeyringPassphrase;
        } else {
            SecretKeyType subKeyType = keyRing.getSecretKeyType(subKeyId);
            switch (subKeyType) {
                case PASSPHRASE_EMPTY:
                    return new Passphrase();
                case GNU_DUMMY:
                    throw new ProviderHelper.NotFoundException("secret key for stripped subkey is not available");
                case UNAVAILABLE:
                    throw new ProviderHelper.NotFoundException("secret key data for this subkey is not available");
            }
            return cachedPassphrases.mSubkeyPassphrase;
        }

    }

    /**
     * Register BroadcastReceiver that is unregistered when service is destroyed. This
     * BroadcastReceiver hears on intents with ACTION_PASSPHRASE_CACHE_SERVICE to then timeout
     * specific passphrases in memory.
     */
    private void registerReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    Log.d(Constants.TAG, "PassphraseCacheService: Received broadcast...");

                    if (action.equals(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE)) {
                        long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                        removeTimeoutedPassphrase(keyId);
                    }

                    if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                        removeScreenLockPassphrases();
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mIntentReceiver, filter);
        }
    }

    /**
     * Build pending intent that is executed by alarm manager to time out a specific passphrase
     */
    private static PendingIntent buildIntent(Context context, long referenceKeyId) {
        Intent intent = new Intent(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE);
        intent.putExtra(EXTRA_KEY_ID, referenceKeyId);
        // request code should be unique for each PendingIntent, thus keyId is used
        return PendingIntent.getBroadcast(context, (int) referenceKeyId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Executed when service is started by intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "PassphraseCacheService.onStartCommand()");

        if (intent == null || intent.getAction() == null) {
            updateService();
            return START_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_PASSPHRASE_CACHE_ADD_MAIN: {
                long masterKeyId = intent.getLongExtra(EXTRA_KEY_ID, Constants.key.none);
                long timeoutTtl = intent.getIntExtra(EXTRA_TTL, DEFAULT_TTL);

                Passphrase passphrase = intent.getParcelableExtra(EXTRA_PASSPHRASE);
                String primaryUserID = intent.getStringExtra(EXTRA_USER_ID);

                Log.d(Constants.TAG,
                        "PassphraseCacheService: Received ACTION_PASSPHRASE_CACHE_ADD_MAIN intent in onStartCommand() with masterkeyId: "
                                + masterKeyId + ", ttl: " + timeoutTtl + ", usrId: " + primaryUserID
                );

                CachedPassphrases cachedPassphrases;
                if (timeoutTtl == 0L) {
                    cachedPassphrases = CachedPassphrases.getPassphraseLock(passphrase, primaryUserID);
                } else if (timeoutTtl >= Integer.MAX_VALUE) {
                    cachedPassphrases = CachedPassphrases.getPassphraseNoTimeout(passphrase, primaryUserID);
                } else {
                    cachedPassphrases = CachedPassphrases.getPassphraseTtlTimeout(passphrase, primaryUserID, timeoutTtl);

                    long triggerTime = new Date().getTime() + (timeoutTtl * 1000);
                    // register new alarm with keyId for this passphrase
                    AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(this, masterKeyId));
                }

                mPassphraseCache.put(masterKeyId, cachedPassphrases);
                break;
            }

            case ACTION_PASSPHRASE_CACHE_ADD_SUB: {
                long masterKeyId = intent.getLongExtra(EXTRA_KEY_ID, Constants.key.none);
                long subKeyId = intent.getLongExtra(EXTRA_SUBKEY_ID, Constants.key.none);
                long timeoutTtl = intent.getIntExtra(EXTRA_TTL, DEFAULT_TTL);
                Passphrase subkeyPassphrase = intent.getParcelableExtra(EXTRA_PASSPHRASE);
                String primaryUserID = intent.getStringExtra(EXTRA_USER_ID);

                Log.d(Constants.TAG,
                        "PassphraseCacheService: Received ACTION_PASSPHRASE_CACHE_ADD_SUB intent in onStartCommand() with masterkeyId: "
                                + masterKeyId + ", subkeyId: " + subKeyId +  ", ttl: " + timeoutTtl + ", usrId: " + primaryUserID
                );

                CachedPassphrases cachedPassphrases = mPassphraseCache.get(masterKeyId);
                if (cachedPassphrases == null) {
                    // keyring uses an empty password & thus is not cached yet
                    // create a new cache object for it
                    Passphrase keyringPassphrase = new Passphrase();
                    if (timeoutTtl == 0L) {
                        cachedPassphrases = CachedPassphrases.getPassphraseLock(keyringPassphrase, subkeyPassphrase, primaryUserID);
                    } else if (timeoutTtl >= Integer.MAX_VALUE) {
                        cachedPassphrases = CachedPassphrases.getPassphraseNoTimeout(keyringPassphrase, subkeyPassphrase, primaryUserID);
                    } else {
                        cachedPassphrases = CachedPassphrases.getPassphraseTtlTimeout(keyringPassphrase, subkeyPassphrase, primaryUserID, timeoutTtl);
                        long triggerTime = new Date().getTime() + (timeoutTtl * 1000);
                        // register new alarm with keyId for this passphrase
                        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                        am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(this, masterKeyId));
                    }
                } else {
                    cachedPassphrases.mSubkeyPassphrase = subkeyPassphrase;
                }

                mPassphraseCache.put(masterKeyId, cachedPassphrases);
                break;
            }

            case ACTION_PASSPHRASE_CACHE_GET: {
                long masterKeyId = intent.getLongExtra(EXTRA_KEY_ID, Constants.key.none);
                long subKeyId = intent.getLongExtra(EXTRA_SUBKEY_ID, Constants.key.none);
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);

                Message msg = Message.obtain();
                try {
                    Passphrase passphrase = getCachedPassphraseImpl(masterKeyId, subKeyId);
                    msg.what = MSG_PASSPHRASE_CACHE_GET_OKAY;
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_PASSPHRASE, passphrase);
                    msg.setData(bundle);
                } catch (ProviderHelper.NotFoundException e) {
                    Log.e(Constants.TAG, "PassphraseCacheService: Passphrases for unknown key was requested!");
                    msg.what = MSG_PASSPHRASE_CACHE_GET_KEY_NOT_FOUND;
                }

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(Constants.TAG, "PassphraseCacheService: Sending message failed", e);
                }
                break;
            }
            case ACTION_PASSPHRASE_CACHE_CLEAR: {
                AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

                if (intent.hasExtra(EXTRA_KEY_ID)) {
                    long keyId = intent.getLongExtra(EXTRA_KEY_ID, 0L);
                    // Stop specific ttl alarm and
                    am.cancel(buildIntent(this, keyId));
                    mPassphraseCache.delete(keyId);
                } else if (intent.hasExtra(EXTRA_SUBKEY_ID)) {
                    long keyId = intent.getLongExtra(EXTRA_KEY_ID, 0L);
                    mPassphraseCache.get(keyId).mSubkeyPassphrase = null;
                } else {
                    // Stop all ttl alarms
                    for (int i = 0; i < mPassphraseCache.size(); i++) {
                        CachedPassphrases cachedPassphrases = mPassphraseCache.valueAt(i);
                        if (cachedPassphrases.mTimeoutMode == TimeoutMode.TTL) {
                            am.cancel(buildIntent(this, mPassphraseCache.keyAt(i)));
                        }
                    }
                    mPassphraseCache.clear();
                }
                break;
            }
            default: {
                Log.e(Constants.TAG, "PassphraseCacheService: Intent or Intent Action not supported!");
                break;
            }
        }

        updateService();

        return START_STICKY;
    }

    /** Called when one specific passphrase for keyId timed out. */
    private void removeTimeoutedPassphrase(long keyId) {

        CachedPassphrases cPass = mPassphraseCache.get(keyId);
        if (cPass != null) {
            if (cPass.mKeyringPassphrase != null) {
                // clean internal char[] from memory!
                cPass.mKeyringPassphrase.removeFromMemory();
            }
            // remove passphrase object
            mPassphraseCache.remove(keyId);
        }

        Log.d(Constants.TAG, "PassphraseCacheService Timeout of keyId " + keyId + ", removed from memory!");

        updateService();
    }

    private void removeScreenLockPassphrases() {

        for (int i = 0; i < mPassphraseCache.size(); ) {
            CachedPassphrases cPass = mPassphraseCache.valueAt(i);
            if (cPass.mTimeoutMode == TimeoutMode.LOCK) {
                // remove passphrase object
                mPassphraseCache.removeAt(i);
                continue;
            }
            // only do this if we didn't remove at, which continues loop by reducing size!
            i += 1;
        }

        Log.d(Constants.TAG, "PassphraseCacheService Removing all cached-until-lock passphrases from memory!");

        updateService();
    }

    private void updateService() {
        if (mPassphraseCache.size() > 0) {
            startForeground(Constants.Notification.PASSPHRASE_CACHE, getNotification());
        } else {
            // stop whole service if no cached passphrases remaining
            Log.d(Constants.TAG, "PassphraseCacheService: No passphrases remaining in memory, stopping service!");
            stopForeground(true);
        }
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setColor(getResources().getColor(R.color.primary))
                .setContentTitle(getResources().getQuantityString(R.plurals.passp_cache_notif_n_keys,
                        mPassphraseCache.size(), mPassphraseCache.size()))
                .setContentText(getString(R.string.passp_cache_notif_touch_to_clear));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(getString(R.string.passp_cache_notif_keys));

        // Moves events into the big view
        for (int i = 0; i < mPassphraseCache.size(); i++) {
            inboxStyle.addLine(mPassphraseCache.valueAt(i).mPrimaryUserId);
        }

        // Moves the big view style object into the notification object.
        builder.setStyle(inboxStyle);

        Intent intent = new Intent(getApplicationContext(), PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);
        PendingIntent clearCachePi = PendingIntent.getService(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Add cache clear PI to normal touch
        builder.setContentIntent(clearCachePi);

        // Add clear PI action below text
        builder.addAction(
                R.drawable.ic_close_white_24dp,
                getString(R.string.passp_cache_notif_clear),
                clearCachePi
        );

        return builder.build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "PassphraseCacheService, onCreate()");

        registerReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "PassphraseCacheService, onDestroy()");

        unregisterReceiver(mIntentReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class PassphraseCacheBinder extends Binder {
        public PassphraseCacheService getService() {
            return PassphraseCacheService.this;
        }
    }

    private final IBinder mBinder = new PassphraseCacheBinder();

    private enum TimeoutMode {
        NEVER, TTL, LOCK
    }

    private static class CachedPassphrases {
        private String mPrimaryUserId;
        private Passphrase mKeyringPassphrase;
        private Passphrase mSubkeyPassphrase;
        private TimeoutMode mTimeoutMode;
        private Long mTimeoutTime;

        private CachedPassphrases(Passphrase keyringPassphrase, Passphrase subkeyPassphrase, String primaryUserId, TimeoutMode timeoutMode, Long timeoutTime) {
            mKeyringPassphrase = keyringPassphrase;
            mSubkeyPassphrase = subkeyPassphrase;
            mPrimaryUserId = primaryUserId;
            mTimeoutMode = timeoutMode;
            mTimeoutTime = timeoutTime;
        }

        static CachedPassphrases getPassphraseNoTimeout(Passphrase keyringPassphrase, String primaryUserId) {
            return new CachedPassphrases(keyringPassphrase, null, primaryUserId, TimeoutMode.NEVER, null);
        }

        static CachedPassphrases getPassphraseTtlTimeout(Passphrase keyringPassphrase, String primaryUserId, long timeoutTime) {
            return new CachedPassphrases(keyringPassphrase, null, primaryUserId, TimeoutMode.TTL, timeoutTime);
        }

        static CachedPassphrases getPassphraseLock(Passphrase keyringPassphrase, String primaryUserId) {
            return new CachedPassphrases(keyringPassphrase, null, primaryUserId, TimeoutMode.LOCK, null);
        }

        static CachedPassphrases getPassphraseNoTimeout(Passphrase keyringPassphrase, Passphrase subkeyPassphrase, String primaryUserId) {
            return new CachedPassphrases(keyringPassphrase, subkeyPassphrase, primaryUserId, TimeoutMode.NEVER, null);
        }

        static CachedPassphrases getPassphraseTtlTimeout(Passphrase keyringPassphrase, Passphrase subkeyPassphrase, String primaryUserId, long timeoutTime) {
            return new CachedPassphrases(keyringPassphrase, subkeyPassphrase, primaryUserId, TimeoutMode.TTL, timeoutTime);
        }

        static CachedPassphrases getPassphraseLock(Passphrase keyringPassphrase, Passphrase subkeyPassphrase, String primaryUserId) {
            return new CachedPassphrases(keyringPassphrase, subkeyPassphrase, primaryUserId, TimeoutMode.LOCK, null);
        }
    }

}
