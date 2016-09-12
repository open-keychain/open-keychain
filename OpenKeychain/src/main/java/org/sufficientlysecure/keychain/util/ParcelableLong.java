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

package org.sufficientlysecure.keychain.util;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableLong implements Parcelable{
    public final long mValue;

    public ParcelableLong(long value) {
        mValue = value;
    }

    public ParcelableLong(Parcel parcel) {
        mValue = parcel.readLong();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParcelableLong that = (ParcelableLong) o;

        return mValue == that.mValue;
    }

    @Override
    public int hashCode() {
        return (int) (mValue ^ (mValue >>> 32));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mValue);
    }

    public static final Creator<ParcelableLong> CREATOR = new Creator<ParcelableLong>() {
        @Override
        public ParcelableLong createFromParcel(Parcel in) {
            return new ParcelableLong(in);
        }

        @Override
        public ParcelableLong[] newArray(int size) {
            return new ParcelableLong[size];
        }
    };
}
