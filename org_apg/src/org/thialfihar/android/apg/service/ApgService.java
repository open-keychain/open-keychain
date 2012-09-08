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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.FileHelper;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.helper.PGPMain.GeneralException;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.provider.DataProvider;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.ProgressDialogUpdater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import org.thialfihar.android.apg.util.Log;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */
public class ApgService extends IntentService implements ProgressDialogUpdater {

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    /* keys for data bundle */

    // encrypt and decrypt
    public static final String TARGET = "target";
    // possible targets:
    public static final int TARGET_BYTES = 1;
    public static final int TARGET_FILE = 2;
    public static final int TARGET_STREAM = 3;

    // encrypt
    public static final String SECRET_KEY_ID = "secretKeyId";
    public static final String USE_ASCII_AMOR = "useAsciiAmor";
    public static final String ENCRYPTION_KEYS_IDS = "encryptionKeysIds";
    public static final String SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String COMPRESSION_ID = "compressionId";
    public static final String GENERATE_SIGNATURE = "generateSignature";
    public static final String SIGN_ONLY = "signOnly";
    public static final String MESSAGE_BYTES = "messageBytes";
    public static final String INPUT_FILE = "inputFile";
    public static final String OUTPUT_FILE = "outputFile";
    public static final String PROVIDER_URI = "providerUri";

    // decrypt
    public static final String SIGNED_ONLY = "signedOnly";
    public static final String RETURN_BYTES = "returnBinary";
    public static final String CIPHERTEXT_BYTES = "ciphertextBytes";
    public static final String ASSUME_SYMMETRIC = "assumeSymmetric";

    // edit keys
    public static final String NEW_PASSPHRASE = "newPassphrase";
    public static final String CURRENT_PASSPHRASE = "currentPassphrase";
    public static final String USER_IDS = "userIds";
    public static final String KEYS = "keys";
    public static final String KEYS_USAGES = "keysUsages";
    public static final String MASTER_KEY_ID = "masterKeyId";

    // generate key
    public static final String ALGORITHM = "algorithm";
    public static final String KEY_SIZE = "key_size";
    public static final String SYMMETRIC_PASSPHRASE = "passphrase";
    public static final String MASTER_KEY = "masterKey";

    // delete file securely
    public static final String DELETE_FILE = "deleteFile";

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_ENCRYPT_SIGN = 10;

    public static final int ACTION_DECRYPT_VERIFY = 20;

    public static final int ACTION_SAVE_KEYRING = 30;
    public static final int ACTION_GENERATE_KEY = 31;
    public static final int ACTION_GENERATE_DEFAULT_RSA_KEYS = 32;

    public static final int ACTION_DELETE_FILE_SECURELY = 40;

    /* possible data keys as result send over messenger */
    // keys
    public static final String RESULT_NEW_KEY = "newKey";
    public static final String RESULT_NEW_KEY2 = "newKey2";

    // encrypt
    public static final String RESULT_SIGNATURE_DATA = "signatureData";
    public static final String RESULT_SIGNATURE_TEXT = "signatureText";
    public static final String RESULT_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String RESULT_ENCRYPTED_DATA = "encryptedData";
    public static final String RESULT_URI = "resultUri";

