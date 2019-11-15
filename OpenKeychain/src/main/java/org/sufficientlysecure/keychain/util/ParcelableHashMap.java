/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import org.sufficientlysecure.keychain.KeychainApplication;


public class ParcelableHashMap <K extends Parcelable, V extends Parcelable> implements Parcelable {

    HashMap<K,V> mInner;

    public ParcelableHashMap(HashMap<K,V> inner) {
        mInner = inner;
    }

    protected ParcelableHashMap(@NonNull Parcel in) {
        mInner = new HashMap<>();
        ClassLoader loader = KeychainApplication.class.getClassLoader();

        int num = in.readInt();
        while (num-- > 0) {
            K key = in.readParcelable(loader);
            V val = in.readParcelable(loader);
            mInner.put(key, val);
        }
    }

    public HashMap<K,V> getMap() {
        return mInner;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mInner.size());
        for (HashMap.Entry<K,V> entry : mInner.entrySet()) {
            parcel.writeParcelable(entry.getKey(), 0);
            parcel.writeParcelable(entry.getValue(), 0);
        }
    }

    public static final Creator<ParcelableHashMap> CREATOR = new Creator<ParcelableHashMap>() {
        @Override
        public ParcelableHashMap createFromParcel(Parcel in) {
            return new ParcelableHashMap(in);
        }

        @Override
        public ParcelableHashMap[] newArray(int size) {
            return new ParcelableHashMap[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
