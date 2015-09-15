/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.net.Uri;
import android.os.Parcel;

import java.util.ArrayList;

public class InputDataResult extends InputPendingResult {

    public final ArrayList<Uri> mOutputUris;
    final public DecryptVerifyResult mDecryptVerifyResult;

    public InputDataResult(OperationLog log, InputPendingResult result) {
        super(log, result);
        mOutputUris = null;
        mDecryptVerifyResult = null;
    }

    public InputDataResult(int result, OperationLog log, DecryptVerifyResult decryptResult, ArrayList<Uri> temporaryUris) {
        super(result, log);
        mOutputUris = temporaryUris;
        mDecryptVerifyResult = decryptResult;
    }

    public InputDataResult(int result, OperationLog log) {
        super(result, log);
        mOutputUris = null;
        mDecryptVerifyResult = null;
    }

    protected InputDataResult(Parcel in) {
        super(in);
        mOutputUris = in.createTypedArrayList(Uri.CREATOR);
        mDecryptVerifyResult = in.readParcelable(DecryptVerifyResult.class.getClassLoader());
    }

    public ArrayList<Uri> getOutputUris() {
        return mOutputUris;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mOutputUris);
        dest.writeParcelable(mDecryptVerifyResult, 0);
    }

    public static final Creator<InputDataResult> CREATOR = new Creator<InputDataResult>() {
        @Override
        public InputDataResult createFromParcel(Parcel in) {
            return new InputDataResult(in);
        }

        @Override
        public InputDataResult[] newArray(int size) {
            return new InputDataResult[size];
        }
    };
}