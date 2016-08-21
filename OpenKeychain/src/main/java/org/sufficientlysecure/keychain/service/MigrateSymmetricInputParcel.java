package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;

import java.util.ArrayList;
import java.util.List;

public class MigrateSymmetricInputParcel implements Parcelable {
    public final List<KeyringPassphrases> mKeyringPassphrasesList;

    public MigrateSymmetricInputParcel(List<KeyringPassphrases> passphraseList) {
        mKeyringPassphrasesList = new ArrayList<>(passphraseList);
    }

    protected MigrateSymmetricInputParcel(Parcel in) {
        mKeyringPassphrasesList = new ArrayList<>();
        in.readList(mKeyringPassphrasesList, KeyringPassphrases.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mKeyringPassphrasesList);
    }

    public static final Creator<MigrateSymmetricInputParcel> CREATOR = new Parcelable.Creator<MigrateSymmetricInputParcel>() {
        @Override
        public MigrateSymmetricInputParcel createFromParcel(Parcel in) {
            return new MigrateSymmetricInputParcel(in);
        }

        @Override
        public MigrateSymmetricInputParcel[] newArray(int size) {
            return new MigrateSymmetricInputParcel[size];
        }
    };

}
