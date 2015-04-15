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
import android.os.Build;
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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.Date;

/**
 * This service runs in its own process, but is available to all other processes as the main
 * passphrase cache. Use the static methods addCachedPassphrase and getCachedPassphrase for
 * convenience.
 *
 * The passphrase cache service always works with both a master key id and a subkey id. The master
 * key id is always used to retrieve relevant info from the database, while the subkey id is used
 * to determine the type behavior (regular passphrase, empty passphrase, stripped key,
 * divert-to-card) for the specific key requested.
 *
 * Caching behavior for subkeys depends on the cacheSubs preference:
 *
 * - If cacheSubs is NOT set, passphrases will be cached and retrieved by master key id. The
 * checks for special subkeys will still be done, but otherwise it is assumed that all subkeys
 * from the same master key will use the same passphrase. This can lead to bad passphrase
 * errors if two subkeys are encrypted differently. This is the default behavior.
 *
 * - If cacheSubs IS set, passphrases will be cached per subkey id. This means that if a keyring
 * has two subkeys for different purposes, passphrases will be cached independently and the
 * user will be asked for a passphrase once per subkey even if it is the same one. This mode
 * of operation is more precise, since we can assume that all passphrases returned from cache
 * will be correct without fail. Since keyrings with differently encrypted subkeys are a very
 * rare occurrence, and caching by keyring is what the user expects in the vast majority of
 * cases, this is not the default behavior.
 *
 */
public class PassphraseCacheService extends Service {

    public static final String ACTION_PASSPHRASE_CACHE_ADD = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_ADD";
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

    private static final long DEFAULT_TTL = 15;

    private static final int NOTIFICATION_ID = 1;

    private static final int MSG_PASSPHRASE_CACHE_GET_OKAY = 1;
    private static final int MSG_PASSPHRASE_CACHE_GET_KEY_NOT_FOUND = 2;

    private BroadcastReceiver mIntentReceiver;

    private LongSparseArray<CachedPassphrase> mPassphraseCache = new LongSparseArray<>();

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
    public static void addCachedPassphrase(Context context, long masterKeyId, long subKeyId,
                                           Passphrase passphrase,
                                           String primaryUserId) {
        Log.d(Constants.TAG, "PassphraseCacheService.addCachedPassphrase() for " + masterKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_ADD);

        intent.putExtra(EXTRA_TTL, Preferences.getPreferences(context).getPassphraseCacheTtl());
        intent.putExtra(EXTRA_PASSPHRASE, passphrase);
        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_SUBKEY_ID, subKeyId);
        intent.putExtra(EXTRA_USER_ID, primaryUserId);

