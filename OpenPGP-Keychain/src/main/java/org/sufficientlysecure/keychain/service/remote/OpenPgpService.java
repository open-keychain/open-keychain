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
import org.openintents.openpgp.IOpenPgpKeyIdsCallback;
import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpData;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.spongycastle.util.Arrays;
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
import org.sufficientlysecure.keychain.service.exception.NoUserIdsException;
import org.sufficientlysecure.keychain.service.exception.UserInteractionRequiredException;
import org.sufficientlysecure.keychain.service.exception.WrongPassphraseException;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

public class OpenPgpService extends RemoteService {

    private String getCachedPassphrase(long keyId, boolean allowUserInteraction)
            throws UserInteractionRequiredException {
        String passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), keyId);

        if (passphrase == null) {
            if (!allowUserInteraction) {
                throw new UserInteractionRequiredException(
                        "Passphrase not found in cache, please enter your passphrase!");
            }

            Log.d(Constants.TAG, "No passphrase! Activity required!");

            // start passphrase dialog
            PassphraseActivityCallback callback = new PassphraseActivityCallback();
            Bundle extras = new Bundle();
            extras.putLong(RemoteServiceActivity.EXTRA_SECRET_KEY_ID, keyId);
            pauseAndStartUserInteraction(RemoteServiceActivity.ACTION_CACHE_PASSPHRASE, callback,
                    extras);

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
    private long[] getKeyIdsFromEmails(String[] encryptionUserIds, boolean allowUserInteraction)
            throws UserInteractionRequiredException {
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

        // convert to long[]
        long[] keyIdsArray = new long[keyIds.size()];
        for (int i = 0; i < keyIdsArray.length; i++) {
            keyIdsArray[i] = keyIds.get(i);
        }

        // allow the user to verify pub key selection
        if (allowUserInteraction && (missingUserIdsCheck || dublicateUserIdsCheck)) {
            SelectPubKeysActivityCallback callback = new SelectPubKeysActivityCallback();

            Bundle extras = new Bundle();
            extras.putLongArray(RemoteServiceActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
            extras.putStringArrayList(RemoteServiceActivity.EXTRA_MISSING_USER_IDS, missingUserIds);
            extras.putStringArrayList(RemoteServiceActivity.EXTRA_DUBLICATE_USER_IDS,
                    dublicateUserIds);

            pauseAndStartUserInteraction(RemoteServiceActivity.ACTION_SELECT_PUB_KEYS, callback,
                    extras);

            if (callback.isSuccess()) {
                Log.d(Constants.TAG, "New selection of pub keys!");
                keyIdsArray = callback.getPubKeyIds();
            } else {
                Log.d(Constants.TAG, "Pub key selection canceled!");
                return null;
            }
        }

        // if no user interaction is allow throw exceptions on duplicate or missing pub keys
        if (!allowUserInteraction) {
            if (missingUserIdsCheck)
                throw new UserInteractionRequiredException(
                        "Pub keys for these user ids are missing:" + missingUserIds.toString());
            if (dublicateUserIdsCheck)
                throw new UserInteractionRequiredException(
                        "More than one pub key with these user ids exist:"
                                + dublicateUserIds.toString());
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

    private synchronized void getKeyIdsSafe(String[] userIds, boolean allowUserInteraction,
            IOpenPgpKeyIdsCallback callback, AppSettings appSettings) {
        try {
            long[] keyIds = getKeyIdsFromEmails(userIds, allowUserInteraction);
            if (keyIds == null) {
                throw new NoUserIdsException("No user ids!");
            }

            callback.onSuccess(keyIds);
        } catch (UserInteractionRequiredException e) {
            callbackOpenPgpError(callback, OpenPgpError.USER_INTERACTION_REQUIRED, e.getMessage());
        } catch (NoUserIdsException e) {
            callbackOpenPgpError(callback, OpenPgpError.NO_USER_IDS, e.getMessage());
        } catch (Exception e) {
            callbackOpenPgpError(callback, OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private synchronized void encryptAndSignSafe(OpenPgpData inputData,
            final OpenPgpData outputData, long[] keyIds, boolean allowUserInteraction,
            IOpenPgpCallback callback, AppSettings appSettings, boolean sign) {
        try {
            // TODO: other options of OpenPgpData!
            byte[] inputBytes = getInput(inputData);
            boolean asciiArmor = false;
            if (outputData.getType() == OpenPgpData.TYPE_STRING) {
                asciiArmor = true;
            }

            // add own key for encryption
            keyIds = Arrays.copyOf(keyIds, keyIds.length + 1);
            keyIds[keyIds.length - 1] = appSettings.getKeyId();

            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputDt = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            PgpOperation operation = new PgpOperation(getContext(), null, inputDt, outputStream);
            if (sign) {
                String passphrase = getCachedPassphrase(appSettings.getKeyId(),
                        allowUserInteraction);
                if (passphrase == null) {
                    throw new WrongPassphraseException("No or wrong passphrase!");
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

            OpenPgpData output = null;
            if (asciiArmor) {
                output = new OpenPgpData(new String(outputBytes));
            } else {
                output = new OpenPgpData(outputBytes);
            }

            // return over handler on client side
            callback.onSuccess(output, null);
        } catch (UserInteractionRequiredException e) {
            callbackOpenPgpError(callback, OpenPgpError.USER_INTERACTION_REQUIRED, e.getMessage());
        } catch (WrongPassphraseException e) {
            callbackOpenPgpError(callback, OpenPgpError.NO_OR_WRONG_PASSPHRASE, e.getMessage());
        } catch (Exception e) {
            callbackOpenPgpError(callback, OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    // TODO: asciiArmor?!
    private void signSafe(byte[] inputBytes, boolean allowUserInteraction,
            IOpenPgpCallback callback, AppSettings appSettings) {
        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            String passphrase = getCachedPassphrase(appSettings.getKeyId(), allowUserInteraction);
            if (passphrase == null) {
                throw new WrongPassphraseException("No or wrong passphrase!");
            }

            PgpOperation operation = new PgpOperation(getContext(), null, inputData, outputStream);
            operation.signText(appSettings.getKeyId(), passphrase, appSettings.getHashAlgorithm(),
                    Preferences.getPreferences(this).getForceV3Signatures());

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();
            OpenPgpData output = new OpenPgpData(new String(outputBytes));

            // return over handler on client side
            callback.onSuccess(output, null);
        } catch (UserInteractionRequiredException e) {
            callbackOpenPgpError(callback, OpenPgpError.USER_INTERACTION_REQUIRED, e.getMessage());
        } catch (WrongPassphraseException e) {
            callbackOpenPgpError(callback, OpenPgpError.NO_OR_WRONG_PASSPHRASE, e.getMessage());
        } catch (Exception e) {
            callbackOpenPgpError(callback, OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private synchronized void decryptAndVerifySafe(byte[] inputBytes, boolean allowUserInteraction,
            IOpenPgpCallback callback, AppSettings appSettings) {
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

            String passphrase = null;
            if (!signedOnly) {
                // BEGIN Get key
                // TODO: this input stream is consumed after PgpMain.getDecryptionKeyId()... do it
                // better!
                InputStream inputStream2 = new ByteArrayInputStream(inputBytes);

                // TODO: duplicates functions from DecryptActivity!
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
                        throw new PgpGeneralException(getString(R.string.error_no_secret_key_found));
                    }
                } catch (NoAsymmetricEncryptionException e) {
                    if (inputStream2.markSupported()) {
                        inputStream2.reset();
                    }
                    secretKeyId = Id.key.symmetric;
                    if (!PgpOperation.hasSymmetricEncryption(this, inputStream2)) {
                        throw new PgpGeneralException(
                                getString(R.string.error_no_known_encryption_found));
                    }
                    // we do not support symmetric decryption from the API!
                    throw new Exception("Symmetric decryption is not supported!");
                }

                Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

                passphrase = getCachedPassphrase(secretKeyId, allowUserInteraction);
                if (passphrase == null) {
                    throw new WrongPassphraseException("No or wrong passphrase!");
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
                outputBundle = operation.verifyText();
            } else {
                outputBundle = operation.decryptAndVerify(passphrase, false);
            }

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // get signature informations from bundle
            boolean signature = outputBundle.getBoolean(KeychainIntentService.RESULT_SIGNATURE);

            OpenPgpSignatureResult sigResult = null;
            if (signature) {
                long signatureKeyId = outputBundle
                        .getLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID);
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
                    signatureStatus = OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY;
                }

                sigResult = new OpenPgpSignatureResult(signatureStatus, signatureUserId,
                        signedOnly, signatureKeyId);
            }
            OpenPgpData output = new OpenPgpData(new String(outputBytes));

            // return over handler on client side
            callback.onSuccess(output, sigResult);
        } catch (UserInteractionRequiredException e) {
            callbackOpenPgpError(callback, OpenPgpError.USER_INTERACTION_REQUIRED, e.getMessage());
        } catch (WrongPassphraseException e) {
            callbackOpenPgpError(callback, OpenPgpError.NO_OR_WRONG_PASSPHRASE, e.getMessage());
        } catch (Exception e) {
            callbackOpenPgpError(callback, OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    /**
     * Returns error to IOpenPgpCallback
     * 
     * @param callback
     * @param errorId
     * @param message
     */
    private void callbackOpenPgpError(IOpenPgpCallback callback, int errorId, String message) {
        try {
            callback.onError(new OpenPgpError(0, message));
        } catch (Exception t) {
            Log.e(Constants.TAG,
                    "Exception while returning OpenPgpError to client via callback.onError()", t);
        }
    }

    private void callbackOpenPgpError(IOpenPgpKeyIdsCallback callback, int errorId, String message) {
        try {
            callback.onError(new OpenPgpError(0, message));
        } catch (Exception t) {
            Log.e(Constants.TAG,
                    "Exception while returning OpenPgpError to client via callback.onError()", t);
        }
    }

    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {

        @Override
        public void encrypt(final OpenPgpData input, final OpenPgpData output, final long[] keyIds,
                final IOpenPgpCallback callback) throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    encryptAndSignSafe(input, output, keyIds, true, callback, settings, false);
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void signAndEncrypt(final OpenPgpData input, final OpenPgpData output,
                final long[] keyIds, final IOpenPgpCallback callback) throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    encryptAndSignSafe(input, output, keyIds, true, callback, settings, true);
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void sign(final OpenPgpData input, final OpenPgpData output,
                final IOpenPgpCallback callback) throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    signSafe(getInput(input), true, callback, settings);
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void decryptAndVerify(final OpenPgpData input, final OpenPgpData output,
                final IOpenPgpCallback callback) throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    decryptAndVerifySafe(getInput(input), true, callback, settings);
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void getKeyIds(final String[] userIds, final boolean allowUserInteraction,
                final IOpenPgpKeyIdsCallback callback) throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    getKeyIdsSafe(userIds, allowUserInteraction, callback, settings);
                }
            };

            checkAndEnqueue(r);
        }

    };

    private static byte[] getInput(OpenPgpData data) {
        // TODO: support Uri and ParcelFileDescriptor

        byte[] inBytes = null;
        switch (data.getType()) {
        case OpenPgpData.TYPE_STRING:
            inBytes = data.getString().getBytes();
            break;

        case OpenPgpData.TYPE_BYTE_ARRAY:
            inBytes = data.getBytes();
            break;

        default:
            Log.e(Constants.TAG, "Uri and ParcelFileDescriptor not supported right now!");
            break;
        }

        return inBytes;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
