package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class ConsolidateInputParcel implements Parcelable {

    public boolean mConsolidateRecovery;

    public ConsolidateInputParcel(boolean consolidateRecovery) {
        mConsolidateRecovery = consolidateRecovery;
    }

    protected ConsolidateInputParcel(Parcel in) {
        mConsolidateRecovery = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mConsolidateRecovery ? 0x01 : 0x00));
    }

    public static final Parcelable.Creator<ConsolidateInputParcel> CREATOR = new Parcelable.Creator<ConsolidateInputParcel>() {
        @Override
        public ConsolidateInputParcel createFromParcel(Parcel in) {
            return new ConsolidateInputParcel(in);
        }

        @Override
        public ConsolidateInputParcel[] newArray(int size) {
            return new ConsolidateInputParcel[size];
        }
    };
}