        context.startService(intent);
    }

    public static void clearCachedPassphrase(Context context, long masterKeyId, long subKeyId) {
        Log.d(Constants.TAG, "PassphraseCacheService.clearCachedPassphrase() for " + masterKeyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);

        intent.putExtra(EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(EXTRA_SUBKEY_ID, subKeyId);

        context.startService(intent);
    }


    /**
     * Gets a cached passphrase from memory by sending an intent to the service. This method is
     * designed to wait until the service returns the passphrase.
     *
     * @return passphrase or null (if no passphrase is cached for this keyId)
     */
    public static Passphrase getCachedPassphrase(Context context, long masterKeyId, long subKeyId) throws KeyNotFoundException {
        Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphrase() for masterKeyId "
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
        // passphrase for symmetric encryption?
        if (masterKeyId == Constants.key.symmetric) {
            Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphraseImpl() for symmetric encryption");
            Passphrase cachedPassphrase = mPassphraseCache.get(Constants.key.symmetric).getPassphrase();
            if (cachedPassphrase == null) {
                return null;
            }
            addCachedPassphrase(this, Constants.key.symmetric, Constants.key.symmetric,
                    cachedPassphrase, getString(R.string.passp_cache_notif_pwd));
            return cachedPassphrase;
        }

        // on "none" key, just do nothing
        if (masterKeyId == Constants.key.none) {
            return null;
        }

        // try to get master key id which is used as an identifier for cached passphrases
        Log.d(Constants.TAG, "PassphraseCacheService.getCachedPassphraseImpl() for masterKeyId "
                + masterKeyId + ", subKeyId " + subKeyId);

        // get the type of key (from the database)
        CachedPublicKeyRing keyRing = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);
        SecretKeyType keyType = keyRing.getSecretKeyType(subKeyId);

        switch (keyType) {
            case DIVERT_TO_CARD:
                if (Preferences.getPreferences(this).useDefaultYubiKeyPin()) {
                    Log.d(Constants.TAG, "PassphraseCacheService: Using default YubiKey PIN: 123456");
                    return new Passphrase("123456"); // default YubiKey PIN, see http://www.yubico.com/2012/12/yubikey-neo-openpgp/
                } else {
                    Log.d(Constants.TAG, "PassphraseCacheService: NOT using default YubiKey PIN");
                    break;
                }
            case PASSPHRASE_EMPTY:
                return new Passphrase("");
            case UNAVAILABLE:
                throw new ProviderHelper.NotFoundException("secret key for this subkey is not available");
            case GNU_DUMMY:
                throw new ProviderHelper.NotFoundException("secret key for stripped subkey is not available");
        }

        // get cached passphrase
        CachedPassphrase cachedPassphrase = mPassphraseCache.get(subKeyId);
        if (cachedPassphrase == null) {

            // If we cache strictly by subkey, exit early
            if (Preferences.getPreferences(mContext).getPassphraseCacheSubs()) {
                Log.d(Constants.TAG, "PassphraseCacheService: specific subkey passphrase not (yet) cached, returning null");
                // not really an error, just means the passphrase is not cached but not empty either
                return null;
            }

            if (subKeyId == masterKeyId) {
                Log.d(Constants.TAG, "PassphraseCacheService: masterkey passphrase not (yet) cached, returning null");
                // not really an error, just means the passphrase is not cached but not empty either
                return null;
            }

            cachedPassphrase = mPassphraseCache.get(masterKeyId);
            // If we cache strictly by subkey, exit early
            if (cachedPassphrase == null) {
                Log.d(Constants.TAG, "PassphraseCacheService: keyring passphrase not (yet) cached, returning null");
                // not really an error, just means the passphrase is not cached but not empty either
                return null;
            }

        }

        // set it again to reset the cache life cycle
        Log.d(Constants.TAG, "PassphraseCacheService: Cache passphrase again when getting it!");
        addCachedPassphrase(this, masterKeyId, subKeyId, cachedPassphrase.getPassphrase(), cachedPassphrase.getPrimaryUserID());
        return cachedPassphrase.getPassphrase();
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
                        timeout(context, keyId);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE);
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

        // register broadcastreceiver
        registerReceiver();

        String action = intent.getAction();
        switch (action) {
            case ACTION_PASSPHRASE_CACHE_ADD: {
                long ttl = intent.getLongExtra(EXTRA_TTL, DEFAULT_TTL);
                long masterKeyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                long subKeyId = intent.getLongExtra(EXTRA_SUBKEY_ID, -1);

                Passphrase passphrase = intent.getParcelableExtra(EXTRA_PASSPHRASE);
                String primaryUserID = intent.getStringExtra(EXTRA_USER_ID);

                Log.d(Constants.TAG,
                        "PassphraseCacheService: Received ACTION_PASSPHRASE_CACHE_ADD intent in onStartCommand() with masterkeyId: "
                                + masterKeyId + ", subKeyId: " + subKeyId + ", ttl: " + ttl + ", usrId: " + primaryUserID
                );

                // if we don't cache by specific subkey id, or the requested subkey is the master key,
                // just add master key id to the cache, otherwise, add this specific subkey to the cache
                long referenceKeyId =
                        Preferences.getPreferences(mContext).getPassphraseCacheSubs() ? subKeyId : masterKeyId;
                mPassphraseCache.put(referenceKeyId, new CachedPassphrase(passphrase, primaryUserID));
                if (ttl > 0) {
                    // register new alarm with keyId for this passphrase
                    long triggerTime = new Date().getTime() + (ttl * 1000);
                    AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(this, referenceKeyId));
                }
                break;
            }
            case ACTION_PASSPHRASE_CACHE_GET: {
                long masterKeyId = intent.getLongExtra(EXTRA_KEY_ID, Constants.key.symmetric);
                long subKeyId = intent.getLongExtra(EXTRA_SUBKEY_ID, Constants.key.symmetric);
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);

                Message msg = Message.obtain();
                try {
                    // If only one of these is symmetric, error out!
                    if (masterKeyId == Constants.key.symmetric ^ subKeyId == Constants.key.symmetric) {
                        Log.e(Constants.TAG, "PassphraseCacheService: Bad request, missing masterKeyId or subKeyId!");
                        msg.what = MSG_PASSPHRASE_CACHE_GET_KEY_NOT_FOUND;
                    } else {
                        Passphrase passphrase = getCachedPassphraseImpl(masterKeyId, subKeyId);
                        msg.what = MSG_PASSPHRASE_CACHE_GET_OKAY;
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_PASSPHRASE, passphrase);
                        msg.setData(bundle);
                    }
                } catch (ProviderHelper.NotFoundException e) {
                    Log.e(Constants.TAG, "PassphraseCacheService: Passphrase for unknown key was requested!");
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

                if (intent.hasExtra(EXTRA_SUBKEY_ID) && intent.hasExtra(EXTRA_KEY_ID)) {

                    long referenceKeyId;
                    if (Preferences.getPreferences(mContext).getPassphraseCacheSubs()) {
                        referenceKeyId = intent.getLongExtra(EXTRA_KEY_ID, 0L);
                    } else {
                        referenceKeyId = intent.getLongExtra(EXTRA_SUBKEY_ID, 0L);
                    }
                    // Stop specific ttl alarm and
                    am.cancel(buildIntent(this, referenceKeyId));
                    mPassphraseCache.delete(referenceKeyId);

                } else {

                    // Stop all ttl alarms
                    for (int i = 0; i < mPassphraseCache.size(); i++) {
                        am.cancel(buildIntent(this, mPassphraseCache.keyAt(i)));
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

    /**
     * Called when one specific passphrase for keyId timed out
     */
    private void timeout(Context context, long keyId) {
        CachedPassphrase cPass = mPassphraseCache.get(keyId);
        // clean internal char[] from memory!
        cPass.getPassphrase().removeFromMemory();
        // remove passphrase object
        mPassphraseCache.remove(keyId);

        Log.d(Constants.TAG, "PassphraseCacheService Timeout of keyId " + keyId + ", removed from memory!");

        updateService();
    }

    private void updateService() {
        if (mPassphraseCache.size() > 0) {
            startForeground(NOTIFICATION_ID, getNotification());
        } else {
            // stop whole service if no cached passphrases remaining
            Log.d(Constants.TAG, "PassphraseCacheService: No passphrases remaining in memory, stopping service!");
            stopForeground(true);
        }
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(String.format(getString(R.string.passp_cache_notif_n_keys),
                            mPassphraseCache.size()));

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            inboxStyle.setBigContentTitle(getString(R.string.passp_cache_notif_keys));

            // Moves events into the big view
            for (int i = 0; i < mPassphraseCache.size(); i++) {
                inboxStyle.addLine(mPassphraseCache.valueAt(i).getPrimaryUserID());
            }

            // Moves the big view style object into the notification object.
            builder.setStyle(inboxStyle);

            // Add purging action
            Intent intent = new Intent(getApplicationContext(), PassphraseCacheService.class);
            intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);
            builder.addAction(
                    R.drawable.abc_ic_clear_mtrl_alpha,
                    getString(R.string.passp_cache_notif_clear),
                    PendingIntent.getService(
                            getApplicationContext(),
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
            );
        } else {
            // Fallback, since expandable notifications weren't available back then
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(String.format(getString(R.string.passp_cache_notif_n_keys),
                            mPassphraseCache.size()))
                    .setContentText(getString(R.string.passp_cache_notif_click_to_clear));

            Intent intent = new Intent(getApplicationContext(), PassphraseCacheService.class);
            intent.setAction(ACTION_PASSPHRASE_CACHE_CLEAR);

            builder.setContentIntent(
                    PendingIntent.getService(
                            getApplicationContext(),
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
            );
        }

        return builder.build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "PassphraseCacheService, onCreate()");
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

    public class CachedPassphrase {
        private String primaryUserID;
        private Passphrase passphrase;

        public CachedPassphrase(Passphrase passphrase, String primaryUserID) {
            setPassphrase(passphrase);
            setPrimaryUserID(primaryUserID);
        }

        public String getPrimaryUserID() {
            return primaryUserID;
        }

        public Passphrase getPassphrase() {
            return passphrase;
        }

        public void setPrimaryUserID(String primaryUserID) {
            this.primaryUserID = primaryUserID;
        }

        public void setPassphrase(Passphrase passphrase) {
            this.passphrase = passphrase;
        }
    }
}