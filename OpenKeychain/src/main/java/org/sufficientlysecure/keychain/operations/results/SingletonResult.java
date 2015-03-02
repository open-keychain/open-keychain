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

/** This is a simple subclass meant to contain only a single log message. This log
 * message is also shown without a log button in the createNotify SuperToast. */
public class SingletonResult extends OperationResult {

    /** Construct from a parcel - trivial because we have no extra data. */
    public SingletonResult(Parcel source) {
        super(source);
    }

    public SingletonResult(int result, LogType reason) {
        super(result, new OperationLog());
        // Prepare the log
        mLog.add(reason, 0);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<SingletonResult> CREATOR = new Creator<SingletonResult>() {
        public SingletonResult createFromParcel(final Parcel source) {
            return new SingletonResult(source);
        }

        public SingletonResult[] newArray(final int size) {
            return new SingletonResult[size];
        }
    };

}
