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

public class PromoteKeyResult extends OperationResult {

    public final Long mMasterKeyId;

    public PromoteKeyResult(int result, OperationLog log, Long masterKeyId) {
        super(result, log);
        mMasterKeyId = masterKeyId;
    }

    public PromoteKeyResult(Parcel source) {
        super(source);
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (mMasterKeyId != null) {
            dest.writeInt(1);
            dest.writeLong(mMasterKeyId);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<PromoteKeyResult> CREATOR = new Creator<PromoteKeyResult>() {
        public PromoteKeyResult createFromParcel(final Parcel source) {
            return new PromoteKeyResult(source);
        }

        public PromoteKeyResult[] newArray(final int size) {
            return new PromoteKeyResult[size];
        }
    };

}
