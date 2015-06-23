package org.sufficientlysecure.keychain.service.input;

import android.os.Parcel;
import android.os.Parcelable;

public class DeleteKeyringParcel implements Parcelable {

    public long[] mMasterKeyIds;
    public boolean mIsSecret;

    public DeleteKeyringParcel(long[] masterKeyIds, boolean isSecret) {
        mMasterKeyIds = masterKeyIds;
        mIsSecret = isSecret;
    }

    protected DeleteKeyringParcel(Parcel in) {
        mIsSecret = in.readByte() != 0x00;
        mMasterKeyIds = in.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mIsSecret ? 0x01 : 0x00));
        dest.writeLongArray(mMasterKeyIds);
    }

    public static final Parcelable.Creator<DeleteKeyringParcel> CREATOR = new Parcelable.Creator<DeleteKeyringParcel>() {
        @Override
        public DeleteKeyringParcel createFromParcel(Parcel in) {
            return new DeleteKeyringParcel(in);
        }

        @Override
        public DeleteKeyringParcel[] newArray(int size) {
            return new DeleteKeyringParcel[size];
        }
    };
}

