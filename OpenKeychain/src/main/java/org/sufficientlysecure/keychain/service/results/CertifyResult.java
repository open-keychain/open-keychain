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

public class CertifyResult extends OperationResult {

    int mCertifyOk, mCertifyError;

    public CertifyResult(int result, OperationLog log) {
        super(result, log);
    }

    public CertifyResult(int result, OperationLog log, int certifyOk, int certifyError) {
        this(result, log);
        mCertifyOk = certifyOk;
        mCertifyError = certifyError;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public CertifyResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<CertifyResult> CREATOR = new Creator<CertifyResult>() {
        public CertifyResult createFromParcel(final Parcel source) {
            return new CertifyResult(source);
        }

        public CertifyResult[] newArray(final int size) {
            return new CertifyResult[size];
        }
    };

}
