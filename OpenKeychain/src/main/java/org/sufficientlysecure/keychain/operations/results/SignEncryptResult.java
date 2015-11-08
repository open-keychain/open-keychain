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

import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;

import java.util.ArrayList;

public class SignEncryptResult extends InputPendingResult {

    ArrayList<PgpSignEncryptResult> mResults;
    byte[] mResultBytes;

    public SignEncryptResult(OperationLog log, RequiredInputParcel requiredInput,
                             ArrayList<PgpSignEncryptResult> results,
                             CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInput, cryptoInputParcel);
        mResults = results;
    }

    public SignEncryptResult(int result, OperationLog log, ArrayList<PgpSignEncryptResult> results) {
        super(result, log);
        mResults = results;
    }

    public SignEncryptResult(int result, OperationLog log, ArrayList<PgpSignEncryptResult> results, byte[] resultBytes) {
        super(result, log);
        mResults = results;
        mResultBytes = resultBytes;
    }

    public SignEncryptResult(Parcel source) {
        super(source);
        mResults = source.createTypedArrayList(PgpSignEncryptResult.CREATOR);
    }

    public byte[] getResultBytes() {
        return mResultBytes;
    }

    public ArrayList<PgpSignEncryptResult> getResults() {
        return mResults;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mResults);
    }

    public static final Creator<SignEncryptResult> CREATOR = new Creator<SignEncryptResult>() {
        public SignEncryptResult createFromParcel(final Parcel source) {
            return new SignEncryptResult(source);
        }

        public SignEncryptResult[] newArray(final int size) {
            return new SignEncryptResult[size];
        }
    };

}