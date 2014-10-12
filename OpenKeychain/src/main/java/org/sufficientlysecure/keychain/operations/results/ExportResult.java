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

public class ExportResult extends OperationResult {

    final int mOkPublic, mOkSecret;

    public ExportResult(int result, OperationLog log) {
        this(result, log, 0, 0);
    }

    public ExportResult(int result, OperationLog log, int okPublic, int okSecret) {
        super(result, log);
        mOkPublic = okPublic;
        mOkSecret = okSecret;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public ExportResult(Parcel source) {
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

    public static Creator<ExportResult> CREATOR = new Creator<ExportResult>() {
        public ExportResult createFromParcel(final Parcel source) {
            return new ExportResult(source);
        }

        public ExportResult[] newArray(final int size) {
            return new ExportResult[size];
        }
    };

}
