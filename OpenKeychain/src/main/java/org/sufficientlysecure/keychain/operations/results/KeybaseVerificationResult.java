/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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
import android.os.Parcelable;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.prover.Prover;

public class KeybaseVerificationResult extends OperationResult implements Parcelable {
    public final String mProofUrl;
    public final String mPresenceUrl;
    public final String mPresenceLabel;

    public KeybaseVerificationResult(int result, OperationLog log) {
        super(result, log);
        mProofUrl = null;
        mPresenceLabel = null;
        mPresenceUrl = null;
    }

    public KeybaseVerificationResult(int result, OperationLog log, Prover prover)
            throws KeybaseException {
        super(result, log);
        mProofUrl = prover.getProofUrl();
        mPresenceUrl = prover.getPresenceUrl();
        mPresenceLabel = prover.getPresenceLabel();
    }

    protected KeybaseVerificationResult(Parcel in) {
        super(in);
        mProofUrl = in.readString();
        mPresenceUrl = in.readString();
        mPresenceLabel = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mProofUrl);
        dest.writeString(mPresenceUrl);
        dest.writeString(mPresenceLabel);
    }

    public static final Parcelable.Creator<KeybaseVerificationResult> CREATOR = new Parcelable.Creator<KeybaseVerificationResult>() {
        @Override
        public KeybaseVerificationResult createFromParcel(Parcel in) {
            return new KeybaseVerificationResult(in);
        }

        @Override
        public KeybaseVerificationResult[] newArray(int size) {
            return new KeybaseVerificationResult[size];
        }
    };
}