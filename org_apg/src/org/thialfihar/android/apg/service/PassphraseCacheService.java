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

package org.thialfihar.android.apg.service;

import java.util.HashMap;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PassphraseCacheService extends Service {
    public static final String BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE = Constants.INTENT_PREFIX
            + "PASSPHRASE_CACHE_SERVICE";

    public static final String EXTRA_TTL = "ttl";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_PASSPHRASE = "passphrase";

    private static final int REQUEST_ID = 0;
    private static final long DEFAULT_TTL = 15;

    private BroadcastReceiver mIntentReceiver;

    // TODO: This is static to be easily retrieved by getCachedPassphrase()
    // To avoid static we would need a messenger from the service back to the activity?
    private static HashMap<Long, CachedPassphrase> mPassphraseCache = new HashMap<Long, CachedPassphrase>();

    /**
     * This caches a new passphrase by sending a new command to the service. An android service is
     * only run once. Thus when it is already started new commands just add new BroadcastReceivers
     * for cached passphrases
     * 
     * @param context
     * @param keyId
     * @param passphrase
     */
    public static void addCachedPassphrase(Context context, long keyId, String passphrase) {
        Log.d(Constants.TAG, "cacheNewPassphrase() for " + keyId);

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
        // try to get real key id
        long realId = keyId;
        if (realId != Id.key.symmetric) {
            PGPSecretKeyRing keyRing = PGPMain.getSecretKeyRing(keyId);
            if (keyRing == null) {
                return null;
            }
            PGPSecretKey masterKey = PGPHelper.getMasterKey(keyRing);
            if (masterKey == null) {
                return null;
            }
            realId = masterKey.getKeyID();
        }

        // get cached passphrase
        CachedPassphrase cpp = mPassphraseCache.get(realId);
        if (cpp == null) {
            return null;
        }
        // set it again to reset the cache life cycle
        addCachedPassphrase(context, realId, cpp.getPassphrase());

        return cpp.getPassphrase();
    }

    /**
     * Register BroadcastReceiver that is unregistered when service is destroyed. This
     * BroadcastReceiver hears on intents with ACTION_PASSPHRASE_CACHE_SERVICE to timeout
     * passphrases in memory.
     */
    private void registerReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (action.equals(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE)) {
                        long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                        timeout(context, keyId);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_ACTION_PASSPHRASE_CACHE_SERVICE);
            LocalBroadcastManager.getInstance(this).registerReceiver(mIntentReceiver, filter);
        }
    }

    /**
     * Build pending intent that is executed by alarm manager when one passphrase times out
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
        Log.d(Constants.TAG, "PassphraseCacheService created!");
    }

    /**
     * Executed when service is started by intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "PassphraseCacheService started");

        // register broadcastreceiver
        registerReceiver();

        if (intent != null) {
            long ttl = intent.getLongExtra(EXTRA_TTL, DEFAULT_TTL);
            long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
            String passphrase = intent.getStringExtra(EXTRA_PASSPHRASE);

            Log.d(Constants.TAG, "received intent with keyId: " + keyId + ", ttl: " + ttl);

            // add keyId and passphrase to memory
            mPassphraseCache.put(keyId,
                    new CachedPassphrase(System.currentTimeMillis(), passphrase));

            // register new alarm with keyId for this passphrase
            long triggerTime = System.currentTimeMillis() + ttl;
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
        Log.d(Constants.TAG, "Timeout of " + keyId);

        // remove passphrase corresponding to keyId from memory
        mPassphraseCache.remove(keyId);

        // stop whole service if no cached passphrases remaining
        if (mPassphraseCache.isEmpty()) {
            Log.d(Constants.TAG, "No passphrases remaining in memory, stopping service!");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "PassphraseCacheService destroyed!");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mIntentReceiver);
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