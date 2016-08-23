/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class CreateSecretKeyRingCacheParcel implements Parcelable {

    public final String mFileName;

    public CreateSecretKeyRingCacheParcel(String fileName) {
        mFileName = fileName;
    }

    protected CreateSecretKeyRingCacheParcel(Parcel in) {
        mFileName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFileName);
    }

    public static final Creator<CreateSecretKeyRingCacheParcel> CREATOR = new Creator<CreateSecretKeyRingCacheParcel>() {
        @Override
        public CreateSecretKeyRingCacheParcel createFromParcel(Parcel in) {
            return new CreateSecretKeyRingCacheParcel(in);
        }

        @Override
        public CreateSecretKeyRingCacheParcel[] newArray(int size) {
            return new CreateSecretKeyRingCacheParcel[size];
        }
    };

}
