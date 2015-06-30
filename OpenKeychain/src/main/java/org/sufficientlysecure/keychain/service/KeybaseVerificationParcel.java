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

public class KeybaseVerificationParcel implements Parcelable {

    public String mKeybaseProof;
    public String mRequiredFingerprint;

    public KeybaseVerificationParcel(String keybaseProof, String requiredFingerprint) {
        mKeybaseProof = keybaseProof;
        mRequiredFingerprint = requiredFingerprint;
    }

    protected KeybaseVerificationParcel(Parcel in) {
        mKeybaseProof = in.readString();
        mRequiredFingerprint = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKeybaseProof);
        dest.writeString(mRequiredFingerprint);
    }

    public static final Parcelable.Creator<KeybaseVerificationParcel> CREATOR = new Parcelable.Creator<KeybaseVerificationParcel>() {
        @Override
        public KeybaseVerificationParcel createFromParcel(Parcel in) {
            return new KeybaseVerificationParcel(in);
        }

        @Override
        public KeybaseVerificationParcel[] newArray(int size) {
            return new KeybaseVerificationParcel[size];
        }
    };
}