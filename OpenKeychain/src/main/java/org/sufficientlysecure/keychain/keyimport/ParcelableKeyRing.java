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
