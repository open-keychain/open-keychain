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

import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.Passphrase;


public class BackupKeyringParcel implements Parcelable {
    public Uri mCanonicalizedPublicKeyringUri;

    public final boolean mExportSecret;
    public final boolean mIsEncrypted;
    public final long mMasterKeyIds[];
    public final Uri mOutputUri;
    public final ParcelableHashMap<ParcelableLong, Passphrase> mParcelablePassphrases;

    public BackupKeyringParcel(long[] masterKeyIds, boolean exportSecret, boolean isEncrypted, Uri outputUri,
                               ParcelableHashMap<ParcelableLong, Passphrase> parcelablePassphrases) {
        mMasterKeyIds = masterKeyIds;
        mExportSecret = exportSecret;
        mOutputUri = outputUri;
        mIsEncrypted = isEncrypted;
        mParcelablePassphrases = parcelablePassphrases;
    }

    protected BackupKeyringParcel(Parcel in) {
        mCanonicalizedPublicKeyringUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mExportSecret = in.readByte() != 0x00;
        mOutputUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mMasterKeyIds = in.createLongArray();
        mIsEncrypted = in.readInt() != 0;
        mParcelablePassphrases = in.readParcelable(ParcelableHashMap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mCanonicalizedPublicKeyringUri);
        dest.writeByte((byte) (mExportSecret ? 0x01 : 0x00));
        dest.writeValue(mOutputUri);
        dest.writeLongArray(mMasterKeyIds);
        dest.writeInt(mIsEncrypted ? 1 : 0);
        dest.writeParcelable(mParcelablePassphrases, 0);
    }

    public static final Parcelable.Creator<BackupKeyringParcel> CREATOR = new Parcelable.Creator<BackupKeyringParcel>() {
        @Override
        public BackupKeyringParcel createFromParcel(Parcel in) {
            return new BackupKeyringParcel(in);
        }

        @Override
        public BackupKeyringParcel[] newArray(int size) {
            return new BackupKeyringParcel[size];
        }
    };
}