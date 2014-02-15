/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpConstants;
import org.spongycastle.util.Arrays;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpOperation;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class OpenPgpService extends RemoteService {

    private static final int PRIVATE_REQUEST_CODE_PASSPHRASE = 551;
    private static final int PRIVATE_REQUEST_CODE_USER_IDS = 552;


    /**
     * Search database for key ids based on emails.
     *
     * @param encryptionUserIds
     * @return
     */
    private Bundle getKeyIdsFromEmails(Bundle params, String[] encryptionUserIds) {
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
        if (missingUserIdsCheck || dublicateUserIdsCheck) {
            // build PendingIntent for passphrase input
            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.setAction(RemoteServiceActivity.ACTION_SELECT_PUB_KEYS);
            intent.putExtra(RemoteServiceActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
            intent.putExtra(RemoteServiceActivity.EXTRA_MISSING_USER_IDS, missingUserIds);
            intent.putExtra(RemoteServiceActivity.EXTRA_DUBLICATE_USER_IDS, dublicateUserIds);
            intent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_USER_IDS, intent, 0);

            // return PendingIntent to be executed by client
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putParcelable(OpenPgpConstants.RESULT_INTENT, pi);

            return result;
        }

        if (keyIdsArray.length == 0) {
            return null;
        }

        Bundle result = new Bundle();
        result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_SUCCESS);
        result.putLongArray(OpenPgpConstants.PARAMS_KEY_IDS, keyIdsArray);
        return result;
    }

    private Bundle getPassphraseBundleIntent(Bundle params, long keyId) {
        // build PendingIntent for passphrase input
        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
        intent.setAction(RemoteServiceActivity.ACTION_CACHE_PASSPHRASE);
        intent.putExtra(RemoteServiceActivity.EXTRA_SECRET_KEY_ID, keyId);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);
        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_PASSPHRASE, intent, 0);

        // return PendingIntent to be executed by client
        Bundle result = new Bundle();
        result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED);
        result.putParcelable(OpenPgpConstants.RESULT_INTENT, pi);

        return result;
    }


    // TODO: asciiArmor?!
    private Bundle signImpl(Bundle params, ParcelFileDescriptor input, ParcelFileDescriptor output, AppSettings appSettings) {
        try {
            // get passphrase from cache, if key has "no" passphrase, this returns an empty String
            String passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), appSettings.getKeyId());
            if (passphrase == null) {
                // get PendingIntent for passphrase input, add it to given params and return to client
                Bundle passphraseBundle = getPassphraseBundleIntent(params, appSettings.getKeyId());
                return passphraseBundle;
            }

            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            try {
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                PgpOperation operation = new PgpOperation(getContext(), null, inputData, os);
                operation.signText(appSettings.getKeyId(), passphrase, appSettings.getHashAlgorithm(),
                        Preferences.getPreferences(this).getForceV3Signatures());
            } finally {
                is.close();
                os.close();
            }

            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Bundle encryptAndSignImpl(Bundle params, ParcelFileDescriptor input,
                                      ParcelFileDescriptor output, AppSettings appSettings,
                                      boolean sign) {
        try {
            boolean asciiArmor = params.getBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, false);

            long[] keyIds;
            if (params.containsKey(OpenPgpConstants.PARAMS_KEY_IDS)) {
                keyIds = params.getLongArray(OpenPgpConstants.PARAMS_KEY_IDS);
            } else {
                // get key ids based on given user ids
                String[] userIds = params.getStringArray(OpenPgpConstants.PARAMS_USER_IDS);
                // give params through to activity...
                Bundle result = getKeyIdsFromEmails(params, userIds);

                if (result.getInt(OpenPgpConstants.RESULT_CODE, 0) == OpenPgpConstants.RESULT_CODE_SUCCESS) {
                    keyIds = result.getLongArray(OpenPgpConstants.PARAMS_KEY_IDS);
                } else {
                    // if not success -> result contains a PendingIntent for user interaction
                    return result;
                }
            }

            // add own key for encryption
            keyIds = Arrays.copyOf(keyIds, keyIds.length + 1);
            keyIds[keyIds.length - 1] = appSettings.getKeyId();

            // build InputData and write into OutputStream
            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            try {
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                PgpOperation operation = new PgpOperation(getContext(), null, inputData, os);
                if (sign) {
                    String passphrase = PassphraseCacheService.getCachedPassphrase(getContext(),
                            appSettings.getKeyId());
                    if (passphrase == null) {
                        // get PendingIntent for passphrase input, add it to given params and return to client
                        Bundle passphraseBundle = getPassphraseBundleIntent(params, appSettings.getKeyId());
                        return passphraseBundle;
                    }

                    operation.signAndEncrypt(asciiArmor, appSettings.getCompression(), keyIds, null,
                            appSettings.getEncryptionAlgorithm(), appSettings.getKeyId(),
                            appSettings.getHashAlgorithm(), true, passphrase);
                } else {
                    operation.signAndEncrypt(asciiArmor, appSettings.getCompression(), keyIds, null,
                            appSettings.getEncryptionAlgorithm(), Id.key.none,
                            appSettings.getHashAlgorithm(), true, null);
                }
            } finally {
                is.close();
                os.close();
            }

            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Bundle decryptAndVerifyImpl(Bundle params, ParcelFileDescriptor input,
                                        ParcelFileDescriptor output, AppSettings appSettings) {
        try {
            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            OpenPgpSignatureResult sigResult = null;
            try {


                // TODOs API 2.0:
                // implement verify-only!
                // fix the mess: http://stackoverflow.com/questions/148130/how-do-i-peek-at-the-first-two-bytes-in-an-inputstream
                // should we allow to decrypt everything under every key id or only the one set?
                // TODO: instead of trying to get the passphrase before
                // pause stream when passphrase is missing and then resume


                // TODO: this is not really needed
                // checked if it is text with BEGIN and END tags
//            String message = new String(inputBytes);
//            Log.d(Constants.TAG, "in: " + message);
                boolean signedOnly = false;
//            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(message);
//            if (matcher.matches()) {
//                Log.d(Constants.TAG, "PGP_MESSAGE matched");
//                message = matcher.group(1);
//                // replace non breakable spaces
//                message = message.replaceAll("\\xa0", " ");
//
//                // overwrite inputBytes
//                inputBytes = message.getBytes();
//            } else {
//                matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(message);
//                if (matcher.matches()) {
//                    signedOnly = true;
//                    Log.d(Constants.TAG, "PGP_SIGNED_MESSAGE matched");
//                    message = matcher.group(1);
//                    // replace non breakable spaces
//                    message = message.replaceAll("\\xa0", " ");
//
//                    // overwrite inputBytes
//                    inputBytes = message.getBytes();
//                } else {
//                    Log.d(Constants.TAG, "Nothing matched! Binary?");
//                }
//            }
                // END TODO

//            Log.d(Constants.TAG, "in: " + new String(inputBytes));

                // TODO: This allows to decrypt messages with ALL secret keys, not only the one for the
                // app, Fix this?

//                String passphrase = null;
//                if (!signedOnly) {
//                    // BEGIN Get key
//                    // TODO: this input stream is consumed after PgpMain.getDecryptionKeyId()... do it
//                    // better!
//                    InputStream inputStream2 = new ByteArrayInputStream(inputBytes);
//
//                    // TODO: duplicates functions from DecryptActivity!
//                    long secretKeyId;
//                    try {
//                        if (inputStream2.markSupported()) {
//                            // should probably set this to the max size of two
//                            // pgpF objects, if it even needs to be anything other
//                            // than 0.
//                            inputStream2.mark(200);
//                        }
//                        secretKeyId = PgpHelper.getDecryptionKeyId(this, inputStream2);
//                        if (secretKeyId == Id.key.none) {
//                            throw new PgpGeneralException(getString(R.string.error_no_secret_key_found));
//                        }
//                    } catch (NoAsymmetricEncryptionException e) {
//                        if (inputStream2.markSupported()) {
//                            inputStream2.reset();
//                        }
//                        secretKeyId = Id.key.symmetric;
//                        if (!PgpOperation.hasSymmetricEncryption(this, inputStream2)) {
//                            throw new PgpGeneralException(
//                                    getString(R.string.error_no_known_encryption_found));
//                        }
//                        // we do not support symmetric decryption from the API!
//                        throw new Exception("Symmetric decryption is not supported!");
//                    }
//
//                    Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

                // NOTE: currently this only gets the passphrase for the saved key
                String passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), appSettings.getKeyId());
                if (passphrase == null) {
                    // get PendingIntent for passphrase input, add it to given params and return to client
                    Bundle passphraseBundle = getPassphraseBundleIntent(params, appSettings.getKeyId());
                    return passphraseBundle;
                }
//                }

                // build InputData and write into OutputStream
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);


                Bundle outputBundle;
                PgpOperation operation = new PgpOperation(getContext(), null, inputData, os);
                if (signedOnly) {
                    outputBundle = operation.verifyText();
                } else {
                    // BIG TODO: instead of trying to get the passphrase before
                    // pause stream when passphrase is missing and then resume
                    outputBundle = operation.decryptAndVerify(passphrase, false);
                }

//                outputStream.close();

//                byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

                // get signature informations from bundle
                boolean signature = outputBundle.getBoolean(KeychainIntentService.RESULT_SIGNATURE);

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
                        signatureStatus = OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED;
                    } else if (signatureUnknown) {
                        signatureStatus = OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY;
                    }

                    sigResult = new OpenPgpSignatureResult(signatureStatus, signatureUserId,
                            signedOnly, signatureKeyId);
                }
            } finally {
                is.close();
                os.close();
            }

            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_SUCCESS);
            result.putParcelable(OpenPgpConstants.RESULT_SIGNATURE, sigResult);
            return result;
        } catch (Exception e) {
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Bundle getKeyIdsImpl(Bundle params) {
        // get key ids based on given user ids
        String[] userIds = params.getStringArray(OpenPgpConstants.PARAMS_USER_IDS);
        Bundle result = getKeyIdsFromEmails(params, userIds);
        return result;
    }

    /**
     * Checks that params != null and API version fits
     *
     * @param params
     * @return
     */

    private Bundle validateParamsAndVersion(Bundle params) {
        if (params == null) {
            Bundle result = new Bundle();
            OpenPgpError error = new OpenPgpError(OpenPgpError.GENERIC_ERROR, "params Bundle required!");
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS, error);
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            return result;
        }

        if (params.getInt(OpenPgpConstants.PARAMS_API_VERSION) != OpenPgpConstants.API_VERSION) {
            // not compatible!
            Bundle result = new Bundle();
            OpenPgpError error = new OpenPgpError(OpenPgpError.INCOMPATIBLE_API_VERSIONS, "Incompatible API versions!");
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS, error);
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            return result;
        }

        return null;
    }

    // TODO: enqueue in thread pool!!!
    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {

        @Override
        public Bundle sign(Bundle params, final ParcelFileDescriptor input, final ParcelFileDescriptor output) {
            final AppSettings appSettings = getAppSettings();

            Bundle errorResult = validateParamsAndVersion(params);
            if (errorResult != null) {
                return errorResult;
            }

            return signImpl(params, input, output, appSettings);
        }

        @Override
        public Bundle encrypt(Bundle params, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            final AppSettings appSettings = getAppSettings();

            Bundle errorResult = validateParamsAndVersion(params);
            if (errorResult != null) {
                return errorResult;
            }

            return encryptAndSignImpl(params, input, output, appSettings, false);
        }

        @Override
        public Bundle signAndEncrypt(Bundle params, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            final AppSettings appSettings = getAppSettings();

            Bundle errorResult = validateParamsAndVersion(params);
            if (errorResult != null) {
                return errorResult;
            }

            return encryptAndSignImpl(params, input, output, appSettings, true);
        }

        @Override
        public Bundle decryptAndVerify(Bundle params, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            final AppSettings appSettings = getAppSettings();

            Bundle errorResult = validateParamsAndVersion(params);
            if (errorResult != null) {
                return errorResult;
            }

            return decryptAndVerifyImpl(params, input, output, appSettings);
        }

        @Override
        public Bundle getKeyIds(Bundle params) {
            Bundle errorResult = validateParamsAndVersion(params);
            if (errorResult != null) {
                return errorResult;
            }

            return getKeyIdsImpl(params);
        }

        // TODO: old example for checkAndEnqueue!
//        @Override
//        public void getKeyIds(final String[] userIds, final boolean allowUserInteraction,
//                final IOpenPgpKeyIdsCallback callback) throws RemoteException {
//
//            final AppSettings settings = getAppSettings();
//
//            Runnable r = new Runnable() {
//                @Override
//                public void run() {
//                    getKeyIdsSafe(userIds, allowUserInteraction, callback, settings);
//                }
//            };
//
//            checkAndEnqueue(r);
//        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
