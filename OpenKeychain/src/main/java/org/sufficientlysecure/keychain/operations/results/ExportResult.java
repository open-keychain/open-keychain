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

import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;

public class ExportResult extends InputPendingResult {

    public ExportResult(int result, OperationLog log) {
        super(result, log);
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public ExportResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
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
