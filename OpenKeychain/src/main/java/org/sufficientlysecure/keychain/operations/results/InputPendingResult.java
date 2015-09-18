/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

public class InputPendingResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    final RequiredInputParcel mRequiredInput;
    // in case operation needs to add to/changes the cryptoInputParcel sent to it
    public final CryptoInputParcel mCryptoInputParcel;

    public InputPendingResult(int result, OperationLog log) {
        super(result, log);
        mRequiredInput = null;
        mCryptoInputParcel = null;
    }

    public InputPendingResult(OperationLog log, InputPendingResult result) {
        super(RESULT_PENDING, log);
        if (!result.isPending()) {
            throw new AssertionError("sub result must be pending!");
        }
        mRequiredInput = result.mRequiredInput;
        mCryptoInputParcel = result.mCryptoInputParcel;
    }

    public InputPendingResult(OperationLog log, RequiredInputParcel requiredInput,
                              CryptoInputParcel cryptoInputParcel) {
        super(RESULT_PENDING, log);
        mRequiredInput = requiredInput;
        mCryptoInputParcel = cryptoInputParcel;
    }

    public InputPendingResult(Parcel source) {
        super(source);
        mRequiredInput = source.readParcelable(getClass().getClassLoader());
        mCryptoInputParcel = source.readParcelable(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mRequiredInput, 0);
        dest.writeParcelable(mCryptoInputParcel, 0);
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public RequiredInputParcel getRequiredInputParcel() {
        return mRequiredInput;
    }

}
