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

package org.sufficientlysecure.keychain.service.input;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcelable;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.parcel.ParcelAdapter;
import org.sufficientlysecure.keychain.util.ByteMapParcelAdapter;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Passphrase;

@AutoValue
public abstract class CryptoInputParcel implements Parcelable {
    @Nullable
    public abstract Date getSignatureTime();
    @Nullable
    public abstract Passphrase getPassphrase();
    @Nullable
    public abstract Long getPassphraseSubkey();
    public abstract boolean isCachePassphrase();

    public boolean hasPassphraseForSubkey(long subKeyId) {
        return getPassphrase() != null && (getPassphraseSubkey() == null || getPassphraseSubkey() == subKeyId);
    }

    public boolean hasPassphraseForSymmetric() {
        return getPassphrase() != null && getPassphraseSubkey() == null;
    }

    // used to supply an explicit proxy to operations that require it
    // this is not final so it can be added to an existing CryptoInputParcel
    // (e.g) CertifyOperation with upload might require both passphrase and orbot to be enabled
    @Nullable
    public abstract ParcelableProxy getParcelableProxy();

    // this map contains both decrypted session keys and signed hashes to be
    // used in the crypto operation described by this parcel.
    @ParcelAdapter(ByteMapParcelAdapter.class)
    public abstract Map<ByteBuffer, byte[]> getCryptoData();


    public static CryptoInputParcel createCryptoInputParcel() {
        return new AutoValue_CryptoInputParcel(null, null, null, true, null, Collections.emptyMap());
    }

    public static CryptoInputParcel createCryptoInputParcel(Date signatureTime, Passphrase passphrase) {
        if (signatureTime == null) {
            signatureTime = new Date();
        }
        return new AutoValue_CryptoInputParcel(signatureTime, passphrase, null, true, null,
                Collections.emptyMap());
    }

    public static CryptoInputParcel createCryptoInputParcel(Passphrase passphrase) {
        return new AutoValue_CryptoInputParcel(null, passphrase, null, true, null, Collections.emptyMap());
    }

    public static CryptoInputParcel createCryptoInputParcel(Date signatureTime) {
        if (signatureTime == null) {
            signatureTime = new Date();
        }
        return new AutoValue_CryptoInputParcel(signatureTime, null, null, true, null,
                Collections.emptyMap());
    }

    public static CryptoInputParcel createCryptoInputParcel(ParcelableProxy parcelableProxy) {
        return new AutoValue_CryptoInputParcel(null, null, null, true, parcelableProxy, new HashMap<ByteBuffer,byte[]>());
    }

    public static CryptoInputParcel createCryptoInputParcel(Date signatureTime, boolean cachePassphrase) {
        if (signatureTime == null) {
            signatureTime = new Date();
        }
        return new AutoValue_CryptoInputParcel(signatureTime, null, null, cachePassphrase, null,
                new HashMap<ByteBuffer,byte[]>());
    }

    public static CryptoInputParcel createCryptoInputParcel(boolean cachePassphrase) {
        return new AutoValue_CryptoInputParcel(null, null, null, cachePassphrase, null, new HashMap<ByteBuffer,byte[]>());
    }

    // TODO get rid of this!
    @CheckResult
    public CryptoInputParcel withCryptoData(byte[] hash, byte[] signedHash) {
        Map<ByteBuffer,byte[]> newCryptoData = new HashMap<>(getCryptoData());
        newCryptoData.put(ByteBuffer.wrap(hash), signedHash);
        newCryptoData = Collections.unmodifiableMap(newCryptoData);

        return new AutoValue_CryptoInputParcel(getSignatureTime(), getPassphrase(), getPassphraseSubkey(),
                isCachePassphrase(), getParcelableProxy(), newCryptoData);
    }

    @CheckResult
    public CryptoInputParcel withCryptoData(Map<ByteBuffer, byte[]> cachedSessionKeys) {
        Map<ByteBuffer,byte[]> newCryptoData = new HashMap<>(getCryptoData());
        newCryptoData.putAll(cachedSessionKeys);
        newCryptoData = Collections.unmodifiableMap(newCryptoData);

        return new AutoValue_CryptoInputParcel(getSignatureTime(), getPassphrase(), getPassphraseSubkey(),
                isCachePassphrase(), getParcelableProxy(), newCryptoData);
    }


    @CheckResult
    public CryptoInputParcel withPassphrase(Passphrase passphrase, Long subKeyId) {
        return new AutoValue_CryptoInputParcel(getSignatureTime(), passphrase, subKeyId, isCachePassphrase(),
                getParcelableProxy(), getCryptoData());
    }

    @CheckResult
    public CryptoInputParcel withNoCachePassphrase() {
        return new AutoValue_CryptoInputParcel(getSignatureTime(), getPassphrase(), getPassphraseSubkey(),
                false, getParcelableProxy(), getCryptoData());
    }

    @CheckResult
    public CryptoInputParcel withSignatureTime(Date signatureTime) {
        return new AutoValue_CryptoInputParcel(signatureTime, getPassphrase(), getPassphraseSubkey(),
                isCachePassphrase(), getParcelableProxy(), getCryptoData());
    }

    @CheckResult
    public CryptoInputParcel withParcelableProxy(ParcelableProxy parcelableProxy) {
        return new AutoValue_CryptoInputParcel(getSignatureTime(), getPassphrase(), getPassphraseSubkey(),
                isCachePassphrase(), parcelableProxy, getCryptoData());
    }
}
