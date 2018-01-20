/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.OpenPgpSignatureResult.AutocryptPeerResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.BackupOperation;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.DecryptVerifySecurityProblem;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptData;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SecurityProblem;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.provider.OverriddenWarningsRepository;
import org.sufficientlysecure.keychain.remote.OpenPgpServiceKeyIdExtractor.KeyIdResult;
import org.sufficientlysecure.keychain.remote.OpenPgpServiceKeyIdExtractor.KeyIdResultStatus;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Passphrase;
import timber.log.Timber;


public class OpenPgpService extends Service {
    public static final int API_VERSION_WITH_KEY_INVALID_INSECURE = 8;
    public static final int API_VERSION_WITHOUT_SIGNATURE_ONLY_FLAG = 8;
    public static final int API_VERSION_WITH_DECRYPTION_RESULT = 8;
    public static final int API_VERSION_WITH_RESULT_NO_SIGNATURE = 8;
    public static final int API_VERSION_WITH_AUTOCRYPT = 12;

    public static final List<Integer> SUPPORTED_VERSIONS =
            Collections.unmodifiableList(Arrays.asList(7, 8, 9, 10, 11, 12));

    private ApiPermissionHelper mApiPermissionHelper;
    private KeyRepository mKeyRepository;
    private ApiDataAccessObject mApiDao;
    private OpenPgpServiceKeyIdExtractor mKeyIdExtractor;
    private ApiPendingIntentFactory mApiPendingIntentFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        mKeyRepository = KeyRepository.create(this);
        mApiDao = new ApiDataAccessObject(this);
        mApiPermissionHelper = new ApiPermissionHelper(this, mApiDao);
        mApiPendingIntentFactory = new ApiPendingIntentFactory(getBaseContext());
        mKeyIdExtractor = OpenPgpServiceKeyIdExtractor.getInstance(getContentResolver(), mApiPendingIntentFactory);
    }

    private Intent signImpl(Intent data, InputStream inputStream,
                            OutputStream outputStream, boolean cleartextSign) {
        try {
            boolean asciiArmor = cleartextSign || data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            // sign-only
            PgpSignEncryptData.Builder pgpData = PgpSignEncryptData.builder();
            pgpData.setEnableAsciiArmorOutput(asciiArmor)
                    .setCleartextSignature(cleartextSign)
                    .setDetachedSignature(!cleartextSign)
                    .setVersionHeader(null);


            Intent signKeyIdIntent = getSignKeyMasterId(data);
            // NOTE: Fallback to return account settings (Old API)
            if (signKeyIdIntent.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
                    == OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED) {
                return signKeyIdIntent;
            }

            long signKeyId = signKeyIdIntent.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);
            if (signKeyId == Constants.key.none) {
                throw new Exception("No signing key given");
            } else {
                pgpData.setSignatureMasterKeyId(signKeyId);

                // get first usable subkey capable of signing
                try {
                    long signSubKeyId = mKeyRepository.getCachedPublicKeyRing(signKeyId).getSecretSignId();
                    pgpData.setSignatureSubKeyId(signSubKeyId);
                } catch (PgpKeyNotFoundException e) {
                    throw new Exception("signing subkey not found!", e);
                }
            }
            pgpData.setAllowedSigningKeyIds(getAllowedKeyIds());

            // Get Input- and OutputStream from ParcelFileDescriptor
            if (!cleartextSign) {
                // output stream only needed for cleartext signatures,
                // detached signatures are returned as extra
                outputStream = null;
            }
            long inputLength = inputStream.available();
            InputData inputData = new InputData(inputStream, inputLength);

            CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (inputParcel == null) {
                inputParcel = CryptoInputParcel.createCryptoInputParcel(new Date());
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                inputParcel = inputParcel.withPassphrase(
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)), null);
            }

            // execute PGP operation!
            PgpSignEncryptOperation pse = new PgpSignEncryptOperation(this, mKeyRepository, null);
            PgpSignEncryptResult pgpResult = pse.execute(pgpData.build(), inputParcel, inputData, outputStream);

            if (pgpResult.isPending()) {
                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = mApiPendingIntentFactory.requiredInputPi(data,
                        requiredInput, pgpResult.mCryptoInputParcel);

                // return PendingIntent to be executed by client
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, pIntent);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;

            } else if (pgpResult.success()) {
                Intent result = new Intent();
                if (pgpResult.getDetachedSignature() != null && !cleartextSign) {
                    result.putExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE, pgpResult.getDetachedSignature());
                    result.putExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG, pgpResult.getMicAlgDigestName());
                }
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                LogEntryParcel errorMsg = pgpResult.getLog().getLast();
                throw new Exception(getString(errorMsg.mType.getMsgId()));
            }
        } catch (Exception e) {
            Timber.d(e, "signImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private Intent encryptAndSignImpl(Intent data, InputStream inputStream,
            OutputStream outputStream, boolean sign, boolean isQueryAutocryptStatus) {
        try {
            PgpSignEncryptData.Builder pgpData = PgpSignEncryptData.builder()
                    .setVersionHeader(null);

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
                }
                long signSubKeyId = mKeyRepository.getCachedPublicKeyRing(signKeyId).getSecretSignId();

                pgpData.setSignatureMasterKeyId(signKeyId)
                        .setSignatureSubKeyId(signSubKeyId)
                        .setAdditionalEncryptId(signKeyId);
            }

            KeyIdResult keyIdResult = mKeyIdExtractor.returnKeyIdsFromIntent(data, false,
                    mApiPermissionHelper.getCurrentCallingPackage());

            KeyIdResultStatus keyIdResultStatus = keyIdResult.getStatus();
            if (isQueryAutocryptStatus) {
                return getAutocryptStatusResult(keyIdResult);
            }

            boolean asciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
            pgpData.setEnableAsciiArmorOutput(asciiArmor);

            boolean enableCompression = data.getBooleanExtra(OpenPgpApi.EXTRA_ENABLE_COMPRESSION, true);
            pgpData.setCompressionAlgorithm(enableCompression ? OpenKeychainCompressionAlgorithmTags.USE_DEFAULT :
                    OpenKeychainCompressionAlgorithmTags.UNCOMPRESSED);

            String originalFilename = data.getStringExtra(OpenPgpApi.EXTRA_ORIGINAL_FILENAME);
            if (originalFilename == null) {
                originalFilename = "";
            }

            if (keyIdResult.hasKeySelectionPendingIntent()) {
                boolean isOpportunistic = data.getBooleanExtra(OpenPgpApi.EXTRA_OPPORTUNISTIC_ENCRYPTION, false);
                if ((keyIdResultStatus == KeyIdResultStatus.MISSING || keyIdResultStatus == KeyIdResultStatus.NO_KEYS ||
                        keyIdResultStatus == KeyIdResultStatus.NO_KEYS_ERROR) && isOpportunistic) {
                    return createErrorResultIntent(OpenPgpError.OPPORTUNISTIC_MISSING_KEYS,
                            "missing keys in opportunistic mode");
                }

                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                result.putExtra(OpenPgpApi.RESULT_INTENT, keyIdResult.getKeySelectionPendingIntent());
                return result;
            }
            pgpData.setEncryptionMasterKeyIds(keyIdResult.getKeyIds());
            pgpData.setAllowedSigningKeyIds(getAllowedKeyIds());

            CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (inputParcel == null) {
                inputParcel = CryptoInputParcel.createCryptoInputParcel(new Date());
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                inputParcel = inputParcel.withPassphrase(
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)), null);
            }

            // TODO this is not correct!
            long inputLength = inputStream.available();
            InputData inputData = new InputData(inputStream, inputLength, originalFilename);

            // execute PGP operation!
            PgpSignEncryptOperation op = new PgpSignEncryptOperation(this, mKeyRepository, null);
            PgpSignEncryptResult pgpResult = op.execute(pgpData.build(), inputParcel, inputData, outputStream);

            if (pgpResult.isPending()) {
                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = mApiPendingIntentFactory.requiredInputPi(data,
                        requiredInput, pgpResult.mCryptoInputParcel);

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
            Timber.d(e, "encryptAndSignImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    @NonNull
    private Intent getAutocryptStatusResult(KeyIdResult keyIdResult) {
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        result.putExtra(OpenPgpApi.RESULT_KEYS_CONFIRMED, keyIdResult.isAllKeysConfirmed());

        int combinedAutocryptState = keyIdResult.getAutocryptRecommendation();
        if (combinedAutocryptState == AutocryptStatus.AUTOCRYPT_PEER_DISABLED) {
            switch (keyIdResult.getStatus()) {
                case NO_KEYS:
                case NO_KEYS_ERROR:
                case MISSING: {
                    result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE);
                    break;
                }
                case DUPLICATE: {
                    if (keyIdResult.hasKeySelectionPendingIntent()) {
                        result.putExtra(OpenPgpApi.RESULT_INTENT, keyIdResult.getKeySelectionPendingIntent());
                    }
                    result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE);
                    break;
                }
                case OK: {
                    result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE);
                    break;
                }
            }
            return result;
        }

        switch (combinedAutocryptState) {
            case AutocryptStatus.AUTOCRYPT_PEER_DISCOURAGED_OLD:
            case AutocryptStatus.AUTOCRYPT_PEER_GOSSIP: {
                result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE);
                break;
            }
            case AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE_EXTERNAL:
            case AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE: {
                result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_AVAILABLE);
                break;
            }
            case AutocryptStatus.AUTOCRYPT_PEER_MUTUAL: {
                result.putExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_MUTUAL);
                break;
            }
            default: {
                throw new IllegalStateException("unhandled case!");
            }
        }

        return result;
    }

    private Intent decryptAndVerifyImpl(Intent data, InputStream inputStream, OutputStream outputStream,
            boolean decryptMetadataOnly, Progressable progressable) {
        try {
            // output is optional, e.g., for verifying detached signatures
            if (decryptMetadataOnly) {
                outputStream = null;
            }

            int targetApiVersion = data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1);

            CryptoInputParcel cryptoInput = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (cryptoInput == null) {
                cryptoInput = CryptoInputParcel.createCryptoInputParcel();
            }
            // override passphrase in input parcel if given by API call
            if (data.hasExtra(OpenPgpApi.EXTRA_PASSPHRASE)) {
                cryptoInput = cryptoInput.withPassphrase(
                        new Passphrase(data.getCharArrayExtra(OpenPgpApi.EXTRA_PASSPHRASE)), null);
            }
            if (data.hasExtra(OpenPgpApi.EXTRA_DECRYPTION_RESULT)) {
                OpenPgpDecryptionResult decryptionResult = data.getParcelableExtra(OpenPgpApi.EXTRA_DECRYPTION_RESULT);
                if (decryptionResult != null && decryptionResult.hasDecryptedSessionKey()) {
                    cryptoInput = cryptoInput.withCryptoData(
                            decryptionResult.getSessionKey(), decryptionResult.getDecryptedSessionKey());
                }
            }

            byte[] detachedSignature = data.getByteArrayExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE);
            String senderAddress = data.getStringExtra(OpenPgpApi.EXTRA_SENDER_ADDRESS);

            updateAutocryptPeerImpl(data);

            PgpDecryptVerifyOperation op = new PgpDecryptVerifyOperation(this, mKeyRepository, progressable);

            long inputLength = data.getLongExtra(OpenPgpApi.EXTRA_DATA_LENGTH, InputData.UNKNOWN_FILESIZE);
            InputData inputData = new InputData(inputStream, inputLength);

            // allow only private keys associated with accounts of this app
            // no support for symmetric encryption
            PgpDecryptVerifyInputParcel input = PgpDecryptVerifyInputParcel.builder()
                    .setAllowSymmetricDecryption(false)
                    .setAllowedKeyIds(new ArrayList<>(getAllowedKeyIds()))
                    .setDecryptMetadataOnly(decryptMetadataOnly)
                    .setDetachedSignature(detachedSignature)
                    .setSenderAddress(senderAddress)
                    .build();

            DecryptVerifyResult pgpResult = op.execute(input, cryptoInput, inputData, outputStream);

            if (pgpResult.isPending()) {
                // prepare and return PendingIntent to be executed by client
                RequiredInputParcel requiredInput = pgpResult.getRequiredInputParcel();
                PendingIntent pIntent = mApiPendingIntentFactory.requiredInputPi(data,
                        requiredInput, pgpResult.mCryptoInputParcel);

                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, pIntent);
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;

            } else if (pgpResult.success()) {
                Intent result = new Intent();

                processDecryptionResultForResultIntent(targetApiVersion, result, pgpResult.getDecryptionResult());
                processMetadataForResultIntent(result, pgpResult.getDecryptionMetadata());
                processSignatureResultForResultIntent(targetApiVersion, data, result, pgpResult);
                processSecurityProblemsPendingIntent(data, result, pgpResult);

                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                long[] skippedDisallowedEncryptionKeys = pgpResult.getSkippedDisallowedKeys();
                if (pgpResult.isKeysDisallowed() &&
                        skippedDisallowedEncryptionKeys != null && skippedDisallowedEncryptionKeys.length > 0) {
                    // allow user to select allowed keys
                    Intent result = new Intent();
                    String packageName = mApiPermissionHelper.getCurrentCallingPackage();
                    result.putExtra(OpenPgpApi.RESULT_INTENT,
                            mApiPendingIntentFactory.createRequestKeyPermissionPendingIntent(
                                    data, packageName, skippedDisallowedEncryptionKeys));
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                    return result;
                }

                String errorMsg = getString(pgpResult.getLog().getLast().mType.getMsgId());
                return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, errorMsg);
            }

        } catch (Exception e) {
            Timber.e(e, "decryptAndVerifyImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private void processSecurityProblemsPendingIntent(Intent data, Intent result,
            DecryptVerifyResult decryptVerifyResult) {
        DecryptVerifySecurityProblem securityProblem = decryptVerifyResult.getSecurityProblem();
        if (securityProblem == null) {
            return;
        }

        boolean supportOverride = data.getBooleanExtra(OpenPgpApi.EXTRA_SUPPORT_OVERRIDE_CRYPTO_WARNING, false);
        if (supportOverride) {
            SecurityProblem prioritySecurityProblem = securityProblem.getPrioritySecurityProblem();
            if (prioritySecurityProblem.isIdentifiable()) {
                String identifier = prioritySecurityProblem.getIdentifier();
                boolean isOverridden = OverriddenWarningsRepository.createOverriddenWarningsRepository(this)
                        .isWarningOverridden(identifier);
                result.putExtra(OpenPgpApi.RESULT_OVERRIDE_CRYPTO_WARNING, isOverridden);
            }
        }

        String packageName = mApiPermissionHelper.getCurrentCallingPackage();
        result.putExtra(OpenPgpApi.RESULT_INSECURE_DETAIL_INTENT,
                mApiPendingIntentFactory.createSecurityProblemIntent(packageName, securityProblem, supportOverride));
    }

    private void processDecryptionResultForResultIntent(int targetApiVersion, Intent result,
            OpenPgpDecryptionResult decryptionResult) {
        if (targetApiVersion < API_VERSION_WITH_DECRYPTION_RESULT) {
            return;
        }

        if (decryptionResult != null) {
            result.putExtra(OpenPgpApi.RESULT_DECRYPTION, decryptionResult);
        }
    }

    private OpenPgpSignatureResult getSignatureResultWithApiCompatibilityFallbacks(
            int targetApiVersion, DecryptVerifyResult pgpResult) {
        OpenPgpSignatureResult signatureResult = pgpResult.getSignatureResult();

        if (targetApiVersion < API_VERSION_WITH_KEY_INVALID_INSECURE) {
            // RESULT_INVALID_INSECURE has been added in version 8, fallback to RESULT_INVALID_SIGNATURE
            if (signatureResult.getResult() == OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE) {
                signatureResult = OpenPgpSignatureResult.createWithInvalidSignature();
            }
        }

        if (targetApiVersion < API_VERSION_WITHOUT_SIGNATURE_ONLY_FLAG) {
            // this info was kept in OpenPgpSignatureResult, so put it there for compatibility
            OpenPgpDecryptionResult decryptionResult = pgpResult.getDecryptionResult();
            boolean signatureOnly = decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED
                    && signatureResult.getResult() != OpenPgpSignatureResult.RESULT_NO_SIGNATURE;
            // noinspection deprecation, this is for backwards compatibility
            signatureResult = signatureResult.withSignatureOnlyFlag(signatureOnly);
        }

        return signatureResult;
    }

    private void processMetadataForResultIntent(Intent result, OpenPgpMetadata metadata) {
        String charset = metadata != null ? metadata.getCharset() : null;
        if (charset != null) {
            result.putExtra(OpenPgpApi.RESULT_CHARSET, charset);
        }

        if (metadata != null) {
            result.putExtra(OpenPgpApi.RESULT_METADATA, metadata);
        }
    }

    private void processSignatureResultForResultIntent(int targetApiVersion, Intent data,
            Intent result, DecryptVerifyResult pgpResult) {
        OpenPgpSignatureResult signatureResult =
                getSignatureResultWithApiCompatibilityFallbacks(targetApiVersion, pgpResult);

        switch (signatureResult.getResult()) {
            case OpenPgpSignatureResult.RESULT_KEY_MISSING: {
                // If signature key is missing we return a PendingIntent to retrieve the key
                result.putExtra(OpenPgpApi.RESULT_INTENT,
                        mApiPendingIntentFactory.createImportFromKeyserverPendingIntent(data,
                                signatureResult.getKeyId()));
                break;
            }
            case OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED:
            case OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE: {
                // If signature key is known, return PendingIntent to show key
                result.putExtra(OpenPgpApi.RESULT_INTENT,
                        mApiPendingIntentFactory.createShowKeyPendingIntent(data, signatureResult.getKeyId()));
                break;
            }
            default:
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE: {
                if (targetApiVersion < API_VERSION_WITH_RESULT_NO_SIGNATURE) {
                    // RESULT_NO_SIGNATURE has been added in version 8, before the signatureResult was null
                    signatureResult = null;
                }
                // otherwise, no pending intent
            }

            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE: {
                // no key id -> no PendingIntent
            }
        }

        String autocryptPeerentity = data.getStringExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID);
        if (autocryptPeerentity != null) {
            if (targetApiVersion < API_VERSION_WITH_AUTOCRYPT) {
                throw new IllegalStateException("API version conflict, autocrypt is supported v12 and up!");
            }
            signatureResult = processAutocryptPeerInfoToSignatureResult(signatureResult, autocryptPeerentity);
        }

        result.putExtra(OpenPgpApi.RESULT_SIGNATURE, signatureResult);
    }

    private OpenPgpSignatureResult processAutocryptPeerInfoToSignatureResult(OpenPgpSignatureResult signatureResult,
            String autocryptPeerId) {
        boolean hasValidSignature =
                signatureResult.getResult() == OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED ||
                signatureResult.getResult() == OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED;
        if (!hasValidSignature) {
            return signatureResult;
        }

        AutocryptPeerDataAccessObject autocryptPeerentityDao = new AutocryptPeerDataAccessObject(getBaseContext(),
                mApiPermissionHelper.getCurrentCallingPackage());
        Long autocryptPeerMasterKeyId = autocryptPeerentityDao.getMasterKeyIdForAutocryptPeer(autocryptPeerId);

        long masterKeyId = signatureResult.getKeyId();
        if (autocryptPeerMasterKeyId == null) {
            Date now = new Date();
            Date effectiveTime = signatureResult.getSignatureTimestamp();
            if (effectiveTime.after(now)) {
                effectiveTime = now;
            }
            autocryptPeerentityDao.updateKeyGossipFromSignature(autocryptPeerId, effectiveTime, masterKeyId);
            return signatureResult.withAutocryptPeerResult(AutocryptPeerResult.NEW);
        } else  if (masterKeyId == autocryptPeerMasterKeyId) {
            return signatureResult.withAutocryptPeerResult(AutocryptPeerResult.OK);
        } else {
            return signatureResult.withAutocryptPeerResult(AutocryptPeerResult.MISMATCH);
        }
    }

    private Intent getKeyImpl(Intent data, OutputStream outputStream) {
        try {
            long masterKeyId;
            if (data.hasExtra(OpenPgpApi.EXTRA_KEY_ID)) {
                masterKeyId = data.getLongExtra(OpenPgpApi.EXTRA_KEY_ID, 0);
            } else if (data.hasExtra(OpenPgpApi.EXTRA_USER_ID)) {
                KeyIdResult keyIdResult = mKeyIdExtractor.returnKeyIdsFromEmails(
                        null, new String[] { data.getStringExtra(OpenPgpApi.EXTRA_USER_ID) },
                        mApiPermissionHelper.getCurrentCallingPackage());
                if (keyIdResult.getStatus() != KeyIdResultStatus.OK) {
                    Intent result = getAutocryptStatusResult(keyIdResult);
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                    return result;
                }

                masterKeyId = keyIdResult.getKeyIds()[0];
            } else {
                throw new IllegalArgumentException("Missing argument key_id or user_id!");
            }

            try {
                // try to find key, throws NotFoundException if not in db!
                CanonicalizedPublicKeyRing keyRing =
                        mKeyRepository.getCanonicalizedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(masterKeyId));

                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);

                if (data.getBooleanExtra(OpenPgpApi.EXTRA_MINIMIZE, false)) {
                    String userIdToKeep = data.getStringExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID);
                    keyRing = keyRing.minimize(userIdToKeep);
                }

                boolean requestedKeyData = outputStream != null;
                if (requestedKeyData) {
                    boolean requestAsciiArmor = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, false);

                    try {
                        if (requestAsciiArmor) {
                            outputStream = new ArmoredOutputStream(outputStream);
                        }
                        keyRing.encode(outputStream);
                    } finally {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            Timber.e(e, "IOException when closing OutputStream");
                        }
                    }
                }

                // also return PendingIntent that opens the key view activity
                result.putExtra(OpenPgpApi.RESULT_INTENT,
                        mApiPendingIntentFactory.createShowKeyPendingIntent(data, masterKeyId));

                return result;
            } catch (KeyRepository.NotFoundException e) {
                // If keys are not in db we return an additional PendingIntent
                // to retrieve the missing key
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT,
                        mApiPendingIntentFactory.createImportFromKeyserverPendingIntent(data, masterKeyId));
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;
            }
        } catch (Exception e) {
            Timber.d(e, "getKeyImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    @NonNull
    private Intent createErrorResultIntent(int errorCode, String errorMsg) {
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_ERROR,
                new OpenPgpError(errorCode, errorMsg));
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        return result;
    }

    private Intent getSignKeyIdImpl(Intent data) {
        // if data already contains EXTRA_SIGN_KEY_ID, it has been executed again
        // after user interaction. Then, we just need to return the long again!
        if (data.hasExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID)) {
            long signKeyId = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, signKeyId);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } else {
            String currentPkg = mApiPermissionHelper.getCurrentCallingPackage();
            String preferredUserId = data.getStringExtra(OpenPgpApi.EXTRA_USER_ID);

            PendingIntent pi = mApiPendingIntentFactory.createSelectSignKeyIdPendingIntent(data, currentPkg, preferredUserId);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

            return result;
        }
    }

    private Intent getKeyIdsImpl(Intent data) {
        KeyIdResult keyIdResult = mKeyIdExtractor.returnKeyIdsFromIntent(data, true,
                mApiPermissionHelper.getCurrentCallingPackage());
        if (keyIdResult.hasKeySelectionPendingIntent()) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_INTENT, keyIdResult.getKeySelectionPendingIntent());
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            return result;
        }
        long[] keyIds = keyIdResult.getKeyIds();

        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_KEY_IDS, keyIds);
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        return result;
    }

    private Intent backupImpl(Intent data, OutputStream outputStream) {
        try {
            long[] masterKeyIds = data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);
            boolean backupSecret = data.getBooleanExtra(OpenPgpApi.EXTRA_BACKUP_SECRET, false);
            boolean enableAsciiArmorOutput = data.getBooleanExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
            if (inputParcel == null) {
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_INTENT, mApiPendingIntentFactory.createBackupPendingIntent(data, masterKeyIds, backupSecret));
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                return result;
            }
            // after user interaction with RemoteBackupActivity,
            // the backup code is cached in CryptoInputParcelCacheService, now we can proceed

            BackupKeyringParcel input = BackupKeyringParcel
                    .createBackupKeyringParcel(masterKeyIds, backupSecret, true, enableAsciiArmorOutput, null);
            BackupOperation op = new BackupOperation(this, mKeyRepository, null);
            ExportResult pgpResult = op.execute(input, inputParcel, outputStream);

            if (pgpResult.success()) {
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                return result;
            } else {
                // should not happen normally...
                String errorMsg = getString(pgpResult.getLog().getLast().mType.getMsgId());
                return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, errorMsg);
            }
        } catch (Exception e) {
            Timber.d(e, "backupImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private Intent updateAutocryptPeerImpl(Intent data) {
        try {
            AutocryptPeerDataAccessObject autocryptPeerDao = new AutocryptPeerDataAccessObject(getBaseContext(),
                    mApiPermissionHelper.getCurrentCallingPackage());
            AutocryptInteractor autocryptInteractor = AutocryptInteractor.getInstance(getBaseContext(), autocryptPeerDao);

            if (data.hasExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID) &&
                    data.hasExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_UPDATE)) {
                String autocryptPeerId = data.getStringExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID);
                AutocryptPeerUpdate autocryptPeerUpdate = data.getParcelableExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_UPDATE);

                if (autocryptPeerUpdate != null) {
                    autocryptInteractor.updateAutocryptPeerState(autocryptPeerId, autocryptPeerUpdate);
                }
            }

            if (data.hasExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_GOSSIP_UPDATES)) {
                Bundle updates = data.getBundleExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_GOSSIP_UPDATES);
                for (String address : updates.keySet()) {
                    Timber.d(Constants.TAG, "Updating gossip state: " + address);
                    AutocryptPeerUpdate update = updates.getParcelable(address);
                    if (update != null) {
                        autocryptInteractor.updateAutocryptPeerGossipState(address, update);
                    }
                }
            }

            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
            return result;
        } catch (Exception e) {
            Timber.d(e, "exception in updateAutocryptPeerImpl");
            return createErrorResultIntent(OpenPgpError.GENERIC_ERROR, e.getMessage());
        }
    }

    private Intent checkPermissionImpl(@NonNull Intent data) {
        Intent permissionIntent = mApiPermissionHelper.isAllowedOrReturnIntent(data);
        if (permissionIntent != null) {
            return permissionIntent;
        }
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        return result;
    }

    private Intent getSignKeyMasterId(Intent data) {
        long signKeyId = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, Constants.key.none);
        if (signKeyId == Constants.key.none) {
            return getSignKeyIdImpl(data);
        }

        return data;
    }

    private HashSet<Long> getAllowedKeyIds() {
        String currentPkg = mApiPermissionHelper.getCurrentCallingPackage();
        return mApiDao.getAllowedKeyIdsForApp(
                KeychainContract.ApiAllowedKeys.buildBaseUri(currentPkg));
    }

    /**
     * Check requirements:
     * - params != null
     * - has supported API version
     * - is allowed to call the service (access has been granted)
     *
     * @return null if everything is okay, or a Bundle with an createErrorPendingIntent/PendingIntent
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
        // History of versions in openpgp-api's CHANGELOG.md
        if (!SUPPORTED_VERSIONS.contains(data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1))) {
            Intent result = new Intent();
            OpenPgpError error = new OpenPgpError
                    (OpenPgpError.INCOMPATIBLE_API_VERSIONS, "Incompatible API versions!\n"
                            + "used API version: " + data.getIntExtra(OpenPgpApi.EXTRA_API_VERSION, -1) + "\n"
                            + "supported API versions: " + SUPPORTED_VERSIONS);
            result.putExtra(OpenPgpApi.RESULT_ERROR, error);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return result;
        }

        // check if caller is allowed to access OpenKeychain
        Intent result = mApiPermissionHelper.isAllowedOrReturnIntent(data);
        if (result != null) {
            return result;
        }

        return null;
    }

    private final IOpenPgpService.Stub mBinder = new IOpenPgpService.Stub() {
        @Override
        public Intent execute(Intent data, ParcelFileDescriptor input, ParcelFileDescriptor output) {
            Timber.w(
                    "You are using a deprecated service which may lead to truncated data on return, please use IOpenPgpService2!");
            return executeInternal(data, input, output);
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Nullable
    protected Intent executeInternal(
            @NonNull Intent data,
            @Nullable ParcelFileDescriptor input,
            @Nullable ParcelFileDescriptor output) {

        OutputStream outputStream =
                (output != null) ? new ParcelFileDescriptor.AutoCloseOutputStream(output) : null;
        InputStream inputStream =
                (input != null) ? new ParcelFileDescriptor.AutoCloseInputStream(input) : null;

        try {
            return executeInternalWithStreams(data, inputStream, outputStream);
        } finally {
            // always close input and output file descriptors even in createErrorPendingIntent cases
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Timber.e(e, "IOException when closing input ParcelFileDescriptor");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Timber.e(e, "IOException when closing output ParcelFileDescriptor");
                }
            }
        }
    }

    @Nullable
    protected Intent executeInternalWithStreams(
            @NonNull Intent data,
            @Nullable InputStream inputStream,
            @Nullable OutputStream outputStream) {

        // We need to be able to load our own parcelables
        data.setExtrasClassLoader(getClassLoader());

        Intent errorResult = checkRequirements(data);
        if (errorResult != null) {
            return errorResult;
        }

        Progressable progressable = null;
        if (data.hasExtra(OpenPgpApi.EXTRA_PROGRESS_MESSENGER)) {
            Messenger messenger = data.getParcelableExtra(OpenPgpApi.EXTRA_PROGRESS_MESSENGER);
            progressable = createMessengerProgressable(messenger);
        }

        String action = data.getAction();
        switch (action) {
            case OpenPgpApi.ACTION_CHECK_PERMISSION: {
                return checkPermissionImpl(data);
            }
            case OpenPgpApi.ACTION_CLEARTEXT_SIGN: {
                return signImpl(data, inputStream, outputStream, true);
            }
            case OpenPgpApi.ACTION_SIGN: {
                // DEPRECATED: same as ACTION_CLEARTEXT_SIGN
                Timber.w(
                        "You are using a deprecated API call, please use ACTION_CLEARTEXT_SIGN instead of ACTION_SIGN!");
                return signImpl(data, inputStream, outputStream, true);
            }
            case OpenPgpApi.ACTION_DETACHED_SIGN: {
                return signImpl(data, inputStream, outputStream, false);
            }
            case OpenPgpApi.ACTION_QUERY_AUTOCRYPT_STATUS: {
                return encryptAndSignImpl(data, inputStream, outputStream, false, true);
            }
            case OpenPgpApi.ACTION_ENCRYPT:
            case OpenPgpApi.ACTION_SIGN_AND_ENCRYPT: {
                boolean enableSign = action.equals(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
                return encryptAndSignImpl(data, inputStream, outputStream, enableSign, false);
            }
            case OpenPgpApi.ACTION_DECRYPT_VERIFY: {
                return decryptAndVerifyImpl(data, inputStream, outputStream, false, progressable);
            }
            case OpenPgpApi.ACTION_DECRYPT_METADATA: {
                return decryptAndVerifyImpl(data, inputStream, outputStream, true, null);
            }
            case OpenPgpApi.ACTION_GET_SIGN_KEY_ID: {
                return getSignKeyIdImpl(data);
            }
            case OpenPgpApi.ACTION_GET_KEY_IDS: {
                return getKeyIdsImpl(data);
            }
            case OpenPgpApi.ACTION_GET_KEY: {
                return getKeyImpl(data, outputStream);
            }
            case OpenPgpApi.ACTION_BACKUP: {
                return backupImpl(data, outputStream);
            }
            case OpenPgpApi.ACTION_UPDATE_AUTOCRYPT_PEER: {
                return updateAutocryptPeerImpl(data);
            }
            default: {
                return null;
            }
        }

    }

    @NonNull
    private static Progressable createMessengerProgressable(final Messenger messenger) {
        return new Progressable() {
            boolean errorState = false;
            @Override
            public void setProgress(String message, int current, int total) {
                setProgress(current, total);
            }

            @Override
            public void setProgress(int resourceId, int current, int total) {
                setProgress(current, total);
            }

            @Override
            public void setProgress(int current, int total) {
                if (errorState) {
                    return;
                }
                Message m = Message.obtain();
                m.arg1 = current;
                m.arg2 = total;
                try {
                    messenger.send(m);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    errorState = true;
                }
            }

            @Override
            public void setPreventCancel() {

            }
        };
    }

}
