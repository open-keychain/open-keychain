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

import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;


public class InputDataParcel implements Parcelable {

    private Uri mInputUri;

    private PgpDecryptVerifyInputParcel mDecryptInput;
    private boolean mMimeDecode = true; // TODO default to false

    public InputDataParcel(Uri inputUri, PgpDecryptVerifyInputParcel decryptInput) {
        mInputUri = inputUri;
        mDecryptInput = decryptInput;
    }

    InputDataParcel(Parcel source) {
        // we do all of those here, so the PgpSignEncryptInput class doesn't have to be parcelable
        mInputUri = source.readParcelable(getClass().getClassLoader());
        mDecryptInput = source.readParcelable(getClass().getClassLoader());
        mMimeDecode = source.readInt() != 0;
    }

    public Uri getInputUri() {
        return mInputUri;
    }

    public PgpDecryptVerifyInputParcel getDecryptInput() {
        return mDecryptInput;
    }

    public boolean getMimeDecode() {
        return mMimeDecode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mInputUri, 0);
        dest.writeParcelable(mDecryptInput, 0);
        dest.writeInt(mMimeDecode ? 1 : 0);
    }

    public static final Creator<InputDataParcel> CREATOR = new Creator<InputDataParcel>() {
        public InputDataParcel createFromParcel(final Parcel source) {
            return new InputDataParcel(source);
        }

        public InputDataParcel[] newArray(final int size) {
            return new InputDataParcel[size];
        }
    };

}

