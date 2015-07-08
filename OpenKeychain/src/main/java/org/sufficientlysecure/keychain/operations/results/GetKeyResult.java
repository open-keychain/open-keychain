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

import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;

public class GetKeyResult extends InputPendingResult {

    public int mNonPgpPartsCount;

    public int getNonPgpPartsCount() {
        return mNonPgpPartsCount;
    }

    public void setNonPgpPartsCount(int nonPgpPartsCount) {
        mNonPgpPartsCount = nonPgpPartsCount;
    }

    public GetKeyResult(int result, OperationLog log) {
        super(result, log);
    }

    public GetKeyResult(OperationLog log, RequiredInputParcel requiredInput) {
        super(log, requiredInput);
    }

    public static final int RESULT_ERROR_NO_VALID_KEYS = RESULT_ERROR + 8;
    public static final int RESULT_ERROR_NO_PGP_PARTS = RESULT_ERROR + 16;
    public static final int RESULT_ERROR_QUERY_TOO_SHORT = RESULT_ERROR + 32;
    public static final int RESULT_ERROR_TOO_MANY_RESPONSES = RESULT_ERROR + 64;
    public static final int RESULT_ERROR_TOO_SHORT_OR_TOO_MANY_RESPONSES = RESULT_ERROR + 128;
    public static final int RESULT_ERROR_QUERY_FAILED = RESULT_ERROR + 256;

    public GetKeyResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<GetKeyResult> CREATOR = new Creator<GetKeyResult>() {
        public GetKeyResult createFromParcel(final Parcel source) {
            return new GetKeyResult(source);
        }

        public GetKeyResult[] newArray(final int size) {
            return new GetKeyResult[size];
        }
    };
}
