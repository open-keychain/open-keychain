/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

package org.sufficientlysecure.keychain.ssh;

import android.os.Parcel;

import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;

/**
 * AuthenticationResult holds the result of a AuthenticationOperation
 */
public class AuthenticationResult extends InputPendingResult {

    public static final int RESULT_KEY_DISALLOWED = RESULT_ERROR + 32;

    byte[] mSignature;
    public long mOperationTime;

    public void setSignature(byte[] signature) {
        mSignature = signature;
    }

    public byte[] getSignature() {
        return mSignature;
    }

    public AuthenticationResult(int result, OperationLog log) {
        super(result, log);
    }

    public AuthenticationResult(OperationLog log, RequiredInputParcel requiredInput,
                                CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInput, cryptoInputParcel);
    }

    public AuthenticationResult(Parcel source) {
        super(source);
        mSignature = source.readInt() != 0 ? source.createByteArray() : null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (mSignature != null) {
            dest.writeInt(1);
            dest.writeByteArray(mSignature);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<AuthenticationResult> CREATOR = new Creator<AuthenticationResult>() {
        public AuthenticationResult createFromParcel(final Parcel source) {
            return new AuthenticationResult(source);
        }

        public AuthenticationResult[] newArray(final int size) {
            return new AuthenticationResult[size];
        }
    };

}
