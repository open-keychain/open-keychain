/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

public class DeleteResult extends OperationResult {

    final public int mOk, mFail;

    public DeleteResult(int result, OperationLog log, int ok, int fail) {
        super(result, log);
        mOk = ok;
        mFail = fail;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public DeleteResult(Parcel source) {
        super(source);
        mOk = source.readInt();
        mFail = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mOk);
        dest.writeInt(mFail);
    }

    public static Creator<DeleteResult> CREATOR = new Creator<DeleteResult>() {
        public DeleteResult createFromParcel(final Parcel source) {
            return new DeleteResult(source);
        }

        public DeleteResult[] newArray(final int size) {
            return new DeleteResult[size];
        }
    };

}
