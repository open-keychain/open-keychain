/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.ssh.authentication;

import android.os.Parcel;
import android.os.Parcelable;

public class SshAuthenticationApiError implements Parcelable {

    /**
     * Error codes
     */
    /* values chosen for compatibility with openpgp-api */
    public static final int CLIENT_SIDE_ERROR = -1;
    public static final int GENERIC_ERROR = 0;
    public static final int INCOMPATIBLE_API_VERSIONS = 1;
    public static final int INTERNAL_ERROR = 2;

    /* values chosen for no intersection with openpgp-api */
    public static final int UNKNOWN_ACTION = -128;
    public static final int NO_KEY_ID = -129;
    public static final int NO_SUCH_KEY = -130;
    public static final int NO_AUTH_KEY = -131;

    /* values chosen to be invalid enumeration values in their respective domain */
    public static final int INVALID_ALGORITHM = -254;
    public static final int INVALID_HASH_ALGORITHM = -253;


    private int mError;
    private String mMessage;


    public SshAuthenticationApiError(int error, String message) {
        mError = error;
        mMessage = message;
    }

    protected SshAuthenticationApiError(Parcel in) {
        mError = in.readInt();
        mMessage = in.readString();
    }

    public static final Creator<SshAuthenticationApiError> CREATOR = new Creator<SshAuthenticationApiError>() {
        @Override
        public SshAuthenticationApiError createFromParcel(Parcel in) {
            return new SshAuthenticationApiError(in);
        }

        @Override
        public SshAuthenticationApiError[] newArray(int size) {
            return new SshAuthenticationApiError[size];
        }
    };

    public int getError() {
        return mError;
    }

    public String getMessage() {
        return mMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mError);
        dest.writeString(mMessage);
    }
}
