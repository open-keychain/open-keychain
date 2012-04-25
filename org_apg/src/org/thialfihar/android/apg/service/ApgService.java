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

import java.util.ArrayList;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Apg;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.ProgressDialogUpdater;
import org.thialfihar.android.apg.util.Utils;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * doing it.
 */
// TODO: ProgressDialogUpdater rework???
public class ApgService extends IntentService implements ProgressDialogUpdater {

    // extras that can be given by intent
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    // keys for data bundle
    // edit keys
    public static final String NEW_PASSPHRASE = "new_passphrase";
    public static final String CURRENT_PASSPHRASE = "current_passphrase";
    public static final String USER_IDS = "user_ids";
    public static final String KEYS = "keys";
    public static final String KEYS_USAGES = "keys_usages";
    public static final String MASTER_KEY_ID = "master_key_id";

    // generate key
    public static final String ALGORITHM = "algorithm";
    public static final String KEY_SIZE = "key_size";
    public static final String PASSPHRASE = "passphrase";
    public static final String MASTER_KEY = "master_key";

    // possible ints for EXTRA_ACTION
    public static final int ACTION_SAVE_KEYRING = 1;
    public static final int ACTION_GENERATE_KEY = 2;
    public static final int ACTION_GENERATE_DEFAULT_RSA_KEYS = 3;

    Messenger mMessenger;

    public ApgService() {
        super("ApgService");
    }

    /**
     * The IntentService calls this method from the default worker thread with the intent that
     * started the service. When this method returns, IntentService stops the service, as
     * appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(Constants.TAG, "Extra bundle is null!");
            return;
        }

        if (!extras.containsKey(EXTRA_MESSENGER)) {
            Log.e(Constants.TAG, "Extra bundle must contain a messenger!");
            return;
        }
        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);

        if (!extras.containsKey(EXTRA_DATA)) {
            Log.e(Constants.TAG, "Extra bundle must contain data bundle!");
            return;
        }
        Bundle data = extras.getBundle(EXTRA_DATA);

        if (!extras.containsKey(EXTRA_ACTION)) {
            Log.e(Constants.TAG, "Extra bundle must contain a action!");
            return;
        }
        int action = extras.getInt(EXTRA_ACTION);

        // execute action from extra bundle
        switch (action) {
        case ACTION_SAVE_KEYRING:

            try {
                // Input
                String oldPassPhrase = data.getString(CURRENT_PASSPHRASE);
                String newPassPhrase = data.getString(NEW_PASSPHRASE);
                if (newPassPhrase == null) {
                    newPassPhrase = oldPassPhrase;
                }
                @SuppressWarnings("unchecked")
                ArrayList<String> userIds = (ArrayList<String>) data.getSerializable(USER_IDS);
                ArrayList<PGPSecretKey> keys = Utils.BytesToPGPSecretKeyList(data
                        .getByteArray(KEYS));
                @SuppressWarnings("unchecked")
                ArrayList<Integer> keysUsages = (ArrayList<Integer>) data
                        .getSerializable(KEYS_USAGES);
                long masterKeyId = data.getLong(MASTER_KEY_ID);

                // Operation
                Apg.buildSecretKey(this, userIds, keys, keysUsages, masterKeyId, oldPassPhrase,
                        newPassPhrase, this);
                Apg.setCachedPassPhrase(masterKeyId, newPassPhrase);

                // Output
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_GENERATE_KEY:

            try {
                // Input
                int algorithm = data.getInt(ALGORITHM);
                String passphrase = data.getString(PASSPHRASE);
                int keysize = data.getInt(KEY_SIZE);
                PGPSecretKey masterKey = null;
                if (data.containsKey(MASTER_KEY)) {
                    masterKey = Utils.BytesToPGPSecretKey(data.getByteArray(MASTER_KEY));
                }

                // Operation
                PGPSecretKeyRing newKeyRing = Apg.createKey(this, algorithm, keysize, passphrase,
                        masterKey);

                // Output
                Bundle resultData = new Bundle();
                resultData.putByteArray(ApgHandler.NEW_KEY,
                        Utils.PGPSecretKeyRingToBytes(newKeyRing));
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, null, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_GENERATE_DEFAULT_RSA_KEYS:
            // generate one RSA 2048 key for signing and one subkey for encrypting!
            try {
                String passphrase = data.getString(PASSPHRASE);

                // Operation
                PGPSecretKeyRing masterKeyRing = Apg.createKey(this, Id.choice.algorithm.rsa, 2048,
                        passphrase, null);

                PGPSecretKeyRing subKeyRing = Apg.createKey(this, Id.choice.algorithm.rsa, 2048,
                        passphrase, masterKeyRing.getSecretKey());

                // Output
                Bundle resultData = new Bundle();
                resultData.putByteArray(ApgHandler.NEW_KEY,
                        Utils.PGPSecretKeyRingToBytes(masterKeyRing));
                resultData.putByteArray(ApgHandler.NEW_KEY2,
                        Utils.PGPSecretKeyRingToBytes(subKeyRing));
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, null, resultData);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Creating initial key failed: +" + e);
            }

            break;

        default:
            break;
        }

    }

    private void sendErrorToHandler(Exception e) {
        Log.e(Constants.TAG, "ApgService Exception: ", e);
        e.printStackTrace();

        Bundle data = new Bundle();
        data.putString(ApgHandler.ERROR, e.getMessage());
        sendMessageToHandler(ApgHandler.MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToHandler(Integer arg1, Integer arg2, Bundle data) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }

    private void sendMessageToHandler(Integer arg1) {
        sendMessageToHandler(arg1, null, null);
    }

    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    public void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress");

        Bundle data = new Bundle();
        if (message != null) {
            data.putString(ApgHandler.MESSAGE, message);
        }
        data.putInt(ApgHandler.PROGRESS, progress);
        data.putInt(ApgHandler.PROGRESS_MAX, max);

        sendMessageToHandler(ApgHandler.MESSAGE_UPDATE_PROGRESS, null, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }
}
