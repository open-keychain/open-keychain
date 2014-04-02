/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

import java.util.Date;
import java.util.Iterator;

/**
 * This service runs in its own process, but is available to all other processes as the main
 * passphrase cache. Use the static methods addCachedPassphrase and getCachedPassphrase for
 * convenience.
 */
public class PassphraseCacheService extends Service {
    public static final String TAG = Constants.TAG + ": PassphraseCacheService";

    public static final String ACTION_PASSPHRASE_CACHE_ADD = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_ADD";
    public static final String ACTION_PASSPHRASE_CACHE_GET = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_GET";

    public static final String BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_BROADCAST";

    public static final String EXTRA_TTL = "ttl";
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_MESSENGER = "messenger";

    private static final int REQUEST_ID = 0;
    private static final long DEFAULT_TTL = 15;

    private BroadcastReceiver mIntentReceiver;

    private LongSparseArray<String> mPassphraseCache = new LongSparseArray<String>();

    Context mContext;

    /**
     * This caches a new passphrase in memory by sending a new command to the service. An android
     * service is only run once. Thus, when the service is already started, new commands just add
     * new events to the alarm manager for new passphrases to let them timeout in the future.
     *
     * @param context
     * @param keyId
     * @param passphrase
     */
    public static void addCachedPassphrase(Context context, long keyId, String passphrase) {
        Log.d(TAG, "cacheNewPassphrase() for " + keyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_ADD);
        intent.putExtra(EXTRA_TTL, Preferences.getPreferences(context).getPassPhraseCacheTtl());
        intent.putExtra(EXTRA_PASSPHRASE, passphrase);
        intent.putExtra(EXTRA_KEY_ID, keyId);

        context.startService(intent);
    }

    /**
     * Gets a cached passphrase from memory by sending an intent to the service. This method is
     * designed to wait until the service returns the passphrase.
     *
     * @param context
     * @param keyId
     * @return passphrase or null (if no passphrase is cached for this keyId)
     */
    public static String getCachedPassphrase(Context context, long keyId) {
        Log.d(TAG, "getCachedPassphrase() get masterKeyId for " + keyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_GET);

        final Object mutex = new Object();
        final Bundle returnBundle = new Bundle();

        HandlerThread handlerThread = new HandlerThread("getPassphraseThread");
        handlerThread.start();
        Handler returnHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.obj != null) {
                    String passphrase = ((Bundle) message.obj).getString(EXTRA_PASSPHRASE);
                    returnBundle.putString(EXTRA_PASSPHRASE, passphrase);
                }
                synchronized (mutex) {
                    mutex.notify();
                }
                // quit handlerThread
                getLooper().quit();
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        intent.putExtra(EXTRA_KEY_ID, keyId);
        intent.putExtra(EXTRA_MESSENGER, messenger);
        // send intent to this service
        context.startService(intent);

        // Wait on mutex until passphrase is returned to handlerThread
        synchronized (mutex) {
            try {
                mutex.wait(3000);
            } catch (InterruptedException e) {
            }
        }

