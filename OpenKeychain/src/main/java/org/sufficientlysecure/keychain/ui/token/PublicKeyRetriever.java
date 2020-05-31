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

package org.sufficientlysecure.keychain.ui.token;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.google.auto.value.AutoValue;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverClient;
import org.sufficientlysecure.keychain.keyimport.KeyserverClient.QueryFailedException;
import org.sufficientlysecure.keychain.keyimport.KeyserverClient.QueryNotFoundException;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.operations.results.GenericOperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class PublicKeyRetriever {
    private final Context context;
    private final List<byte[]> fingerprints;
    private final SecurityTokenInfo securityTokenInfo;


    PublicKeyRetriever(Context context, SecurityTokenInfo securityTokenInfo) {
        this.context = context;
        this.fingerprints = securityTokenInfo.getFingerprints();
        this.securityTokenInfo = securityTokenInfo;
    }

    public KeyRetrievalResult retrieveLocal() {
        KeyRepository keyRepository = KeyRepository.create(context);
        OperationLog log = new OperationLog();
        log.add(LogType.MSG_RET_LOCAL_START, 0);

        for (byte[] fingerprint : fingerprints) {
            long keyId = KeyFormattingUtils.getKeyIdFromFingerprint(fingerprint);
            if (keyId == 0L) {
                continue;
            }

            log.add(LogType.MSG_RET_LOCAL_SEARCH, 1, KeyFormattingUtils.convertKeyIdToHex(keyId));
            try {
                Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(keyId);
                if (masterKeyId == null) {
                    log.add(LogType.MSG_RET_LOCAL_NOT_FOUND, 2);
                    continue;
                }

                // TODO check fingerprint
                // if (!Arrays.equals(fingerprints, cachedPublicKeyRing.getFingerprint())) {
                //     log.add(LogType.MSG_RET_LOCAL_FP_MISMATCH, 1);
                //     return KeyRetrievalResult.createWithError(log);
                // } else {
                //     log.add(LogType.MSG_RET_LOCAL_FP_MATCH, 1);
                // }

                switch (keyRepository.getSecretKeyType(keyId)) {
                    case PASSPHRASE:
                    case PASSPHRASE_EMPTY: {
                        log.add(LogType.MSG_RET_LOCAL_SECRET, 1);
                        log.add(LogType.MSG_RET_LOCAL_OK, 1);
                        return KeyRetrievalResult.createWithMasterKeyIdAndSecretAvailable(log, masterKeyId);
                    }

                    case GNU_DUMMY:
                    case DIVERT_TO_CARD:
                    case UNAVAILABLE: {
                        log.add(LogType.MSG_RET_LOCAL_OK, 1);
                        return KeyRetrievalResult.createWithMasterKeyId(log, masterKeyId);
                    }

                    default: {
                        throw new IllegalStateException("Unhandled SecretKeyType!");
                    }
                }
            } catch (NotFoundException e) {
                log.add(LogType.MSG_RET_LOCAL_NOT_FOUND, 2);
            }
        }

        log.add(LogType.MSG_RET_LOCAL_NONE_FOUND, 1);
        return KeyRetrievalResult.createWithError(log);
    }

    public KeyRetrievalResult retrieveUri() {
        OperationLog log = new OperationLog();

        String tokenUri = securityTokenInfo.getUrl();

        try {
            log.add(LogType.MSG_RET_URI_START, 0);
            if (TextUtils.isEmpty(tokenUri)) {
                log.add(LogType.MSG_RET_URI_NULL, 1);
                return KeyRetrievalResult.createWithError(log);
            }

            log.add(LogType.MSG_RET_URI_FETCHING, 1, tokenUri);

            HttpUrl httpUrl = HttpUrl.parse(tokenUri);
            if (httpUrl == null) {
                log.add(LogType.MSG_RET_URI_ERROR_PARSE, 1);
                return KeyRetrievalResult.createWithError(log);
            }

            Call call = OkHttpClientFactory.getSimpleClient().newCall(new Builder().url(httpUrl).build());
            Response execute = call.execute();
            if (!execute.isSuccessful()) {
                log.add(LogType.MSG_RET_URI_ERROR_FETCH, 1);
            }

            IteratorWithIOThrow<UncachedKeyRing> uncachedKeyRingIterator = UncachedKeyRing.fromStream(
                    execute.body().byteStream());
            while (uncachedKeyRingIterator.hasNext()) {
                UncachedKeyRing keyRing = uncachedKeyRingIterator.next();
                log.add(LogType.MSG_RET_URI_TEST, 1, KeyFormattingUtils.convertKeyIdToHex(keyRing.getMasterKeyId()));
                if (keyRing.containsKeyWithAnyFingerprint(fingerprints)) {
                    log.add(LogType.MSG_RET_URI_OK, 1);
                    return KeyRetrievalResult.createWithKeyringdata(log, keyRing.getMasterKeyId(), keyRing.getEncoded());
                }
            }

            log.add(LogType.MSG_RET_URI_ERROR_NO_MATCH, 1);
        } catch (IOException e) {
            log.add(LogType.MSG_RET_URI_ERROR_FETCH, 1);
            Timber.e(e, "error retrieving key from uri");
        }

        return KeyRetrievalResult.createWithError(log);
    }

    public KeyRetrievalResult retrieveKeyserver() {
        OperationLog log = new OperationLog();

        HkpKeyserverAddress preferredKeyserver = Preferences.getPreferences(context).getPreferredKeyserver();
        ParcelableProxy parcelableProxy = Preferences.getPreferences(context).getParcelableProxy();

        HkpKeyserverClient keyserverClient = HkpKeyserverClient.fromHkpKeyserverAddress(preferredKeyserver);

        try {
            log.add(LogType.MSG_RET_KS_START, 0);

            String keyString = keyserverClient.get(
                    "0x" + KeyFormattingUtils.convertFingerprintToHex(fingerprints.get(0)), parcelableProxy);
            UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(keyString.getBytes());

            if (!keyRing.containsKeyWithAnyFingerprint(fingerprints)) {
                log.add(LogType.MSG_RET_KS_FP_MISMATCH, 1);
                return KeyRetrievalResult.createWithError(log);
            } else {
                log.add(LogType.MSG_RET_KS_FP_MATCH, 1);
            }

            log.add(LogType.MSG_RET_KS_OK, 1);
            return KeyRetrievalResult.createWithKeyringdata(log, keyRing.getMasterKeyId(), keyRing.getEncoded());
        } catch (QueryNotFoundException e) {
            log.add(LogType.MSG_RET_KS_ERROR_NOT_FOUND, 1);
        } catch (QueryFailedException | IOException | PgpGeneralException e) {
            log.add(LogType.MSG_RET_KS_ERROR, 1);
            Timber.e(e, "error retrieving key from keyserver");
        }

        return KeyRetrievalResult.createWithError(log);
    }

    public KeyRetrievalResult retrieveContentUri(Uri uri) {
        OperationLog log = new OperationLog();

        try {
            log.add(LogType.MSG_RET_CURI_START, 0);

            log.add(LogType.MSG_RET_CURI_OPEN, 1, uri.toString());
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                log.add(LogType.MSG_RET_CURI_ERROR_NOT_FOUND, 1);
                return KeyRetrievalResult.createWithError(log);
            }

            IteratorWithIOThrow<UncachedKeyRing> uncachedKeyRingIterator = UncachedKeyRing.fromStream(is);
            while (uncachedKeyRingIterator.hasNext()) {
                UncachedKeyRing keyRing = uncachedKeyRingIterator.next();
                log.add(LogType.MSG_RET_CURI_FOUND, 1, KeyFormattingUtils.convertKeyIdToHex(keyRing.getMasterKeyId()));
                if (keyRing.containsKeyWithAnyFingerprint(fingerprints)) {
                    log.add(LogType.MSG_RET_CURI_OK, 1);
                    return KeyRetrievalResult.createWithKeyringdata(log, keyRing.getMasterKeyId(), keyRing.getEncoded());
                } else {
                    log.add(LogType.MSG_RET_CURI_MISMATCH, 1);
                }
            }
            log.add(LogType.MSG_RET_CURI_ERROR_NO_MATCH, 1);
        } catch (IOException e) {
            Timber.e(e, "error reading keyring from file");
            log.add(LogType.MSG_RET_CURI_ERROR_IO, 1);
        }

        return KeyRetrievalResult.createWithError(log);
    }

    @AutoValue
    static abstract class KeyRetrievalResult {
        abstract GenericOperationResult getOperationResult();

        @Nullable
        abstract Long getMasterKeyId();
        @Nullable
        @SuppressWarnings("mutable")
        abstract byte[] getKeyData();
        abstract boolean isSecretKeyAvailable();

        boolean isSuccess() {
            return getMasterKeyId() != null || getKeyData() != null;
        }

        static KeyRetrievalResult createWithError(OperationLog log) {
            return new AutoValue_PublicKeyRetriever_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_ERROR, log),
                    null, null, false);
        }

        static KeyRetrievalResult createWithKeyringdata(OperationLog log, long masterKeyId, byte[] keyringData) {
            return new AutoValue_PublicKeyRetriever_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, keyringData, false);
        }

        static KeyRetrievalResult createWithMasterKeyIdAndSecretAvailable(OperationLog log, long masterKeyId) {
            return new AutoValue_PublicKeyRetriever_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, null, true);
        }

        static KeyRetrievalResult createWithMasterKeyId(OperationLog log, long masterKeyId) {
            return new AutoValue_PublicKeyRetriever_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, null, false);
        }
    }
}
