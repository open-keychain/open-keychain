package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

public class PassphraseChangeParcel implements Parcelable {

    // the master key id to be edited.
    public Long mMasterKeyId;
    // the first sub key id that is not stripped.
    public Long mValidSubkeyId;
    // the key fingerprint, for safety.
    public byte[] mFingerprint;

    public ChangeUnlockParcel mNewUnlock;


    public PassphraseChangeParcel(long masterKeyId, byte[] fingerprint) {
        mMasterKeyId = masterKeyId;
        mFingerprint = fingerprint;
    }

    public PassphraseChangeParcel(Parcel source) {
        mValidSubkeyId = source.readInt() != 0 ? source.readLong() : null;
        mMasterKeyId = source.readLong();
        mFingerprint = source.createByteArray();

        mNewUnlock = source.readParcelable(getClass().getClassLoader());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(mValidSubkeyId == null ? 0 : 1);
        if (mValidSubkeyId != null) {
            destination.writeLong(mValidSubkeyId);
        }
        destination.writeLong(mMasterKeyId);
        destination.writeByteArray(mFingerprint);
        destination.writeParcelable(mNewUnlock, flags);
    }

    public static final Creator<PassphraseChangeParcel> CREATOR = new Creator<PassphraseChangeParcel>() {
        public PassphraseChangeParcel createFromParcel(final Parcel source) {
            return new PassphraseChangeParcel(source);
        }

        public PassphraseChangeParcel[] newArray(final int size) {
            return new PassphraseChangeParcel[size];
        }
    };

    public String toString() {
        String out = "mMasterKeyId: " + mMasterKeyId + "\n";
        out += "mNewUnlock: " + mNewUnlock + "\n";

        return out;
    }
}
