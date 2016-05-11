/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.pgp;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;


public class PgpSignEncryptInputParcel implements Parcelable {

    private PgpSignEncryptData data;

    private Uri mInputUri;
    private Uri mOutputUri;
    private byte[] mInputBytes;

    public PgpSignEncryptInputParcel(PgpSignEncryptData data) {
        this.data = data;
    }

    PgpSignEncryptInputParcel(Parcel source) {
        mInputUri = source.readParcelable(getClass().getClassLoader());
        mOutputUri = source.readParcelable(getClass().getClassLoader());
        mInputBytes = source.createByteArray();

        data = source.readParcelable(getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mInputUri, 0);
        dest.writeParcelable(mOutputUri, 0);
        dest.writeByteArray(mInputBytes);

        data.writeToParcel(dest, 0);
    }

    public void setInputBytes(byte[] inputBytes) {
        this.mInputBytes = inputBytes;
    }

    byte[] getInputBytes() {
        return mInputBytes;
    }

    public PgpSignEncryptInputParcel setInputUri(Uri uri) {
        mInputUri = uri;
        return this;
    }

    Uri getInputUri() {
        return mInputUri;
    }

    public PgpSignEncryptInputParcel setOutputUri(Uri uri) {
        mOutputUri = uri;
        return this;
    }

    Uri getOutputUri() {
        return mOutputUri;
    }

    public void setData(PgpSignEncryptData data) {
        this.data = data;
    }

    public PgpSignEncryptData getData() {
        return data;
    }

    public static final Creator<PgpSignEncryptInputParcel> CREATOR = new Creator<PgpSignEncryptInputParcel>() {
        public PgpSignEncryptInputParcel createFromParcel(final Parcel source) {
            return new PgpSignEncryptInputParcel(source);
        }

        public PgpSignEncryptInputParcel[] newArray(final int size) {
            return new PgpSignEncryptInputParcel[size];
        }
    };

}

