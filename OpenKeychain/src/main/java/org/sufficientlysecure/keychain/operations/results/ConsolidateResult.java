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

public class ConsolidateResult extends OperationResult {

    public ConsolidateResult(int result, OperationLog log) {
        super(result, log);
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public ConsolidateResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<ConsolidateResult> CREATOR = new Creator<ConsolidateResult>() {
        public ConsolidateResult createFromParcel(final Parcel source) {
            return new ConsolidateResult(source);
        }

        public ConsolidateResult[] newArray(final int size) {
            return new ConsolidateResult[size];
        }
    };

    public static class WriteKeyRingsResult extends OperationResult {

        public WriteKeyRingsResult(int result, OperationLog log) {
            super(result, log);
        }

        /** Construct from a parcel - trivial because we have no extra data. */
        public WriteKeyRingsResult(Parcel source) {
            super(source);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
        }

        public static Creator<WriteKeyRingsResult> CREATOR = new Creator<WriteKeyRingsResult>() {
            public WriteKeyRingsResult createFromParcel(final Parcel source) {
                return new WriteKeyRingsResult(source);
            }

            public WriteKeyRingsResult[] newArray(final int size) {
                return new WriteKeyRingsResult[size];
            }
        };
    }
}
