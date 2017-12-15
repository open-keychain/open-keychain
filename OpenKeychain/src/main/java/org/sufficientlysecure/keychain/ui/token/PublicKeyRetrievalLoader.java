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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

import com.google.auto.value.AutoValue;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.sufficientlysecure.keychain.Constants;
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
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.token.PublicKeyRetrievalLoader.KeyRetrievalResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;


public abstract class PublicKeyRetrievalLoader extends AsyncTaskLoader<KeyRetrievalResult> {
    private static final long MIN_OPERATION_TIME_MILLIS = 1500;


    private KeyRetrievalResult cachedResult;

    protected final List<byte[]> fingerprints;


    private PublicKeyRetrievalLoader(Context context, List<byte[]> fingerprints) {
        super(context);

        this.fingerprints = fingerprints;
    }

    @Override
    protected KeyRetrievalResult onLoadInBackground() {
        long startTime = SystemClock.elapsedRealtime();

        KeyRetrievalResult keyRetrievalResult = super.onLoadInBackground();

        try {
            long elapsedTime = SystemClock.elapsedRealtime() - startTime;
            if (elapsedTime < MIN_OPERATION_TIME_MILLIS) {
                Thread.sleep(MIN_OPERATION_TIME_MILLIS - elapsedTime);
            }
        } catch (InterruptedException e) {
            // nvm
        }

        return keyRetrievalResult;
    }

    static class LocalKeyLookupLoader extends PublicKeyRetrievalLoader {
        private final KeyRepository keyRepository;

        LocalKeyLookupLoader(Context context, List<byte[]> fingerprints) {
            super(context, fingerprints);

            this.keyRepository = KeyRepository.create(context);
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_RET_LOCAL_START, 0);

            for (byte[] fingerprint : fingerprints) {
                long keyId = KeyFormattingUtils.getKeyIdFromFingerprint(fingerprint);
                if (keyId == 0L) {
                    continue;
                }

                log.add(LogType.MSG_RET_LOCAL_SEARCH, 1, KeyFormattingUtils.convertKeyIdToHex(keyId));
                try {
                    CachedPublicKeyRing cachedPublicKeyRing = keyRepository.getCachedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(keyId)
                    );

                    long masterKeyId = cachedPublicKeyRing.getMasterKeyId();
                    // TODO check fingerprint
                    // if (!Arrays.equals(fingerprints, cachedPublicKeyRing.getFingerprint())) {
                    //     log.add(LogType.MSG_RET_LOCAL_FP_MISMATCH, 1);
                    //     return KeyRetrievalResult.createWithError(log);
                    // } else {
                    //     log.add(LogType.MSG_RET_LOCAL_FP_MATCH, 1);
                    // }

                    switch (cachedPublicKeyRing.getSecretKeyType(keyId)) {
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
                } catch (PgpKeyNotFoundException | NotFoundException e) {
                    log.add(LogType.MSG_RET_LOCAL_NOT_FOUND, 2);
                }
            }

            log.add(LogType.MSG_RET_LOCAL_NONE_FOUND, 1);
            return KeyRetrievalResult.createWithError(log);
        }
    }

    static class UriKeyRetrievalLoader extends PublicKeyRetrievalLoader {
        private final String tokenUri;

        UriKeyRetrievalLoader(Context context, String tokenUri, List<byte[]> fingerprints) {
            super(context, fingerprints);

            this.tokenUri = tokenUri;
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            OperationLog log = new OperationLog();

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
                Log.e(Constants.TAG, "error retrieving key from uri", e);
            }

            return KeyRetrievalResult.createWithError(log);
        }
    }

    static class KeyserverRetrievalLoader extends PublicKeyRetrievalLoader {
        KeyserverRetrievalLoader(Context context, List<byte[]> fingerprints) {
            super(context, fingerprints);
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            OperationLog log = new OperationLog();

            HkpKeyserverAddress preferredKeyserver = Preferences.getPreferences(getContext()).getPreferredKeyserver();
            ParcelableProxy parcelableProxy = Preferences.getPreferences(getContext()).getParcelableProxy();

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
                Log.e(Constants.TAG, "error retrieving key from keyserver", e);
            }

            return KeyRetrievalResult.createWithError(log);
        }
    }

    static class ContentUriRetrievalLoader extends PublicKeyRetrievalLoader {
        private final ContentResolver contentResolver;
        private final Uri uri;

        ContentUriRetrievalLoader(Context context, List<byte[]> fingerprints, Uri uri) {
            super(context, fingerprints);

            this.uri = uri;
            this.contentResolver = context.getContentResolver();
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            OperationLog log = new OperationLog();

            try {
                log.add(LogType.MSG_RET_CURI_START, 0);

                log.add(LogType.MSG_RET_CURI_OPEN, 1, uri.toString());
                InputStream is = contentResolver.openInputStream(uri);
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
                Log.e(Constants.TAG, "error reading keyring from file", e);
                log.add(LogType.MSG_RET_CURI_ERROR_IO, 1);
            }

            return KeyRetrievalResult.createWithError(log);
        }
    }

    @Override
    public void deliverResult(KeyRetrievalResult result) {
        cachedResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cachedResult != null) {
            deliverResult(cachedResult);
        }

        if (takeContentChanged() || cachedResult == null) {
            forceLoad();
        }
    }

    @AutoValue
    static abstract class KeyRetrievalResult {
        abstract GenericOperationResult getOperationResult();

        @Nullable
        abstract Long getMasterKeyId();
        @Nullable
        abstract byte[] getKeyData();
        abstract boolean isSecretKeyAvailable();

        boolean isSuccess() {
            return getMasterKeyId() != null || getKeyData() != null;
        }

        static KeyRetrievalResult createWithError(OperationLog log) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_ERROR, log),
                    null, null, false);
        }

        static KeyRetrievalResult createWithKeyringdata(OperationLog log, long masterKeyId, byte[] keyringData) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, keyringData, false);
        }

        static KeyRetrievalResult createWithMasterKeyIdAndSecretAvailable(OperationLog log, long masterKeyId) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, null, true);
        }

        static KeyRetrievalResult createWithMasterKeyId(OperationLog log, long masterKeyId) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(
                    new GenericOperationResult(GenericOperationResult.RESULT_OK, log),
                    masterKeyId, null, false);
        }
    }
}
