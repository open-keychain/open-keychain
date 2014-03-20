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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.spongycastle.openpgp.*;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.*;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.DataStream;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */
public class KeychainIntentService extends IntentService
        implements ProgressDialogUpdater, KeychainServiceListener {

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    /* possible actions */
    public static final String ACTION_ENCRYPT_SIGN = Constants.INTENT_PREFIX + "ENCRYPT_SIGN";

    public static final String ACTION_DECRYPT_VERIFY = Constants.INTENT_PREFIX + "DECRYPT_VERIFY";

    public static final String ACTION_SAVE_KEYRING = Constants.INTENT_PREFIX + "SAVE_KEYRING";
    public static final String ACTION_GENERATE_KEY = Constants.INTENT_PREFIX + "GENERATE_KEY";
    public static final String ACTION_GENERATE_DEFAULT_RSA_KEYS = Constants.INTENT_PREFIX
            + "GENERATE_DEFAULT_RSA_KEYS";

    public static final String ACTION_DELETE_FILE_SECURELY = Constants.INTENT_PREFIX
            + "DELETE_FILE_SECURELY";

    public static final String ACTION_IMPORT_KEYRING = Constants.INTENT_PREFIX + "IMPORT_KEYRING";
    public static final String ACTION_EXPORT_KEYRING = Constants.INTENT_PREFIX + "EXPORT_KEYRING";

    public static final String ACTION_UPLOAD_KEYRING = Constants.INTENT_PREFIX + "UPLOAD_KEYRING";
    public static final String ACTION_DOWNLOAD_AND_IMPORT_KEYS = Constants.INTENT_PREFIX + "QUERY_KEYRING";

    public static final String ACTION_CERTIFY_KEYRING = Constants.INTENT_PREFIX + "SIGN_KEYRING";

    /* keys for data bundle */

    // encrypt, decrypt, import export
    public static final String TARGET = "target";
    // possible targets:
    public static final int TARGET_BYTES = 1;
    public static final int TARGET_URI = 2;
    public static final int TARGET_STREAM = 3;

    // encrypt
    public static final String ENCRYPT_SECRET_KEY_ID = "secret_key_id";
    public static final String ENCRYPT_USE_ASCII_ARMOR = "use_ascii_armor";
    public static final String ENCRYPT_ENCRYPTION_KEYS_IDS = "encryption_keys_ids";
    public static final String ENCRYPT_COMPRESSION_ID = "compression_id";
    public static final String ENCRYPT_GENERATE_SIGNATURE = "generate_signature";
    public static final String ENCRYPT_SIGN_ONLY = "sign_only";
    public static final String ENCRYPT_MESSAGE_BYTES = "message_bytes";
    public static final String ENCRYPT_INPUT_FILE = "input_file";
    public static final String ENCRYPT_OUTPUT_FILE = "output_file";
    public static final String ENCRYPT_PROVIDER_URI = "provider_uri";

    // decrypt/verify
    public static final String DECRYPT_RETURN_BYTES = "return_binary";
    public static final String DECRYPT_CIPHERTEXT_BYTES = "ciphertext_bytes";
    public static final String DECRYPT_ASSUME_SYMMETRIC = "assume_symmetric";

    // save keyring
    public static final String SAVE_KEYRING_NEW_PASSPHRASE = "new_passphrase";
    public static final String SAVE_KEYRING_CURRENT_PASSPHRASE = "current_passphrase";
    public static final String SAVE_KEYRING_USER_IDS = "user_ids";
    public static final String SAVE_KEYRING_KEYS = "keys";
    public static final String SAVE_KEYRING_KEYS_USAGES = "keys_usages";
    public static final String SAVE_KEYRING_KEYS_EXPIRY_DATES = "keys_expiry_dates";
    public static final String SAVE_KEYRING_MASTER_KEY_ID = "master_key_id";
    public static final String SAVE_KEYRING_CAN_SIGN = "can_sign";

    // generate key
    public static final String GENERATE_KEY_ALGORITHM = "algorithm";
    public static final String GENERATE_KEY_KEY_SIZE = "key_size";
    public static final String GENERATE_KEY_SYMMETRIC_PASSPHRASE = "passphrase";
    public static final String GENERATE_KEY_MASTER_KEY = "master_key";

    // delete file securely
    public static final String DELETE_FILE = "deleteFile";

    // import key
    public static final String IMPORT_KEY_LIST = "import_key_list";

    // export key
    public static final String EXPORT_OUTPUT_STREAM = "export_output_stream";
    public static final String EXPORT_FILENAME = "export_filename";
    public static final String EXPORT_KEY_TYPE = "export_key_type";
    public static final String EXPORT_ALL = "export_all";
    public static final String EXPORT_KEY_RING_MASTER_KEY_ID = "export_key_ring_id";

    // upload key
    public static final String UPLOAD_KEY_SERVER = "upload_key_server";

    // query key
    public static final String DOWNLOAD_KEY_SERVER = "query_key_server";
    public static final String DOWNLOAD_KEY_LIST = "query_key_id";

    // sign key
    public static final String CERTIFY_KEY_MASTER_KEY_ID = "sign_key_master_key_id";
    public static final String CERTIFY_KEY_PUB_KEY_ID = "sign_key_pub_key_id";
    public static final String CERTIFY_KEY_UIDS = "sign_key_uids";

    /*
     * possible data keys as result send over messenger
     */
    // keys
    public static final String RESULT_NEW_KEY = "new_key";
    public static final String RESULT_NEW_KEY2 = "new_key2";

    // encrypt
    public static final String RESULT_SIGNATURE_BYTES = "signature_data";
    public static final String RESULT_SIGNATURE_STRING = "signature_text";
    public static final String RESULT_ENCRYPTED_STRING = "encrypted_message";
    public static final String RESULT_ENCRYPTED_BYTES = "encrypted_data";
    public static final String RESULT_URI = "result_uri";

    // decrypt/verify
    public static final String RESULT_DECRYPTED_STRING = "decrypted_message";
    public static final String RESULT_DECRYPTED_BYTES = "decrypted_data";
    public static final String RESULT_DECRYPT_VERIFY_RESULT = "signature";

    // import
    public static final String RESULT_IMPORT_ADDED = "added";
    public static final String RESULT_IMPORT_UPDATED = "updated";
    public static final String RESULT_IMPORT_BAD = "bad";

    // export
    public static final String RESULT_EXPORT = "exported";

    // query
    public static final String RESULT_QUERY_KEY_DATA = "query_key_data";
    public static final String RESULT_QUERY_KEY_SEARCH_RESULT = "query_key_search_result";

    Messenger mMessenger;

    private boolean mIsCanceled;

    public KeychainIntentService() {
        super("KeychainIntentService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mIsCanceled = true;
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

        if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) || (intent
                .getAction() == null))) {
            Log.e(Constants.TAG,
                    "Extra bundle must contain a messenger, a data bundle, and an action!");
            return;
        }

        Uri dataUri = intent.getData();

        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
        Bundle data = extras.getBundle(EXTRA_DATA);

        OtherHelper.logDebugBundle(data, "EXTRA_DATA");

        String action = intent.getAction();

        // executeServiceMethod action from extra bundle
        if (ACTION_ENCRYPT_SIGN.equals(action)) {
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
                String encryptionPassphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmor = data.getBoolean(ENCRYPT_USE_ASCII_ARMOR);
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
                    case TARGET_URI: /* encrypting file */
                        String inputFile = data.getString(ENCRYPT_INPUT_FILE);
                        String outputFile = data.getString(ENCRYPT_OUTPUT_FILE);

                        // check if storage is ready
                        if (!FileHelper.isStorageMounted(inputFile)
                                || !FileHelper.isStorageMounted(outputFile)) {
                            throw new PgpGeneralException(
                                    getString(R.string.error_external_storage_not_ready));
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
                        inLength = PgpHelper.getLengthOfStream(in);
                        inputData = new InputData(in, inLength);

                        // OutputStream
                        try {
                            while (true) {
                                streamFilename = PgpHelper.generateRandomFilename(32);
                                if (streamFilename == null) {
                                    throw new PgpGeneralException("couldn't generate random file name");
                                }
                                openFileInput(streamFilename).close();
                            }
                        } catch (FileNotFoundException e) {
                            // found a name that isn't used yet
                        }
                        outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                        break;

                    default:
                        throw new PgpGeneralException("No target choosen!");

                }

                /* Operation */
                PgpSignEncrypt.Builder builder =
                        new PgpSignEncrypt.Builder(this, inputData, outStream);
                builder.progress(this);

                if (generateSignature) {
                    Log.d(Constants.TAG, "generating signature...");
                    builder.enableAsciiArmorOutput(useAsciiArmor)
                            .signatureForceV3(Preferences.getPreferences(this).getForceV3Signatures())
                            .signatureKeyId(secretKeyId)
                            .signatureHashAlgorithm(
                                    Preferences.getPreferences(this).getDefaultHashAlgorithm())
                            .signaturePassphrase(
                                    PassphraseCacheService.getCachedPassphrase(this, secretKeyId));

                    builder.build().generateSignature();
                } else if (signOnly) {
                    Log.d(Constants.TAG, "sign only...");
                    builder.enableAsciiArmorOutput(useAsciiArmor)
                            .signatureForceV3(Preferences.getPreferences(this).getForceV3Signatures())
                            .signatureKeyId(secretKeyId)
                            .signatureHashAlgorithm(
                                    Preferences.getPreferences(this).getDefaultHashAlgorithm())
                            .signaturePassphrase(
                                    PassphraseCacheService.getCachedPassphrase(this, secretKeyId));

                    builder.build().execute();
                } else {
                    Log.d(Constants.TAG, "encrypt...");
                    builder.enableAsciiArmorOutput(useAsciiArmor)
                            .compressionId(compressionId)
                            .symmetricEncryptionAlgorithm(
                                    Preferences.getPreferences(this).getDefaultEncryptionAlgorithm())
                            .signatureForceV3(Preferences.getPreferences(this).getForceV3Signatures())
                            .encryptionKeyIds(encryptionKeyIds)
                            .encryptionPassphrase(encryptionPassphrase)
                            .signatureKeyId(secretKeyId)
                            .signatureHashAlgorithm(
                                    Preferences.getPreferences(this).getDefaultHashAlgorithm())
                            .signaturePassphrase(
                                    PassphraseCacheService.getCachedPassphrase(this, secretKeyId));

                    builder.build().execute();
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
                    case TARGET_URI:
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
        } else if (ACTION_DECRYPT_VERIFY.equals(action)) {
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
                byte[] bytes = data.getByteArray(DECRYPT_CIPHERTEXT_BYTES);
                boolean returnBytes = data.getBoolean(DECRYPT_RETURN_BYTES);
                boolean assumeSymmetricEncryption = data.getBoolean(DECRYPT_ASSUME_SYMMETRIC);

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

                    case TARGET_URI: /* decrypting file */
                        String inputFile = data.getString(ENCRYPT_INPUT_FILE);
                        String outputFile = data.getString(ENCRYPT_OUTPUT_FILE);

                        // check if storage is ready
                        if (!FileHelper.isStorageMounted(inputFile)
                                || !FileHelper.isStorageMounted(outputFile)) {
                            throw new PgpGeneralException(
                                    getString(R.string.error_external_storage_not_ready));
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
                        inLength = PgpHelper.getLengthOfStream(in);
                        inputData = new InputData(in, inLength);

                        // OutputStream
                        try {
                            while (true) {
                                streamFilename = PgpHelper.generateRandomFilename(32);
                                if (streamFilename == null) {
                                    throw new PgpGeneralException("couldn't generate random file name");
                                }
                                openFileInput(streamFilename).close();
                            }
                        } catch (FileNotFoundException e) {
                            // found a name that isn't used yet
                        }
                        outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);

                        break;

                    default:
                        throw new PgpGeneralException("No target choosen!");

                }

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(this, inputData, outStream);
                builder.progressDialogUpdater(this);

                builder.assumeSymmetric(assumeSymmetricEncryption)
                        .passphrase(PassphraseCacheService.getCachedPassphrase(this, secretKeyId));

                PgpDecryptVerifyResult decryptVerifyResult = builder.build().execute();

                outStream.close();

                resultData.putParcelable(RESULT_DECRYPT_VERIFY_RESULT, decryptVerifyResult);

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
                    case TARGET_URI:
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
        } else if (ACTION_SAVE_KEYRING.equals(action)) {
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
                ArrayList<GregorianCalendar> keysExpiryDates =
                        (ArrayList<GregorianCalendar>) data.getSerializable(SAVE_KEYRING_KEYS_EXPIRY_DATES);

                long masterKeyId = data.getLong(SAVE_KEYRING_MASTER_KEY_ID);

                PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);
                /* Operation */
                if (!canSign) {
                    keyOperations.changeSecretKeyPassphrase(
                            ProviderHelper.getPGPSecretKeyRingByKeyId(this, masterKeyId),
                            oldPassPhrase, newPassPhrase);
                } else {
                    PGPPublicKey pubkey = ProviderHelper.getPGPPublicKeyByKeyId(this, masterKeyId);
                    keyOperations.buildSecretKey(userIds, keys, keysUsages, keysExpiryDates,
                            pubkey, oldPassPhrase, newPassPhrase);
                }
                PassphraseCacheService.addCachedPassphrase(this, masterKeyId, newPassPhrase);

                /* Output */
                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_GENERATE_KEY.equals(action)) {
            try {
                /* Input */
                int algorithm = data.getInt(GENERATE_KEY_ALGORITHM);
                String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);
                int keysize = data.getInt(GENERATE_KEY_KEY_SIZE);
                boolean masterKey = data.getBoolean(GENERATE_KEY_MASTER_KEY);

                /* Operation */
                PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);
                PGPSecretKey newKey = keyOperations.createKey(algorithm, keysize,
                        passphrase, masterKey);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PgpConversionHelper.PGPSecretKeyToBytes(newKey));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_GENERATE_DEFAULT_RSA_KEYS.equals(action)) {
            // generate one RSA 4096 key for signing and one subkey for encrypting!
            try {
                /* Input */
                String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                /* Operation */
                int keysTotal = 2;
                int keysCreated = 0;
                setProgress(
                        getApplicationContext().getResources().
                                getQuantityString(R.plurals.progress_generating, keysTotal),
                        keysCreated,
                        keysTotal);
                PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);

                PGPSecretKey masterKey = keyOperations.createKey(Id.choice.algorithm.rsa,
                        4096, passphrase, true);
                keysCreated++;
                setProgress(keysCreated, keysTotal);

                PGPSecretKey subKey = keyOperations.createKey(Id.choice.algorithm.rsa,
                        4096, passphrase, false);
                keysCreated++;
                setProgress(keysCreated, keysTotal);

                // TODO: default to one master for cert, one sub for encrypt and one sub
                //       for sign

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY,
                        PgpConversionHelper.PGPSecretKeyToBytes(masterKey));
                resultData.putByteArray(RESULT_NEW_KEY2,
                        PgpConversionHelper.PGPSecretKeyToBytes(subKey));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_DELETE_FILE_SECURELY.equals(action)) {
            try {
                /* Input */
                String deleteFile = data.getString(DELETE_FILE);

                /* Operation */
                try {
                    PgpHelper.deleteFileSecurely(this, this, new File(deleteFile));
                } catch (FileNotFoundException e) {
                    throw new PgpGeneralException(
                            getString(R.string.error_file_not_found, deleteFile));
                } catch (IOException e) {
                    throw new PgpGeneralException(getString(R.string.error_file_delete_failed,
                            deleteFile));
                }

                /* Output */
                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_IMPORT_KEYRING.equals(action)) {
            try {
                List<ImportKeysListEntry> entries = data.getParcelableArrayList(IMPORT_KEY_LIST);

                Bundle resultData = new Bundle();

                PgpImportExport pgpImportExport = new PgpImportExport(this, this);
                resultData = pgpImportExport.importKeyRings(entries);

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_EXPORT_KEYRING.equals(action)) {
            try {

                /* Input */
                int keyType = Id.type.public_key;
                if (data.containsKey(EXPORT_KEY_TYPE)) {
                    keyType = data.getInt(EXPORT_KEY_TYPE);
                }
                long[] masterKeyIds = data.getLongArray(EXPORT_KEY_RING_MASTER_KEY_ID);
                String outputFile = data.getString(EXPORT_FILENAME);

                // If not exporting all keys get the masterKeyIds of the keys to export from the intent
                boolean exportAll = data.getBoolean(EXPORT_ALL);

                /* Operation */

                // check if storage is ready
                if (!FileHelper.isStorageMounted(outputFile)) {
                    throw new PgpGeneralException(getString(R.string.error_external_storage_not_ready));
                }

                ArrayList<Long> publicMasterKeyIds = new ArrayList<Long>();
                ArrayList<Long> secretMasterKeyIds = new ArrayList<Long>();
                ArrayList<Long> allPublicMasterKeyIds = ProviderHelper.getPublicKeyRingsMasterKeyIds(this);
                ArrayList<Long> allSecretMasterKeyIds = ProviderHelper.getSecretKeyRingsMasterKeyIds(this);

                if (exportAll) {
                    // get all public key ring MasterKey ids
                    if (keyType == Id.type.public_key || keyType == Id.type.public_secret_key) {
                        publicMasterKeyIds = allPublicMasterKeyIds;
                    }
                    // get all secret key ring MasterKey ids
                    if (keyType == Id.type.secret_key || keyType == Id.type.public_secret_key) {
                        secretMasterKeyIds = allSecretMasterKeyIds;
                    }
                } else {

                    for (long masterKeyId : masterKeyIds) {
                        if ((keyType == Id.type.public_key || keyType == Id.type.public_secret_key)
                                && allPublicMasterKeyIds.contains(masterKeyId)) {
                            publicMasterKeyIds.add(masterKeyId);
                        }
                        if ((keyType == Id.type.secret_key || keyType == Id.type.public_secret_key)
                                && allSecretMasterKeyIds.contains(masterKeyId)) {
                            secretMasterKeyIds.add(masterKeyId);
                        }
                    }
                }

                PgpImportExport pgpImportExport = new PgpImportExport(this, this, this);
                Bundle resultData = pgpImportExport
                        .exportKeyRings(publicMasterKeyIds, secretMasterKeyIds,
                                new FileOutputStream(outputFile));

                if (mIsCanceled) {
                    boolean isDeleted = new File(outputFile).delete();
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_UPLOAD_KEYRING.equals(action)) {
            try {

                /* Input */
                String keyServer = data.getString(UPLOAD_KEY_SERVER);
                // and dataUri!

                /* Operation */
                HkpKeyServer server = new HkpKeyServer(keyServer);

                PGPPublicKeyRing keyring = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(this, dataUri);
                if (keyring != null) {
                    PgpImportExport pgpImportExport = new PgpImportExport(this, null);

                    boolean uploaded = pgpImportExport.uploadKeyRingToServer(server,
                            (PGPPublicKeyRing) keyring);
                    if (!uploaded) {
                        throw new PgpGeneralException("Unable to export key to selected server");
                    }
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_DOWNLOAD_AND_IMPORT_KEYS.equals(action)) {
            try {
                ArrayList<ImportKeysListEntry> entries = data.getParcelableArrayList(DOWNLOAD_KEY_LIST);
                String keyServer = data.getString(DOWNLOAD_KEY_SERVER);

                // TODO: add extra which requires fingerprint suport and force verification!
                // only supported by newer sks keyserver versions

                // this downloads the keys and places them into the ImportKeysListEntry entries
                HkpKeyServer server = new HkpKeyServer(keyServer);

                for (ImportKeysListEntry entry : entries) {
                    // if available use complete fingerprint for get request
                    byte[] downloadedKeyBytes;
                    if (entry.getFingerPrintHex() != null) {
                        downloadedKeyBytes = server.get(entry.getFingerPrintHex()).getBytes();
                    } else {
                        downloadedKeyBytes = server.get(entry.getKeyIdHex()).getBytes();
                    }

                    // create PGPKeyRing object based on downloaded armored key
                    PGPKeyRing downloadedKey = null;
                    BufferedInputStream bufferedInput =
                            new BufferedInputStream(new ByteArrayInputStream(downloadedKeyBytes));
                    if (bufferedInput.available() > 0) {
                        InputStream in = PGPUtil.getDecoderStream(bufferedInput);
                        PGPObjectFactory objectFactory = new PGPObjectFactory(in);

                        // get first object in block
                        Object obj;
                        if ((obj = objectFactory.nextObject()) != null) {
                            Log.d(Constants.TAG, "Found class: " + obj.getClass());

                            if (obj instanceof PGPKeyRing) {
                                downloadedKey = (PGPKeyRing) obj;
                            } else {
                                throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
                            }
                        }
                    }

                    // verify downloaded key by comparing fingerprints
                    if (entry.getFingerPrintHex() != null) {
                        String downloadedKeyFp = PgpKeyHelper.convertFingerprintToHex(downloadedKey.getPublicKey().getFingerprint(), false);
                        if (downloadedKeyFp.equals(entry.getFingerPrintHex())) {
                            Log.d(Constants.TAG, "fingerprint of downloaded key is the same as the requested fingerprint!");
                        } else {
                            throw new PgpGeneralException("fingerprint of downloaded key is NOT the same as the requested fingerprint!");
                        }
                    }

                    // save key bytes in entry object for doing the
                    // actual import afterwards
                    entry.setBytes(downloadedKey.getEncoded());
                }


                Intent importIntent = new Intent(this, KeychainIntentService.class);
                importIntent.setAction(ACTION_IMPORT_KEYRING);
                Bundle importData = new Bundle();
                importData.putParcelableArrayList(IMPORT_KEY_LIST, entries);
                importIntent.putExtra(EXTRA_DATA, importData);
                importIntent.putExtra(EXTRA_MESSENGER, mMessenger);

                // now import it with this service
                onHandleIntent(importIntent);

                // result is handled in ACTION_IMPORT_KEYRING
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_CERTIFY_KEYRING.equals(action)) {
            try {

                /* Input */
                long masterKeyId = data.getLong(CERTIFY_KEY_MASTER_KEY_ID);
                long pubKeyId = data.getLong(CERTIFY_KEY_PUB_KEY_ID);
                ArrayList<String> userIds = data.getStringArrayList(CERTIFY_KEY_UIDS);

                /* Operation */
                String signaturePassPhrase = PassphraseCacheService.getCachedPassphrase(this,
                        masterKeyId);

                PgpKeyOperation keyOperation = new PgpKeyOperation(this, this);
                PGPPublicKeyRing signedPubKeyRing = keyOperation.certifyKey(masterKeyId, pubKeyId,
                        userIds, signaturePassPhrase);

                // store the signed key in our local cache
                PgpImportExport pgpImportExport = new PgpImportExport(this, null);
                int retval = pgpImportExport.storeKeyRingInCache(signedPubKeyRing);
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
        // Service was canceled. Do not send error to handler.
        if (this.mIsCanceled) {
            return;
        }
        Log.e(Constants.TAG, "ApgService Exception: ", e);
        e.printStackTrace();

        Bundle data = new Bundle();
        data.putString(KeychainIntentServiceHandler.DATA_ERROR, e.getMessage());
        sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToHandler(Integer arg1, Integer arg2, Bundle data) {
        // Service was canceled. Do not send message to handler.
        if (this.mIsCanceled) {
            return;
        }
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
     * Set progressDialogUpdater of ProgressDialog by sending message to handler on UI thread
     */
    public void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress with progressDialogUpdater=" + progress + ", max="
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

    @Override
    public boolean hasServiceStopped() {
        return mIsCanceled;
    }
}
