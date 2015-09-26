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


public class UploadResult extends InputPendingResult {

    final int mOkPublic, mOkSecret;

    public UploadResult(int result, OperationLog log) {
        this(result, log, 0, 0);
    }

    public UploadResult(int result, OperationLog log, int okPublic, int okSecret) {
        super(result, log);
        mOkPublic = okPublic;
        mOkSecret = okSecret;
    }


    public UploadResult(OperationLog log, RequiredInputParcel requiredInputParcel,
            CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInputParcel, cryptoInputParcel);
        // we won't use these values
        mOkPublic = -1;
        mOkSecret = -1;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public UploadResult(Parcel source) {
        super(source);
        mOkPublic = source.readInt();
        mOkSecret = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mOkPublic);
        dest.writeInt(mOkSecret);
    }

    public static Creator<UploadResult> CREATOR = new Creator<UploadResult>() {
        public UploadResult createFromParcel(final Parcel source) {
            return new UploadResult(source);
        }

        public UploadResult[] newArray(final int size) {
            return new UploadResult[size];
        }
    };

}
