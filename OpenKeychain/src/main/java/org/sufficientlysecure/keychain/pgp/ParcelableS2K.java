/*
 * Copyright (C) 2016 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.pgp;


import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import org.bouncycastle.bcpg.S2K;


/** This is an immutable and parcelable class which stores the full s2k parametrization
 * of an encrypted secret key, i.e. all fields of the {@link S2K} class (type, hash algo,
 * iteration count, iv) plus the encryptionAlgorithm. This class is intended to be used
 * as key in a HashMap for session key caching purposes, and overrides the
 * {@link #hashCode} and {@link #equals} methods in a suitable way.
 *
 * Note that although it is a rather unlikely scenario that secret keys of the same key
 * are encrypted with different ciphers, the encryption algorithm still determines the
 * length of the specific session key and thus needs to be considered for purposes of
 * session key caching.
 *
 * @see org.bouncycastle.bcpg.S2K
 */
@AutoValue
public abstract class ParcelableS2K implements Parcelable {
    abstract int getEncryptionAlgorithm();
    abstract int getS2kType();
    abstract int getS2kHashAlgo();
    abstract long getS2kItCount();
    @SuppressWarnings("mutable")
    abstract byte[] getS2kIV();

    @Memoized
    @Override
    public abstract int hashCode();

    public static ParcelableS2K fromS2K(int encryptionAlgorithm, S2K s2k) {
        return new AutoValue_ParcelableS2K(encryptionAlgorithm,
                s2k.getType(), s2k.getHashAlgorithm(), s2k.getIterationCount(), s2k.getIV());
    }
}
