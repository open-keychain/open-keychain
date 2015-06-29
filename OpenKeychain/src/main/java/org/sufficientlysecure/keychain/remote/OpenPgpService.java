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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.pgp.PgpConstants;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAccounts;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.ui.RemoteServiceActivity;
import org.sufficientlysecure.keychain.remote.ui.SelectAllowedKeysActivity;
import org.sufficientlysecure.keychain.remote.ui.SelectSignKeyIdActivity;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.ImportKeysActivity;
import org.sufficientlysecure.keychain.ui.NfcOperationActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;

public class OpenPgpService extends RemoteService {

    static final String[] EMAIL_SEARCH_PROJECTION = new String[]{
            KeyRings._ID,
            KeyRings.MASTER_KEY_ID,
            KeyRings.IS_EXPIRED,
            KeyRings.IS_REVOKED,
    };

    // do not pre-select revoked or expired keys
    static final String EMAIL_SEARCH_WHERE = Tables.KEYS + "." + KeychainContract.KeyRings.IS_REVOKED
            + " = 0 AND " + KeychainContract.KeyRings.IS_EXPIRED + " = 0";

    /**
     * Search database for key ids based on emails.
     */
    private Intent returnKeyIdsFromEmails(Intent data, String[] encryptionUserIds) {
        boolean noUserIdsCheck = (encryptionUserIds == null || encryptionUserIds.length == 0);
        boolean missingUserIdsCheck = false;
        boolean duplicateUserIdsCheck = false;

        ArrayList<Long> keyIds = new ArrayList<>();
        ArrayList<String> missingUserIds = new ArrayList<>();
        ArrayList<String> duplicateUserIds = new ArrayList<>();
        if (!noUserIdsCheck) {
            for (String email : encryptionUserIds) {
                // try to find the key for this specific email
                Uri uri = KeyRings.buildUnifiedKeyRingsFindByEmailUri(email);
                Cursor cursor = getContentResolver().query(uri, EMAIL_SEARCH_PROJECTION, EMAIL_SEARCH_WHERE, null, null);
                try {
                    // result should be one entry containing the key id
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndex(KeyRings.MASTER_KEY_ID));
                        keyIds.add(id);
                    } else {
                        missingUserIdsCheck = true;
                        missingUserIds.add(email);
                        Log.d(Constants.TAG, "user id missing");
                    }
                    // another entry for this email -> too keys with the same email inside user id
                    if (cursor != null && cursor.moveToNext()) {
                        duplicateUserIdsCheck = true;
                        duplicateUserIds.add(email);

                        // also pre-select
                        long id = cursor.getLong(cursor.getColumnIndex(KeyRings.MASTER_KEY_ID));
                        keyIds.add(id);
                        Log.d(Constants.TAG, "more than one user id with the same email");
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        // convert ArrayList<Long> to long[]
        long[] keyIdsArray = new long[keyIds.size()];
        for (int i = 0; i < keyIdsArray.length; i++) {
            keyIdsArray[i] = keyIds.get(i);
        }

        if (noUserIdsCheck || missingUserIdsCheck || duplicateUserIdsCheck) {
            // allow the user to verify pub key selection

            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.setAction(RemoteServiceActivity.ACTION_SELECT_PUB_KEYS);
            intent.putExtra(RemoteServiceActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
            intent.putExtra(RemoteServiceActivity.EXTRA_NO_USER_IDS_CHECK, noUserIdsCheck);
            intent.putExtra(RemoteServiceActivity.EXTRA_MISSING_USER_IDS, missingUserIds);
            intent.putExtra(RemoteServiceActivity.EXTRA_DUPLICATE_USER_IDS, duplicateUserIds);
            intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            return result;
        } else {
            // everything was easy, we have exactly one key for every email

            if (keyIdsArray.length == 0) {
                Log.e(Constants.TAG, "keyIdsArray.length == 0, should never happen!");
            }

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_KEY_IDS, keyIdsArray);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        }
    }

    private static PendingIntent getRequiredInputPendingIntent(Context context,
                                                               Intent data, RequiredInputParcel requiredInput) {

        switch (requiredInput.mType) {
            case NFC_MOVE_KEY_TO_CARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                // build PendingIntent for YubiKey NFC operations
                Intent intent = new Intent(context, NfcOperationActivity.class);
                // pass params through to activity that it can be returned again later to repeat pgp operation
                intent.putExtra(NfcOperationActivity.EXTRA_SERVICE_INTENT, data);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                return PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
            }

            case PASSPHRASE: {
                // build PendingIntent for Passphrase request
                Intent intent = new Intent(context, PassphraseDialogActivity.class);
                // pass params through to activity that it can be returned again later to repeat pgp operation
                intent.putExtra(PassphraseDialogActivity.EXTRA_SERVICE_INTENT, data);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                return PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
            }

            default:
                throw new AssertionError("Unhandled required input type!");
        }

    }

    private PendingIntent getKeyserverPendingIntent(Intent data, long masterKeyId) {
        // If signature is unknown we return an _additional_ PendingIntent
        // to retrieve the missing key
        Intent intent = new Intent(getBaseContext(), ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_TO_SERVICE);
        intent.putExtra(ImportKeysActivity.EXTRA_KEY_ID, masterKeyId);
        intent.putExtra(ImportKeysActivity.EXTRA_PENDING_INTENT_DATA, data);

        return PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getSelectAllowedKeysIntent(Intent data) {
        // If signature is unknown we return an _additional_ PendingIntent
        // to retrieve the missing key
        Intent intent = new Intent(getBaseContext(), SelectAllowedKeysActivity.class);
        intent.putExtra(SelectAllowedKeysActivity.EXTRA_SERVICE_INTENT, data);
        intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(getCurrentCallingPackage()));

        return PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getShowKeyPendingIntent(long masterKeyId) {
        Intent intent = new Intent(getBaseContext(), ViewKeyActivity.class);
        intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));

        return PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Intent signImpl(Intent data, ParcelFileDescriptor input,
                            ParcelFileDescriptor output, boolean cleartextSign) {
        InputStream is = null;
        OutputStream os = null;
        try {
            boolean asciiArmor = cleartextSign || data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            // sign-only
            PgpSignEncryptInputParcel pseInput = new PgpSignEncryptInputParcel()
                    .setEnableAsciiArmorOutput(asciiArmor)
                    .setCleartextSignature(cleartextSign)
                    .setDetachedSignature(!cleartextSign)
                    .setVersionHeader(null)
                    .setSignatureHashAlgorithm(PgpConstants.OpenKeychainHashAlgorithmTags.USE_PREFERRED);

            Intent signKeyIdIntent = getSignKeyMasterId(data);
            // NOTE: Fallback to return account settings (Old API)
            if (signKeyIdIntent.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)
                    == OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED) {
                return signKeyIdIntent;
            }

            long signKeyId = signKeyIdIntent.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);
            if (signKeyId == Constants.key.none) {
                throw new Exception("No signing key given");
            } else {
                pseInput.setSignatureMasterKeyId(signKeyId);

                // get first usable subkey capable of signing
                try {
                    long signSubKeyId = mProviderHelper.getCachedPublicKeyRing(
                            pseInput.getSignatureMasterKeyId()).getSecretSignId();
                    pseInput.setSignatureSubKeyId(signSubKeyId);
                } catch (PgpKeyNotFoundException e) {
                    throw new Exception("signing subkey not found!", e);
                }
            }

            // Get Input- and OutputStream from ParcelFileDescriptor
            is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            if (cleartextSign) {
                // output stream only needed for cleartext signatures,
                // detached signatures are returned as extra
                os = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            }
            long inputLength = is.available();
            InputData inputData = new InputData(is, inputLength);

            CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (inputParcel == null) {
                inputParcel = new CryptoInputParcel();
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                inputParcel = new CryptoInputParcel(inputParcel.getSignatureTime(),
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)));
            }

            // execute PGP operation!
            PgpSignEncryptOperation pse = new PgpSignEncryptOperation(this, new ProviderHelper(getContext()), null);
            PgpSignEncryptResult pgpResult = pse.execute(pseInput, inputParcel, inputData, os);

            if (pgpResult.isPending()) {

                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = getRequiredInputPendingIntent(getBaseContext(), data, requiredInput);

                // return PendingIntent to be executed by client
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, pIntent);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;

            } else if (pgpResult.success()) {
                Intent result = new Intent();
                if (pgpResult.getDetachedSignature() != null && !cleartextSign) {
                    result.putExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE, pgpResult.getDetachedSignature());
                }
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                LogEntryParcel errorMsg = pgpResult.getLog().getLast();
                throw new Exception(getString(errorMsg.mType.getMsgId()));
            }
        } catch (Exception e) {
            Log.d(Constants.TAG, "signImpl", e);
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing InputStream", e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing OutputStream", e);
                }
            }
        }
    }

    private Intent encryptAndSignImpl(Intent data, ParcelFileDescriptor input,
                                      ParcelFileDescriptor output, boolean sign) {
        InputStream is = null;
        OutputStream os = null;
        try {
            boolean asciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
            String originalFilename = data.getStringExtra(OpenPgpApi.EXTRA_ORIGINAL_FILENAME);
            if (originalFilename == null) {
                originalFilename = "";
            }

            boolean enableCompression = data.getBooleanExtra(OpenPgpApi.EXTRA_ENABLE_COMPRESSION, true);
            int compressionId;
            if (enableCompression) {
                compressionId = CompressionAlgorithmTags.ZLIB;
            } else {
                compressionId = CompressionAlgorithmTags.UNCOMPRESSED;
            }

            // first try to get key ids from non-ambiguous key id extra
            long[] keyIds = data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);
            if (keyIds == null) {
                // get key ids based on given user ids
                String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
                // give params through to activity...
                Intent result = returnKeyIdsFromEmails(data, userIds);

                if (result.getIntExtra(OpenPgpApi.RESULT_CODE, 0) == OpenPgpApi.RESULT_CODE_SUCCESS) {
                    keyIds = result.getLongArrayExtra(OpenPgpApi.RESULT_KEY_IDS);
                } else {
                    // if not success -> result contains a PendingIntent for user interaction
                    return result;
                }
            }

            // build InputData and write into OutputStream
            // Get Input- and OutputStream from ParcelFileDescriptor
            is = new ParcelFileDescriptor.AutoCloseInputStream(input);
            os = new ParcelFileDescriptor.AutoCloseOutputStream(output);

            long inputLength = is.available();
            InputData inputData = new InputData(is, inputLength, originalFilename);

            PgpSignEncryptInputParcel pseInput = new PgpSignEncryptInputParcel();
            pseInput.setEnableAsciiArmorOutput(asciiArmor)
                    .setVersionHeader(null)
                    .setCompressionId(compressionId)
                    .setSymmetricEncryptionAlgorithm(PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED)
                    .setEncryptionMasterKeyIds(keyIds)
                    .setFailOnMissingEncryptionKeyIds(true);

            if (sign) {

                Intent signKeyIdIntent = getSignKeyMasterId(data);
                // NOTE: Fallback to return account settings (Old API)
                if (signKeyIdIntent.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)
                        == OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED) {
                    return signKeyIdIntent;
                }
                long signKeyId = signKeyIdIntent.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);
                if (signKeyId == Constants.key.none) {
                    throw new Exception("No signing key given");
                } else {
                    pseInput.setSignatureMasterKeyId(signKeyId);

                    // get first usable subkey capable of signing
                    try {
                        long signSubKeyId = mProviderHelper.getCachedPublicKeyRing(
                                pseInput.getSignatureMasterKeyId()).getSecretSignId();
                        pseInput.setSignatureSubKeyId(signSubKeyId);
                    } catch (PgpKeyNotFoundException e) {
                        throw new Exception("signing subkey not found!", e);
                    }
                }

                // sign and encrypt
                pseInput.setSignatureHashAlgorithm(PgpConstants.OpenKeychainHashAlgorithmTags.USE_PREFERRED)
                        .setAdditionalEncryptId(signKeyId); // add sign key for encryption
            }

            // OLD: Even if the message is not signed: Do self-encrypt to account key id
            if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) < 7) {
                String accName = data.getStringExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME);
                // if no account name is given use name "default"
                if (TextUtils.isEmpty(accName)) {
                    accName = "default";
                }
                final AccountSettings accSettings = getAccSettings(accName);
                if (accSettings == null || (accSettings.getKeyId() == Constants.key.none)) {
                    return getCreateAccountIntent(data, accName);
                }
                pseInput.setAdditionalEncryptId(accSettings.getKeyId());
            }

            CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (inputParcel == null) {
                inputParcel = new CryptoInputParcel();
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                inputParcel = new CryptoInputParcel(inputParcel.getSignatureTime(),
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)));
            }

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(this, new ProviderHelper(getContext()), null);

            // execute PGP operation!
            PgpSignEncryptResult pgpResult = op.execute(pseInput, inputParcel, inputData, os);

            if (pgpResult.isPending()) {
                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = getRequiredInputPendingIntent(getBaseContext(), data, requiredInput);

                // return PendingIntent to be executed by client
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, pIntent);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;
            } else if (pgpResult.success()) {
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                LogEntryParcel errorMsg = pgpResult.getLog().getLast();
                throw new Exception(getString(errorMsg.mType.getMsgId()));
            }
        } catch (Exception e) {
            Log.d(Constants.TAG, "encryptAndSignImpl", e);
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing InputStream", e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing OutputStream", e);
                }
            }
        }
    }

    private Intent decryptAndVerifyImpl(Intent data, ParcelFileDescriptor inputDescriptor,
                                        ParcelFileDescriptor output, boolean decryptMetadataOnly) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Get Input- and OutputStream from ParcelFileDescriptor
            inputStream = new ParcelFileDescriptor.AutoCloseInputStream(inputDescriptor);

            // output is optional, e.g., for verifying detached signatures
            if (decryptMetadataOnly || output == null) {
                outputStream = null;
            } else {
                outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            }

            String currentPkg = getCurrentCallingPackage();
            HashSet<Long> allowedKeyIds = mProviderHelper.getAllowedKeyIdsForApp(
                    KeychainContract.ApiAllowedKeys.buildBaseUri(currentPkg));

            if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) < 7) {
                allowedKeyIds.addAll(mProviderHelper.getAllKeyIdsForApp(
                        ApiAccounts.buildBaseUri(currentPkg)));
            }

            CryptoInputParcel cryptoInput = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (cryptoInput == null) {
                cryptoInput = new CryptoInputParcel();
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                cryptoInput = new CryptoInputParcel(cryptoInput.getSignatureTime(),
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)));
            }

            byte[] detachedSignature = data.getByteArrayExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE);

            PgpDecryptVerify op = new PgpDecryptVerify(this, mProviderHelper, null);

            long inputLength = inputStream.available();
            InputData inputData = new InputData(inputStream, inputLength);

            // allow only private keys associated with accounts of this app
            // no support for symmetric encryption
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel()
                .setAllowSymmetricDecryption(false)
                .setAllowedKeyIds(allowedKeyIds)
                .setDecryptMetadataOnly(decryptMetadataOnly)
                .setDetachedSignature(detachedSignature);

            DecryptVerifyResult pgpResult = op.execute(input, cryptoInput, inputData, outputStream);

            if (pgpResult.isPending()) {
                // prepare and return PendingIntent to be executed by client
                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = getRequiredInputPendingIntent(getBaseContext(), data, requiredInput);

                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, pIntent);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;

            } else if (pgpResult.success()) {
                Intent result = new Intent();

                OpenPgpSignatureResult signatureResult = pgpResult.getSignatureResult();
                // TODO: currently RESULT_TYPE_UNENCRYPTED_UNSIGNED is never returned
                // instead an error is returned when no pgp data has been found
                int resultType = OpenPgpApi.RESULT_TYPE_UNENCRYPTED_UNSIGNED;
                if (signatureResult != null) {
                    resultType |= OpenPgpApi.RESULT_TYPE_SIGNED;
                    if (!signatureResult.isSignatureOnly()) {
                        resultType |= OpenPgpApi.RESULT_TYPE_ENCRYPTED;
                    }

                    result.putExtra(OpenPgpApi.RESULT_SIGNATURE, signatureResult);

                    if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) < 5) {
                        // SIGNATURE_KEY_REVOKED and SIGNATURE_KEY_EXPIRED have been added in version 5
                        if (signatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED
                                || signatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED) {
                            signatureResult.setStatus(OpenPgpSignatureResult.SIGNATURE_ERROR);
                        }
                    }

                    if (signatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_KEY_MISSING) {
                        // If signature is unknown we return an _additional_ PendingIntent
                        // to retrieve the missing key
                        result.putExtra(OpenPgpApi.RESULT_INTENT, getKeyserverPendingIntent(data, signatureResult.getKeyId()));
                    } else {
                        // If signature key is known, return PendingIntent to show key
                        result.putExtra(OpenPgpApi.RESULT_INTENT, getShowKeyPendingIntent(signatureResult.getKeyId()));
                    }
                } else {
                    resultType |= OpenPgpApi.RESULT_TYPE_ENCRYPTED;
                }
                result.putExtra(OpenPgpApi.RESULT_TYPE, resultType);

                if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) >= 4) {
                    OpenPgpMetadata metadata = pgpResult.getDecryptMetadata();
                    if (metadata != null) {
                        result.putExtra(OpenPgpApi.RESULT_METADATA, metadata);
                    }
                }

                String charset = pgpResult.getCharset();
                if (charset != null) {
                    result.putExtra(OpenPgpApi.RESULT_CHARSET, charset);
                }

                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                //
                if (pgpResult.isKeysDisallowed()) {
                    // allow user to select allowed keys
                    Intent result = new Intent();
                    result.putExtra(OpenPgpApi.RESULT_INTENT, getSelectAllowedKeysIntent(data));
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                    return result;
                }

                String errorMsg = getString(pgpResult.getLog().getLast().mType.getMsgId());
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_ERROR, new OpenPgpError(OpenPgpError.GENERIC_ERROR, errorMsg));
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                return result;
            }

        } catch (IOException e) {
            Log.e(Constants.TAG, "decryptAndVerifyImpl", e);
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR, new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing InputStream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException when closing OutputStream", e);
                }
            }
        }
    }

    private Intent getKeyImpl(Intent data) {
        try {
            long masterKeyId = data.getLongExtra(OpenPgpApi.EXTRA_KEY_ID, 0);

            try {
                // try to find key, throws NotFoundException if not in db!
                mProviderHelper.getCanonicalizedPublicKeyRing(masterKeyId);

                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                // also return PendingIntent that opens the key view activity
                result.putExtra(OpenPgpApi.RESULT_INTENT, getShowKeyPendingIntent(masterKeyId));

                return result;
            } catch (ProviderHelper.NotFoundException e) {
                // If keys are not in db we return an additional PendingIntent
                // to retrieve the missing key
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, getKeyserverPendingIntent(data, masterKeyId));
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;
            }
        } catch (Exception e) {
            Log.d(Constants.TAG, "getKeyImpl", e);
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }
    }

    private Intent getSignKeyIdImpl(Intent data) {
        String preferredUserId = data.getStringExtra(OpenPgpApi.EXTRA_USER_ID);

        Intent intent = new Intent(getBaseContext(), SelectSignKeyIdActivity.class);
        String currentPkg = getCurrentCallingPackage();
        intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(currentPkg));
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_USER_ID, preferredUserId);
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_DATA, data);

        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // return PendingIntent to be executed by client
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

        return result;
    }

    private Intent getKeyIdsImpl(Intent data) {
        // if data already contains key ids extra GET_KEY_IDS has been executed again
        // after user interaction. Then, we just need to return the array again!
        if (data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
            long[] keyIdsArray = data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_KEY_IDS, keyIdsArray);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } else {
            // get key ids based on given user ids
            String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
            return returnKeyIdsFromEmails(data, userIds);
        }
    }

    private Intent getSignKeyMasterId(Intent data) {
        // NOTE: Accounts are deprecated on API version >= 7
        if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) < 7) {
            String accName = data.getStringExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME);
            // if no account name is given use name "default"
            if (TextUtils.isEmpty(accName)) {
                accName = "default";
            }
            Log.d(Constants.TAG, "accName: " + accName);
            // fallback to old API
            final AccountSettings accSettings = getAccSettings(accName);
            if (accSettings == null || (accSettings.getKeyId() == Constants.key.none)) {
                return getCreateAccountIntent(data, accName);
            }

            // NOTE: just wrapping the key id
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, accSettings.getKeyId());
            return result;
        } else {
            long signKeyId = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);
            if (signKeyId == Constants.key.none) {
                return getSignKeyIdImpl(data);
            }

            return data;
        }
    }

    /**
     * Check requirements:
     * - params != null
     * - has supported API version
     * - is allowed to call the service (access has been granted)
     *
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
        // History of versions in org.openintents.openpgp.util.OpenPgpApi
        // we support 3, 4, 5, 6
        if (data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != 3
                && data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != 4
                && data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != 5
                && data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != 6
                && data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) != 7) {
            Intent result = new Intent();
            OpenPgpError error = new OpenPgpError
                    (OpenPgpError.INCOMPATIBLE_API_VERSIONS, "Incompatible API versions!\n"
                            + "used API version: " + data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) + "\n"
                            + "supported API versions: 3-7");
            result.putExtra(OpenPgpApi.RESULT_ERROR, error);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }

        // check if caller is allowed to access OpenKeychain
        Intent result = isAllowed(data);
        if (result != null) {
            return result;
        }

        return null;
    }

    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {
        @Override
        public Intent execute(Intent data, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            try {
                Intent errorResult = checkRequirements(data);
                if (errorResult != null) {
                    return errorResult;
                }

                String action = data.getAction();
                switch (action) {
                    case OpenPgpApi.ACTION_CLEARTEXT_SIGN: {
                        return signImpl(data, input, output, true);
                    }
                    case OpenPgpApi.ACTION_SIGN: {
                        // DEPRECATED: same as ACTION_CLEARTEXT_SIGN
                        Log.w(Constants.TAG, "You are using a deprecated API call, please use ACTION_CLEARTEXT_SIGN instead of ACTION_SIGN!");
                        return signImpl(data, input, output, true);
                    }
                    case OpenPgpApi.ACTION_DETACHED_SIGN: {
                        return signImpl(data, input, output, false);
                    }
                    case OpenPgpApi.ACTION_ENCRYPT: {
                        return encryptAndSignImpl(data, input, output, false);
                    }
                    case OpenPgpApi.ACTION_SIGN_AND_ENCRYPT: {
                        return encryptAndSignImpl(data, input, output, true);
                    }
                    case OpenPgpApi.ACTION_DECRYPT_VERIFY: {
                        return decryptAndVerifyImpl(data, input, output, false);
                    }
                    case OpenPgpApi.ACTION_DECRYPT_METADATA: {
                        return decryptAndVerifyImpl(data, input, output, true);
                    }
                    case OpenPgpApi.ACTION_GET_SIGN_KEY_ID: {
                        return getSignKeyIdImpl(data);
                    }
                    case OpenPgpApi.ACTION_GET_KEY_IDS: {
                        return getKeyIdsImpl(data);
                    }
                    case OpenPgpApi.ACTION_GET_KEY: {
                        return getKeyImpl(data);
                    }
                    default: {
                        return null;
                    }
                }
            } finally {
                // always close input and output file descriptors even in error cases
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "IOException when closing input ParcelFileDescriptor", e);
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "IOException when closing output ParcelFileDescriptor", e);
                    }
                }
            }
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