    // decrypt
    public static final String RESULT_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String RESULT_DECRYPTED_DATA = "decryptedData";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String RESULT_SIGNATURE_USER_ID = "signatureUserId";
    public static final String RESULT_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String RESULT_SIGNATURE_UNKNOWN = "signatureUnknown";

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
            Log.e(Constants.TAG, "Extras bundle is null!");
            return;
        }

        if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) || extras
                .containsKey(EXTRA_ACTION))) {
            Log.e(Constants.TAG,
                    "Extra bundle must contain a messenger, a data bundle, and an action!");
            return;
        }

        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
        Bundle data = extras.getBundle(EXTRA_DATA);

        OtherHelper.logDebugBundle(data, "EXTRA_DATA");

        int action = extras.getInt(EXTRA_ACTION);

        // execute action from extra bundle
        switch (action) {
        case ACTION_ENCRYPT_SIGN:

            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(SECRET_KEY_ID);
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmour = data.getBoolean(USE_ASCII_AMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPTION_KEYS_IDS);
                long signatureKeyId = data.getLong(SIGNATURE_KEY_ID);
                int compressionId = data.getInt(COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(SIGN_ONLY);

                InputStream inStream = null;
                long inLength = -1;
                InputData inputData = null;
                OutputStream outStream = null;
                String streamFilename = null;
                switch (target) {
                case TARGET_BYTES: /* encrypting bytes directly */
                    byte[] bytes = data.getByteArray(MESSAGE_BYTES);

                    inStream = new ByteArrayInputStream(bytes);
                    inLength = bytes.length;

                    inputData = new InputData(inStream, inLength);
                    outStream = new ByteArrayOutputStream();

                    break;
                case TARGET_FILE: /* encrypting file */
                    String inputFile = data.getString(INPUT_FILE);
                    String outputFile = data.getString(OUTPUT_FILE);

                    // check if storage is ready
                    if (!FileHelper.isStorageMounted(inputFile)
                            || !FileHelper.isStorageMounted(outputFile)) {
                        sendErrorToHandler(new GeneralException(
                                getString(R.string.error_externalStorageNotReady)));
                        return;
                    }

                    inStream = new FileInputStream(inputFile);
                    File file = new File(inputFile);
                    inLength = file.length();
                    inputData = new InputData(inStream, inLength);

                    outStream = new FileOutputStream(outputFile);

                    break;

                case TARGET_STREAM: /* Encrypting stream from content uri */
                    Uri providerUri = Uri.parse(data.getString(PROVIDER_URI));

                    // InputStream
                    InputStream in = getContentResolver().openInputStream(providerUri);
                    inLength = PGPMain.getLengthOfStream(in);
                    inputData = new InputData(in, inLength);

                    // OutputStream
                    try {
                        while (true) {
                            streamFilename = PGPMain.generateRandomString(32);
                            if (streamFilename == null) {
                                throw new PGPMain.GeneralException(
                                        "couldn't generate random file name");
                            }
                            openFileInput(streamFilename).close();
                        }
                    } catch (FileNotFoundException e) {
                        // found a name that isn't used yet
                    }
                    outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                    break;

                default:
                    throw new PGPMain.GeneralException("No target choosen!");

                }

                /* Operation */

                if (generateSignature) {
                    Log.d(Constants.TAG, "generating signature...");
                    PGPMain.generateSignature(this, inputData, outStream, useAsciiArmour, false,
                            secretKeyId, PGPMain.getCachedPassPhrase(secretKeyId), Preferences
                                    .getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(), this);
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    PGPMain.signText(this, inputData, outStream, secretKeyId, PGPMain
                            .getCachedPassPhrase(secretKeyId), Preferences.getPreferences(this)
                            .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                            .getForceV3Signatures(), this);
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    PGPMain.encrypt(this, inputData, outStream, useAsciiArmour, encryptionKeyIds,
                            signatureKeyId, PGPMain.getCachedPassPhrase(signatureKeyId), this,
                            Preferences.getPreferences(this).getDefaultEncryptionAlgorithm(),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(),
                            compressionId, Preferences.getPreferences(this).getForceV3Signatures(),
                            passphrase);
                }

                outStream.close();

                /* Output */

                Bundle resultData = new Bundle();

                switch (target) {
                case TARGET_BYTES:
                    if (useAsciiArmour) {
                        String output = new String(
                                ((ByteArrayOutputStream) outStream).toByteArray());
                        if (generateSignature) {
                            resultData.putString(RESULT_SIGNATURE_TEXT, output);
                        } else {
                            resultData.putString(RESULT_ENCRYPTED_MESSAGE, output);
                        }
                    } else {
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        if (generateSignature) {
                            resultData.putByteArray(RESULT_SIGNATURE_DATA, output);
                        } else {
                            resultData.putByteArray(RESULT_ENCRYPTED_DATA, output);
                        }
                    }

                    break;
                case TARGET_FILE:
                    // nothing, file was written, just send okay

                    break;
                case TARGET_STREAM:
                    String uri = "content://" + DataProvider.AUTHORITY + "/data/" + streamFilename;
                    resultData.putString(RESULT_URI, uri);

                    break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_DECRYPT_VERIFY:
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(SECRET_KEY_ID);
                byte[] bytes = data.getByteArray(CIPHERTEXT_BYTES);
                boolean signedOnly = data.getBoolean(SIGNED_ONLY);
                boolean returnBytes = data.getBoolean(RETURN_BYTES);
                boolean assumeSymmetricEncryption = data.getBoolean(ASSUME_SYMMETRIC);

                InputStream inStream = null;
                long inLength = -1;
                InputData inputData = null;
                OutputStream outStream = null;
                String streamFilename = null;
                switch (target) {
                case TARGET_BYTES: /* decrypting bytes directly */
                    inStream = new ByteArrayInputStream(bytes);
                    inLength = bytes.length;

                    inputData = new InputData(inStream, inLength);
                    outStream = new ByteArrayOutputStream();

                    break;

                case TARGET_FILE: /* decrypting file */
                    String inputFile = data.getString(INPUT_FILE);
                    String outputFile = data.getString(OUTPUT_FILE);

                    // check if storage is ready
                    if (!FileHelper.isStorageMounted(inputFile)
                            || !FileHelper.isStorageMounted(outputFile)) {
                        sendErrorToHandler(new GeneralException(
                                getString(R.string.error_externalStorageNotReady)));
                        return;
                    }

                    // InputStream
                    inLength = -1;
                    inStream = new FileInputStream(inputFile);
                    File file = new File(inputFile);
                    inLength = file.length();
                    inputData = new InputData(inStream, inLength);

                    // OutputStream
                    outStream = new FileOutputStream(outputFile);

                    break;

                case TARGET_STREAM: /* decrypting stream from content uri */
                    Uri providerUri = Uri.parse(data.getString(PROVIDER_URI));

                    // InputStream
                    InputStream in = getContentResolver().openInputStream(providerUri);
                    inLength = PGPMain.getLengthOfStream(in);
                    inputData = new InputData(in, inLength);

                    // OutputStream
                    try {
                        while (true) {
                            streamFilename = PGPMain.generateRandomString(32);
                            if (streamFilename == null) {
                                throw new PGPMain.GeneralException(
                                        "couldn't generate random file name");
                            }
                            openFileInput(streamFilename).close();
                        }
                    } catch (FileNotFoundException e) {
                        // found a name that isn't used yet
                    }
                    outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                    break;

                default:
                    throw new PGPMain.GeneralException("No target choosen!");

                }

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                if (signedOnly) {
                    resultData = PGPMain.verifyText(this, inputData, outStream, this);
                } else {
                    resultData = PGPMain.decrypt(this, inputData, outStream,
                            PGPMain.getCachedPassPhrase(secretKeyId), this,
                            assumeSymmetricEncryption);
                }

                outStream.close();

                /* Output */

                switch (target) {
                case TARGET_BYTES:
                    if (returnBytes) {
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        resultData.putByteArray(RESULT_DECRYPTED_DATA, output);
                    } else {
                        String output = new String(
                                ((ByteArrayOutputStream) outStream).toByteArray());
                        resultData.putString(RESULT_DECRYPTED_MESSAGE, output);
                    }

                    break;
                case TARGET_FILE:
                    // nothing, file was written, just send okay and verification bundle

                    break;
                case TARGET_STREAM:
                    String uri = "content://" + DataProvider.AUTHORITY + "/data/" + streamFilename;
                    resultData.putString(RESULT_URI, uri);

                    break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_SAVE_KEYRING:

            try {
                /* Input */
                String oldPassPhrase = data.getString(CURRENT_PASSPHRASE);
                String newPassPhrase = data.getString(NEW_PASSPHRASE);
                if (newPassPhrase == null) {
                    newPassPhrase = oldPassPhrase;
                }
                @SuppressWarnings("unchecked")
                ArrayList<String> userIds = (ArrayList<String>) data.getSerializable(USER_IDS);
                ArrayList<PGPSecretKey> keys = PGPConversionHelper.BytesToPGPSecretKeyList(data
                        .getByteArray(KEYS));
                @SuppressWarnings("unchecked")
                ArrayList<Integer> keysUsages = (ArrayList<Integer>) data
                        .getSerializable(KEYS_USAGES);
                long masterKeyId = data.getLong(MASTER_KEY_ID);

                /* Operation */
                PGPMain.buildSecretKey(this, userIds, keys, keysUsages, masterKeyId, oldPassPhrase,
                        newPassPhrase, this);
                PGPMain.setCachedPassPhrase(masterKeyId, newPassPhrase);

                /* Output */
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_GENERATE_KEY:

            try {
                /* Input */
                int algorithm = data.getInt(ALGORITHM);
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);
                int keysize = data.getInt(KEY_SIZE);
                PGPSecretKey masterKey = null;
                if (data.containsKey(MASTER_KEY)) {
                    masterKey = PGPConversionHelper.BytesToPGPSecretKey(data
                            .getByteArray(MASTER_KEY));
                }

                /* Operation */
                PGPSecretKeyRing newKeyRing = PGPMain.createKey(this, algorithm, keysize,
                        passphrase, masterKey);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(newKeyRing));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_GENERATE_DEFAULT_RSA_KEYS:
            // generate one RSA 2048 key for signing and one subkey for encrypting!
            try {
                /* Input */
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);

                /* Operation */
                PGPSecretKeyRing masterKeyRing = PGPMain.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, null);

                PGPSecretKeyRing subKeyRing = PGPMain.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, masterKeyRing.getSecretKey());

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(masterKeyRing));
                resultData.putByteArray(RESULT_NEW_KEY2,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(subKeyRing));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_DELETE_FILE_SECURELY:
            try {
                /* Input */
                String deleteFile = data.getString(DELETE_FILE);

                /* Operation */
                try {
                    PGPMain.deleteFileSecurely(this, new File(deleteFile), this);
                } catch (FileNotFoundException e) {
                    throw new PGPMain.GeneralException(getString(R.string.error_fileNotFound,
                            deleteFile));
                } catch (IOException e) {
                    throw new PGPMain.GeneralException(getString(R.string.error_fileDeleteFailed,
                            deleteFile));
                }

                /* Output */
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
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
        data.putString(ApgHandler.DATA_ERROR, e.getMessage());
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

    private void sendMessageToHandler(Integer arg1, Bundle data) {
        sendMessageToHandler(arg1, null, data);
    }

    private void sendMessageToHandler(Integer arg1) {
        sendMessageToHandler(arg1, null, null);
    }

    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    public void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress with progress=" + progress + ", max="
                + max);

        Bundle data = new Bundle();
        if (message != null) {
            data.putString(ApgHandler.DATA_MESSAGE, message);
        }
        data.putInt(ApgHandler.DATA_PROGRESS, progress);
        data.putInt(ApgHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(ApgHandler.MESSAGE_UPDATE_PROGRESS, null, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }
}
