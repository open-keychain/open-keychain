/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.CertifyOperation;
import org.sufficientlysecure.keychain.operations.DeleteOperation;
import org.sufficientlysecure.keychain.operations.EditKeyOperation;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.operations.ImportExportOperation;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This Service contains all important long lasting operations for OpenKeychain. It receives Intents with
 * data from the activities or other apps, queues these intents, executes them, and stops itself
 * after doing them.
 */
public class KeychainIntentService extends IntentService implements Progressable {

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    /* possible actions */
    public static final String ACTION_SIGN_ENCRYPT = Constants.INTENT_PREFIX + "SIGN_ENCRYPT";

    public static final String ACTION_DECRYPT_VERIFY = Constants.INTENT_PREFIX + "DECRYPT_VERIFY";

    public static final String ACTION_DECRYPT_METADATA = Constants.INTENT_PREFIX + "DECRYPT_METADATA";

    public static final String ACTION_EDIT_KEYRING = Constants.INTENT_PREFIX + "EDIT_KEYRING";

    public static final String ACTION_IMPORT_KEYRING = Constants.INTENT_PREFIX + "IMPORT_KEYRING";
    public static final String ACTION_EXPORT_KEYRING = Constants.INTENT_PREFIX + "EXPORT_KEYRING";

    public static final String ACTION_UPLOAD_KEYRING = Constants.INTENT_PREFIX + "UPLOAD_KEYRING";

    public static final String ACTION_CERTIFY_KEYRING = Constants.INTENT_PREFIX + "SIGN_KEYRING";

    public static final String ACTION_DELETE = Constants.INTENT_PREFIX + "DELETE";

    public static final String ACTION_CONSOLIDATE = Constants.INTENT_PREFIX + "CONSOLIDATE";

    public static final String ACTION_CANCEL = Constants.INTENT_PREFIX + "CANCEL";

    /* keys for data bundle */

    // encrypt, decrypt, import export
    public static final String TARGET = "target";
    public static final String SOURCE = "source";
    // possible targets:
    public static final int IO_BYTES = 1;
    public static final int IO_URI = 2;
    public static final int IO_URIS = 3;

    public static final String SELECTED_URI = "selected_uri";

    // encrypt
    public static final String ENCRYPT_SIGNATURE_MASTER_ID = "secret_key_id";
    public static final String ENCRYPT_SIGNATURE_KEY_PASSPHRASE = "secret_key_passphrase";
    public static final String ENCRYPT_SIGNATURE_NFC_TIMESTAMP = "signature_nfc_timestamp";
    public static final String ENCRYPT_SIGNATURE_NFC_HASH = "signature_nfc_hash";
    public static final String ENCRYPT_USE_ASCII_ARMOR = "use_ascii_armor";
    public static final String ENCRYPT_ENCRYPTION_KEYS_IDS = "encryption_keys_ids";
    public static final String ENCRYPT_COMPRESSION_ID = "compression_id";
    public static final String ENCRYPT_MESSAGE_BYTES = "message_bytes";
    public static final String ENCRYPT_DECRYPT_INPUT_URI = "input_uri";
    public static final String ENCRYPT_INPUT_URIS = "input_uris";
    public static final String ENCRYPT_DECRYPT_OUTPUT_URI = "output_uri";
    public static final String ENCRYPT_OUTPUT_URIS = "output_uris";
    public static final String ENCRYPT_SYMMETRIC_PASSPHRASE = "passphrase";

    // decrypt/verify
    public static final String DECRYPT_CIPHERTEXT_BYTES = "ciphertext_bytes";
    public static final String DECRYPT_PASSPHRASE = "passphrase";
    public static final String DECRYPT_NFC_DECRYPTED_SESSION_KEY = "nfc_decrypted_session_key";

    // save keyring
    public static final String EDIT_KEYRING_PARCEL = "save_parcel";
    public static final String EDIT_KEYRING_PASSPHRASE = "passphrase";

    // delete keyring(s)
    public static final String DELETE_KEY_LIST = "delete_list";
    public static final String DELETE_IS_SECRET = "delete_is_secret";

