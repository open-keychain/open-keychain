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

package org.sufficientlysecure.keychain.ssh;


import java.util.Collection;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bouncycastle.openpgp.AuthenticationSignatureGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PassphraseCacheInterface;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.util.Passphrase;
import timber.log.Timber;

import static java.lang.String.format;


/**
 * This class supports a single, low-level, authentication operation.
 * <p/>
 * The operation of this class takes a AuthenticationParcel
 * as input, and signs the included challenge as parametrized in the
 * AuthenticationData object. It returns its status
 * and a possible signature as a AuthenticationResult.
 * <p/>
 *
 * @see AuthenticationParcel
 */
public class AuthenticationOperation extends BaseOperation<AuthenticationParcel> {

    private static final String TAG = "AuthenticationOperation";

    public AuthenticationOperation(Context context, KeyRepository keyRepository) {
        super(context, keyRepository, null);
    }

    @NonNull
    @Override
    public AuthenticationResult execute(AuthenticationParcel input,
                                        CryptoInputParcel cryptoInput) {
        return executeInternal(input.getAuthenticationData(), cryptoInput, input);
    }

    @NonNull
    public AuthenticationResult execute(AuthenticationData data,
                                        CryptoInputParcel cryptoInput,
                                        AuthenticationParcel authenticationParcel) {
        return executeInternal(data, cryptoInput, authenticationParcel);
    }

    /**
     * Signs challenge based on given parameters
     */
    private AuthenticationResult executeInternal(AuthenticationData data,
                                                 CryptoInputParcel cryptoInput,
                                                 AuthenticationParcel authenticationParcel) {
        int indent = 0;
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_AUTH, indent);
        indent += 1;

        Timber.d(data.toString());

        long opTime;
        long startTime = System.currentTimeMillis();

        byte[] signature;

        byte[] challenge = authenticationParcel.getChallenge();

        int hashAlgorithm = data.getHashAlgorithm();

        long authMasterKeyId = data.getAuthenticationMasterKeyId();
        Long authSubKeyId = data.getAuthenticationSubKeyId();
        if (authSubKeyId == null) {
            try { // Get the key id of the authentication key belonging to the master key id
                authSubKeyId = mKeyRepository.getEffectiveAuthenticationKeyId(authMasterKeyId);
            } catch (NotFoundException e) {
                log.add(LogType.MSG_AUTH_ERROR_KEY_AUTH, indent);
                return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
            }
        }

