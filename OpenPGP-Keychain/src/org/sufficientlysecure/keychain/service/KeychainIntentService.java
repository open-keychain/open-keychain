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

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.helper.PgpConversionHelper;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.helper.PgpMain.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.DataStream;
import org.sufficientlysecure.keychain.util.HkpKeyServer;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;
import org.sufficientlysecure.keychain.util.KeyServer.KeyInfo;
import org.sufficientlysecure.keychain.R;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */
public class KeychainIntentService extends IntentService implements ProgressDialogUpdater {

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    /* possible EXTRA_ACTIONs */
    public static final String ACTION_ENCRYPT_SIGN = Constants.INTENT_PREFIX + "ENCRYPT_SIGN";

    public static final String ACTION_DECRYPT_VERIFY = Constants.INTENT_PREFIX + "DECRYPT_VERIFY";

    public static final String ACTION_SAVE_KEYRING = Constants.INTENT_PREFIX + "SAVE_KEYRING";
    public static final String ACTION_GENERATE_KEY = Constants.INTENT_PREFIX + "GENERATE_KEY";
    public static final String ACTION_GENERATE_DEFAULT_RSA_KEYS = Constants.INTENT_PREFIX + "GENERATE_DEFAULT_RSA_KEYS";

    public static final String ACTION_DELETE_FILE_SECURELY = Constants.INTENT_PREFIX + "DELETE_FILE_SECURELY";

    public static final String ACTION_IMPORT_KEYRING = Constants.INTENT_PREFIX + "IMPORT_KEYRING";
    public static final String ACTION_EXPORT_KEYRING = Constants.INTENT_PREFIX + "EXPORT_KEYRING";

    public static final String ACTION_UPLOAD_KEYRING = Constants.INTENT_PREFIX + "UPLOAD_KEYRING";
    public static final String ACTION_QUERY_KEYRING = Constants.INTENT_PREFIX + "QUERY_KEYRING";

    public static final String ACTION_SIGN_KEYRING = Constants.INTENT_PREFIX + "SIGN_KEYRING";

    /* keys for data bundle */

    // encrypt, decrypt, import export
    public static final String TARGET = "target";
    // possible targets:
    public static final int TARGET_BYTES = 1;
    public static final int TARGET_FILE = 2;
    public static final int TARGET_STREAM = 3;

    // encrypt
    public static final String ENCRYPT_SECRET_KEY_ID = "secretKeyId";
    public static final String ENCRYPT_USE_ASCII_AMOR = "useAsciiAmor";
    public static final String ENCRYPT_ENCRYPTION_KEYS_IDS = "encryptionKeysIds";
    public static final String ENCRYPT_COMPRESSION_ID = "compressionId";
    public static final String ENCRYPT_GENERATE_SIGNATURE = "generateSignature";
    public static final String ENCRYPT_SIGN_ONLY = "signOnly";
    public static final String ENCRYPT_MESSAGE_BYTES = "messageBytes";
    public static final String ENCRYPT_INPUT_FILE = "inputFile";
    public static final String ENCRYPT_OUTPUT_FILE = "outputFile";
    public static final String ENCRYPT_PROVIDER_URI = "providerUri";

    // decrypt/verify
    public static final String DECRYPT_SIGNED_ONLY = "signedOnly";
    public static final String DECRYPT_RETURN_BYTES = "returnBinary";
    public static final String DECRYPT_CIPHERTEXT_BYTES = "ciphertextBytes";
    public static final String DECRYPT_ASSUME_SYMMETRIC = "assumeSymmetric";
    public static final String DECRYPT_LOOKUP_UNKNOWN_KEY = "lookupUnknownKey";

    // save keyring
    public static final String SAVE_KEYRING_NEW_PASSPHRASE = "newPassphrase";
    public static final String SAVE_KEYRING_CURRENT_PASSPHRASE = "currentPassphrase";
    public static final String SAVE_KEYRING_USER_IDS = "userIds";
    public static final String SAVE_KEYRING_KEYS = "keys";
    public static final String SAVE_KEYRING_KEYS_USAGES = "keysUsages";
    public static final String SAVE_KEYRING_MASTER_KEY_ID = "masterKeyId";
    public static final String SAVE_KEYRING_CAN_SIGN = "can_sign";

