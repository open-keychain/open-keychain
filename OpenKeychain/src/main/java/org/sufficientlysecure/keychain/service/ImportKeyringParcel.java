/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;

import java.util.ArrayList;

public class ImportKeyringParcel extends ImportExportParcel {
    // if null, keys are expected to be read from a cache file in ImportExportOperations
    public ArrayList<ParcelableKeyRing> mKeyList;
    public String mKeyserver; // must be set if keys are to be imported from a keyserver

    public ImportKeyringParcel (ArrayList<ParcelableKeyRing> keyList, String keyserver) {
        mKeyList = keyList;
        mKeyserver = keyserver;
    }

    protected ImportKeyringParcel(Parcel in) {
        if (in.readByte() == 0x01) {
            mKeyList = new ArrayList<>();
            in.readList(mKeyList, ParcelableKeyRing.class.getClassLoader());
        } else {
            mKeyList = null;
        }
        mKeyserver = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mKeyList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mKeyList);
        }
        dest.writeString(mKeyserver);
    }

    public static final Parcelable.Creator<ImportKeyringParcel> CREATOR = new Parcelable.Creator<ImportKeyringParcel>() {
        @Override
        public ImportKeyringParcel createFromParcel(Parcel in) {
            return new ImportKeyringParcel(in);
        }

        @Override
        public ImportKeyringParcel[] newArray(int size) {
            return new ImportKeyringParcel[size];
        }
    };
}