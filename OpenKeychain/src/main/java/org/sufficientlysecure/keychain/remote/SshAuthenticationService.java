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


import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.openintents.ssh.authentication.ISshAuthenticationService;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.openintents.ssh.authentication.SshAuthenticationApiError;
import org.openintents.ssh.authentication.response.KeySelectionResponse;
import org.openintents.ssh.authentication.response.PublicKeyResponse;
import org.openintents.ssh.authentication.response.SigningResponse;
import org.openintents.ssh.authentication.response.SshPublicKeyResponse;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.SshPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationData;
import org.sufficientlysecure.keychain.ssh.AuthenticationOperation;
import org.sufficientlysecure.keychain.ssh.AuthenticationParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationResult;
import org.sufficientlysecure.keychain.ssh.signature.SshSignatureConverter;
import timber.log.Timber;


public class SshAuthenticationService extends Service {
    private ApiPermissionHelper mApiPermissionHelper;
    private KeyRepository mKeyRepository;
    private ApiAppDao mApiAppDao;
    private ApiPendingIntentFactory mApiPendingIntentFactory;

    private static final List<Integer> SUPPORTED_VERSIONS = Collections.unmodifiableList(Collections.singletonList(1));
    private static final int INVALID_API_VERSION = -1;

    private static final int HASHALGORITHM_NONE = SshAuthenticationApiError.INVALID_HASH_ALGORITHM;

    @Override
    public void onCreate() {
        super.onCreate();
        mApiPermissionHelper = new ApiPermissionHelper(this, ApiAppDao.getInstance(this));
        mKeyRepository = KeyRepository.create(this);
        mApiAppDao = ApiAppDao.getInstance(this);

        mApiPendingIntentFactory = new ApiPendingIntentFactory(getBaseContext());
    }

    private final ISshAuthenticationService.Stub mSSHAgent = new ISshAuthenticationService.Stub() {
        @Override
        public Intent execute(Intent intent) {
            return checkIntent(intent);
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mSSHAgent;
    }

    private Intent checkIntent(Intent intent) {
        Intent errorResult = checkRequirements(intent);
        if (errorResult == null) {
            return executeInternal(intent);
        } else {
            return errorResult;
        }
    }

    private Intent executeInternal(Intent intent) {
        switch (intent.getAction()) {
            case SshAuthenticationApi.ACTION_SIGN:
                return authenticate(intent);
            case SshAuthenticationApi.ACTION_SELECT_KEY:
                return getAuthenticationKey(intent);
            case SshAuthenticationApi.ACTION_GET_PUBLIC_KEY:
                return getAuthenticationPublicKey(intent, false);
            case SshAuthenticationApi.ACTION_GET_SSH_PUBLIC_KEY:
                return getAuthenticationPublicKey(intent, true);
            default:
                return createErrorResult(SshAuthenticationApiError.UNKNOWN_ACTION, "Unknown action");
        }
    }

    private Intent authenticate(Intent data) {
        Intent errorIntent = checkForKeyId(data);
        if (errorIntent != null) {
            return errorIntent;
        }

        String keyIdString = data.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        // requestKeyId is assumed to be a auth subkey id
        long requestKeyId = Long.valueOf(keyIdString);

        int hashAlgorithmTag = getHashAlgorithm(data);
        if (hashAlgorithmTag == HASHALGORITHM_NONE) {
            return createErrorResult(SshAuthenticationApiError.GENERIC_ERROR, "No valid hash algorithm!");
        }

        byte[] challenge = data.getByteArrayExtra(SshAuthenticationApi.EXTRA_CHALLENGE);
        if (challenge == null || challenge.length == 0) {
            return createErrorResult(SshAuthenticationApiError.GENERIC_ERROR, "No challenge given");
        }

        long masterKeyId;
        long authSubKeyId;
        int authSubKeyAlgorithm;
        String authSubKeyCurveOid = null;

        try {
            masterKeyId = mKeyRepository.getMasterKeyIdByAuthSubkeyId(requestKeyId);
        } catch (NotFoundException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                    "Master key for sub key id not found", e);
        }
        authSubKeyId = requestKeyId;

        try {
            CanonicalizedPublicKey publicKey = getPublicKey(masterKeyId, authSubKeyId);
            // needed for encoding the resulting signature
            authSubKeyAlgorithm = publicKey.getAlgorithm();
            if (authSubKeyAlgorithm == PublicKeyAlgorithmTags.ECDSA) {
                authSubKeyCurveOid = publicKey.getCurveOid();
            }
        } catch (NotFoundException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                    "Authentication key for key id not found", e);
        }

        // carries the metadata necessary for authentication
        AuthenticationData.Builder authData = AuthenticationData.builder();
        authData.setAuthenticationMasterKeyId(masterKeyId);
        authData.setAuthenticationSubKeyId(authSubKeyId);
        authData.setHashAlgorithm(hashAlgorithmTag);

        authData.setAllowedAuthenticationKeyIds(getAllowedKeyIds());

