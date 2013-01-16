/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.service;

import java.util.Date;
import java.util.HashMap;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.PgpHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class PassphraseCacheService extends Service {
    public static final String TAG = Constants.TAG + ": PassphraseCacheService";

    public static final String BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_SERVICE";

    public static final String EXTRA_TTL = "ttl";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_PASSPHRASE = "passphrase";

    private static final int REQUEST_ID = 0;
    private static final long DEFAULT_TTL = 15;

    private BroadcastReceiver mIntentReceiver;

    // This is static to be easily retrieved by getCachedPassphrase() without the need of callback
    // functions
    private static HashMap<Long, String> mPassphraseCache = new HashMap<Long, String>();

    /**
     * This caches a new passphrase by sending a new command to the service. An android service is
     * only run once. Thus, when the service is already started, new commands just add new events to
     * the alarm manager for new passphrases to let them timeout in the future.
     * 
     * @param context
     * @param keyId
     * @param passphrase
     */
    public static void addCachedPassphrase(Context context, long keyId, String passphrase) {
        Log.d(TAG, "cacheNewPassphrase() for " + keyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.putExtra(EXTRA_TTL, Preferences.getPreferences(context).getPassPhraseCacheTtl());
        intent.putExtra(EXTRA_PASSPHRASE, passphrase);
        intent.putExtra(EXTRA_KEY_ID, keyId);

        context.startService(intent);
    }

    /**
     * Gets a cached passphrase from memory
     * 
     * @param context
     * @param keyId
     * @return
     */
    public static String getCachedPassphrase(Context context, long keyId) {
        // try to get master key id which is used as an identifier for cached passphrases
        long masterKeyId = keyId;
        if (masterKeyId != Id.key.symmetric) {
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, keyId);
            if (keyRing == null) {
                return null;
            }
            PGPSecretKey masterKey = PgpHelper.getMasterKey(keyRing);
            if (masterKey == null) {
                return null;
            }
            masterKeyId = masterKey.getKeyID();
        }

        // get cached passphrase
        String cachedPassphrase = mPassphraseCache.get(masterKeyId);
        if (cachedPassphrase == null) {
            return null;
        }
        // set it again to reset the cache life cycle
        Log.d(TAG, "Cache passphrase again when getting it!");
        addCachedPassphrase(context, masterKeyId, cachedPassphrase);

        return cachedPassphrase;
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

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
    }

    /**
     * Executed when service is started by intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        // register broadcastreceiver
        registerReceiver();

        if (intent != null) {
            long ttl = intent.getLongExtra(EXTRA_TTL, DEFAULT_TTL);
            long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
            String passphrase = intent.getStringExtra(EXTRA_PASSPHRASE);

            Log.d(TAG, "Received intent in onStartCommand() with keyId: " + keyId + ", ttl: " + ttl);

            // add keyId and passphrase to memory
            mPassphraseCache.put(keyId, passphrase);

            // register new alarm with keyId for this passphrase
            long triggerTime = new Date().getTime() + (ttl * 1000);
            AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(this, keyId));
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

        Log.d(TAG, "Timeout of " + keyId + ", removed from memory!");

        // stop whole service if no cached passphrases remaining
        if (mPassphraseCache.isEmpty()) {
            Log.d(TAG, "No passphrases remaining in memory, stopping service!");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        unregisterReceiver(mIntentReceiver);
    }

    public class PassphraseCacheBinder extends Binder {
        public PassphraseCacheService getService() {
            return PassphraseCacheService.this;
        }
    }

    private final IBinder mBinder = new PassphraseCacheBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}