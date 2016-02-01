package org.sufficientlysecure.keychain.pgp;


import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

import org.spongycastle.bcpg.S2K;


public class ComparableS2K implements Parcelable {

    int encryptionAlgorithm;
    int s2kType;
    int s2kHashAlgo;
    long s2kItCount;
    byte[] s2kIV;

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
            cachedHashCode *= 31 * s2kType;
            cachedHashCode *= 31 * s2kHashAlgo;
            cachedHashCode *= (int) (31 * s2kItCount);
            cachedHashCode *= 31 * Arrays.hashCode(s2kIV);
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
