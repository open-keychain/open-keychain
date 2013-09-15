/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.openintents.openpgp.IOpenPgpCallback;
import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpOperation;
import org.sufficientlysecure.keychain.pgp.exception.NoAsymmetricEncryptionException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class OpenPgpService extends RemoteService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "OpenPgpService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "OpenPgpService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private String getCachedPassphrase(long keyId) {
        String passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), keyId);

        if (passphrase == null) {
            Log.d(Constants.TAG, "No passphrase! Activity required!");

            // start passphrase dialog
            Bundle extras = new Bundle();
            extras.putLong(RemoteServiceActivity.EXTRA_SECRET_KEY_ID, keyId);

            PassphraseActivityCallback callback = new PassphraseActivityCallback();
            Messenger messenger = new Messenger(new Handler(getMainLooper(), callback));

            pauseQueueAndStartServiceActivity(RemoteServiceActivity.ACTION_CACHE_PASSPHRASE,
                    messenger, extras);

            if (callback.isSuccess()) {
                Log.d(Constants.TAG, "New passphrase entered!");

                // get again after it was entered
                passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), keyId);
            } else {
                Log.d(Constants.TAG, "Passphrase dialog canceled!");

                return null;
            }

        }

        return passphrase;
    }

    public class PassphraseActivityCallback extends UserInputCallback {

        private boolean success = false;

        public boolean isSuccess() {
            return success;
        }

        @Override
        public void handleUserInput(Message msg) {
            if (msg.arg1 == OKAY) {
                success = true;
            } else {
                success = false;
            }
        }
    };

    /**
     * Search database for key ids based on emails.
     * 
     * @param encryptionUserIds
     * @return
     */
    private long[] getKeyIdsFromEmails(String[] encryptionUserIds, long ownKeyId) {
        // find key ids to given emails in database
        ArrayList<Long> keyIds = new ArrayList<Long>();

        boolean missingUserIdsCheck = false;
        boolean dublicateUserIdsCheck = false;
        ArrayList<String> missingUserIds = new ArrayList<String>();
        ArrayList<String> dublicateUserIds = new ArrayList<String>();

        for (String email : encryptionUserIds) {
            Uri uri = KeychainContract.KeyRings.buildPublicKeyRingsByEmailsUri(email);
            Cursor cur = getContentResolver().query(uri, null, null, null, null);
            if (cur.moveToFirst()) {
                long id = cur.getLong(cur.getColumnIndex(KeychainContract.KeyRings.MASTER_KEY_ID));
                keyIds.add(id);
            } else {
                missingUserIdsCheck = true;
                missingUserIds.add(email);
                Log.d(Constants.TAG, "user id missing");
            }
            if (cur.moveToNext()) {
                dublicateUserIdsCheck = true;
                dublicateUserIds.add(email);
                Log.d(Constants.TAG, "more than one user id with the same email");
            }
        }

        // also encrypt to our self (so that we can decrypt it later!)
        keyIds.add(ownKeyId);

        // convert to long[]
        long[] keyIdsArray = new long[keyIds.size()];
        for (int i = 0; i < keyIdsArray.length; i++) {
            keyIdsArray[i] = keyIds.get(i);
        }

        if (missingUserIdsCheck || dublicateUserIdsCheck) {
            SelectPubKeysActivityCallback callback = new SelectPubKeysActivityCallback();
            Messenger messenger = new Messenger(new Handler(getMainLooper(), callback));

            Bundle extras = new Bundle();
            extras.putLongArray(RemoteServiceActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
            extras.putStringArrayList(RemoteServiceActivity.EXTRA_MISSING_USER_IDS, missingUserIds);
            extras.putStringArrayList(RemoteServiceActivity.EXTRA_DUBLICATE_USER_IDS,
                    dublicateUserIds);

            pauseQueueAndStartServiceActivity(RemoteServiceActivity.ACTION_SELECT_PUB_KEYS,
                    messenger, extras);

            if (callback.isSuccess()) {
                Log.d(Constants.TAG, "New selection of pub keys!");
                keyIdsArray = callback.getPubKeyIds();
            } else {
                Log.d(Constants.TAG, "Pub key selection canceled!");
                return null;
            }
        }

        if (keyIdsArray.length == 0) {
            return null;
        }
        return keyIdsArray;
    }

    public class SelectPubKeysActivityCallback extends UserInputCallback {
        public static final String PUB_KEY_IDS = "pub_key_ids";

        private boolean success = false;
        private long[] pubKeyIds;

        public boolean isSuccess() {
            return success;
        }

        public long[] getPubKeyIds() {
            return pubKeyIds;
        }

        @Override
        public void handleUserInput(Message msg) {
            if (msg.arg1 == OKAY) {
                success = true;
                pubKeyIds = msg.getData().getLongArray(PUB_KEY_IDS);
            } else {
                success = false;
            }
        }
    };

    private synchronized void encryptAndSignSafe(byte[] inputBytes, String[] encryptionUserIds,
            boolean asciiArmor, IOpenPgpCallback callback, AppSettings appSettings, boolean sign)
            throws RemoteException {
        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            long[] keyIds = getKeyIdsFromEmails(encryptionUserIds, appSettings.getKeyId());
            if (keyIds == null) {
                callback.onError(new OpenPgpError(OpenPgpError.ID_NO_USER_IDS, "No user ids!"));
                return;
            }

            PgpOperation operation = new PgpOperation(getContext(), null, inputData, outputStream);
            if (sign) {
                String passphrase = getCachedPassphrase(appSettings.getKeyId());
                if (passphrase == null) {
                    callback.onError(new OpenPgpError(OpenPgpError.ID_NO_OR_WRONG_PASSPHRASE,
                            "No or wrong passphrase!"));
                    return;
                }

                operation.signAndEncrypt(asciiArmor, appSettings.getCompression(), keyIds, null,
                        appSettings.getEncryptionAlgorithm(), appSettings.getKeyId(),
                        appSettings.getHashAlgorithm(), true, passphrase);
            } else {
                operation.signAndEncrypt(asciiArmor, appSettings.getCompression(), keyIds, null,
                        appSettings.getEncryptionAlgorithm(), Id.key.none,
                        appSettings.getHashAlgorithm(), true, null);
            }

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // return over handler on client side
            callback.onSuccess(outputBytes, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new OpenPgpError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    // TODO: asciiArmor?!
    private void signSafe(byte[] inputBytes, IOpenPgpCallback callback, AppSettings appSettings)
            throws RemoteException {
        try {
            Log.d(Constants.TAG, "current therad id: " + Thread.currentThread().getId());

            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            String passphrase = getCachedPassphrase(appSettings.getKeyId());
            if (passphrase == null) {
                callback.onError(new OpenPgpError(OpenPgpError.ID_NO_OR_WRONG_PASSPHRASE,
                        "No or wrong passphrase!"));
                return;
            }

            PgpOperation operation = new PgpOperation(getContext(), null, inputData, outputStream);
            operation.signText(appSettings.getKeyId(), passphrase, appSettings.getHashAlgorithm(),
                    Preferences.getPreferences(this).getForceV3Signatures());

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // return over handler on client side
            callback.onSuccess(outputBytes, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new OpenPgpError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private synchronized void decryptAndVerifySafe(byte[] inputBytes, IOpenPgpCallback callback,
            AppSettings appSettings) throws RemoteException {
        try {
            // TODO: this is not really needed
            // checked if it is text with BEGIN and END tags
            String message = new String(inputBytes);
            Log.d(Constants.TAG, "in: " + message);
            boolean signedOnly = false;
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(message);
            if (matcher.matches()) {
                Log.d(Constants.TAG, "PGP_MESSAGE matched");
                message = matcher.group(1);
                // replace non breakable spaces
                message = message.replaceAll("\\xa0", " ");

                // overwrite inputBytes
                inputBytes = message.getBytes();
            } else {
                matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(message);
                if (matcher.matches()) {
                    signedOnly = true;
                    Log.d(Constants.TAG, "PGP_SIGNED_MESSAGE matched");
                    message = matcher.group(1);
                    // replace non breakable spaces
                    message = message.replaceAll("\\xa0", " ");

                    // overwrite inputBytes
                    inputBytes = message.getBytes();
                } else {
                    Log.d(Constants.TAG, "Nothing matched! Binary?");
                }
            }
            // END TODO

            Log.d(Constants.TAG, "in: " + new String(inputBytes));

            // TODO: This allows to decrypt messages with ALL secret keys, not only the one for the
            // app, Fix this?
            // long secretKeyId = PgpMain.getDecryptionKeyId(getContext(), inputStream);
            // if (secretKeyId == Id.key.none) {
            // throw new PgpMain.PgpGeneralException(getString(R.string.error_noSecretKeyFound));
            // }

            String passphrase = null;
            boolean assumeSymmetricEncryption = false;
            if (!signedOnly) {
                // BEGIN Get key
                // TODO: this input stream is consumed after PgpMain.getDecryptionKeyId()... do it
                // better!
                InputStream inputStream2 = new ByteArrayInputStream(inputBytes);

                // TODO: duplicates functions from DecryptActivity!
                // TODO: we need activity to input symmetric passphrase
                long secretKeyId;
                try {
                    if (inputStream2.markSupported()) {
                        // should probably set this to the max size of two
                        // pgpF objects, if it even needs to be anything other
                        // than 0.
                        inputStream2.mark(200);
                    }
                    secretKeyId = PgpHelper.getDecryptionKeyId(this, inputStream2);
                    if (secretKeyId == Id.key.none) {
                        throw new PgpGeneralException(getString(R.string.error_noSecretKeyFound));
                    }
                    assumeSymmetricEncryption = false;
                } catch (NoAsymmetricEncryptionException e) {
                    if (inputStream2.markSupported()) {
                        inputStream2.reset();
                    }
                    secretKeyId = Id.key.symmetric;
                    if (!PgpOperation.hasSymmetricEncryption(this, inputStream2)) {
                        throw new PgpGeneralException(
                                getString(R.string.error_noKnownEncryptionFound));
                    }
                    assumeSymmetricEncryption = true;
                }

                Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

                passphrase = getCachedPassphrase(secretKeyId);
                if (passphrase == null) {
                    callback.onError(new OpenPgpError(OpenPgpError.ID_NO_OR_WRONG_PASSPHRASE,
                            "No or wrong passphrase!"));
                    return;
                }
            }

            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            Bundle outputBundle;
            PgpOperation operation = new PgpOperation(getContext(), null, inputData, outputStream);
            if (signedOnly) {
                // TODO: download missing keys from keyserver?
                outputBundle = operation.verifyText(false);
            } else {
                // TODO: assume symmetric: callback to enter symmetric pass
                outputBundle = operation.decryptAndVerify(passphrase, assumeSymmetricEncryption);
            }

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // get signature informations from bundle
            boolean signature = outputBundle.getBoolean(KeychainIntentService.RESULT_SIGNATURE);

            OpenPgpSignatureResult sigResult = null;
            if (signature) {
                // long signatureKeyId = outputBundle
                // .getLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID);
                String signatureUserId = outputBundle
                        .getString(KeychainIntentService.RESULT_SIGNATURE_USER_ID);
                boolean signatureSuccess = outputBundle
                        .getBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS);
                boolean signatureUnknown = outputBundle
                        .getBoolean(KeychainIntentService.RESULT_SIGNATURE_UNKNOWN);

                int signatureStatus = OpenPgpSignatureResult.SIGNATURE_ERROR;
                if (signatureSuccess) {
                    signatureStatus = OpenPgpSignatureResult.SIGNATURE_SUCCESS_TRUSTED;
                } else if (signatureUnknown) {
                    signatureStatus = OpenPgpSignatureResult.SIGNATURE_UNKNOWN;
                }

                sigResult = new OpenPgpSignatureResult(signatureStatus, signatureUserId, signedOnly);
            }

            // return over handler on client side
            callback.onSuccess(outputBytes, sigResult);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new OpenPgpError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {

        @Override
        public void encrypt(final byte[] inputBytes, final String[] encryptionUserIds,
                final boolean asciiArmor, final IOpenPgpCallback callback) throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        encryptAndSignSafe(inputBytes, encryptionUserIds, asciiArmor, callback,
                                settings, false);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "OpenPgpService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void signAndEncrypt(final byte[] inputBytes, final String[] encryptionUserIds,
                final boolean asciiArmor, final IOpenPgpCallback callback) throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        encryptAndSignSafe(inputBytes, encryptionUserIds, asciiArmor, callback,
                                settings, true);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "OpenPgpService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void sign(final byte[] inputBytes, boolean asciiArmor,
                final IOpenPgpCallback callback) throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        signSafe(inputBytes, callback, settings);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "OpenPgpService", e);
                    }
                }
            };

            checkAndEnqueue(r);

        }

        @Override
        public void decryptAndVerify(final byte[] inputBytes, final IOpenPgpCallback callback)
                throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        decryptAndVerifySafe(inputBytes, callback, settings);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "OpenPgpService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

    };

}
