/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.keyimport;

import android.os.Parcel;
import android.os.Parcelable;

/** This class is a parcelable representation of either a keyring as raw data,
 * or a (unique) reference to one as a fingerprint, keyid, or keybase name.
 */
public class ParcelableKeyRing implements Parcelable {

    public final byte[] mBytes;

    // dual role!
    public final String mExpectedFingerprint;
    public final String mKeyIdHex;
    public final String mKeybaseName;
    public final String mFbUsername;

    public ParcelableKeyRing(byte[] bytes) {
        this(null, bytes, false);
    }

    /**
     * @param disAmbiguator useless parameter intended to distinguish this overloaded constructor
     *                      for when null is passed as first two arguments
     */
    public ParcelableKeyRing(String expectedFingerprint, byte[] bytes, boolean disAmbiguator) {
        mBytes = bytes;
        mExpectedFingerprint = expectedFingerprint;
        mKeyIdHex = null;
        mKeybaseName = null;
        mFbUsername = null;
    }

    public ParcelableKeyRing(String expectedFingerprint, String keyIdHex) {
        mBytes = null;
        mExpectedFingerprint = expectedFingerprint;
        mKeyIdHex = keyIdHex;
        mKeybaseName = null;
        mFbUsername = null;
    }

    public ParcelableKeyRing(String expectedFingerprint, String keyIdHex, String keybaseName,
                             String fbUsername) {
        mBytes = null;
        mExpectedFingerprint = expectedFingerprint;
        mKeyIdHex = keyIdHex;
        mKeybaseName = keybaseName;
        mFbUsername = fbUsername;
    }

    private ParcelableKeyRing(Parcel source) {
        mBytes = source.createByteArray();

        mExpectedFingerprint = source.readString();
        mKeyIdHex = source.readString();
        mKeybaseName = source.readString();
        mFbUsername = source.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mBytes);
        dest.writeString(mExpectedFingerprint);
        dest.writeString(mKeyIdHex);
        dest.writeString(mKeybaseName);
        dest.writeString(mFbUsername);
    }

    public static final Creator<ParcelableKeyRing> CREATOR = new Creator<ParcelableKeyRing>() {
        public ParcelableKeyRing createFromParcel(final Parcel source) {
            return new ParcelableKeyRing(source);
        }

        public ParcelableKeyRing[] newArray(final int size) {
            return new ParcelableKeyRing[size];
        }
    };

    public int describeContents() {
        return 0;
    }

}
