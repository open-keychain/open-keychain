package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class PromoteKeyringParcel implements Parcelable {

    public long mKeyRingId;
    public byte[] mCardAid;
    public long[] mSubKeyIds;

    public PromoteKeyringParcel(long keyRingId, byte[] cardAid, long[] subKeyIds) {
        mKeyRingId = keyRingId;
        mCardAid = cardAid;
        mSubKeyIds = subKeyIds;
    }

    protected PromoteKeyringParcel(Parcel in) {
        mKeyRingId = in.readLong();
        mCardAid = in.createByteArray();
        mSubKeyIds = in.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mKeyRingId);
        dest.writeByteArray(mCardAid);
        dest.writeLongArray(mSubKeyIds);
    }

    public static final Parcelable.Creator<PromoteKeyringParcel> CREATOR = new Parcelable.Creator<PromoteKeyringParcel>() {
        @Override
        public PromoteKeyringParcel createFromParcel(Parcel in) {
            return new PromoteKeyringParcel(in);
        }

        @Override
        public PromoteKeyringParcel[] newArray(int size) {
            return new PromoteKeyringParcel[size];
        }
    };
}