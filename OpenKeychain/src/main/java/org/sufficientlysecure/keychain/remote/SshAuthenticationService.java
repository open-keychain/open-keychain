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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
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
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.SshPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationData;
import org.sufficientlysecure.keychain.ssh.AuthenticationOperation;
import org.sufficientlysecure.keychain.ssh.AuthenticationParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationResult;
import org.sufficientlysecure.keychain.ssh.signature.SshSignatureConverter;
import timber.log.Timber;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


public class SshAuthenticationService extends Service {
    private static final String TAG = "SshAuthService";

    private ApiPermissionHelper mApiPermissionHelper;
    private KeyRepository mKeyRepository;
    private ApiDataAccessObject mApiDao;
    private ApiPendingIntentFactory mApiPendingIntentFactory;

    private static final List<Integer> SUPPORTED_VERSIONS = Collections.unmodifiableList(Collections.singletonList(1));
    private static final int INVALID_API_VERSION = -1;

    private static final int HASHALGORITHM_NONE = SshAuthenticationApiError.INVALID_HASH_ALGORITHM;

    @Override
    public void onCreate() {
        super.onCreate();
        mApiPermissionHelper = new ApiPermissionHelper(this, new ApiDataAccessObject(this));
        mKeyRepository = KeyRepository.create(this);
        mApiDao = new ApiDataAccessObject(this);

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

        // keyid == masterkeyid -> authkeyid
        // keyId is the pgp master keyId, the keyId used will be the first authentication
        // key in the keyring designated by the master keyId
        String keyIdString = data.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        long masterKeyId = Long.valueOf(keyIdString);

        int hashAlgorithmTag = getHashAlgorithm(data);
        if (hashAlgorithmTag == HASHALGORITHM_NONE) {
            return createErrorResult(SshAuthenticationApiError.GENERIC_ERROR, "No valid hash algorithm!");
        }

        byte[] challenge = data.getByteArrayExtra(SshAuthenticationApi.EXTRA_CHALLENGE);
        if (challenge == null || challenge.length == 0) {
            return createErrorResult(SshAuthenticationApiError.GENERIC_ERROR, "No challenge given");
        }

        // carries the metadata necessary for authentication
        AuthenticationData.Builder authData = AuthenticationData.builder();
        authData.setAuthenticationMasterKeyId(masterKeyId);

        CachedPublicKeyRing cachedPublicKeyRing = mKeyRepository.getCachedPublicKeyRing(masterKeyId);

        long authSubKeyId;
        int authSubKeyAlgorithm;
        String authSubKeyCurveOid = null;
        try {
            // get first usable subkey capable of authentication
            authSubKeyId = cachedPublicKeyRing.getSecretAuthenticationId();
            // needed for encoding the resulting signature
            authSubKeyAlgorithm = getPublicKey(masterKeyId).getAlgorithm();
            if (authSubKeyAlgorithm == PublicKeyAlgorithmTags.ECDSA) {
                authSubKeyCurveOid = getPublicKey(masterKeyId).getCurveOid();
            }
        } catch (PgpKeyNotFoundException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.NO_AUTH_KEY,
                    "authentication key for master key id not found in keychain", e);
        } catch (KeyRepository.NotFoundException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                    "Key for master key id not found", e);
        }

        authData.setAuthenticationSubKeyId(authSubKeyId);

        authData.setAllowedAuthenticationKeyIds(getAllowedKeyIds());

        authData.setHashAlgorithm(hashAlgorithmTag);

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
        long authMasterKeyId = getKeyId(data);
        if (authMasterKeyId == Constants.key.none) {
            return createErrorResult(SshAuthenticationApiError.NO_KEY_ID,
                    "No key id in request");
        }
        return null;
    }

    private long getKeyId(Intent data) {
        String keyIdString = data.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        long authMasterKeyId = Constants.key.none;
        if (keyIdString != null) {
            try {
                authMasterKeyId = Long.valueOf(keyIdString);
            } catch (NumberFormatException e) {
                return Constants.key.none;
            }
        }
        return authMasterKeyId;
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
        long masterKeyId = getKeyId(data);
        if (masterKeyId != Constants.key.none) {
            String description;

            try {
                description = getDescription(masterKeyId);
            } catch (PgpKeyNotFoundException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                        "Could not create description", e);
            }

            return new KeySelectionResponse(String.valueOf(masterKeyId), description).toIntent();
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
        long masterKeyId = getKeyId(data);
        if (masterKeyId != Constants.key.none) {
            try {
                if (asSshKey) {
                    return getSSHPublicKey(masterKeyId);
                } else {
                    return getX509PublicKey(masterKeyId);
                }
            } catch (KeyRepository.NotFoundException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.NO_SUCH_KEY,
                        "Key for master key id not found", e);
            } catch (PgpKeyNotFoundException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.NO_AUTH_KEY,
                        "Authentication key for master key id not found in keychain", e);
            } catch (NoSuchAlgorithmException e) {
                return createExceptionErrorResult(SshAuthenticationApiError.INVALID_ALGORITHM,
                        "Algorithm not supported", e);
            }
        } else {
            return createErrorResult(SshAuthenticationApiError.NO_KEY_ID,
                    "No key id in request");
        }
    }

    private Intent getX509PublicKey(long masterKeyId) throws KeyRepository.NotFoundException, PgpKeyNotFoundException, NoSuchAlgorithmException {
        byte[] encodedPublicKey;
        int algorithm;

        PublicKey publicKey;
        try {
            publicKey = getPublicKey(masterKeyId).getJcaPublicKey();
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

    private Intent getSSHPublicKey(long masterKeyId) throws KeyRepository.NotFoundException, PgpKeyNotFoundException {
        String sshPublicKeyBlob;

        CanonicalizedPublicKey publicKey = getPublicKey(masterKeyId);

        SshPublicKey sshPublicKey = new SshPublicKey(publicKey);
        try {
            sshPublicKeyBlob = sshPublicKey.getEncodedKey();
        } catch (PgpGeneralException | NoSuchAlgorithmException e) {
            return createExceptionErrorResult(SshAuthenticationApiError.GENERIC_ERROR,
                    "Error converting public key to SSH format", e);
        }

        return new SshPublicKeyResponse(sshPublicKeyBlob).toIntent();
    }

    private CanonicalizedPublicKey getPublicKey(long masterKeyId)
            throws PgpKeyNotFoundException, KeyRepository.NotFoundException {
        KeyRepository keyRepository = KeyRepository.create(getApplicationContext());
        long authSubKeyId = keyRepository.getCachedPublicKeyRing(masterKeyId)
                .getAuthenticationId();
        return keyRepository.getCanonicalizedPublicKeyRing(masterKeyId)
                .getPublicKey(authSubKeyId);
    }

    private String getDescription(long masterKeyId) throws PgpKeyNotFoundException {
        CachedPublicKeyRing cachedPublicKeyRing = mKeyRepository.getCachedPublicKeyRing(masterKeyId);

        String description = "";
        long authSubKeyId = cachedPublicKeyRing.getSecretAuthenticationId();
        description += cachedPublicKeyRing.getPrimaryUserId();
        description += " (" + Long.toHexString(authSubKeyId) + ")";

        return description;
    }

    private HashSet<Long> getAllowedKeyIds() {
        String currentPkg = mApiPermissionHelper.getCurrentCallingPackage();
        return mApiDao.getAllowedKeyIdsForApp(KeychainContract.ApiAllowedKeys.buildBaseUri(currentPkg));
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
