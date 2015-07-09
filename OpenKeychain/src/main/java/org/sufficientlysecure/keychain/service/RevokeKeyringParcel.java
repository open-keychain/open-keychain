package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class RevokeKeyringParcel implements Parcelable {

    final public long mMasterKeyId;
    final public boolean mUpload;
    final public String mKeyserver;

    public RevokeKeyringParcel(long masterKeyId, boolean upload, String keyserver) {
        mMasterKeyId = masterKeyId;
        mUpload = upload;
        mKeyserver = keyserver;
    }

    protected RevokeKeyringParcel(Parcel in) {
        mMasterKeyId = in.readLong();
        mUpload = in.readByte() != 0x00;
        mKeyserver = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mMasterKeyId);
        dest.writeByte((byte) (mUpload ? 0x01 : 0x00));
        dest.writeString(mKeyserver);
    }

    public static final Parcelable.Creator<RevokeKeyringParcel> CREATOR = new Parcelable.Creator<RevokeKeyringParcel>() {
        @Override
        public RevokeKeyringParcel createFromParcel(Parcel in) {
            return new RevokeKeyringParcel(in);
        }

        @Override
        public RevokeKeyringParcel[] newArray(int size) {
            return new RevokeKeyringParcel[size];
        }
    };
}