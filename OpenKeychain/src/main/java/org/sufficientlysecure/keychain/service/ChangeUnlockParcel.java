/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.util.Passphrase;

public class ChangeUnlockParcel implements Parcelable {

    // the master key id of keyring.
    public Long mMasterKeyId;
    // the key fingerprint, for safety.
    public byte[] mFingerprint;
    // The new passphrase to use
    public final Passphrase mNewPassphrase;

    public ChangeUnlockParcel(Passphrase newPassphrase) {
        mNewPassphrase = newPassphrase;
    }

    public ChangeUnlockParcel(Long masterKeyId, byte[] fingerprint, Passphrase newPassphrase) {
        if (newPassphrase == null) {
            throw new AssertionError("newPassphrase must be non-null. THIS IS A BUG!");
        }

        mMasterKeyId = masterKeyId;
        mFingerprint = fingerprint;
        mNewPassphrase = newPassphrase;
    }

    public ChangeUnlockParcel(Parcel source) {
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
        mFingerprint = source.createByteArray();
        mNewPassphrase = source.readParcelable(Passphrase.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(mMasterKeyId == null ? 0 : 1);
        if (mMasterKeyId != null) {
            destination.writeLong(mMasterKeyId);
        }
        destination.writeByteArray(mFingerprint);
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
        String out = "mMasterKeyId: " + mMasterKeyId + "\n";
        out += "passphrase (" + mNewPassphrase + ")";

        return out;
    }

}
