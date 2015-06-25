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

package org.sufficientlysecure.keychain.service;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ExportKeyringParcel implements Parcelable {
    public String mKeyserver;
    public Uri mCanonicalizedPublicKeyringUri;

    public boolean mExportSecret;
    public long mMasterKeyIds[];
    public String mOutputFile;
    public Uri mOutputUri;
    public ExportType mExportType;

    public enum ExportType {
        UPLOAD_KEYSERVER,
        EXPORT_FILE,
        EXPORT_URI
    }

    public ExportKeyringParcel(String keyserver, Uri keyringUri) {
        mExportType = ExportType.UPLOAD_KEYSERVER;
        mKeyserver = keyserver;
        mCanonicalizedPublicKeyringUri = keyringUri;
    }

    public ExportKeyringParcel(long[] masterKeyIds, boolean exportSecret, String outputFile) {
        mExportType = ExportType.EXPORT_FILE;
        mMasterKeyIds = masterKeyIds;
        mExportSecret = exportSecret;
        mOutputFile = outputFile;
    }

    public ExportKeyringParcel(long[] masterKeyIds, boolean exportSecret, Uri outputUri) {
        mExportType = ExportType.EXPORT_URI;
        mMasterKeyIds = masterKeyIds;
        mExportSecret = exportSecret;
        mOutputUri = outputUri;
    }

    protected ExportKeyringParcel(Parcel in) {
        mKeyserver = in.readString();
        mCanonicalizedPublicKeyringUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mExportSecret = in.readByte() != 0x00;
        mOutputFile = in.readString();
        mOutputUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mExportType = (ExportType) in.readValue(ExportType.class.getClassLoader());
        mMasterKeyIds = in.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKeyserver);
        dest.writeValue(mCanonicalizedPublicKeyringUri);
        dest.writeByte((byte) (mExportSecret ? 0x01 : 0x00));
        dest.writeString(mOutputFile);
        dest.writeValue(mOutputUri);
        dest.writeValue(mExportType);
        dest.writeLongArray(mMasterKeyIds);
    }

    public static final Parcelable.Creator<ExportKeyringParcel> CREATOR = new Parcelable.Creator<ExportKeyringParcel>() {
        @Override
        public ExportKeyringParcel createFromParcel(Parcel in) {
            return new ExportKeyringParcel(in);
        }

        @Override
        public ExportKeyringParcel[] newArray(int size) {
            return new ExportKeyringParcel[size];
        }
    };
}