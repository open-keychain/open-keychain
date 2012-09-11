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
import java.util.Vector;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRing;
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
import org.thialfihar.android.apg.util.HkpKeyServer;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.KeyServer.KeyInfo;
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

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_ENCRYPT_SIGN = 10;

    public static final int ACTION_DECRYPT_VERIFY = 20;

    public static final int ACTION_SAVE_KEYRING = 30;
    public static final int ACTION_GENERATE_KEY = 31;
    public static final int ACTION_GENERATE_DEFAULT_RSA_KEYS = 32;

    public static final int ACTION_DELETE_FILE_SECURELY = 40;

    public static final int ACTION_IMPORT_KEY = 50;
    public static final int ACTION_EXPORT_KEY = 51;

    public static final int ACTION_UPLOAD_KEY = 60;
    public static final int ACTION_QUERY_KEY = 61;

    public static final int ACTION_SIGN_KEY = 70;

    /* keys for data bundle */

    // encrypt, decrypt, import export
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

    // decrypt/verify
    public static final String SIGNED_ONLY = "signedOnly";
    public static final String RETURN_BYTES = "returnBinary";
    public static final String CIPHERTEXT_BYTES = "ciphertextBytes";
    public static final String ASSUME_SYMMETRIC = "assumeSymmetric";
    public static final String LOOKUP_UNKNOWN_KEY = "lookupUnknownKey";

    // edit keys
    public static final String NEW_PASSPHRASE = "newPassphrase";
    public static final String CURRENT_PASSPHRASE = "currentPassphrase";
    public static final String USER_IDS = "userIds";
    public static final String KEYS = "keys";
    public static final String KEYS_USAGES = "keysUsages";
    public static final String MASTER_KEY_ID = "masterKeyId";

    // generate key
    public static final String ALGORITHM = "algorithm";
    public static final String KEY_SIZE = "keySize";
    public static final String SYMMETRIC_PASSPHRASE = "passphrase";
    public static final String MASTER_KEY = "masterKey";

    // delete file securely
    public static final String DELETE_FILE = "deleteFile";

    // import key
    public static final String IMPORT_INPUT_STREAM = "importInputStream";
    public static final String IMPORT_FILENAME = "importFilename";
    public static final String IMPORT_BYTES = "importBytes";
    public static final String IMPORT_KEY_TYPE = "importKeyType";

    // export key
    public static final String EXPORT_OUTPUT_STREAM = "exportOutputStream";
    public static final String EXPORT_FILENAME = "exportFilename";
    public static final String EXPORT_KEY_TYPE = "exportKeyType";
    public static final String EXPORT_ALL = "exportAll";
    public static final String EXPORT_KEY_RING_ID = "exportKeyRingId";

    // upload key
    public static final String UPLOAD_KEY_SERVER = "uploadKeyServer";
    public static final String UPLOAD_KEY_KEYRING_ID = "uploadKeyRingId";

    // query key
    public static final String QUERY_KEY_SERVER = "queryKeyServer";
    public static final String QUERY_KEY_TYPE = "queryKeyType";
    public static final String QUERY_KEY_STRING = "queryKeyString";
    public static final String QUERY_KEY_ID = "queryKeyId";

    // sign key
    public static final String SIGN_KEY_MASTER_KEY_ID = "signKeyMasterKeyId";
    public static final String SIGN_KEY_PUB_KEY_ID = "signKeyPubKeyId";

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

    // decrypt/verify
    public static final String RESULT_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String RESULT_DECRYPTED_DATA = "decryptedData";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String RESULT_SIGNATURE_USER_ID = "signatureUserId";

    public static final String RESULT_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String RESULT_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String RESULT_SIGNATURE_LOOKUP_KEY = "lookupKey";

    // import
    public static final String RESULT_IMPORT_ADDED = "added";
    public static final String RESULT_IMPORT_UPDATED = "updated";
    public static final String RESULT_IMPORT_BAD = "bad";

    // export
    public static final String RESULT_EXPORT = "exported";

    // query
    public static final String RESULT_QUERY_KEY_KEY_DATA = "queryKeyKeyData";
    public static final String RESULT_QUERY_KEY_SEARCH_RESULT = "queryKeySearchResult";

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
                            secretKeyId, PassphraseCacheService.getCachedPassphrase(this,
                                    secretKeyId), Preferences.getPreferences(this)
                                    .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                                    .getForceV3Signatures(), this);
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    PGPMain.signText(this, inputData, outStream, secretKeyId,
                            PassphraseCacheService.getCachedPassphrase(this, secretKeyId),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(), this);
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    PGPMain.encrypt(this, inputData, outStream, useAsciiArmour, encryptionKeyIds,
                            signatureKeyId,
                            PassphraseCacheService.getCachedPassphrase(this, signatureKeyId), this,
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

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
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

                boolean lookupUnknownKey = data.getBoolean(LOOKUP_UNKNOWN_KEY);

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
                    resultData = PGPMain.verifyText(this, inputData, outStream, lookupUnknownKey,
                            this);
                } else {
                    resultData = PGPMain.decrypt(this, inputData, outStream,
                            PassphraseCacheService.getCachedPassphrase(this, secretKeyId), this,
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

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
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
                PassphraseCacheService.addCachedPassphrase(this, masterKeyId, newPassPhrase);

                /* Output */
                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY);
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

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
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

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
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
                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_IMPORT_KEY:
            try {

                /* Input */
                int target = data.getInt(TARGET);

                int keyType = Id.type.public_key;
                if (data.containsKey(IMPORT_KEY_TYPE)) {
                    keyType = data.getInt(IMPORT_KEY_TYPE);
                }

                /* Operation */
                InputStream inStream = null;
                long inLength = -1;
                InputData inputData = null;
                switch (target) {
                case TARGET_BYTES: /* import key from bytes directly */
                    byte[] bytes = data.getByteArray(IMPORT_BYTES);

                    inStream = new ByteArrayInputStream(bytes);
                    inLength = bytes.length;

                    inputData = new InputData(inStream, inLength);

                    break;
                case TARGET_FILE: /* import key from file */
                    String inputFile = data.getString(IMPORT_FILENAME);

                    inStream = new FileInputStream(inputFile);
                    File file = new File(inputFile);
                    inLength = file.length();
                    inputData = new InputData(inStream, inLength);

                    break;

                case TARGET_STREAM:
                    // TODO: not implemented
                    break;
                }

                Bundle resultData = new Bundle();
                resultData = PGPMain.importKeyRings(this, keyType, inputData, this);

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_EXPORT_KEY:
            try {

                /* Input */
                int keyType = Id.type.public_key;
                if (data.containsKey(EXPORT_KEY_TYPE)) {
                    keyType = data.getInt(EXPORT_KEY_TYPE);
                }

                String outputFile = data.getString(EXPORT_FILENAME);

                boolean exportAll = data.getBoolean(EXPORT_ALL);
                int keyRingId = -1;
                if (!exportAll) {
                    keyRingId = data.getInt(EXPORT_KEY_RING_ID);
                }

                /* Operation */

                // check if storage is ready
                if (!FileHelper.isStorageMounted(outputFile)) {
                    sendErrorToHandler(new GeneralException(
                            getString(R.string.error_externalStorageNotReady)));
                    return;
                }

                // OutputStream
                FileOutputStream outStream = new FileOutputStream(outputFile);

                Vector<Integer> keyRingIds = new Vector<Integer>();
                if (exportAll) {
                    keyRingIds = PGPMain
                            .getKeyRingIds(keyType == Id.type.public_key ? Id.database.type_public
                                    : Id.database.type_secret);
                } else {
                    keyRingIds.add(keyRingId);
                }

                Bundle resultData = new Bundle();
                resultData = PGPMain.exportKeyRings(this, keyRingIds, outStream, this);

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_UPLOAD_KEY:
            try {

                /* Input */
                int keyRingId = data.getInt(UPLOAD_KEY_KEYRING_ID);
                String keyServer = data.getString(UPLOAD_KEY_SERVER);

                /* Operation */
                HkpKeyServer server = new HkpKeyServer(keyServer);

                PGPKeyRing keyring = PGPMain.getKeyRing(keyRingId);
                if (keyring != null && keyring instanceof PGPPublicKeyRing) {
                    boolean uploaded = PGPMain.uploadKeyRingToServer(server,
                            (PGPPublicKeyRing) keyring);
                    if (!uploaded) {
                        sendErrorToHandler(new GeneralException(
                                "Unable to export key to selected server"));
                        return;
                    }
                }

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_QUERY_KEY:
            try {

                /* Input */
                int queryType = data.getInt(QUERY_KEY_TYPE);
                String keyServer = data.getString(QUERY_KEY_SERVER);

                String queryString = data.getString(QUERY_KEY_STRING);
                long keyId = data.getLong(QUERY_KEY_ID);

                /* Operation */
                Bundle resultData = new Bundle();

                HkpKeyServer server = new HkpKeyServer(keyServer);
                if (queryType == Id.keyserver.search) {
                    ArrayList<KeyInfo> searchResult = server.search(queryString);

                    resultData.putParcelableArrayList(RESULT_QUERY_KEY_SEARCH_RESULT, searchResult);
                } else if (queryType == Id.keyserver.get) {
                    String keyData = server.get(keyId);

                    resultData.putString(RESULT_QUERY_KEY_KEY_DATA, keyData);
                }

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_SIGN_KEY:
            try {

                /* Input */
                long masterKeyId = data.getLong(SIGN_KEY_MASTER_KEY_ID);
                long pubKeyId = data.getLong(SIGN_KEY_PUB_KEY_ID);

                /* Operation */
                String signaturePassPhrase = PassphraseCacheService.getCachedPassphrase(this,
                        masterKeyId);

                PGPPublicKeyRing signedPubKeyRing = PGPMain.signKey(this, masterKeyId, pubKeyId,
                        signaturePassPhrase);

                // store the signed key in our local cache
                int retval = PGPMain.storeKeyRingInCache(signedPubKeyRing);
                if (retval != Id.return_value.ok && retval != Id.return_value.updated) {
                    throw new GeneralException("Failed to store signed key in local cache");
                }

                sendMessageToHandler(ApgServiceHandler.MESSAGE_OKAY);
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
        data.putString(ApgServiceHandler.DATA_ERROR, e.getMessage());
        sendMessageToHandler(ApgServiceHandler.MESSAGE_EXCEPTION, null, data);
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
            data.putString(ApgServiceHandler.DATA_MESSAGE, message);
        }
        data.putInt(ApgServiceHandler.DATA_PROGRESS, progress);
        data.putInt(ApgServiceHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(ApgServiceHandler.MESSAGE_UPDATE_PROGRESS, null, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }
}