    // generate key
    public static final String GENERATE_KEY_ALGORITHM = "algorithm";
    public static final String GENERATE_KEY_KEY_SIZE = "keySize";
    public static final String GENERATE_KEY_SYMMETRIC_PASSPHRASE = "passphrase";
    public static final String GENERATE_KEY_MASTER_KEY = "masterKey";

    // delete file securely
    public static final String DELETE_FILE = "deleteFile";

    // import key
    public static final String IMPORT_INPUT_STREAM = "importInputStream";
    public static final String IMPORT_FILENAME = "importFilename";
    public static final String IMPORT_BYTES = "importBytes";
    // public static final String IMPORT_KEY_TYPE = "importKeyType";

    // export key
    public static final String EXPORT_OUTPUT_STREAM = "exportOutputStream";
    public static final String EXPORT_FILENAME = "exportFilename";
    public static final String EXPORT_KEY_TYPE = "exportKeyType";
    public static final String EXPORT_ALL = "exportAll";
    public static final String EXPORT_KEY_RING_MASTER_KEY_ID = "exportKeyRingId";

    // upload key
    public static final String UPLOAD_KEY_SERVER = "uploadKeyServer";
    public static final String UPLOAD_KEY_KEYRING_ROW_ID = "uploadKeyRingId";

    // query key
    public static final String QUERY_KEY_SERVER = "queryKeyServer";
    public static final String QUERY_KEY_TYPE = "queryKeyType";
    public static final String QUERY_KEY_STRING = "queryKeyString";
    public static final String QUERY_KEY_ID = "queryKeyId";

    // sign key
    public static final String SIGN_KEY_MASTER_KEY_ID = "signKeyMasterKeyId";
    public static final String SIGN_KEY_PUB_KEY_ID = "signKeyPubKeyId";

    /*
     * possible data keys as result send over messenger
     */
    // keys
    public static final String RESULT_NEW_KEY = "newKey";
    public static final String RESULT_NEW_KEY2 = "newKey2";

    // encrypt
    public static final String RESULT_SIGNATURE_BYTES = "signatureData";
    public static final String RESULT_SIGNATURE_STRING = "signatureText";
    public static final String RESULT_ENCRYPTED_STRING = "encryptedMessage";
    public static final String RESULT_ENCRYPTED_BYTES = "encryptedData";
    public static final String RESULT_URI = "resultUri";

    // decrypt/verify
    public static final String RESULT_DECRYPTED_STRING = "decryptedMessage";
    public static final String RESULT_DECRYPTED_BYTES = "decryptedData";
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

