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
import java.util.ArrayList;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.ProgressDialogUpdater;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.FileHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.helper.PGPHelper.GeneralException;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.provider.DataProvider;
import org.thialfihar.android.apg.util.InputData;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */

/**
 * TODO:
 * 
 * - ProgressDialogUpdater rework???
 * 
 * - put recurring things into private functions when possible
 * 
 * 
 */
public class ApgService extends IntentService implements ProgressDialogUpdater {

    // extras that can be given by intent
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    // keys for data bundle

    // encrypt
    public static final String SECRET_KEY_ID = "secret_key_id";
    public static final String USE_ASCII_AMOR = "use_ascii_amor";
    public static final String ENCRYPTION_KEYS_IDS = "encryption_keys_ids";
    public static final String SIGNATURE_KEY_ID = "signature_key_id";
    public static final String COMPRESSION_ID = "compression_id";
    public static final String GENERATE_SIGNATURE = "generate_signature";
    public static final String SIGN_ONLY = "sign_only";
    public static final String MESSAGE_BYTES = "message_bytes";
    public static final String INPUT_FILE = "input_file";
    public static final String OUTPUT_FILE = "output_file";
    public static final String PROVIDER_URI = "provider_uri";

    // decrypt
    public static final String SIGNED_ONLY = "signed_only";
    public static final String RETURN_BYTES = "return_binary";
    public static final String CIPHERTEXT_BYTES = "ciphertext_bytes";
    public static final String ASSUME_SYMMETRIC = "assume_symmetric";

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
    public static final String SYMMETRIC_PASSPHRASE = "passphrase";
    public static final String MASTER_KEY = "master_key";

    // delete file securely
    public static final String DELETE_FILE = "delete_file";

    // possible EXTRA_ACTIONs
    public static final int ACTION_ENCRYPT_SIGN_BYTES = 10;
    public static final int ACTION_ENCRYPT_SIGN_FILE = 11;
    public static final int ACTION_ENCRYPT_SIGN_STREAM = 12;

    public static final int ACTION_DECRYPT_BYTES = 20;
    public static final int ACTION_DECRYPT_FILE = 21;
    public static final int ACTION_DECRYPT_STREAM = 22;

    public static final int ACTION_SAVE_KEYRING = 30;
    public static final int ACTION_GENERATE_KEY = 31;
    public static final int ACTION_GENERATE_DEFAULT_RSA_KEYS = 32;

    public static final int ACTION_DELETE_FILE_SECURELY = 40;

    // possible data keys as result
    // keys
    public static final String RESULT_NEW_KEY = "new_key";
    public static final String RESULT_NEW_KEY2 = "new_key2";

    // encrypt
    public static final String RESULT_SIGNATURE_DATA = "signature_data";
    public static final String RESULT_SIGNATURE_TEXT = "signature_text";
    public static final String RESULT_ENCRYPTED_MESSAGE = "encrypted_message";
    public static final String RESULT_ENCRYPTED_DATA = "encrypted_data";
    public static final String RESULT_URI = "result_uri";

    // decrypt
    public static final String RESULT_DECRYPTED_MESSAGE = "decrypted_message";
    public static final String RESULT_DECRYPTED_DATA = "decrypted_data";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String EXTRA_SIGNATURE_USER_ID = "signature_user_id";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signature_success";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signature_unknown";

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
        int action = extras.getInt(EXTRA_ACTION);