    // import key
    public static final String IMPORT_KEY_LIST = "import_key_list";
    public static final String IMPORT_KEY_SERVER = "import_key_server";

    // export key
    public static final String EXPORT_FILENAME = "export_filename";
    public static final String EXPORT_URI = "export_uri";
    public static final String EXPORT_SECRET = "export_secret";
    public static final String EXPORT_ALL = "export_all";
    public static final String EXPORT_KEY_RING_MASTER_KEY_ID = "export_key_ring_id";

    // upload key
    public static final String UPLOAD_KEY_SERVER = "upload_key_server";

    // certify key
    public static final String CERTIFY_PARCEL = "certify_parcel";

    // consolidate
    public static final String CONSOLIDATE_RECOVERY = "consolidate_recovery";


    /*
     * possible data keys as result send over messenger
     */

    // encrypt
    public static final String RESULT_BYTES = "encrypted_data";

    // decrypt/verify
    public static final String RESULT_DECRYPTED_BYTES = "decrypted_data";

    Messenger mMessenger;

    // this attribute can possibly merged with the one above? not sure...
    private AtomicBoolean mActionCanceled = new AtomicBoolean(false);

    public KeychainIntentService() {
        super("KeychainIntentService");
    }

    /**
     * The IntentService calls this method from the default worker thread with the intent that
     * started the service. When this method returns, IntentService stops the service, as
     * appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // We have not been cancelled! (yet)
        mActionCanceled.set(false);

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
        if (data == null) {
            Log.e(Constants.TAG, "data extra is null!");
            return;
        }

        Log.logDebugBundle(data, "EXTRA_DATA");

        ProviderHelper providerHelper = new ProviderHelper(this);

        String action = intent.getAction();

        // executeServiceMethod action from extra bundle
        if (ACTION_CERTIFY_KEYRING.equals(action)) {

            // Input
            CertifyActionsParcel parcel = data.getParcelable(CERTIFY_PARCEL);
            String keyServerUri = data.getString(UPLOAD_KEY_SERVER);

            // Operation
            CertifyOperation op = new CertifyOperation(this, providerHelper, this, mActionCanceled);
            CertifyResult result = op.certify(parcel, keyServerUri);

            // Result
            sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);

        } else if (ACTION_CONSOLIDATE.equals(action)) {

            // Operation
            ConsolidateResult result;
            if (data.containsKey(CONSOLIDATE_RECOVERY) && data.getBoolean(CONSOLIDATE_RECOVERY)) {
                result = new ProviderHelper(this).consolidateDatabaseStep2(this);
            } else {
                result = new ProviderHelper(this).consolidateDatabaseStep1(this);
            }

            // Result
            sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);

        } else if (ACTION_DECRYPT_METADATA.equals(action)) {

            try {
                /* Input */
                String passphrase = data.getString(DECRYPT_PASSPHRASE);
                byte[] nfcDecryptedSessionKey = data.getByteArray(DECRYPT_NFC_DECRYPTED_SESSION_KEY);

                InputData inputData = createDecryptInputData(data);

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(
                        this, new ProviderHelper(this), this, inputData, null
                );
                builder.setAllowSymmetricDecryption(true)
                        .setPassphrase(passphrase)
                        .setDecryptMetadataOnly(true)
                        .setNfcState(nfcDecryptedSessionKey);

