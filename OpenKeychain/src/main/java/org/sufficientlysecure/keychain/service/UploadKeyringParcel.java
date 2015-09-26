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


import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;


public class UploadKeyringParcel implements Parcelable {
    public String mKeyserver;
    public Uri mCanonicalizedPublicKeyringUri;

    public UploadKeyringParcel(String keyserver, Uri keyringUri) {
        mKeyserver = keyserver;
        mCanonicalizedPublicKeyringUri = keyringUri;
    }

    protected UploadKeyringParcel(Parcel in) {
        mKeyserver = in.readString();
        mCanonicalizedPublicKeyringUri = (Uri) in.readValue(Uri.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKeyserver);
        dest.writeValue(mCanonicalizedPublicKeyringUri);
    }

    public static final Creator<UploadKeyringParcel> CREATOR = new Creator<UploadKeyringParcel>() {
        @Override
        public UploadKeyringParcel createFromParcel(Parcel in) {
            return new UploadKeyringParcel(in);
        }

        @Override
        public UploadKeyringParcel[] newArray(int size) {
            return new UploadKeyringParcel[size];
        }
    };
}