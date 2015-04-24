/*
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

package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;


public class PgpSignEncryptResult extends InputPendingResult {

    byte[] mDetachedSignature;

    public void setDetachedSignature(byte[] detachedSignature) {
        mDetachedSignature = detachedSignature;
    }

    public byte[] getDetachedSignature() {
        return mDetachedSignature;
    }

    public PgpSignEncryptResult(int result, OperationLog log) {
        super(result, log);
    }

    public PgpSignEncryptResult(OperationLog log, RequiredInputParcel requiredInput) {
        super(log, requiredInput);
    }

    public PgpSignEncryptResult(Parcel source) {
        super(source);
        mDetachedSignature = source.readInt() != 0 ? source.createByteArray() : null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (mDetachedSignature != null) {
            dest.writeInt(1);
            dest.writeByteArray(mDetachedSignature);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<PgpSignEncryptResult> CREATOR = new Creator<PgpSignEncryptResult>() {
        public PgpSignEncryptResult createFromParcel(final Parcel source) {
            return new PgpSignEncryptResult(source);
        }

        public PgpSignEncryptResult[] newArray(final int size) {
            return new PgpSignEncryptResult[size];
        }
    };

}
