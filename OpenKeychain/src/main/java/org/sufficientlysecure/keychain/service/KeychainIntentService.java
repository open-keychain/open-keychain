/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CachedSecretKey;
import org.sufficientlysecure.keychain.pgp.CachedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpImportExport;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.util.HkpKeyServer;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.KeychainServiceListener;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

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
import java.util.List;

/**
 * This Service contains all important long lasting operations for APG. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */
public class KeychainIntentService extends IntentService
        implements Progressable, KeychainServiceListener {

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

    // encrypt
    public static final String ENCRYPT_SIGNATURE_KEY_ID = "secret_key_id";
    public static final String ENCRYPT_USE_ASCII_ARMOR = "use_ascii_armor";
    public static final String ENCRYPT_ENCRYPTION_KEYS_IDS = "encryption_keys_ids";
    public static final String ENCRYPT_COMPRESSION_ID = "compression_id";
    public static final String ENCRYPT_MESSAGE_BYTES = "message_bytes";
    public static final String ENCRYPT_INPUT_FILE = "input_file";
    public static final String ENCRYPT_OUTPUT_FILE = "output_file";
    public static final String ENCRYPT_SYMMETRIC_PASSPHRASE = "passphrase";

    // decrypt/verify
    public static final String DECRYPT_CIPHERTEXT_BYTES = "ciphertext_bytes";
    public static final String DECRYPT_PASSPHRASE = "passphrase";

    // save keyring
    public static final String SAVE_KEYRING_PARCEL = "save_parcel";
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
    public static final String EXPORT_SECRET = "export_secret";
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
    public static final String RESULT_KEY_USAGES = "new_key_usages";

    // encrypt
    public static final String RESULT_BYTES = "encrypted_data";

    // decrypt/verify
    public static final String RESULT_DECRYPTED_BYTES = "decrypted_data";
    public static final String RESULT_DECRYPT_VERIFY_RESULT = "signature";

    // import
    public static final String RESULT_IMPORT_ADDED = "added";
    public static final String RESULT_IMPORT_UPDATED = "updated";
    public static final String RESULT_IMPORT_BAD = "bad";

    // export
    public static final String RESULT_EXPORT = "exported";

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

                long signatureKeyId = data.getLong(ENCRYPT_SIGNATURE_KEY_ID);
                String symmetricPassphrase = data.getString(ENCRYPT_SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmor = data.getBoolean(ENCRYPT_USE_ASCII_ARMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPT_ENCRYPTION_KEYS_IDS);
                int compressionId = data.getInt(ENCRYPT_COMPRESSION_ID);
                InputStream inStream;
                long inLength;
                InputData inputData;
                OutputStream outStream;
//                String streamFilename = null;
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

                    // TODO: not used currently
//                    case TARGET_STREAM: /* Encrypting stream from content uri */
//                        Uri providerUri = (Uri) data.getParcelable(ENCRYPT_PROVIDER_URI);
//
//                        // InputStream
//                        InputStream in = getContentResolver().openInputStream(providerUri);
//                        inLength = PgpHelper.getLengthOfStream(in);
//                        inputData = new InputData(in, inLength);
//
//                        // OutputStream
//                        try {
//                            while (true) {
//                                streamFilename = PgpHelper.generateRandomFilename(32);
//                                if (streamFilename == null) {
//                                    throw new PgpGeneralException("couldn't generate random file name");
//                                }
//                                openFileInput(streamFilename).close();
//                            }
//                        } catch (FileNotFoundException e) {
//                            // found a name that isn't used yet
//                        }
//                        outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);
//
//                        break;

                    default:
                        throw new PgpGeneralException("No target choosen!");

                }

                /* Operation */
                PgpSignEncrypt.Builder builder =
                        new PgpSignEncrypt.Builder(
                                new ProviderHelper(this),
                                PgpHelper.getFullVersion(this),
                                inputData, outStream);
                builder.setProgressable(this);

                builder.setEnableAsciiArmorOutput(useAsciiArmor)
                        .setCompressionId(compressionId)
                        .setSymmetricEncryptionAlgorithm(
                                Preferences.getPreferences(this).getDefaultEncryptionAlgorithm())
                        .setSignatureForceV3(Preferences.getPreferences(this).getForceV3Signatures())
                        .setEncryptionMasterKeyIds(encryptionKeyIds)
                        .setSymmetricPassphrase(symmetricPassphrase)
                        .setSignatureMasterKeyId(signatureKeyId)
                        .setSignatureHashAlgorithm(
                                Preferences.getPreferences(this).getDefaultHashAlgorithm())
                        .setSignaturePassphrase(
                                PassphraseCacheService.getCachedPassphrase(this, signatureKeyId));

                // this assumes that the bytes are cleartext (valid for current implementation!)
                if (target == TARGET_BYTES) {
                    builder.setCleartextInput(true);
                }

                builder.build().execute();

                outStream.close();

                /* Output */

                Bundle resultData = new Bundle();

                switch (target) {
                    case TARGET_BYTES:
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();

                        resultData.putByteArray(RESULT_BYTES, output);

                        break;
                    case TARGET_URI:
                        // nothing, file was written, just send okay

                        break;
//                    case TARGET_STREAM:
//                        String uri = DataStream.buildDataStreamUri(streamFilename).toString();
//                        resultData.putString(RESULT_URI, uri);
//
//                        break;
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

                byte[] bytes = data.getByteArray(DECRYPT_CIPHERTEXT_BYTES);
                String passphrase = data.getString(DECRYPT_PASSPHRASE);

                InputStream inStream;
                long inLength;
                InputData inputData;
                OutputStream outStream;
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

                    // TODO: not used, maybe contains code useful for new decrypt method for files?
//                    case TARGET_STREAM: /* decrypting stream from content uri */
//                        Uri providerUri = (Uri) data.getParcelable(ENCRYPT_PROVIDER_URI);
//
//                        // InputStream
//                        InputStream in = getContentResolver().openInputStream(providerUri);
//                        inLength = PgpHelper.getLengthOfStream(in);
//                        inputData = new InputData(in, inLength);
//
//                        // OutputStream
//                        try {
//                            while (true) {
//                                streamFilename = PgpHelper.generateRandomFilename(32);
//                                if (streamFilename == null) {
//                                    throw new PgpGeneralException("couldn't generate random file name");
//                                }
//                                openFileInput(streamFilename).close();
//                            }
//                        } catch (FileNotFoundException e) {
//                            // found a name that isn't used yet
//                        }
//                        outStream = openFileOutput(streamFilename, Context.MODE_PRIVATE);
//
//                        break;

                    default:
                        throw new PgpGeneralException("No target choosen!");

                }

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(
                        new ProviderHelper(this),
                        new PgpDecryptVerify.PassphraseCache() {
                            @Override
                            public String getCachedPassphrase(long masterKeyId) {
                                return PassphraseCacheService.getCachedPassphrase(
                                        KeychainIntentService.this, masterKeyId);
                            }
                        },
                        inputData, outStream);
                builder.setProgressable(this);

                builder.setAllowSymmetricDecryption(true)
                        .setPassphrase(passphrase);

                PgpDecryptVerifyResult decryptVerifyResult = builder.build().execute();

                outStream.close();

                resultData.putParcelable(RESULT_DECRYPT_VERIFY_RESULT, decryptVerifyResult);

                /* Output */

                switch (target) {
                    case TARGET_BYTES:
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        resultData.putByteArray(RESULT_DECRYPTED_BYTES, output);
                        break;
                    case TARGET_URI:
                        // nothing, file was written, just send okay and verification bundle

                        break;
//                    case TARGET_STREAM:
//                        String uri = DataStream.buildDataStreamUri(streamFilename).toString();
//                        resultData.putString(RESULT_URI, uri);
//
//                        break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_SAVE_KEYRING.equals(action)) {
            try {
                /* Input */
                SaveKeyringParcel saveParcel = data.getParcelable(SAVE_KEYRING_PARCEL);
                String oldPassphrase = saveParcel.oldPassphrase;
                String newPassphrase = saveParcel.newPassphrase;
                boolean canSign = true;

                if (data.containsKey(SAVE_KEYRING_CAN_SIGN)) {
                    canSign = data.getBoolean(SAVE_KEYRING_CAN_SIGN);
                }

                if (newPassphrase == null) {
                    newPassphrase = oldPassphrase;
                }

                long masterKeyId = saveParcel.keys.get(0).getKeyId();

                /* Operation */
                ProviderHelper providerHelper = new ProviderHelper(this);
                if (!canSign) {
                    setProgress(R.string.progress_building_key, 0, 100);
                    CachedSecretKeyRing keyRing = providerHelper.getCachedSecretKeyRing(masterKeyId);
                    UncachedSecretKeyRing newKeyRing =
                            keyRing.changeSecretKeyPassphrase(oldPassphrase, newPassphrase);
                    setProgress(R.string.progress_saving_key_ring, 50, 100);
                    providerHelper.saveKeyRing(newKeyRing);
                    setProgress(R.string.progress_done, 100, 100);
                } else {
                    PgpKeyOperation keyOperations = new PgpKeyOperation(new ProgressScaler(this, 0, 90, 100));
                    UncachedKeyRing pair;
                    try {
                        CachedSecretKeyRing privkey = providerHelper.getCachedSecretKeyRing(masterKeyId);
                        CachedPublicKeyRing pubkey = providerHelper.getCachedPublicKeyRing(masterKeyId);

                        pair = keyOperations.buildSecretKey(privkey, pubkey, saveParcel); // edit existing
                    } catch (ProviderHelper.NotFoundException e) {
                        pair = keyOperations.buildNewSecretKey(saveParcel); //new Keyring
                    }

                    setProgress(R.string.progress_saving_key_ring, 90, 100);
                    // save the pair
                    providerHelper.saveKeyRing(pair);
                    setProgress(R.string.progress_done, 100, 100);
                }
                PassphraseCacheService.addCachedPassphrase(this, masterKeyId, newPassphrase);

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
                PgpKeyOperation keyOperations = new PgpKeyOperation(new ProgressScaler(this, 0, 100, 100));
                byte[] newKey = keyOperations.createKey(algorithm, keysize, passphrase, masterKey);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY, newKey);

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
                ArrayList<Integer> keyUsageList = new ArrayList<Integer>();

                /* Operation */
                int keysTotal = 3;
                int keysCreated = 0;
                setProgress(
                        getApplicationContext().getResources().
                                getQuantityString(R.plurals.progress_generating, keysTotal),
                        keysCreated,
                        keysTotal);
                PgpKeyOperation keyOperations = new PgpKeyOperation(new ProgressScaler(this, 0, 100, 100));

                ByteArrayOutputStream os = new ByteArrayOutputStream();

                byte[] buf;

                buf = keyOperations.createKey(Constants.choice.algorithm.rsa,
                        4096, passphrase, true);
                os.write(buf);
                keyUsageList.add(KeyFlags.CERTIFY_OTHER);
                keysCreated++;
                setProgress(keysCreated, keysTotal);

                buf = keyOperations.createKey(Constants.choice.algorithm.rsa,
                        4096, passphrase, false);
                os.write(buf);
                keyUsageList.add(KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
                keysCreated++;
                setProgress(keysCreated, keysTotal);

                buf = keyOperations.createKey(Constants.choice.algorithm.rsa,
                        4096, passphrase, false);
                os.write(buf);
                keyUsageList.add(KeyFlags.SIGN_DATA);
                keysCreated++;
                setProgress(keysCreated, keysTotal);

                // TODO: default to one master for cert, one sub for encrypt and one sub
                //       for sign

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY, os.toByteArray());
                resultData.putIntegerArrayList(RESULT_KEY_USAGES, keyUsageList);

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

                PgpImportExport pgpImportExport = new PgpImportExport(this, this);
                Bundle resultData = pgpImportExport.importKeyRings(entries);

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_EXPORT_KEYRING.equals(action)) {
            try {

                boolean exportSecret = data.getBoolean(EXPORT_SECRET, false);
                long[] masterKeyIds = data.getLongArray(EXPORT_KEY_RING_MASTER_KEY_ID);
                String outputFile = data.getString(EXPORT_FILENAME);

                // If not exporting all keys get the masterKeyIds of the keys to export from the intent
                boolean exportAll = data.getBoolean(EXPORT_ALL);

                // check if storage is ready
                if (!FileHelper.isStorageMounted(outputFile)) {
                    throw new PgpGeneralException(getString(R.string.error_external_storage_not_ready));
                }

                ArrayList<Long> publicMasterKeyIds = new ArrayList<Long>();
                ArrayList<Long> secretMasterKeyIds = new ArrayList<Long>();

                String selection = null;
                if (!exportAll) {
                    selection = KeychainDatabase.Tables.KEYS + "." + KeyRings.MASTER_KEY_ID + " IN( ";
                    for (long l : masterKeyIds) {
                        selection += Long.toString(l) + ",";
                    }
                    selection = selection.substring(0, selection.length() - 1) + " )";
                }

                Cursor cursor = getContentResolver().query(KeyRings.buildUnifiedKeyRingsUri(),
                        new String[]{KeyRings.MASTER_KEY_ID, KeyRings.HAS_ANY_SECRET},
                        selection, null, null);
                try {
                    cursor.moveToFirst();
                    do {
                        // export public either way
                        publicMasterKeyIds.add(cursor.getLong(0));
                        // add secret if available (and requested)
                        if (exportSecret && cursor.getInt(1) != 0)
                            secretMasterKeyIds.add(cursor.getLong(0));
                    } while (cursor.moveToNext());
                } finally {
                    cursor.close();
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

                ProviderHelper providerHelper = new ProviderHelper(this);
                CachedPublicKeyRing keyring = providerHelper.getCachedPublicKeyRing(dataUri);
                PgpImportExport pgpImportExport = new PgpImportExport(this, null);

                boolean uploaded = pgpImportExport.uploadKeyRingToServer(server, keyring);
                if (!uploaded) {
                    throw new PgpGeneralException("Unable to export key to selected server");
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_DOWNLOAD_AND_IMPORT_KEYS.equals(action)) {
            try {
                ArrayList<ImportKeysListEntry> entries = data.getParcelableArrayList(DOWNLOAD_KEY_LIST);
                String keyServer = data.getString(DOWNLOAD_KEY_SERVER);

                // this downloads the keys and places them into the ImportKeysListEntry entries
                HkpKeyServer server = new HkpKeyServer(keyServer);

                for (ImportKeysListEntry entry : entries) {
                    // if available use complete fingerprint for get request
                    byte[] downloadedKeyBytes;
                    if (entry.getFingerPrintHex() != null) {
                        downloadedKeyBytes = server.get("0x" + entry.getFingerPrintHex()).getBytes();
                    } else {
                        downloadedKeyBytes = server.get(entry.getKeyIdHex()).getBytes();
                    }

                    // create PGPKeyRing object based on downloaded armored key
                    UncachedKeyRing downloadedKey =
                            UncachedKeyRing.decodePubkeyFromData(downloadedKeyBytes);

                    // verify downloaded key by comparing fingerprints
                    if (entry.getFingerPrintHex() != null) {
                        String downloadedKeyFp = PgpKeyHelper.convertFingerprintToHex(
                                downloadedKey.getFingerprint());
                        if (downloadedKeyFp.equals(entry.getFingerPrintHex())) {
                            Log.d(Constants.TAG, "fingerprint of downloaded key is the same as " +
                                    "the requested fingerprint!");
                        } else {
                            throw new PgpGeneralException("fingerprint of downloaded key is " +
                                    "NOT the same as the requested fingerprint!");
                        }
                    }

                    // save key bytes in entry object for doing the
                    // actual import afterwards
                    entry.setBytes(downloadedKeyBytes);
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
                String signaturePassphrase = PassphraseCacheService.getCachedPassphrase(this,
                        masterKeyId);
                if (signaturePassphrase == null) {
                    throw new PgpGeneralException("Unable to obtain passphrase");
                }

                ProviderHelper providerHelper = new ProviderHelper(this);
                CachedPublicKeyRing publicRing = providerHelper.getCachedPublicKeyRing(pubKeyId);
                CachedSecretKeyRing secretKeyRing = providerHelper.getCachedSecretKeyRing(masterKeyId);
                CachedSecretKey certificationKey = secretKeyRing.getSubKey();
                if(!certificationKey.unlock(signaturePassphrase)) {
                    throw new PgpGeneralException("Error extracting key (bad passphrase?)");
                }
                UncachedKeyRing newRing = certificationKey.certifyUserIds(publicRing, userIds);

                // store the signed key in our local cache
                PgpImportExport pgpImportExport = new PgpImportExport(this, null);
                int retval = pgpImportExport.storeKeyRingInCache(newRing);
                if (retval != PgpImportExport.RETURN_OK && retval != PgpImportExport.RETURN_UPDATED) {
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
        // TODO: Implement a better exception handling here
        // contextualize the exception, if necessary
        String message;
        if (e instanceof PgpGeneralMsgIdException) {
            e = ((PgpGeneralMsgIdException) e).getContextualized(this);
            message = e.getMessage();
        } else if (e instanceof PgpSignEncrypt.KeyExtractionException) {
            message = getString(R.string.error_could_not_extract_private_key);
        } else if (e instanceof PgpSignEncrypt.NoPassphraseException) {
            message = getString(R.string.error_no_signature_passphrase);
        } else if (e instanceof PgpSignEncrypt.NoSigningKeyException) {
            message = getString(R.string.error_no_signature_key);
        } else if (e instanceof PgpDecryptVerify.InvalidDataException) {
            message = getString(R.string.error_invalid_data);
        } else if (e instanceof PgpDecryptVerify.KeyExtractionException) {
            message = getString(R.string.error_could_not_extract_private_key);
        } else if (e instanceof PgpDecryptVerify.WrongPassphraseException) {
            message = getString(R.string.error_wrong_passphrase);
        } else if (e instanceof PgpDecryptVerify.NoSecretKeyException) {
            message = getString(R.string.error_no_secret_key_found);
        } else if (e instanceof PgpDecryptVerify.IntegrityCheckFailedException) {
            message = getString(R.string.error_integrity_check_failed);
        } else {
            message = e.getMessage();
        }

        Log.e(Constants.TAG, "KeychainIntentService Exception: ", e);

        Bundle data = new Bundle();
        data.putString(KeychainIntentServiceHandler.DATA_ERROR, message);
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

    @Override
    public boolean hasServiceStopped() {
        return mIsCanceled;
    }
}
