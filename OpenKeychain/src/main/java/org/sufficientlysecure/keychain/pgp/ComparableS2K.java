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


import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

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
public class ComparableS2K implements Parcelable {

    private final int encryptionAlgorithm;
    private final int s2kType;
    private final int s2kHashAlgo;
    private final long s2kItCount;
    private final byte[] s2kIV;

    Integer cachedHashCode;

    public ComparableS2K(int encryptionAlgorithm, S2K s2k) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.s2kType = s2k.getType();
        this.s2kHashAlgo = s2k.getHashAlgorithm();
        this.s2kItCount = s2k.getIterationCount();
        this.s2kIV = s2k.getIV();
    }

    protected ComparableS2K(Parcel in) {
        encryptionAlgorithm = in.readInt();
        s2kType = in.readInt();
        s2kHashAlgo = in.readInt();
        s2kItCount = in.readLong();
        s2kIV = in.createByteArray();
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == null) {
            cachedHashCode = encryptionAlgorithm;
            cachedHashCode = 31 * cachedHashCode + s2kType;
            cachedHashCode = 31 * cachedHashCode + s2kHashAlgo;
            cachedHashCode = 31 * cachedHashCode + (int) (s2kItCount ^ (s2kItCount >>> 32));
            cachedHashCode = 31 * cachedHashCode + Arrays.hashCode(s2kIV);
        }

        return cachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        boolean isComparableS2K = o instanceof ComparableS2K;
        if (!isComparableS2K) {
            return false;
        }
        ComparableS2K other = (ComparableS2K) o;
        return encryptionAlgorithm == other.encryptionAlgorithm
                && s2kType == other.s2kType
                && s2kHashAlgo == other.s2kHashAlgo
                && s2kItCount == other.s2kItCount
                && Arrays.equals(s2kIV, other.s2kIV);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(encryptionAlgorithm);
        dest.writeInt(s2kType);
        dest.writeInt(s2kHashAlgo);
        dest.writeLong(s2kItCount);
        dest.writeByteArray(s2kIV);
    }

    public static final Creator<ComparableS2K> CREATOR = new Creator<ComparableS2K>() {
        @Override
        public ComparableS2K createFromParcel(Parcel in) {
            return new ComparableS2K(in);
        }

        @Override
        public ComparableS2K[] newArray(int size) {
            return new ComparableS2K[size];
        }
    };

}
