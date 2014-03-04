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
import org.openintents.openpgp.util.OpenPgpApi;
import org.spongycastle.util.Arrays;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt;
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
    private static final int PRIVATE_REQUEST_CODE_GET_KEYS = 553;

    /**
     * Search database for key ids based on emails.
     *
     * @param encryptionUserIds
     * @return
     */
    private Intent getKeyIdsFromEmails(Intent data, String[] encryptionUserIds) {
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
            // build PendingIntent
            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.setAction(RemoteServiceActivity.ACTION_SELECT_PUB_KEYS);
            intent.putExtra(RemoteServiceActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
            intent.putExtra(RemoteServiceActivity.EXTRA_MISSING_USER_IDS, missingUserIds);
            intent.putExtra(RemoteServiceActivity.EXTRA_DUBLICATE_USER_IDS, dublicateUserIds);
            intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_USER_IDS, intent, 0);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

            return result;
        }

        if (keyIdsArray.length == 0) {
            return null;
        }

        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        result.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIdsArray);
        return result;
    }

    private Intent getPassphraseBundleIntent(Intent data, long keyId) {
        // build PendingIntent for passphrase input
        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
        intent.setAction(RemoteServiceActivity.ACTION_CACHE_PASSPHRASE);
        intent.putExtra(RemoteServiceActivity.EXTRA_SECRET_KEY_ID, keyId);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);
        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_PASSPHRASE, intent, 0);

        // return PendingIntent to be executed by client
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

        return result;
    }

    private Intent signImpl(Intent data, ParcelFileDescriptor input,
                            ParcelFileDescriptor output, AppSettings appSettings) {
        try {
            boolean asciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            // get passphrase from cache, if key has "no" passphrase, this returns an empty String
            String passphrase;
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
            } else {
                passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), appSettings.getKeyId());
            }
            if (passphrase == null) {
                // get PendingIntent for passphrase input, add it to given params and return to client
                Intent passphraseBundle = getPassphraseBundleIntent(data, appSettings.getKeyId());
                return passphraseBundle;
            }

            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            try {
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                // sign-only
                PgpSignEncrypt.Builder builder = new PgpSignEncrypt.Builder(getContext(), inputData, os);
                builder.enableAsciiArmorOutput(asciiArmor)
                        .signatureHashAlgorithm(appSettings.getHashAlgorithm())
                        .signatureForceV3(false)
                        .signatureKeyId(appSettings.getKeyId())
                        .signaturePassphrase(passphrase);
                builder.build().execute();
            } finally {
                is.close();
                os.close();
            }

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            result.putExtra(OpenPgpApi.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Intent encryptAndSignImpl(Intent data, ParcelFileDescriptor input,
                                      ParcelFileDescriptor output, AppSettings appSettings, boolean sign) {
        try {
            boolean asciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            long[] keyIds;
            if (data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
                keyIds = data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);
            } else if (data.hasExtra(OpenPgpApi.EXTRA_USER_IDS)) {
                // get key ids based on given user ids
                String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
                // give params through to activity...
                Intent result = getKeyIdsFromEmails(data, userIds);

                if (result.getIntExtra(OpenPgpApi.RESULT_CODE, 0) == OpenPgpApi.RESULT_CODE_SUCCESS) {
                    keyIds = result.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);
                } else {
                    // if not success -> result contains a PendingIntent for user interaction
                    return result;
                }
            } else {
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                result.putExtra(OpenPgpApi.RESULT_ERRORS,
                        new OpenPgpError(OpenPgpError.GENERIC_ERROR, "Missing parameter user_ids or key_ids!"));
                return result;
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

                PgpSignEncrypt.Builder builder = new PgpSignEncrypt.Builder(getContext(), inputData, os);
                builder.enableAsciiArmorOutput(asciiArmor)
                        .compressionId(appSettings.getCompression())
                        .symmetricEncryptionAlgorithm(appSettings.getEncryptionAlgorithm())
                        .encryptionKeyIds(keyIds);

                if (sign) {
                    String passphrase;
                    if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                        passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
                    } else {
                        passphrase = PassphraseCacheService.getCachedPassphrase(getContext(),
                                appSettings.getKeyId());
                    }
                    if (passphrase == null) {
                        // get PendingIntent for passphrase input, add it to given params and return to client
                        Intent passphraseBundle = getPassphraseBundleIntent(data, appSettings.getKeyId());
                        return passphraseBundle;
                    }

                    // sign and encrypt
                    builder.signatureHashAlgorithm(appSettings.getHashAlgorithm())
                            .signatureForceV3(false)
                            .signatureKeyId(appSettings.getKeyId())
                            .signaturePassphrase(passphrase);
                } else {
                    // encrypt only
                    builder.signatureKeyId(Id.key.none);
                }
                // execute PGP operation!
                builder.build().execute();
            } finally {
                is.close();
                os.close();
            }

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            result.putExtra(OpenPgpApi.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Intent decryptAndVerifyImpl(Intent data, ParcelFileDescriptor input,
                                        ParcelFileDescriptor output, AppSettings appSettings) {
        try {
            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);

            Intent result = new Intent();
            try {

                // TODO:
                // fix the mess: http://stackoverflow.com/questions/148130/how-do-i-peek-at-the-first-two-bytes-in-an-inputstream
                // should we allow to decrypt everything under every key id or only the one set?
                // TODO: instead of trying to get the passphrase before
                // pause stream when passphrase is missing and then resume

                // TODO: put this code into PgpDecryptVerify class

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
//                        if (!PgpDecryptVerify.hasSymmetricEncryption(this, inputStream2)) {
//                            throw new PgpGeneralException(
//                                    getString(R.string.error_no_known_encryption_found));
//                        }
//                        // we do not support symmetric decryption from the API!
//                        throw new Exception("Symmetric decryption is not supported!");
//                    }
//
//                    Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

                // NOTE: currently this only gets the passphrase for the key set for this client
                String passphrase;
                if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                    passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
                } else {
                    passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), appSettings.getKeyId());
                }
                if (passphrase == null) {
                    // get PendingIntent for passphrase input, add it to given params and return to client
                    Intent passphraseBundle = getPassphraseBundleIntent(data, appSettings.getKeyId());
                    return passphraseBundle;
                }

                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                Bundle outputBundle;
                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(this, inputData, os);

                builder.assumeSymmetric(false)
                        .passphrase(passphrase);

                // TODO: this also decrypts with other secret keys that have no passphrase!!!
                outputBundle = builder.build().execute();

                //TODO: instead of using all these wrapping use OpenPgpSignatureResult directly
                // in DecryptVerify class and then in DecryptActivity
                boolean signature = outputBundle.getBoolean(KeychainIntentService.RESULT_SIGNATURE, false);
                if (signature) {
                    long signatureKeyId = outputBundle
                            .getLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID, 0);
                    String signatureUserId = outputBundle
                            .getString(KeychainIntentService.RESULT_SIGNATURE_USER_ID);
                    boolean signatureSuccess = outputBundle
                            .getBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS, false);
                    boolean signatureUnknown = outputBundle
                            .getBoolean(KeychainIntentService.RESULT_SIGNATURE_UNKNOWN, false);
                    boolean signatureOnly = outputBundle
                            .getBoolean(KeychainIntentService.RESULT_CLEARTEXT_SIGNATURE_ONLY, false);

                    // TODO: SIGNATURE_SUCCESS_CERTIFIED is currently not implemented
                    int signatureStatus = OpenPgpSignatureResult.SIGNATURE_ERROR;
                    if (signatureSuccess) {
                        signatureStatus = OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED;
                    } else if (signatureUnknown) {
                        signatureStatus = OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY;

                        // If signature is unknown we return an additional PendingIntent
                        // to retrieve the missing key
                        // TODO!!!
                        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
                        intent.setAction(RemoteServiceActivity.ACTION_ERROR_MESSAGE);
                        intent.putExtra(RemoteServiceActivity.EXTRA_ERROR_MESSAGE, "todo");
                        intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

                        PendingIntent pi = PendingIntent.getActivity(getBaseContext(),
                                PRIVATE_REQUEST_CODE_GET_KEYS, intent, 0);

                        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
                    }


                    OpenPgpSignatureResult sigResult = new OpenPgpSignatureResult(signatureStatus,
                            signatureUserId, signatureOnly, signatureKeyId);
                    result.putExtra(OpenPgpApi.RESULT_SIGNATURE, sigResult);
                }
            } finally {
                is.close();
                os.close();
            }

            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            result.putExtra(OpenPgpApi.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            return result;
        }
    }

    private Intent getKeyIdsImpl(Intent data) {
        // get key ids based on given user ids
        String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
        Intent result = getKeyIdsFromEmails(data, userIds);
        return result;
    }

    /**
     * Check requirements:
     * - params != null
     * - has supported API version
     * - is allowed to call the service (access has been granted)
     *
     * @param data
     * @return null if everything is okay, or a Bundle with an error/PendingIntent
     */
    private Intent checkRequirements(Intent data) {
        // params Bundle is required!
        if (data == null) {
            Intent result = new Intent();
            OpenPgpError error = new OpenPgpError(OpenPgpError.GENERIC_ERROR, "params Bundle required!");
            result.putExtra(OpenPgpApi.RESULT_ERRORS, error);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }

        // version code is required and needs to correspond to version code of service!
        if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != OpenPgpApi.API_VERSION) {
            Intent result = new Intent();
            OpenPgpError error = new OpenPgpError(OpenPgpError.INCOMPATIBLE_API_VERSIONS, "Incompatible API versions!");
            result.putExtra(OpenPgpApi.RESULT_ERRORS, error);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }

        // check if caller is allowed to access openpgp keychain
        Intent result = isAllowed(data);
        if (result != null) {
            return result;
        }

        return null;
    }

    // TODO: multi-threading
    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {

        @Override
        public Intent execute(Intent data, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            Intent errorResult = checkRequirements(data);
            if (errorResult != null) {
                return errorResult;
            }

            final AppSettings appSettings = getAppSettings();

            String action = data.getAction();
            if (OpenPgpApi.ACTION_SIGN.equals(action)) {
                return signImpl(data, input, output, appSettings);
            } else if (OpenPgpApi.ACTION_ENCRYPT.equals(action)) {
                return encryptAndSignImpl(data, input, output, appSettings, false);
            } else if (OpenPgpApi.ACTION_SIGN_AND_ENCTYPT.equals(action)) {
                return encryptAndSignImpl(data, input, output, appSettings, true);
            } else if (OpenPgpApi.ACTION_DECRYPT_VERIFY.equals(action)) {
                return decryptAndVerifyImpl(data, input, output, appSettings);
            } else if (OpenPgpApi.ACTION_DOWNLOAD_KEYS.equals(action)) {
                // TODO!
                return null;
            } else if (OpenPgpApi.ACTION_GET_KEY_IDS.equals(action)) {
                return getKeyIdsImpl(data);
            } else {
                return null;
            }
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
