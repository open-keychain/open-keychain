/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class DeleteKeyringParcel implements Parcelable {

    public long[] mMasterKeyIds;
    public boolean mIsSecret;

    public DeleteKeyringParcel(long[] masterKeyIds, boolean isSecret) {
        mMasterKeyIds = masterKeyIds;
        mIsSecret = isSecret;
    }

    protected DeleteKeyringParcel(Parcel in) {
        mIsSecret = in.readByte() != 0x00;
        mMasterKeyIds = in.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mIsSecret ? 0x01 : 0x00));
        dest.writeLongArray(mMasterKeyIds);
    }

    public static final Parcelable.Creator<DeleteKeyringParcel> CREATOR = new Parcelable.Creator<DeleteKeyringParcel>() {
        @Override
        public DeleteKeyringParcel createFromParcel(Parcel in) {
            return new DeleteKeyringParcel(in);
        }

        @Override
        public DeleteKeyringParcel[] newArray(int size) {
            return new DeleteKeyringParcel[size];
        }
    };
}