        // Get keyring with the authentication key
        CanonicalizedSecretKeyRing authKeyRing;
        try {
            authKeyRing = mKeyRepository.getCanonicalizedSecretKeyRing(authMasterKeyId);
        } catch (KeyRepository.NotFoundException e) {
            log.add(LogType.MSG_AUTH_ERROR_KEY_AUTH, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        CanonicalizedSecretKey authKey = authKeyRing.getSecretKey(authSubKeyId);

        // Make sure the client is allowed to access this key
        Collection<Long> allowedAuthenticationKeyIds = data.getAllowedAuthenticationKeyIds();
        if (allowedAuthenticationKeyIds != null && !allowedAuthenticationKeyIds.contains(authMasterKeyId)) {
            // this key is in our db, but NOT allowed!
            log.add(LogType.MSG_AUTH_ERROR_KEY_NOT_ALLOWED, indent + 1);
            return new AuthenticationResult(AuthenticationResult.RESULT_KEY_DISALLOWED, log);
        }

        // Make sure key is not expired or revoked
        if (authKeyRing.isExpired() || authKeyRing.isRevoked()
                || authKey.isExpired() || authKey.isRevoked()) {
            log.add(LogType.MSG_AUTH_ERROR_REVOKED_OR_EXPIRED, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        // Make sure the selected key is allowed to authenticate
        if (!authKey.canAuthenticate()) {
            log.add(LogType.MSG_AUTH_ERROR_KEY_AUTH, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        CanonicalizedSecretKey.SecretKeyType secretKeyType;
        try {
            secretKeyType = mKeyRepository.getSecretKeyType(authSubKeyId);
        } catch (KeyRepository.NotFoundException e) {
            log.add(LogType.MSG_AUTH_ERROR_KEY_AUTH, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        switch (secretKeyType) {
            case DIVERT_TO_CARD:
            case PASSPHRASE_EMPTY: {
                boolean isUnlocked;
                try {
                    isUnlocked = authKey.unlock(new Passphrase());
                } catch (PgpGeneralException e) {
                    log.add(LogType.MSG_AUTH_ERROR_UNLOCK, indent);
                    return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
                }

                if (!isUnlocked) {
                    throw new AssertionError(
                            "PASSPHRASE_EMPTY/DIVERT_TO_CARD keyphrase not unlocked with empty passphrase."
                                    + " This is a programming error!");
                }
                break;
            }

            case PASSPHRASE: {
                Passphrase localPassphrase = cryptoInput.getPassphrase();
                if (localPassphrase == null) {
                    try {
                        localPassphrase = getCachedPassphrase(authMasterKeyId, authKey.getKeyId());
                    } catch (PassphraseCacheInterface.NoSecretKeyException ignored) {
                    }
                }
                if (localPassphrase == null) {
                    log.add(LogType.MSG_AUTH_PENDING_PASSPHRASE, indent + 1);
                    return new AuthenticationResult(log,
                            RequiredInputParcel.createRequiredAuthenticationPassphrase(
                                    authMasterKeyId, authKey.getKeyId()),
                            cryptoInput);
                }

                boolean isUnlocked;
                try {
                    isUnlocked = authKey.unlock(localPassphrase);
                } catch (PgpGeneralException e) {
                    log.add(LogType.MSG_AUTH_ERROR_UNLOCK, indent);
                    return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
                }
                if (!isUnlocked) {
                    log.add(LogType.MSG_AUTH_ERROR_BAD_PASSPHRASE, indent);
                    return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
                }
                break;
            }

            case GNU_DUMMY: {
                log.add(LogType.MSG_AUTH_ERROR_UNLOCK, indent);
                return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
            }
            default: {
                throw new AssertionError("Unhandled SecretKeyType! (should not happen)");
            }

        }

        AuthenticationSignatureGenerator signatureGenerator;
        try {
            signatureGenerator = authKey.getAuthenticationSignatureGenerator(
                    hashAlgorithm, cryptoInput.getCryptoData());
        } catch (PgpGeneralException e) {
            log.add(LogType.MSG_AUTH_ERROR_NFC, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        signatureGenerator.update(challenge, 0, challenge.length);

        try {
            signature = signatureGenerator.getSignature();
        } catch (NfcSyncPGPContentSignerBuilder.NfcInteractionNeeded e) {
            // this secret key diverts to a OpenPGP card, thus requires user interaction
            log.add(LogType.MSG_AUTH_PENDING_NFC, indent);
            return new AuthenticationResult(log, RequiredInputParcel.createSecurityTokenAuthenticationOperation(
                    authKey.getRing().getMasterKeyId(), authKey.getKeyId(),
                    e.hashToSign, e.hashAlgo), cryptoInput);
        } catch (PGPException e) {
            log.add(LogType.MSG_AUTH_ERROR_SIG, indent);
            return new AuthenticationResult(AuthenticationResult.RESULT_ERROR, log);
        }

        opTime = System.currentTimeMillis() - startTime;
        Timber.d("Authentication operation duration : " + format("%.2f", opTime / 1000.0) + "s");

        log.add(LogType.MSG_AUTH_OK, indent);
        AuthenticationResult result = new AuthenticationResult(AuthenticationResult.RESULT_OK, log);

        result.setSignature(signature);
        result.mOperationTime = opTime;

        return result;
    }


}