        if (returnBundle.containsKey(EXTRA_PASSPHRASE)) {
            return returnBundle.getString(EXTRA_PASSPHRASE);
        } else {
            return null;
        }
    }

    /**
     * Internal implementation to get cached passphrase.
     *
     * @param keyId
     * @return
     */
    private String getCachedPassphraseImpl(long keyId) {
        Log.d(TAG, "getCachedPassphraseImpl() get masterKeyId for " + keyId);

        // try to get master key id which is used as an identifier for cached passphrases
        long masterKeyId = keyId;
        if (masterKeyId != Id.key.symmetric) {
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(this, keyId);
            if (keyRing == null) {
                return null;
            }
            PGPSecretKey masterKey = PgpKeyHelper.getMasterKey(keyRing);
            if (masterKey == null) {
                return null;
            }
            masterKeyId = masterKey.getKeyID();
        }
        Log.d(TAG, "getCachedPassphraseImpl() for masterKeyId " + masterKeyId);

        // get cached passphrase
        String cachedPassphrase = mPassphraseCache.get(masterKeyId);
        if (cachedPassphrase == null) {
            // if key has no passphrase -> cache and return empty passphrase
            if (!hasPassphrase(this, masterKeyId)) {
                Log.d(Constants.TAG, "Key has no passphrase! Caches and returns empty passphrase!");

                addCachedPassphrase(this, masterKeyId, "");
                return "";
            } else {
                return null;
            }
        }
        // set it again to reset the cache life cycle
        Log.d(TAG, "Cache passphrase again when getting it!");
        addCachedPassphrase(this, masterKeyId, cachedPassphrase);

        return cachedPassphrase;
    }

    /**
     * Checks if key has a passphrase.
     *
     * @param secretKeyId
     * @return true if it has a passphrase
     */
    public static boolean hasPassphrase(Context context, long secretKeyId) {
        // check if the key has no passphrase
        try {
            PGPSecretKeyRing secRing = ProviderHelper
                    .getPGPSecretKeyRingByKeyId(context, secretKeyId);
            PGPSecretKey secretKey = null;
            boolean foundValidKey = false;
            for (Iterator keys = secRing.getSecretKeys(); keys.hasNext(); ) {
                secretKey = (PGPSecretKey) keys.next();
                if (!secretKey.isPrivateKeyEmpty()) {
                    foundValidKey = true;
                    break;
                }
            }

            if (!foundValidKey) {
                return false;
            }
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    "SC").build("".toCharArray());
            PGPPrivateKey testKey = secretKey.extractPrivateKey(keyDecryptor);
            if (testKey != null) {
                return false;
            }
        } catch (PGPException e) {
            // silently catch
        }

        return true;
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

                    Log.d(TAG, "Received broadcast...");

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
     *
     * @param context
     * @param keyId
     * @return
     */
    private static PendingIntent buildIntent(Context context, long keyId) {
        Intent intent = new Intent(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE);
        intent.putExtra(EXTRA_KEY_ID, keyId);
        PendingIntent sender = PendingIntent.getBroadcast(context, REQUEST_ID, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        return sender;
    }

    /**
     * Executed when service is started by intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        // register broadcastreceiver
        registerReceiver();

        if (intent != null && intent.getAction() != null) {
            if (ACTION_PASSPHRASE_CACHE_ADD.equals(intent.getAction())) {
                long ttl = intent.getLongExtra(EXTRA_TTL, DEFAULT_TTL);
                long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                String passphrase = intent.getStringExtra(EXTRA_PASSPHRASE);

                Log.d(TAG,
                        "Received ACTION_PASSPHRASE_CACHE_ADD intent in onStartCommand() with keyId: "
                                + keyId + ", ttl: " + ttl);

                // add keyId and passphrase to memory
                mPassphraseCache.put(keyId, passphrase);

                if (ttl > 0) {
                    // register new alarm with keyId for this passphrase
                    long triggerTime = new Date().getTime() + (ttl * 1000);
                    AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(this, keyId));
                }
            } else if (ACTION_PASSPHRASE_CACHE_GET.equals(intent.getAction())) {
                long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);

                String passphrase = getCachedPassphraseImpl(keyId);

                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_PASSPHRASE, passphrase);
                msg.obj = bundle;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(Constants.TAG, "Sending message failed", e);
                }
            } else {
                Log.e(Constants.TAG, "Intent or Intent Action not supported!");
            }
        }

        return START_STICKY;
    }

    /**
     * Called when one specific passphrase for keyId timed out
     *
     * @param context
     * @param keyId
     */
    private void timeout(Context context, long keyId) {
        // remove passphrase corresponding to keyId from memory
        mPassphraseCache.remove(keyId);

        Log.d(TAG, "Timeout of keyId " + keyId + ", removed from memory!");

        // stop whole service if no cached passphrases remaining
        if (mPassphraseCache.size() == 0) {
            Log.d(TAG, "No passphrases remaining in memory, stopping service!");
            stopSelf();
        }
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

}
