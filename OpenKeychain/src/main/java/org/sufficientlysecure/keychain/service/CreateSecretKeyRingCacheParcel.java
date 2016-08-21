package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class CreateSecretKeyRingCacheParcel implements Parcelable {

    public final String mFileName;

    public CreateSecretKeyRingCacheParcel(String fileName) {
        mFileName = fileName;
    }

    protected CreateSecretKeyRingCacheParcel(Parcel in) {
        mFileName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFileName);
    }

    public static final Creator<CreateSecretKeyRingCacheParcel> CREATOR = new Creator<CreateSecretKeyRingCacheParcel>() {
        @Override
        public CreateSecretKeyRingCacheParcel createFromParcel(Parcel in) {
            return new CreateSecretKeyRingCacheParcel(in);
        }

        @Override
        public CreateSecretKeyRingCacheParcel[] newArray(int size) {
            return new CreateSecretKeyRingCacheParcel[size];
        }
    };

}
