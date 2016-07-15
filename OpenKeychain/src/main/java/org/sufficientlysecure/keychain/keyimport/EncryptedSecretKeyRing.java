/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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
import android.util.Pair;

import java.util.ArrayList;

/**
 * Parcelable representation of an encrypted secret keyring block as raw data,
 * with associated fields for convenience.
 */
public class EncryptedSecretKeyRing implements Parcelable{

    public final byte[] mBytes;
    public final long mMasterKeyId;
    public final ArrayList<Pair<Long, Integer>> mSubKeyIdsAndType;

    public EncryptedSecretKeyRing(byte[] bytes, long keyId,
                                  ArrayList<Pair<Long, Integer>> subKeysAndType) {
        mBytes = bytes;
        mMasterKeyId = keyId;
        mSubKeyIdsAndType = (subKeysAndType == null) ? new ArrayList<Pair<Long, Integer>>()
                                                     : subKeysAndType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByteArray(mBytes);
        parcel.writeLong(mMasterKeyId);
        parcel.writeInt(mSubKeyIdsAndType.size());
        for (Pair<Long, Integer> idTypePair : mSubKeyIdsAndType) {
            parcel.writeLong(idTypePair.first);
            parcel.writeInt(idTypePair.second);
        }
    }

    private EncryptedSecretKeyRing(Parcel source) {
        mBytes = source.createByteArray();
        mMasterKeyId = source.readLong();
        mSubKeyIdsAndType = new ArrayList<>();
        int arrayCount = source.readInt();
        for (int i = 0; i < arrayCount; i++) {
            mSubKeyIdsAndType.add(new Pair<>(source.readLong(),
                                            source.readInt()));
        }
    }

    public static final Creator<EncryptedSecretKeyRing> CREATOR =
            new Creator<EncryptedSecretKeyRing>() {
        public EncryptedSecretKeyRing createFromParcel(final Parcel source) {
            return new EncryptedSecretKeyRing(source);
        }

        public EncryptedSecretKeyRing[] newArray(final int size) {
            return new EncryptedSecretKeyRing[size];
        }
    };
}
