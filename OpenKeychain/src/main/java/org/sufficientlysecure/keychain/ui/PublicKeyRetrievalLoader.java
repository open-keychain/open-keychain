/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui;


import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.auto.value.AutoValue;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverClient;
import org.sufficientlysecure.keychain.keyimport.KeyserverClient.QueryFailedException;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.KeyRetrievalResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;


public abstract class PublicKeyRetrievalLoader extends AsyncTaskLoader<KeyRetrievalResult> {
    private static final long MIN_OPERATION_TIME_MILLIS = 1500;


    private KeyRetrievalResult cachedResult;


    public PublicKeyRetrievalLoader(Context context) {
        super(context);
    }

    @Override
    protected KeyRetrievalResult onLoadInBackground() {
        long startTime = SystemClock.elapsedRealtime();

        KeyRetrievalResult keyRetrievalResult = super.onLoadInBackground();

        try {
            long elapsedTime = startTime - SystemClock.elapsedRealtime();
            if (elapsedTime < MIN_OPERATION_TIME_MILLIS) {
                Thread.sleep(MIN_OPERATION_TIME_MILLIS - elapsedTime);
            }
        } catch (InterruptedException e) {
            // nvm
        }

        return keyRetrievalResult;
    }

    public static class LocalKeyLookupLoader extends PublicKeyRetrievalLoader {
        private final KeyRepository keyRepository;
        private final byte[][] fingerprints;

        public LocalKeyLookupLoader(Context context, byte[][] fingerprints) {
            super(context);

            this.fingerprints = fingerprints;
            this.keyRepository = KeyRepository.createDatabaseInteractor(context);
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            try {
                // TODO check other fingerprints
                long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(fingerprints[0]);
                CachedPublicKeyRing cachedPublicKeyRing = keyRepository.getCachedPublicKeyRing(masterKeyId);
                switch (cachedPublicKeyRing.getSecretKeyType(masterKeyId)) {
                    case PASSPHRASE:
                    case PASSPHRASE_EMPTY: {
                        return KeyRetrievalResult.createWithMasterKeyIdAndSecretAvailable(masterKeyId);
                    }

                    case GNU_DUMMY:
                    case DIVERT_TO_CARD:
                    case UNAVAILABLE: {
                        return KeyRetrievalResult.createWithMasterKeyId(masterKeyId);
                    }

                    default: {
                        throw new IllegalStateException("Unhandled SecretKeyType!");
                    }
                }
            } catch (NotFoundException e) {
                return KeyRetrievalResult.createWithError();
            }
        }
    }

    public static class UriKeyRetrievalLoader extends PublicKeyRetrievalLoader {
        byte[][] fingerprints;
        String yubikeyUri;

        public UriKeyRetrievalLoader(Context context, String yubikeyUri, byte[][] fingerprints) {
            super(context);

            this.yubikeyUri = yubikeyUri;
            this.fingerprints = fingerprints;
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            try {
                Response execute =
                        OkHttpClientFactory.getSimpleClient().newCall(new Builder().url(yubikeyUri).build()).execute();
                if (execute.isSuccessful()) {
                    UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(execute.body().bytes());
                    if (Arrays.equals(fingerprints[0], keyRing.getFingerprint())) {
                        return KeyRetrievalResult.createWithKeyringdata(keyRing.getMasterKeyId(), keyRing.getEncoded());
                    }
                }
            } catch (IOException | PgpGeneralException e) {
                Log.e(Constants.TAG, "error retrieving key from uri", e);
            }

            return KeyRetrievalResult.createWithError();
        }
    }

    public static class KeyserverRetrievalLoader extends PublicKeyRetrievalLoader {
        byte[] fingerprint;

        public KeyserverRetrievalLoader(Context context, byte[] fingerprint) {
            super(context);

            this.fingerprint = fingerprint;
        }

        @Override
        public KeyRetrievalResult loadInBackground() {
            HkpKeyserverAddress preferredKeyserver = Preferences.getPreferences(getContext()).getPreferredKeyserver();
            ParcelableProxy parcelableProxy = Preferences.getPreferences(getContext()).getParcelableProxy();

            HkpKeyserverClient keyserverClient = HkpKeyserverClient.fromHkpKeyserverAddress(preferredKeyserver);

            try {
                String keyString =
                        keyserverClient.get("0x" + KeyFormattingUtils.convertFingerprintToHex(fingerprint), parcelableProxy);
                UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(keyString.getBytes());

                return KeyRetrievalResult.createWithKeyringdata(keyRing.getMasterKeyId(), keyRing.getEncoded());
            } catch (QueryFailedException | IOException | PgpGeneralException e) {
                Log.e(Constants.TAG, "error retrieving key from keyserver", e);
            }

            return KeyRetrievalResult.createWithError();
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
        @Nullable
        abstract Long getMasterKeyId();
        @Nullable
        abstract byte[] getKeyData();
        abstract boolean isSecretKeyAvailable();

        boolean isSuccess() {
            return getMasterKeyId() != null || getKeyData() != null;
        }

        static KeyRetrievalResult createWithError() {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(null, null, false);
        }

        static KeyRetrievalResult createWithKeyringdata(long masterKeyId, byte[] keyringData) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(masterKeyId, keyringData, false);
        }

        static KeyRetrievalResult createWithMasterKeyIdAndSecretAvailable(long masterKeyId) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(masterKeyId, null, true);
        }

        static KeyRetrievalResult createWithMasterKeyId(long masterKeyId) {
            return new AutoValue_PublicKeyRetrievalLoader_KeyRetrievalResult(masterKeyId, null, false);
        }
    }
}