    public KeychainIntentService() {
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

        if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) ||
        		(intent.getAction() == null))) {
            Log.e(Constants.TAG,
                    "Extra bundle must contain a messenger, a data bundle, and an action!");
            return;
        }

        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
        Bundle data = extras.getBundle(EXTRA_DATA);

        OtherHelper.logDebugBundle(data, "EXTRA_DATA");

        String action = intent.getAction();
        
        // execute action from extra bundle
        if( ACTION_ENCRYPT_SIGN.equals(action)) {
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
                String encryptionPassphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmor = data.getBoolean(ENCRYPT_USE_ASCII_AMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPT_ENCRYPTION_KEYS_IDS);
                int compressionId = data.getInt(ENCRYPT_COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(ENCRYPT_GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(ENCRYPT_SIGN_ONLY);

                InputStream inStream = null;
                long inLength = -1;
                InputData inputData = null;
                OutputStream outStream = null;
                String streamFilename = null;
                switch (target) {
                case TARGET_BYTES: /* encrypting bytes directly */
                    byte[] bytes = data.getByteArray(ENCRYPT_MESSAGE_BYTES);

                    inStream = new ByteArrayInputStream(bytes);
                    inLength = bytes.length;

                    inputData = new InputData(inStream, inLength);
                    outStream = new ByteArrayOutputStream();

                    break;
                case TARGET_FILE: /* encrypting file */
                    String inputFile = data.getString(ENCRYPT_INPUT_FILE);
                    String outputFile = data.getString(ENCRYPT_OUTPUT_FILE);

                    // check if storage is ready
                    if (!FileHelper.isStorageMounted(inputFile)
                            || !FileHelper.isStorageMounted(outputFile)) {
                        throw new PgpGeneralException(
                                getString(R.string.error_externalStorageNotReady));
                    }

                    inStream = new FileInputStream(inputFile);
                    File file = new File(inputFile);
                    inLength = file.length();
                    inputData = new InputData(inStream, inLength);

                    outStream = new FileOutputStream(outputFile);

                    break;

                case TARGET_STREAM: /* Encrypting stream from content uri */
                    Uri providerUri = (Uri) data.getParcelable(ENCRYPT_PROVIDER_URI);

                    // InputStream
                    InputStream in = getContentResolver().openInputStream(providerUri);
                    inLength = PgpMain.getLengthOfStream(in);
                    inputData = new InputData(in, inLength);

                    // OutputStream
                    try {
                        while (true) {
                            streamFilename = PgpMain.generateRandomFilename(32);
                            if (streamFilename == null) {
                                throw new PgpMain.PgpGeneralException(
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
                    throw new PgpMain.PgpGeneralException("No target choosen!");

                }

                /* Operation */

                if (generateSignature) {
                    Log.d(Constants.TAG, "generating signature...");
                    PgpMain.generateSignature(this, this, inputData, outStream, useAsciiArmor,
                            false, secretKeyId, PassphraseCacheService.getCachedPassphrase(this,
                                    secretKeyId), Preferences.getPreferences(this)
                                    .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                                    .getForceV3Signatures());
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    PgpMain.signText(this, this, inputData, outStream, secretKeyId,
                            PassphraseCacheService.getCachedPassphrase(this, secretKeyId),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures());
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    PgpMain.encryptAndSign(this, this, inputData, outStream, useAsciiArmor,
                            compressionId, encryptionKeyIds, encryptionPassphrase, Preferences
                                    .getPreferences(this).getDefaultEncryptionAlgorithm(),
                            secretKeyId,
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(),
                            PassphraseCacheService.getCachedPassphrase(this, secretKeyId));
                }

                outStream.close();

                /* Output */

                Bundle resultData = new Bundle();

                switch (target) {
                case TARGET_BYTES:
                    if (useAsciiArmor) {
                        String output = new String(
                                ((ByteArrayOutputStream) outStream).toByteArray());
                        if (generateSignature) {
                            resultData.putString(RESULT_SIGNATURE_STRING, output);
                        } else {
                            resultData.putString(RESULT_ENCRYPTED_STRING, output);
                        }
                    } else {
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        if (generateSignature) {
                            resultData.putByteArray(RESULT_SIGNATURE_BYTES, output);
                        } else {
                            resultData.putByteArray(RESULT_ENCRYPTED_BYTES, output);
                        }
                    }

                    break;
                case TARGET_FILE:
                    // nothing, file was written, just send okay

                    break;
                case TARGET_STREAM:
                    String uri = DataStream.buildDataStreamUri(streamFilename).toString();
                    resultData.putString(RESULT_URI, uri);

                    break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_DECRYPT_VERIFY.equals(action)) {
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
                byte[] bytes = data.getByteArray(DECRYPT_CIPHERTEXT_BYTES);
                boolean signedOnly = data.getBoolean(DECRYPT_SIGNED_ONLY);
                boolean returnBytes = data.getBoolean(DECRYPT_RETURN_BYTES);
                boolean assumeSymmetricEncryption = data.getBoolean(DECRYPT_ASSUME_SYMMETRIC);

                boolean lookupUnknownKey = data.getBoolean(DECRYPT_LOOKUP_UNKNOWN_KEY);

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
                    String inputFile = data.getString(ENCRYPT_INPUT_FILE);
                    String outputFile = data.getString(ENCRYPT_OUTPUT_FILE);

                    // check if storage is ready
                    if (!FileHelper.isStorageMounted(inputFile)
                            || !FileHelper.isStorageMounted(outputFile)) {
                        throw new PgpGeneralException(
                                getString(R.string.error_externalStorageNotReady));
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
                    Uri providerUri = (Uri) data.getParcelable(ENCRYPT_PROVIDER_URI);

                    // InputStream
                    InputStream in = getContentResolver().openInputStream(providerUri);
                    inLength = PgpMain.getLengthOfStream(in);
                    inputData = new InputData(in, inLength);

                    // OutputStream
                    try {
                        while (true) {
                            streamFilename = PgpMain.generateRandomFilename(32);
                            if (streamFilename == null) {
                                throw new PgpMain.PgpGeneralException(
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
                    throw new PgpMain.PgpGeneralException("No target choosen!");

                }

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                if (signedOnly) {
                    resultData = PgpMain.verifyText(this, this, inputData, outStream,
                            lookupUnknownKey);
                } else {
                    resultData = PgpMain.decryptAndVerify(this, this, inputData, outStream,
                            PassphraseCacheService.getCachedPassphrase(this, secretKeyId),
                            assumeSymmetricEncryption);
                }

                outStream.close();

                /* Output */

                switch (target) {
                case TARGET_BYTES:
                    if (returnBytes) {
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        resultData.putByteArray(RESULT_DECRYPTED_BYTES, output);
                    } else {
                        String output = new String(
                                ((ByteArrayOutputStream) outStream).toByteArray());
                        resultData.putString(RESULT_DECRYPTED_STRING, output);
                    }

                    break;
                case TARGET_FILE:
                    // nothing, file was written, just send okay and verification bundle

                    break;
                case TARGET_STREAM:
                    String uri = DataStream.buildDataStreamUri(streamFilename).toString();
                    resultData.putString(RESULT_URI, uri);

                    break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_SAVE_KEYRING.equals(action)) {
            try {
                /* Input */
                String oldPassPhrase = data.getString(SAVE_KEYRING_CURRENT_PASSPHRASE);
                String newPassPhrase = data.getString(SAVE_KEYRING_NEW_PASSPHRASE);
                boolean canSign = true;

                if (data.containsKey(SAVE_KEYRING_CAN_SIGN)) {
                    canSign = data.getBoolean(SAVE_KEYRING_CAN_SIGN);
                }

                if (newPassPhrase == null) {
                    newPassPhrase = oldPassPhrase;
                }
                ArrayList<String> userIds = data.getStringArrayList(SAVE_KEYRING_USER_IDS);
                ArrayList<PGPSecretKey> keys = PgpConversionHelper.BytesToPGPSecretKeyList(data
                        .getByteArray(SAVE_KEYRING_KEYS));
                ArrayList<Integer> keysUsages = data.getIntegerArrayList(SAVE_KEYRING_KEYS_USAGES);
                long masterKeyId = data.getLong(SAVE_KEYRING_MASTER_KEY_ID);

                /* Operation */
                if (!canSign) {
                    PgpMain.changeSecretKeyPassphrase(this,
                            ProviderHelper.getPGPSecretKeyRingByKeyId(this, masterKeyId),
                            oldPassPhrase, newPassPhrase, this);
                } else {
                    PgpMain.buildSecretKey(this, userIds, keys, keysUsages, masterKeyId,
                            oldPassPhrase, newPassPhrase, this);
                }
                PassphraseCacheService.addCachedPassphrase(this, masterKeyId, newPassPhrase);

                /* Output */
                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_GENERATE_KEY.equals(action)) {
            try {
                /* Input */
                int algorithm = data.getInt(GENERATE_KEY_ALGORITHM);
                String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);
                int keysize = data.getInt(GENERATE_KEY_KEY_SIZE);
                PGPSecretKey masterKey = null;
                if (data.containsKey(GENERATE_KEY_MASTER_KEY)) {
                    masterKey = PgpConversionHelper.BytesToPGPSecretKey(data
                            .getByteArray(GENERATE_KEY_MASTER_KEY));
                }

                /* Operation */
                PGPSecretKeyRing newKeyRing = PgpMain.createKey(this, algorithm, keysize,
                        passphrase, masterKey);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PgpConversionHelper.PGPSecretKeyRingToBytes(newKeyRing));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_GENERATE_DEFAULT_RSA_KEYS.equals(action)) {
            // generate one RSA 2048 key for signing and one subkey for encrypting!
            try {
                /* Input */
                String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                /* Operation */
                PGPSecretKeyRing masterKeyRing = PgpMain.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, null);

                PGPSecretKeyRing subKeyRing = PgpMain.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, masterKeyRing.getSecretKey());

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PgpConversionHelper.PGPSecretKeyRingToBytes(masterKeyRing));
                resultData.putByteArray(RESULT_NEW_KEY2,
                        PgpConversionHelper.PGPSecretKeyRingToBytes(subKeyRing));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_DELETE_FILE_SECURELY.equals(action)) {
            try {
                /* Input */
                String deleteFile = data.getString(DELETE_FILE);

                /* Operation */
                try {
                    PgpMain.deleteFileSecurely(this, this, new File(deleteFile));
                } catch (FileNotFoundException e) {
                    throw new PgpMain.PgpGeneralException(getString(R.string.error_fileNotFound,
                            deleteFile));
                } catch (IOException e) {
                    throw new PgpMain.PgpGeneralException(getString(
                            R.string.error_fileDeleteFailed, deleteFile));
                }

                /* Output */
                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_IMPORT_KEYRING.equals(action)) {
            try {

                /* Input */
                int target = data.getInt(TARGET);

                // int keyType = Id.type.public_key;
                // if (data.containsKey(IMPORT_KEY_TYPE)) {
                // keyType = data.getInt(IMPORT_KEY_TYPE);
                // }

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
                resultData = PgpMain.importKeyRings(this, inputData, this);

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_EXPORT_KEYRING.equals(action)) {
            try {

                /* Input */
                int keyType = Id.type.public_key;
                if (data.containsKey(EXPORT_KEY_TYPE)) {
                    keyType = data.getInt(EXPORT_KEY_TYPE);
                }

                String outputFile = data.getString(EXPORT_FILENAME);

                boolean exportAll = data.getBoolean(EXPORT_ALL);
                long keyRingMasterKeyId = -1;
                if (!exportAll) {
                    keyRingMasterKeyId = data.getLong(EXPORT_KEY_RING_MASTER_KEY_ID);
                }

                /* Operation */

                // check if storage is ready
                if (!FileHelper.isStorageMounted(outputFile)) {
                    throw new PgpGeneralException(getString(R.string.error_externalStorageNotReady));
                }

                // OutputStream
                FileOutputStream outStream = new FileOutputStream(outputFile);

                ArrayList<Long> keyRingMasterKeyIds = new ArrayList<Long>();
                if (exportAll) {
                    // get all key ring row ids based on export type

                    if (keyType == Id.type.public_key) {
                        keyRingMasterKeyIds = ProviderHelper.getPublicKeyRingsMasterKeyIds(this);
                    } else {
                        keyRingMasterKeyIds = ProviderHelper.getSecretKeyRingsMasterKeyIds(this);
                    }
                } else {
                    keyRingMasterKeyIds.add(keyRingMasterKeyId);
                }

                Bundle resultData = new Bundle();
                resultData = PgpMain.exportKeyRings(this, keyRingMasterKeyIds, keyType, outStream,
                        this);

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_UPLOAD_KEYRING.equals(action)) {
            try {

                /* Input */
                int keyRingRowId = data.getInt(UPLOAD_KEY_KEYRING_ROW_ID);
                String keyServer = data.getString(UPLOAD_KEY_SERVER);

                /* Operation */
                HkpKeyServer server = new HkpKeyServer(keyServer);

                PGPPublicKeyRing keyring = ProviderHelper.getPGPPublicKeyRingByRowId(this,
                        keyRingRowId);
                if (keyring != null) {
                    boolean uploaded = PgpMain.uploadKeyRingToServer(server,
                            (PGPPublicKeyRing) keyring);
                    if (!uploaded) {
                        throw new PgpGeneralException("Unable to export key to selected server");
                    }
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_QUERY_KEYRING.equals(action)) {
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

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
        else if(ACTION_SIGN_KEYRING.equals(action)) {
            try {

                /* Input */
                long masterKeyId = data.getLong(SIGN_KEY_MASTER_KEY_ID);
                long pubKeyId = data.getLong(SIGN_KEY_PUB_KEY_ID);

                /* Operation */
                String signaturePassPhrase = PassphraseCacheService.getCachedPassphrase(this,
                        masterKeyId);

                PGPPublicKeyRing signedPubKeyRing = PgpMain.signKey(this, masterKeyId, pubKeyId,
                        signaturePassPhrase);

                // store the signed key in our local cache
                int retval = PgpMain.storeKeyRingInCache(this, signedPubKeyRing);
                if (retval != Id.return_value.ok && retval != Id.return_value.updated) {
                    throw new PgpGeneralException("Failed to store signed key in local cache");
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }
    }

    private void sendErrorToHandler(Exception e) {
        Log.e(Constants.TAG, "ApgService Exception: ", e);
        e.printStackTrace();

        Bundle data = new Bundle();
        data.putString(KeychainIntentServiceHandler.DATA_ERROR, e.getMessage());
        sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
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
            data.putString(KeychainIntentServiceHandler.DATA_MESSAGE, message);
        }
        data.putInt(KeychainIntentServiceHandler.DATA_PROGRESS, progress);
        data.putInt(KeychainIntentServiceHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_UPDATE_PROGRESS, null, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }
}