                DecryptVerifyResult decryptVerifyResult = builder.build().execute();

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, decryptVerifyResult);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

        } else if (ACTION_DECRYPT_VERIFY.equals(action)) {

            try {
                /* Input */
                String passphrase = data.getString(DECRYPT_PASSPHRASE);
                byte[] nfcDecryptedSessionKey = data.getByteArray(DECRYPT_NFC_DECRYPTED_SESSION_KEY);

                InputData inputData = createDecryptInputData(data);
                OutputStream outStream = createCryptOutputStream(data);

                /* Operation */

                Bundle resultData = new Bundle();

                // verifyText and decrypt returning additional resultData values for the
                // verification of signatures
                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(
                        this, new ProviderHelper(this), this,
                        inputData, outStream
                );
                builder.setAllowSymmetricDecryption(true)
                        .setPassphrase(passphrase)
                        .setNfcState(nfcDecryptedSessionKey);

                DecryptVerifyResult decryptVerifyResult = builder.build().execute();

                outStream.close();

                resultData.putParcelable(DecryptVerifyResult.EXTRA_RESULT, decryptVerifyResult);

                /* Output */

                finalizeDecryptOutputStream(data, resultData, outStream);

                Log.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

        } else if (ACTION_DELETE.equals(action)) {

            // Input
            long[] masterKeyIds = data.getLongArray(DELETE_KEY_LIST);
            boolean isSecret = data.getBoolean(DELETE_IS_SECRET);

            // Operation
            DeleteOperation op = new DeleteOperation(this, new ProviderHelper(this), this);
            DeleteResult result = op.execute(masterKeyIds, isSecret);

            // Result
            sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);

        } else if (ACTION_EDIT_KEYRING.equals(action)) {

            // Input
            SaveKeyringParcel saveParcel = data.getParcelable(EDIT_KEYRING_PARCEL);
            String passphrase = data.getString(EDIT_KEYRING_PASSPHRASE);

            // Operation
            EditKeyOperation op = new EditKeyOperation(this, providerHelper, this, mActionCanceled);
            EditKeyResult result = op.execute(saveParcel, passphrase);

            // Result
            sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);

        } else if (ACTION_EXPORT_KEYRING.equals(action)) {

            // Input
            boolean exportSecret = data.getBoolean(EXPORT_SECRET, false);
            String outputFile = data.getString(EXPORT_FILENAME);
            Uri outputUri = data.getParcelable(EXPORT_URI);

            boolean exportAll = data.getBoolean(EXPORT_ALL);
            long[] masterKeyIds = exportAll ? null : data.getLongArray(EXPORT_KEY_RING_MASTER_KEY_ID);

            // Operation
            ImportExportOperation importExportOperation = new ImportExportOperation(this, new ProviderHelper(this), this);
            ExportResult result;
            if (outputFile != null) {
                result = importExportOperation.exportToFile(masterKeyIds, exportSecret, outputFile);
            } else {
                result = importExportOperation.exportToUri(masterKeyIds, exportSecret, outputUri);
            }

            // Result
            sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);

        } else if (ACTION_IMPORT_KEYRING.equals(action)) {

            try {

                // Input
                String keyServer = data.getString(IMPORT_KEY_SERVER);
                Iterator<ParcelableKeyRing> entries;
                int numEntries;
                if (data.containsKey(IMPORT_KEY_LIST)) {
                    // get entries from intent
                    ArrayList<ParcelableKeyRing> list = data.getParcelableArrayList(IMPORT_KEY_LIST);
                    entries = list.iterator();
                    numEntries = list.size();
                } else {
                    // get entries from cached file
                    ParcelableFileCache<ParcelableKeyRing> cache =
                            new ParcelableFileCache<ParcelableKeyRing>(this, "key_import.pcl");
                    IteratorWithSize<ParcelableKeyRing> it = cache.readCache();
                    entries = it;
                    numEntries = it.getSize();
                }

                // Operation
                ImportExportOperation importExportOperation = new ImportExportOperation(
                        this, providerHelper, this, mActionCanceled);
                ImportKeyResult result = importExportOperation.importKeyRings(entries, numEntries, keyServer);

                // Special: consolidate on secret key import (cannot be cancelled!)
                if (result.mSecret > 0) {
                    // TODO move this into the import operation
                    providerHelper.consolidateDatabaseStep1(this);
                }

                // Special: make sure new data is synced into contacts
                ContactSyncAdapterService.requestSync();

                // Result
                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY, result);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }

        } else if (ACTION_SIGN_ENCRYPT.equals(action)) {

            try {
                /* Input */
                int source = data.get(SOURCE) != null ? data.getInt(SOURCE) : data.getInt(TARGET);
                Bundle resultData = new Bundle();

                long sigMasterKeyId = data.getLong(ENCRYPT_SIGNATURE_MASTER_ID);
                String sigKeyPassphrase = data.getString(ENCRYPT_SIGNATURE_KEY_PASSPHRASE);

                byte[] nfcHash = data.getByteArray(ENCRYPT_SIGNATURE_NFC_HASH);
                Date nfcTimestamp = (Date) data.getSerializable(ENCRYPT_SIGNATURE_NFC_TIMESTAMP);

                String symmetricPassphrase = data.getString(ENCRYPT_SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmor = data.getBoolean(ENCRYPT_USE_ASCII_ARMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPT_ENCRYPTION_KEYS_IDS);
                int compressionId = data.getInt(ENCRYPT_COMPRESSION_ID);
                int urisCount = data.containsKey(ENCRYPT_INPUT_URIS) ? data.getParcelableArrayList(ENCRYPT_INPUT_URIS).size() : 1;
                for (int i = 0; i < urisCount; i++) {
                    data.putInt(SELECTED_URI, i);
                    InputData inputData = createEncryptInputData(data);
                    OutputStream outStream = createCryptOutputStream(data);
                    String originalFilename = getOriginalFilename(data);

                    /* Operation */
                    PgpSignEncrypt.Builder builder = new PgpSignEncrypt.Builder(
                            this, new ProviderHelper(this), this, inputData, outStream
                    );
                    builder.setEnableAsciiArmorOutput(useAsciiArmor)
                            .setVersionHeader(PgpHelper.getVersionForHeader(this))
                            .setCompressionId(compressionId)
                            .setSymmetricEncryptionAlgorithm(
                                    Preferences.getPreferences(this).getDefaultEncryptionAlgorithm())
                            .setEncryptionMasterKeyIds(encryptionKeyIds)
                            .setSymmetricPassphrase(symmetricPassphrase)
                            .setOriginalFilename(originalFilename);

                    try {

                        // Find the appropriate subkey to sign with
                        CachedPublicKeyRing signingRing =
                                new ProviderHelper(this).getCachedPublicKeyRing(sigMasterKeyId);
                        long sigSubKeyId = signingRing.getSecretSignId();

                        // Set signature settings
                        builder.setSignatureMasterKeyId(sigMasterKeyId)
                                .setSignatureSubKeyId(sigSubKeyId)
                                .setSignaturePassphrase(sigKeyPassphrase)
                                .setSignatureHashAlgorithm(
                                        Preferences.getPreferences(this).getDefaultHashAlgorithm())
                                .setAdditionalEncryptId(sigMasterKeyId);
                        if (nfcHash != null && nfcTimestamp != null) {
                            builder.setNfcState(nfcHash, nfcTimestamp);
                        }

                    } catch (PgpKeyNotFoundException e) {
                        // encrypt-only
                        // TODO Just silently drop the requested signature? Shouldn't we throw here?
                    }

                    // this assumes that the bytes are cleartext (valid for current implementation!)
                    if (source == IO_BYTES) {
                        builder.setCleartextInput(true);
                    }

                    SignEncryptResult result = builder.build().execute();
                    resultData.putParcelable(SignEncryptResult.EXTRA_RESULT, result);

                    outStream.close();

                    /* Output */

                    finalizeEncryptOutputStream(data, resultData, outStream);

                }

                Log.logDebugBundle(resultData, "resultData");

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
                HkpKeyserver server = new HkpKeyserver(keyServer);

                CanonicalizedPublicKeyRing keyring = providerHelper.getCanonicalizedPublicKeyRing(dataUri);
                ImportExportOperation importExportOperation = new ImportExportOperation(this, new ProviderHelper(this), this);

                try {
                    importExportOperation.uploadKeyRingToServer(server, keyring);
                } catch (Keyserver.AddKeyException e) {
                    throw new PgpGeneralException("Unable to export key to selected server");
                }

                sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }

    }

    private void sendErrorToHandler(Exception e) {
        // TODO: Implement a better exception handling here
        // contextualize the exception, if necessary
        String message;
        if (e instanceof PgpGeneralMsgIdException) {
            e = ((PgpGeneralMsgIdException) e).getContextualized(this);
            message = e.getMessage();
        } else {
            message = e.getMessage();
        }

        Log.d(Constants.TAG, "KeychainIntentService Exception: ", e);

        Bundle data = new Bundle();
        data.putString(KeychainIntentServiceHandler.DATA_ERROR, message);
        sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToHandler(Integer arg1, Integer arg2, Bundle data) {

        Message msg = Message.obtain();
        assert msg != null;
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

    private void sendMessageToHandler(Integer arg1, OperationResult data) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(OperationResult.EXTRA_RESULT, data);
        sendMessageToHandler(arg1, null, bundle);
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
    public void setPreventCancel() {
        sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_PREVENT_CANCEL);
    }

    private InputData createDecryptInputData(Bundle data) throws IOException, PgpGeneralException {
        return createCryptInputData(data, DECRYPT_CIPHERTEXT_BYTES);
    }

    private InputData createEncryptInputData(Bundle data) throws IOException, PgpGeneralException {
        return createCryptInputData(data, ENCRYPT_MESSAGE_BYTES);
    }

    private InputData createCryptInputData(Bundle data, String bytesName) throws PgpGeneralException, IOException {
        int source = data.get(SOURCE) != null ? data.getInt(SOURCE) : data.getInt(TARGET);
        switch (source) {
            case IO_BYTES: /* encrypting bytes directly */
                byte[] bytes = data.getByteArray(bytesName);
                return new InputData(new ByteArrayInputStream(bytes), bytes.length);

            case IO_URI: /* encrypting content uri */
                Uri providerUri = data.getParcelable(ENCRYPT_DECRYPT_INPUT_URI);

                // InputStream
                return new InputData(getContentResolver().openInputStream(providerUri), FileHelper.getFileSize(this, providerUri, 0));

            case IO_URIS:
                providerUri = data.<Uri>getParcelableArrayList(ENCRYPT_INPUT_URIS).get(data.getInt(SELECTED_URI));

                // InputStream
                return new InputData(getContentResolver().openInputStream(providerUri), FileHelper.getFileSize(this, providerUri, 0));

            default:
                throw new PgpGeneralException("No target choosen!");
        }
    }

    private String getOriginalFilename(Bundle data) throws PgpGeneralException, FileNotFoundException {
        int target = data.getInt(TARGET);
        switch (target) {
            case IO_BYTES:
                return "";

            case IO_URI:
                Uri providerUri = data.getParcelable(ENCRYPT_DECRYPT_INPUT_URI);

                return FileHelper.getFilename(this, providerUri);

            case IO_URIS:
                providerUri = data.<Uri>getParcelableArrayList(ENCRYPT_INPUT_URIS).get(data.getInt(SELECTED_URI));

                return FileHelper.getFilename(this, providerUri);

            default:
                throw new PgpGeneralException("No target choosen!");
        }
    }

    private OutputStream createCryptOutputStream(Bundle data) throws PgpGeneralException, FileNotFoundException {
        int target = data.getInt(TARGET);
        switch (target) {
            case IO_BYTES:
                return new ByteArrayOutputStream();

            case IO_URI:
                Uri providerUri = data.getParcelable(ENCRYPT_DECRYPT_OUTPUT_URI);

                return getContentResolver().openOutputStream(providerUri);

            case IO_URIS:
                providerUri = data.<Uri>getParcelableArrayList(ENCRYPT_OUTPUT_URIS).get(data.getInt(SELECTED_URI));

                return getContentResolver().openOutputStream(providerUri);

            default:
                throw new PgpGeneralException("No target choosen!");
        }
    }

    private void finalizeEncryptOutputStream(Bundle data, Bundle resultData, OutputStream outStream) {
        finalizeCryptOutputStream(data, resultData, outStream, RESULT_BYTES);
    }

    private void finalizeDecryptOutputStream(Bundle data, Bundle resultData, OutputStream outStream) {
        finalizeCryptOutputStream(data, resultData, outStream, RESULT_DECRYPTED_BYTES);
    }

    private void finalizeCryptOutputStream(Bundle data, Bundle resultData, OutputStream outStream, String bytesName) {
        int target = data.getInt(TARGET);
        switch (target) {
            case IO_BYTES:
                byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                resultData.putByteArray(bytesName, output);
                break;
            case IO_URI:
            case IO_URIS:
                // nothing, output was written, just send okay and verification bundle

                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_CANCEL.equals(intent.getAction())) {
            mActionCanceled.set(true);
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