        CryptoInputParcel inputParcel = CryptoInputParcelCacheService.getCryptoInputParcel(this, data);
        if (inputParcel == null) {
            // fresh request, assign UUID
            inputParcel = CryptoInputParcel.createCryptoInputParcel(new Date());
        }

        AuthenticationParcel authParcel = AuthenticationParcel
                .createAuthenticationParcel(authData.build(), challenge);

        // execute authentication operation!
        AuthenticationOperation authOperation = new AuthenticationOperation(this, mKeyRepository);
        AuthenticationResult authResult = authOperation.execute(authData.build(), inputParcel, authParcel);

        if (authResult.isPending()) {
            RequiredInputParcel requiredInput = authResult.getRequiredInputParcel();
            PendingIntent pi = mApiPendingIntentFactory.requiredInputPi(data, requiredInput,
                    authResult.mCryptoInputParcel);
            // return PendingIntent to be executed by client
            return packagePendingIntent(pi);
        } else if (authResult.success()) {
            byte[] rawSignature = authResult.getSignature();
            byte[] sshSignature;
            try {
                switch (authSubKeyAlgorithm) {
                    case PublicKeyAlgorithmTags.EDDSA:
                        sshSignature = SshSignatureConverter.getSshSignatureEdDsa(rawSignature);
                        break;
                    case PublicKeyAlgorithmTags.RSA_SIGN:
                    case PublicKeyAlgorithmTags.RSA_GENERAL:
                        sshSignature = SshSignatureConverter.getSshSignatureRsa(rawSignature, hashAlgorithmTag);
                        break;
                    case PublicKeyAlgorithmTags.ECDSA:
                        sshSignature = SshSignatureConverter.getSshSignatureEcDsa(rawSignature, authSubKeyCurveOid);
                        break;
                    case PublicKeyAlgorithmTags.DSA:
                        sshSignature = SshSignatureConverter.getSshSignatureDsa(rawSignature);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("Unknown algorithm");
                }
            } catch (NoSuchAlgorithmException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.INTERNAL_ERROR,
                        "Error converting signature", e);
            }
            return new SigningResponse(sshSignature).toIntent();
        } else {
            LogEntryParcel errorMsg = authResult.getLog().getLast();
            return createErrorResult(SshAuthenticationApiError.INTERNAL_ERROR, getString(errorMsg.mType.getMsgId()));
        }
    }

    private Intent checkForKeyId(Intent data) {
        long keyId = getKeyId(data);
        if (keyId == Constants.key.none) {
            return createErrorResult(SshAuthenticationApiError.NO_KEY_ID,
                    "No key id in request");
        }
        return null;
    }

    private long getKeyId(Intent data) {
        String keyIdString = data.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        long keyId = Constants.key.none;
        if (keyIdString != null) {
            try {
                keyId = Long.valueOf(keyIdString);
            } catch (NumberFormatException e) {
                return Constants.key.none;
            }
        }
        return keyId;
    }

    private int getHashAlgorithm(Intent data) {
        int hashAlgorithm = data.getIntExtra(SshAuthenticationApi.EXTRA_HASH_ALGORITHM, HASHALGORITHM_NONE);

        switch (hashAlgorithm) {
            case SshAuthenticationApi.SHA1:
                return HashAlgorithmTags.SHA1;
            case SshAuthenticationApi.RIPEMD160:
                return HashAlgorithmTags.RIPEMD160;
            case SshAuthenticationApi.SHA224:
                return HashAlgorithmTags.SHA224;
            case SshAuthenticationApi.SHA256:
                return HashAlgorithmTags.SHA256;
            case SshAuthenticationApi.SHA384:
                return HashAlgorithmTags.SHA384;
            case SshAuthenticationApi.SHA512:
                return HashAlgorithmTags.SHA512;
            default:
                return HASHALGORITHM_NONE;
        }
    }

    private Intent getAuthenticationKey(Intent data) {
        long subKeyId = getKeyId(data);
        if (subKeyId != Constants.key.none) {
            String description;

            long masterKeyId;
            try {
                masterKeyId = mKeyRepository.getMasterKeyIdByAuthSubkeyId(subKeyId);
            } catch (NotFoundException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                        "Master key for sub key id not found", e);
            }
            description = getDescription(masterKeyId, subKeyId);

            return new KeySelectionResponse(String.valueOf(subKeyId), description).toIntent();
        } else {
            return redirectToKeySelection(data);
        }
    }

    private Intent redirectToKeySelection(Intent data) {
        String currentPkg = mApiPermissionHelper.getCurrentCallingPackage();
        PendingIntent pi = mApiPendingIntentFactory.createSelectAuthenticationKeyIdPendingIntent(data, currentPkg);
        return packagePendingIntent(pi);
    }

    private Intent packagePendingIntent(PendingIntent pi) {
        Intent result = new Intent();
        result.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE,
                SshAuthenticationApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        result.putExtra(SshAuthenticationApi.EXTRA_PENDING_INTENT, pi);
        return result;
    }

    private Intent getAuthenticationPublicKey(Intent data, boolean asSshKey) {
        long keyId = getKeyId(data);
        if (keyId != Constants.key.none) {
            try {
                if (asSshKey) {
                    return getSSHPublicKey(keyId);
                } else {
                    return getX509PublicKey(keyId);
                }
            } catch (KeyRepository.NotFoundException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                        "Key for master key id not found", e);
            } catch (NoSuchAlgorithmException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.INVALID_ALGORITHM,
                        "Algorithm not supported", e);
            }
        } else {
            return createErrorResult(SshAuthenticationApiError.NO_KEY_ID,
                    "No key id in request");
        }
    }

    private Intent getX509PublicKey(long subKeyId) throws KeyRepository.NotFoundException, NoSuchAlgorithmException {
        byte[] encodedPublicKey;
        int algorithm;

        PublicKey publicKey;
        try {
            long masterKeyId = mKeyRepository.getMasterKeyIdByAuthSubkeyId(subKeyId);
            publicKey = getPublicKey(masterKeyId, subKeyId).getJcaPublicKey();
        } catch (PgpGeneralException e) { // this should probably never happen
            return createExceptionErrorResult(SshAuthenticationApiError.GENERIC_ERROR,
                    "Error converting public key", e);
        }

        encodedPublicKey = publicKey.getEncoded();
        algorithm = translateAlgorithm(publicKey.getAlgorithm());

        return new PublicKeyResponse(encodedPublicKey, algorithm).toIntent();
    }

    private int translateAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        switch (algorithm) {
            case "RSA":
                return SshAuthenticationApi.RSA;
            case "ECDSA":
                return SshAuthenticationApi.ECDSA;
            case "EdDSA":
                return SshAuthenticationApi.EDDSA;
            case "DSA":
                return SshAuthenticationApi.DSA;
            default:
                throw new NoSuchAlgorithmException("Error matching key algorithm to API supported algorithm: "
                        + algorithm);
        }
    }

    private Intent getSSHPublicKey(long subKeyId) throws KeyRepository.NotFoundException {
        String sshPublicKeyBlob;

        long masterKeyId = mKeyRepository.getMasterKeyIdByAuthSubkeyId(subKeyId);
        CanonicalizedPublicKey publicKey = getPublicKey(masterKeyId, subKeyId);

        SshPublicKey sshPublicKey = new SshPublicKey(publicKey);
        try {
            sshPublicKeyBlob = sshPublicKey.getEncodedKey();
        } catch (PgpGeneralException | NoSuchAlgorithmException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.GENERIC_ERROR,
                    "Error converting public key to SSH format", e);
        }

        return new SshPublicKeyResponse(sshPublicKeyBlob).toIntent();
    }

    private CanonicalizedPublicKey getPublicKey(long masterKeyId, long authSubKeyId) throws NotFoundException {
        KeyRepository keyRepository = KeyRepository.create(getApplicationContext());
        return keyRepository.getCanonicalizedPublicKeyRing(masterKeyId).getPublicKey(authSubKeyId);
    }

    private String getDescription(long masterKeyId, long authSubKeyId) {
        String description = "";
        UnifiedKeyInfo unifiedKeyInfo = mKeyRepository.getUnifiedKeyInfo(masterKeyId);
        description += unifiedKeyInfo.user_id();
        description += " (" + Long.toHexString(authSubKeyId) + ")";

        return description;
    }

    private HashSet<Long> getAllowedKeyIds() {
        String currentPkg = mApiPermissionHelper.getCurrentCallingPackage();
        return mApiAppDao.getAllowedKeyIdsForApp(currentPkg);
    }

    /**
     * @return null if basic requirements are met
     */
    private Intent checkRequirements(Intent data) {
        if (data == null) {
            return createErrorResult(SshAuthenticationApiError.GENERIC_ERROR, "No parameter bundle");
        }

        // check version
        int apiVersion = data.getIntExtra(SshAuthenticationApi.EXTRA_API_VERSION, INVALID_API_VERSION);
        if (!SUPPORTED_VERSIONS.contains(apiVersion)) {
            String errorMsg = "Incompatible API versions:\n"
                    + "used : " + data.getIntExtra(SshAuthenticationApi.EXTRA_API_VERSION, INVALID_API_VERSION) + "\n"
                    + "supported : " + SUPPORTED_VERSIONS;

            return createErrorResult(SshAuthenticationApiError.INCOMPATIBLE_API_VERSIONS, errorMsg);
        }

        // check if caller is allowed to access OpenKeychain
        Intent result = mApiPermissionHelper.isAllowedOrReturnIntent(data);
        if (result != null) {
            return result; // disallowed, redirect to registration
        }

        return null;
    }

    private Intent createErrorResult(int errorCode, String errorMessage) {
        Timber.e(errorMessage);
        Intent result = new Intent();
        result.putExtra(SshAuthenticationApi.EXTRA_ERROR, new SshAuthenticationApiError(errorCode, errorMessage));
        result.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_ERROR);
        return result;
    }

    private Intent createExceptionErrorResult(int errorCode, String errorMessage, Exception e) {
        String message = errorMessage + " : " + e.getMessage();
        return createErrorResult(errorCode, message);
    }

}
