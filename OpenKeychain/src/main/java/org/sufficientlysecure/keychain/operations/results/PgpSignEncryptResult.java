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

package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;


public class PgpSignEncryptResult extends InputPendingResult {

    public static final int RESULT_KEY_DISALLOWED = RESULT_ERROR + 32;

    byte[] mOutputBytes;

    byte[] mDetachedSignature;
    public long mOperationTime;
    // this is the micalg parameter used in PGP/MIME, see RFC3156:
    // https://tools.ietf.org/html/rfc3156#section-5
    private String mMicAlgDigestName;

    public void setDetachedSignature(byte[] detachedSignature) {
        mDetachedSignature = detachedSignature;
    }

    public byte[] getDetachedSignature() {
        return mDetachedSignature;
    }

    public PgpSignEncryptResult(int result, OperationLog log) {
        super(result, log);
    }

    public PgpSignEncryptResult(OperationLog log, RequiredInputParcel requiredInput,
                                CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInput, cryptoInputParcel);
    }

    public PgpSignEncryptResult(Parcel source) {
        super(source);
        mDetachedSignature = source.readInt() != 0 ? source.createByteArray() : null;
    }

    public void setOutputBytes(byte[] outputBytes) {
        mOutputBytes = outputBytes;
    }

    public byte[] getOutputBytes() {
        return mOutputBytes;
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

    public void setMicAlgDigestName(String micAlgDigestName) {
        mMicAlgDigestName = micAlgDigestName;
    }

    public String getMicAlgDigestName() {
        return mMicAlgDigestName;
    }
}
