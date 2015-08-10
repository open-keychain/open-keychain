/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

public class MimeParsingParcel implements Parcelable {

    private Uri mInputUri;
    private Uri mOutputUri;

    public MimeParsingParcel() {
    }

    public MimeParsingParcel(Uri inputUri, Uri outputUri) {
        mInputUri = inputUri;
        mOutputUri = outputUri;
    }

    MimeParsingParcel(Parcel source) {
        // we do all of those here, so the PgpSignEncryptInput class doesn't have to be parcelable
        mInputUri = source.readParcelable(getClass().getClassLoader());
        mOutputUri = source.readParcelable(getClass().getClassLoader());
    }

    public Uri getInputUri() {
        return mInputUri;
    }

    public Uri getOutputUri() {
        return mOutputUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mInputUri, 0);
        dest.writeParcelable(mOutputUri, 0);
    }

    public static final Creator<MimeParsingParcel> CREATOR = new Creator<MimeParsingParcel>() {
        public MimeParsingParcel createFromParcel(final Parcel source) {
            return new MimeParsingParcel(source);
        }

        public MimeParsingParcel[] newArray(final int size) {
            return new MimeParsingParcel[size];
        }
    };

}