        // execute action from extra bundle
        switch (action) {
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
                PGPHelper.buildSecretKey(this, userIds, keys, keysUsages, masterKeyId,
                        oldPassPhrase, newPassPhrase, this);
                PGPHelper.setCachedPassPhrase(masterKeyId, newPassPhrase);

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
                PGPSecretKeyRing newKeyRing = PGPHelper.createKey(this, algorithm, keysize,
                        passphrase, masterKey);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(newKeyRing));
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
                PGPSecretKeyRing masterKeyRing = PGPHelper.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, null);

                PGPSecretKeyRing subKeyRing = PGPHelper.createKey(this, Id.choice.algorithm.rsa,
                        2048, passphrase, masterKeyRing.getSecretKey());

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(masterKeyRing));
                resultData.putByteArray(RESULT_NEW_KEY2,
                        PGPConversionHelper.PGPSecretKeyRingToBytes(subKeyRing));
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_ENCRYPT_SIGN_BYTES:

            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);

                byte[] bytes = data.getByteArray(MESSAGE_BYTES);

                boolean useAsciiArmour = data.getBoolean(USE_ASCII_AMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPTION_KEYS_IDS);
                long signatureKeyId = data.getLong(SIGNATURE_KEY_ID);
                int compressionId = data.getInt(COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(SIGN_ONLY);

                /* Operation */
                ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
                int inLength = bytes.length;

                InputData inputData = new InputData(inStream, inLength);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();

                if (generateSignature) {
                    Log.d(Constants.TAG, "generate signature...");
                    PGPHelper.generateSignature(this, inputData, outStream, useAsciiArmour, false,
                            secretKeyId, PGPHelper.getCachedPassPhrase(secretKeyId), Preferences
                                    .getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(), this);
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    PGPHelper.signText(this, inputData, outStream, secretKeyId, PGPHelper
                            .getCachedPassPhrase(secretKeyId), Preferences.getPreferences(this)
                            .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                            .getForceV3Signatures(), this);
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    PGPHelper.encrypt(this, inputData, outStream, useAsciiArmour, encryptionKeyIds,
                            signatureKeyId, PGPHelper.getCachedPassPhrase(signatureKeyId), this,
                            Preferences.getPreferences(this).getDefaultEncryptionAlgorithm(),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(),
                            compressionId, Preferences.getPreferences(this).getForceV3Signatures(),
                            passphrase);
                }

                outStream.close();

                /* Output */
                Bundle resultData = new Bundle();

                if (useAsciiArmour) {
                    String output = new String(outStream.toByteArray());
                    if (generateSignature) {
                        resultData.putString(RESULT_SIGNATURE_TEXT, output);
                    } else {
                        resultData.putString(RESULT_ENCRYPTED_MESSAGE, output);
                    }
                } else {
                    byte output[] = outStream.toByteArray();
                    if (generateSignature) {
                        resultData.putByteArray(RESULT_SIGNATURE_DATA, output);
                    } else {
                        resultData.putByteArray(RESULT_ENCRYPTED_DATA, output);
                    }
                }

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_ENCRYPT_SIGN_FILE:
            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);

                String inputFile = data.getString(INPUT_FILE);
                String outputFile = data.getString(OUTPUT_FILE);

                boolean useAsciiArmour = data.getBoolean(USE_ASCII_AMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPTION_KEYS_IDS);
                long signatureKeyId = data.getLong(SIGNATURE_KEY_ID);
                int compressionId = data.getInt(COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(SIGN_ONLY);

                /* Operation */
                // check if storage is ready
                if (!FileHelper.isStorageMounted(inputFile)
                        || !FileHelper.isStorageMounted(outputFile)) {
                    sendErrorToHandler(new GeneralException(
                            getString(R.string.error_externalStorageNotReady)));
                    return;
                }

                // InputStream
                long inLength = -1;
                FileInputStream inStream = new FileInputStream(inputFile);
                File file = new File(inputFile);
                inLength = file.length();
                InputData inputData = new InputData(inStream, inLength);

                // OutputStream
                FileOutputStream outStream = new FileOutputStream(outputFile);

                if (generateSignature) {
                    Log.d(Constants.TAG, "generate signature...");
                    PGPHelper.generateSignature(this, inputData, outStream, useAsciiArmour, true,
                            secretKeyId, PGPHelper.getCachedPassPhrase(secretKeyId), Preferences
                                    .getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(), this);
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    PGPHelper.signText(this, inputData, outStream, secretKeyId, PGPHelper
                            .getCachedPassPhrase(secretKeyId), Preferences.getPreferences(this)
                            .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                            .getForceV3Signatures(), this);
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    PGPHelper.encrypt(this, inputData, outStream, useAsciiArmour, encryptionKeyIds,
                            signatureKeyId, PGPHelper.getCachedPassPhrase(signatureKeyId), this,
                            Preferences.getPreferences(this).getDefaultEncryptionAlgorithm(),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(),
                            compressionId, Preferences.getPreferences(this).getForceV3Signatures(),
                            passphrase);
                }

                outStream.close();

                /* Output */
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
            break;

        case ACTION_ENCRYPT_SIGN_STREAM:
            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                String passphrase = data.getString(SYMMETRIC_PASSPHRASE);

                Uri providerUri = Uri.parse(data.getString(PROVIDER_URI));

                boolean useAsciiArmour = data.getBoolean(USE_ASCII_AMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPTION_KEYS_IDS);
                long signatureKeyId = data.getLong(SIGNATURE_KEY_ID);
                int compressionId = data.getInt(COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(SIGN_ONLY);
                
                /* Operation */
                // InputStream
                InputStream in = getContentResolver().openInputStream(providerUri);
                long inLength = PGPHelper.getLengthOfStream(in);
                InputData inputData = new InputData(in, inLength);

                // OutputStream
                String streamFilename = null;
                try {
                    while (true) {
                        streamFilename = PGPHelper.generateRandomString(32);
                        if (streamFilename == null) {
                            throw new PGPHelper.GeneralException(
                                    "couldn't generate random file name");
                        }
                        openFileInput(streamFilename).close();
                    }
                } catch (FileNotFoundException e) {
                    // found a name that isn't used yet
                }
                FileOutputStream outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                if (generateSignature) {
                    PGPHelper.generateSignature(this, inputData, outStream, useAsciiArmour, true,
                            secretKeyId, PGPHelper.getCachedPassPhrase(secretKeyId), Preferences
                                    .getPreferences(this).getDefaultHashAlgorithm(), Preferences
                                    .getPreferences(this).getForceV3Signatures(), this);
                } else if (signOnly) {
                    PGPHelper.signText(this, inputData, outStream, secretKeyId, PGPHelper
                            .getCachedPassPhrase(secretKeyId), Preferences.getPreferences(this)
                            .getDefaultHashAlgorithm(), Preferences.getPreferences(this)
                            .getForceV3Signatures(), this);
                } else {
                    PGPHelper.encrypt(this, inputData, outStream, useAsciiArmour, encryptionKeyIds,
                            signatureKeyId, PGPHelper.getCachedPassPhrase(signatureKeyId), this,
                            Preferences.getPreferences(this).getDefaultEncryptionAlgorithm(),
                            Preferences.getPreferences(this).getDefaultHashAlgorithm(),
                            compressionId, Preferences.getPreferences(this).getForceV3Signatures(),
                            passphrase);
                }

                outStream.close();

                /* Output */
                Bundle resultData = new Bundle();

                String uri = "content://" + DataProvider.AUTHORITY + "/data/" + streamFilename;
                resultData.putString(RESULT_URI, uri);

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
                    PGPHelper.deleteFileSecurely(this, new File(deleteFile), this);
                } catch (FileNotFoundException e) {
                    throw new PGPHelper.GeneralException(getString(R.string.error_fileNotFound,
                            deleteFile));
                } catch (IOException e) {
                    throw new PGPHelper.GeneralException(getString(R.string.error_fileDeleteFailed,
                            deleteFile));
                }

                /* Output */
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_DECRYPT_BYTES:
            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                byte[] bytes = data.getByteArray(CIPHERTEXT_BYTES);
                boolean signedOnly = data.getBoolean(SIGNED_ONLY);
                boolean returnBytes = data.getBoolean(RETURN_BYTES);
                boolean assumeSymmetricEncryption = data.getBoolean(ASSUME_SYMMETRIC);

                /* Operation */
                ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
                int inLength = bytes.length;

                InputData inputData = new InputData(inStream, inLength);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                if (signedOnly) {
                    resultData = PGPHelper.verifyText(this, inputData, outStream, this);
                } else {
                    resultData = PGPHelper.decrypt(this, inputData, outStream,
                            PGPHelper.getCachedPassPhrase(secretKeyId), this,
                            assumeSymmetricEncryption);
                }

                outStream.close();

                /* Output */
                if (returnBytes) {
                    byte output[] = outStream.toByteArray();
                    resultData.putByteArray(RESULT_DECRYPTED_DATA, output);
                } else {
                    String output = new String(outStream.toByteArray());
                    resultData.putString(RESULT_DECRYPTED_MESSAGE, output);
                }

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        case ACTION_DECRYPT_FILE:
            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                boolean signedOnly = data.getBoolean(SIGNED_ONLY);
                boolean assumeSymmetricEncryption = data.getBoolean(ASSUME_SYMMETRIC);

                String inputFile = data.getString(INPUT_FILE);
                String outputFile = data.getString(OUTPUT_FILE);

                /* Operation */
                // check if storage is ready
                if (!FileHelper.isStorageMounted(inputFile)
                        || !FileHelper.isStorageMounted(outputFile)) {
                    sendErrorToHandler(new GeneralException(
                            getString(R.string.error_externalStorageNotReady)));
                    return;
                }

                // InputStream
                long inLength = -1;
                FileInputStream inStream = new FileInputStream(inputFile);
                File file = new File(inputFile);
                inLength = file.length();
                InputData inputData = new InputData(inStream, inLength);

                // OutputStream
                FileOutputStream outStream = new FileOutputStream(outputFile);

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional output values for the
                // verification of signatures
                if (signedOnly) {
                    resultData = PGPHelper.verifyText(this, inputData, outStream, this);
                } else {
                    resultData = PGPHelper.decrypt(this, inputData, outStream,
                            PGPHelper.getCachedPassPhrase(secretKeyId), this,
                            assumeSymmetricEncryption);
                }

                outStream.close();
                
                /* Output */
                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
            break;

        case ACTION_DECRYPT_STREAM:
            try {
                /* Input */
                long secretKeyId = data.getLong(SECRET_KEY_ID);
                boolean signedOnly = data.getBoolean(SIGNED_ONLY);
                boolean assumeSymmetricEncryption = data.getBoolean(ASSUME_SYMMETRIC);
                Uri providerUri = Uri.parse(data.getString(PROVIDER_URI));

                /* Operation */
                // InputStream
                InputStream in = getContentResolver().openInputStream(providerUri);
                long inLength = PGPHelper.getLengthOfStream(in);
                InputData inputData = new InputData(in, inLength);

                // OutputStream
                String streamFilename = null;
                try {
                    while (true) {
                        streamFilename = PGPHelper.generateRandomString(32);
                        if (streamFilename == null) {
                            throw new PGPHelper.GeneralException(
                                    "couldn't generate random file name");
                        }
                        openFileInput(streamFilename).close();
                    }
                } catch (FileNotFoundException e) {
                    // found a name that isn't used yet
                }
                FileOutputStream outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional output values for the
                // verification of signatures
                if (signedOnly) {
                    resultData = PGPHelper.verifyText(this, inputData, outStream, this);
                } else {
                    resultData = PGPHelper.decrypt(this, inputData, outStream,
                            PGPHelper.getCachedPassPhrase(secretKeyId), this,
                            assumeSymmetricEncryption);
                }

                outStream.close();

                /* Output */
                String uri = "content://" + DataProvider.AUTHORITY + "/data/" + streamFilename;
                resultData.putString(RESULT_URI, uri);

                sendMessageToHandler(ApgHandler.MESSAGE_OKAY, resultData);
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
        Log.d(Constants.TAG, "Send message by setProgress");

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
