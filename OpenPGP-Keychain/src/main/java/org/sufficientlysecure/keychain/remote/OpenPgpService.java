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

package org.sufficientlysecure.keychain.remote;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.ui.RemoteServiceActivity;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class OpenPgpService extends RemoteService {

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

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            return result;
        }

        if (keyIdsArray.length == 0) {
            return null;
        }

        Intent result = new Intent();
        result.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIdsArray);
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        return result;
    }

    private Intent getPassphraseBundleIntent(Intent data, long keyId) {
        // build PendingIntent for passphrase input
        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
        intent.setAction(RemoteServiceActivity.ACTION_CACHE_PASSPHRASE);
        intent.putExtra(RemoteServiceActivity.EXTRA_SECRET_KEY_ID, keyId);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);
        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // return PendingIntent to be executed by client
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        return result;
    }

    private Intent signImpl(Intent data, ParcelFileDescriptor input,
                            ParcelFileDescriptor output, AccountSettings accSettings) {
        try {
            boolean asciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            // get passphrase from cache, if key has "no" passphrase, this returns an empty String
            String passphrase;
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
            } else {
                passphrase = PassphraseCacheService.getCachedPassphrase(getContext(), accSettings.getKeyId());
            }
            if (passphrase == null) {
                // get PendingIntent for passphrase input, add it to given params and return to client
                Intent passphraseBundle = getPassphraseBundleIntent(data, accSettings.getKeyId());
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
                        .signatureHashAlgorithm(accSettings.getHashAlgorithm())
                        .signatureForceV3(false)
                        .signatureKeyId(accSettings.getKeyId())
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
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }
    }

    private Intent encryptAndSignImpl(Intent data, ParcelFileDescriptor input,
                                      ParcelFileDescriptor output, AccountSettings accSettings, boolean sign) {
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
                result.putExtra(OpenPgpApi.RESULT_ERROR,
                        new OpenPgpError(OpenPgpError.GENERIC_ERROR,
                                "Missing parameter user_ids or key_ids!"));
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                return result;
            }

            // add own key for encryption
            keyIds = Arrays.copyOf(keyIds, keyIds.length + 1);
            keyIds[keyIds.length - 1] = accSettings.getKeyId();

            // build InputData and write into OutputStream
            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            try {
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                PgpSignEncrypt.Builder builder = new PgpSignEncrypt.Builder(getContext(), inputData, os);
                builder.enableAsciiArmorOutput(asciiArmor)
                        .compressionId(accSettings.getCompression())
                        .symmetricEncryptionAlgorithm(accSettings.getEncryptionAlgorithm())
                        .encryptionKeyIds(keyIds);

                if (sign) {
                    String passphrase;
                    if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                        passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
                    } else {
                        passphrase = PassphraseCacheService.getCachedPassphrase(getContext(),
                                accSettings.getKeyId());
                    }
                    if (passphrase == null) {
                        // get PendingIntent for passphrase input, add it to given params and return to client
                        Intent passphraseBundle = getPassphraseBundleIntent(data, accSettings.getKeyId());
                        return passphraseBundle;
                    }

                    // sign and encrypt
                    builder.signatureHashAlgorithm(accSettings.getHashAlgorithm())
                            .signatureForceV3(false)
                            .signatureKeyId(accSettings.getKeyId())
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
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }
    }

    private Intent decryptAndVerifyImpl(Intent data, ParcelFileDescriptor input,
                                        ParcelFileDescriptor output, AccountSettings accSettings) {
        try {
            // Get Input- and OutputStream from ParcelFileDescriptor
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(output);

            Intent result = new Intent();
            try {

                String passphrase = data.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE);
                long inputLength = is.available();
                InputData inputData = new InputData(is, inputLength);

                PgpDecryptVerify.Builder builder = new PgpDecryptVerify.Builder(this, inputData, os);
                builder.assumeSymmetric(false) // no support for symmetric encryption
                        // allow only the private key for this app for decryption
                        .enforcedKeyId(accSettings.getKeyId())
                        .passphrase(passphrase);

                // TODO: currently does not support binary signed-only content
                PgpDecryptVerifyResult decryptVerifyResult = builder.build().execute();

                if (PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                    // get PendingIntent for passphrase input, add it to given params and return to client
                    Intent passphraseBundle = getPassphraseBundleIntent(data, accSettings.getKeyId());
                    return passphraseBundle;
                } else if (PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                    throw new PgpGeneralException("Decryption of symmetric content not supported by API!");
                }

                OpenPgpSignatureResult signatureResult = decryptVerifyResult.getSignatureResult();
                if (signatureResult != null) {
                    if (signatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY) {
                        // If signature is unknown we return an _additional_ PendingIntent
                        // to retrieve the missing key
                        // TODO!!!
                        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
                        intent.setAction(RemoteServiceActivity.ACTION_ERROR_MESSAGE);
                        intent.putExtra(RemoteServiceActivity.EXTRA_ERROR_MESSAGE, "todo");
                        intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

                        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                                intent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
                    }

                    result.putExtra(OpenPgpApi.RESULT_SIGNATURE, signatureResult);
                }

            } finally {
                is.close();
                os.close();
            }

            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }
    }

    private Intent getKeyImpl(Intent data) {
        try {
            long keyId = data.getLongExtra(OpenPgpApi.EXTRA_KEY_ID, 0);

            if (ProviderHelper.getPGPPublicKeyByKeyId(this, keyId) == null) {
                Intent result = new Intent();

                // If keys are not in db we return an additional PendingIntent
                // to retrieve the missing key
                // TODO!!!
                Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
                intent.setAction(RemoteServiceActivity.ACTION_ERROR_MESSAGE);
                intent.putExtra(RemoteServiceActivity.EXTRA_ERROR_MESSAGE, "todo");
                intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

                PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;
            } else {
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            }
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
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
            result.putExtra(OpenPgpApi.RESULT_ERROR, error);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }

        // version code is required and needs to correspond to version code of service!
        if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != OpenPgpApi.API_VERSION) {
            Intent result = new Intent();
            OpenPgpError error = new OpenPgpError
                    (OpenPgpError.INCOMPATIBLE_API_VERSIONS, "Incompatible API versions!");
            result.putExtra(OpenPgpApi.RESULT_ERROR, error);
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

            String accName;
            if (data.getStringExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME) != null) {
                accName = data.getStringExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME);
            } else {
                accName = "default";
            }
            final AccountSettings accSettings = getAccSettings(accName);
            if (accSettings == null) {
                return getCreateAccountIntent(data, accName);
            }

            String action = data.getAction();
            if (OpenPgpApi.ACTION_SIGN.equals(action)) {
                return signImpl(data, input, output, accSettings);
            } else if (OpenPgpApi.ACTION_ENCRYPT.equals(action)) {
                return encryptAndSignImpl(data, input, output, accSettings, false);
            } else if (OpenPgpApi.ACTION_SIGN_AND_ENCRYPT.equals(action)) {
                return encryptAndSignImpl(data, input, output, accSettings, true);
            } else if (OpenPgpApi.ACTION_DECRYPT_VERIFY.equals(action)) {
                return decryptAndVerifyImpl(data, input, output, accSettings);
            } else if (OpenPgpApi.ACTION_GET_KEY.equals(action)) {
                return getKeyImpl(data);
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
