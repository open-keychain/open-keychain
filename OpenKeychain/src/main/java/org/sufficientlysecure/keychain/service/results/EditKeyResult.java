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

package org.sufficientlysecure.keychain.service.results;

import android.os.Parcel;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;

public class EditKeyResult extends OperationResultParcel {

    private transient UncachedKeyRing mRing;
    public final long mRingMasterKeyId;

    public EditKeyResult(int result, OperationLog log,
                         UncachedKeyRing ring) {
        super(result, log);
        mRing = ring;
        mRingMasterKeyId = ring != null ? ring.getMasterKeyId() : Constants.key.none;
    }

    public UncachedKeyRing getRing() {
        return mRing;
    }

    public EditKeyResult(Parcel source) {
        super(source);
        mRingMasterKeyId = source.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mRingMasterKeyId);
    }

    public static Creator<EditKeyResult> CREATOR = new Creator<EditKeyResult>() {
        public EditKeyResult createFromParcel(final Parcel source) {
            return new EditKeyResult(source);
        }

        public EditKeyResult[] newArray(final int size) {
            return new EditKeyResult[size];
        }
    };

}
