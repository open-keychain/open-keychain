package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.util.Passphrase;

public class ChangeUnlockParcel implements Parcelable {

    // The new passphrase to use
    public final Passphrase mNewPassphrase;

    public ChangeUnlockParcel(Passphrase newPassphrase) {
        if (newPassphrase == null) {
            throw new AssertionError("newPassphrase must be non-null. THIS IS A BUG!");
        }
        mNewPassphrase = newPassphrase;
    }

    public ChangeUnlockParcel(Parcel source) {
        mNewPassphrase = source.readParcelable(Passphrase.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(mNewPassphrase, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChangeUnlockParcel> CREATOR = new Creator<ChangeUnlockParcel>() {
        public ChangeUnlockParcel createFromParcel(final Parcel source) {
            return new ChangeUnlockParcel(source);
        }

        public ChangeUnlockParcel[] newArray(final int size) {
            return new ChangeUnlockParcel[size];
        }
    };

    public String toString() {
        return "passphrase (" + mNewPassphrase + ")";
    }

}
