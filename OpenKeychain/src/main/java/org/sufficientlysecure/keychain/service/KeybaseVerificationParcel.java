package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class KeybaseVerificationParcel implements Parcelable {

    public String mKeybaseProof;
    public String mRequiredFingerprint;

    public KeybaseVerificationParcel(String keybaseProof, String requiredFingerprint) {
        mKeybaseProof = keybaseProof;
        mRequiredFingerprint = requiredFingerprint;
    }

    protected KeybaseVerificationParcel(Parcel in) {
        mKeybaseProof = in.readString();
        mRequiredFingerprint = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKeybaseProof);
        dest.writeString(mRequiredFingerprint);
    }

    public static final Parcelable.Creator<KeybaseVerificationParcel> CREATOR = new Parcelable.Creator<KeybaseVerificationParcel>() {
        @Override
        public KeybaseVerificationParcel createFromParcel(Parcel in) {
            return new KeybaseVerificationParcel(in);
        }

        @Override
        public KeybaseVerificationParcel[] newArray(int size) {
            return new KeybaseVerificationParcel[size];
        }
    };
}