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

public class MimeParsingResult extends OperationResult {

    public final ArrayList<Uri> mTemporaryUris;

    public ArrayList<Uri> getTemporaryUris() {
        return mTemporaryUris;
    }

    public MimeParsingResult(int result, OperationLog log, ArrayList<Uri> temporaryUris) {
        super(result, log);
        mTemporaryUris = temporaryUris;
    }

    protected MimeParsingResult(Parcel in) {
        super(in);
        mTemporaryUris = in.createTypedArrayList(Uri.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mTemporaryUris);
    }

    public static final Creator<MimeParsingResult> CREATOR = new Creator<MimeParsingResult>() {
        @Override
        public MimeParsingResult createFromParcel(Parcel in) {
            return new MimeParsingResult(in);
        }

        @Override
        public MimeParsingResult[] newArray(int size) {
            return new MimeParsingResult[size];
        }
    };
}