package org.sufficientlysecure.keychain.keyimport;

import android.os.Parcel;
import android.os.Parcelable;

/** This is a trivial wrapper around keyring bytes which implements Parcelable. It exists
 * for the sole purpose of keeping spongycastle and android imports in separate packages.
 */
public class ParcelableKeyRing implements Parcelable {

    final byte[] mBytes;
    final String mExpectedFingerprint;

    public ParcelableKeyRing(byte[] bytes) {
        mBytes = bytes;
        mExpectedFingerprint = null;
    }
    public ParcelableKeyRing(byte[] bytes, String expectedFingerprint) {
        mBytes = bytes;
        mExpectedFingerprint = expectedFingerprint;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mBytes);
        dest.writeString(mExpectedFingerprint);
    }

    public static final Creator<ParcelableKeyRing> CREATOR = new Creator<ParcelableKeyRing>() {
        public ParcelableKeyRing createFromParcel(final Parcel source) {
            byte[] bytes = source.createByteArray();
            String expectedFingerprint = source.readString();
            return new ParcelableKeyRing(bytes, expectedFingerprint);
        }

        public ParcelableKeyRing[] newArray(final int size) {
            return new ParcelableKeyRing[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public byte[] getBytes() {
        return mBytes;
    }

    public String getExpectedFingerprint() {
        return mExpectedFingerprint;
    }
}
