/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This service caches CryptoInputParcels, which contain sensitive data like passphrases.
 * This way, they are not exposed to the client app using the API.
 */
public class CryptoInputParcelCacheService extends Service {

    public static final String ACTION_ADD = Constants.INTENT_PREFIX + "ADD";
    public static final String ACTION_GET = Constants.INTENT_PREFIX + "GET";

    public static final String EXTRA_CRYPTO_INPUT_PARCEL = "crypto_input_parcel";
    public static final String EXTRA_UUID1 = "uuid1";
    public static final String EXTRA_UUID2 = "uuid2";
    public static final String EXTRA_MESSENGER = "messenger";

    private static final int MSG_GET_OKAY = 1;
    private static final int MSG_GET_NOT_FOUND = 2;

    Context mContext;

    private static final UUID NULL_UUID = new UUID(0, 0);

    private ConcurrentHashMap<UUID, CryptoInputParcel> mCache = new ConcurrentHashMap<>();

    public static class InputParcelNotFound extends Exception {
        public InputParcelNotFound() {
        }

        public InputParcelNotFound(String name) {
            super(name);
        }
    }

    public static void addCryptoInputParcel(Context context, Intent data, CryptoInputParcel inputParcel) {
        UUID mTicket = addCryptoInputParcel(context, inputParcel);
        // And write out the UUID most and least significant bits.
        data.putExtra(OpenPgpApi.EXTRA_CALL_UUID1, mTicket.getMostSignificantBits());
        data.putExtra(OpenPgpApi.EXTRA_CALL_UUID2, mTicket.getLeastSignificantBits());
    }

    public static CryptoInputParcel getCryptoInputParcel(Context context, Intent data) {
        if (!data.getExtras().containsKey(OpenPgpApi.EXTRA_CALL_UUID1)
                || !data.getExtras().containsKey(OpenPgpApi.EXTRA_CALL_UUID2)) {
            return null;
        }
        long mostSig = data.getLongExtra(OpenPgpApi.EXTRA_CALL_UUID1, 0);
        long leastSig = data.getLongExtra(OpenPgpApi.EXTRA_CALL_UUID2, 0);
        UUID uuid = new UUID(mostSig, leastSig);
        try {
            return getCryptoInputParcel(context, uuid);
        } catch (InputParcelNotFound inputParcelNotFound) {
            return null;
        }
    }

    private static UUID addCryptoInputParcel(Context context, CryptoInputParcel inputParcel) {
        UUID uuid = UUID.randomUUID();

        Intent intent = new Intent(context, CryptoInputParcelCacheService.class);
        intent.setAction(ACTION_ADD);
        intent.putExtra(EXTRA_CRYPTO_INPUT_PARCEL, inputParcel);
        intent.putExtra(EXTRA_UUID1, uuid.getMostSignificantBits());
        intent.putExtra(EXTRA_UUID2, uuid.getLeastSignificantBits());
        context.startService(intent);
        return uuid;
    }

    private static CryptoInputParcel getCryptoInputParcel(Context context, UUID uuid) throws InputParcelNotFound {
        Intent intent = new Intent(context, CryptoInputParcelCacheService.class);
        intent.setAction(ACTION_GET);

        final Object mutex = new Object();
        final Message returnMessage = Message.obtain();

        HandlerThread handlerThread = new HandlerThread("getParcelableThread");
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
        intent.putExtra(EXTRA_UUID1, uuid.getMostSignificantBits());
        intent.putExtra(EXTRA_UUID2, uuid.getLeastSignificantBits());
        intent.putExtra(EXTRA_MESSENGER, messenger);
        // send intent to this service
        context.startService(intent);

        // Wait on mutex until parcelable is returned to handlerThread. Note that this local
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
            case MSG_GET_OKAY:
                Bundle returnData = returnMessage.getData();
                returnData.setClassLoader(context.getClassLoader());
                return returnData.getParcelable(EXTRA_CRYPTO_INPUT_PARCEL);
            case MSG_GET_NOT_FOUND:
                throw new InputParcelNotFound();
            default:
                Log.e(Constants.TAG, "timeout!");
                throw new InputParcelNotFound("should not happen!");
        }
    }

    /**
     * Executed when service is started by intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_ADD: {
                long uuid1 = intent.getLongExtra(EXTRA_UUID1, 0);
                long uuid2 = intent.getLongExtra(EXTRA_UUID2, 0);
                UUID uuid = new UUID(uuid1, uuid2);
                CryptoInputParcel inputParcel = intent.getParcelableExtra(EXTRA_CRYPTO_INPUT_PARCEL);
                mCache.put(uuid, inputParcel);

                break;
            }
            case ACTION_GET: {
                long uuid1 = intent.getLongExtra(EXTRA_UUID1, 0);
                long uuid2 = intent.getLongExtra(EXTRA_UUID2, 0);
                UUID uuid = new UUID(uuid1, uuid2);
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);

                Message msg = Message.obtain();
                // UUID.equals isn't well documented; we use compareTo instead.
                if (NULL_UUID.compareTo(uuid) == 0) {
                    msg.what = MSG_GET_NOT_FOUND;
                } else {
                    CryptoInputParcel inputParcel = mCache.get(uuid);
                    mCache.remove(uuid);
                    msg.what = MSG_GET_OKAY;
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_CRYPTO_INPUT_PARCEL, inputParcel);
                    msg.setData(bundle);
                }

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(Constants.TAG, "CryptoInputParcelCacheService: Sending message failed", e);
                }
                break;
            }
            default: {
                Log.e(Constants.TAG, "CryptoInputParcelCacheService: Intent or Intent Action not supported!");
                break;
            }
        }

        if (mCache.size() <= 0) {
            // stop whole service if cache is empty
            Log.d(Constants.TAG, "CryptoInputParcelCacheService: No passphrases remaining in memory, stopping service!");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "CryptoInputParcelCacheService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "CryptoInputParcelCacheService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class CryptoInputParcelCacheServiceBinder extends Binder {
        public CryptoInputParcelCacheService getService() {
            return CryptoInputParcelCacheService.this;
        }
    }

    private final IBinder mBinder = new CryptoInputParcelCacheServiceBinder();

}