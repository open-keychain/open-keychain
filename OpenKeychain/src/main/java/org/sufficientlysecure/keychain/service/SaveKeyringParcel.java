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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class is a a transferable representation for a collection of changes
 * to be done on a keyring.
 * <p/>
 * This class should include all types of operations supported in the backend.
 * <p/>
 * All changes are done in a differential manner. Besides the two key
 * identification attributes, all attributes may be null, which indicates no
 * change to the keyring. This is also the reason why boxed values are used
 * instead of primitives in the subclasses.
 * <p/>
 * Application of operations in the backend should be fail-fast, which means an
 * error in any included operation (for example revocation of a non-existent
 * subkey) will cause the operation as a whole to fail.
 */
public class SaveKeyringParcel implements Parcelable {

    // the master key id to be edited. if this is null, a new one will be created
    public Long mMasterKeyId;
    // the key fingerprint, for safety. MUST be null for a new key.
    public byte[] mFingerprint;

    public String mNewPassphrase;

    public ArrayList<String> mAddUserIds;
    public ArrayList<SubkeyAdd> mAddSubKeys;

    public ArrayList<SubkeyChange> mChangeSubKeys;
    public String mChangePrimaryUserId;

    public ArrayList<String> mRevokeUserIds;
    public ArrayList<Long> mRevokeSubKeys;

    public SaveKeyringParcel() {
        reset();
    }

    public SaveKeyringParcel(long masterKeyId, byte[] fingerprint) {
        this();
        mMasterKeyId = masterKeyId;
        mFingerprint = fingerprint;
    }

    public void reset() {
        mNewPassphrase = null;
        mAddUserIds = new ArrayList<String>();
        mAddSubKeys = new ArrayList<SubkeyAdd>();
        mChangePrimaryUserId = null;
        mChangeSubKeys = new ArrayList<SubkeyChange>();
        mRevokeUserIds = new ArrayList<String>();
        mRevokeSubKeys = new ArrayList<Long>();
    }

    // performance gain for using Parcelable here would probably be negligible,
    // use Serializable instead.
    public static class SubkeyAdd implements Serializable {
        public Algorithm mAlgorithm;
        public Integer mKeySize;
        public Curve mCurve;
        public int mFlags;
        public Long mExpiry;

        public SubkeyAdd(Algorithm algorithm, Integer keySize, Curve curve, int flags, Long expiry) {
            mAlgorithm = algorithm;
            mKeySize = keySize;
            mCurve = curve;
            mFlags = flags;
            mExpiry = expiry;
        }

        @Override
        public String toString() {
            String out = "mAlgorithm: " + mAlgorithm + ", ";
            out += "mKeySize: " + mKeySize + ", ";
            out += "mCurve: " + mCurve + ", ";
            out += "mFlags: " + mFlags;
            out += "mExpiry: " + mExpiry;

            return out;
        }
    }

    public static class SubkeyChange implements Serializable {
        public long mKeyId;
        public Integer mFlags;
        // this is a long unix timestamp, in seconds (NOT MILLISECONDS!)
        public Long mExpiry;

        public SubkeyChange(long keyId) {
            mKeyId = keyId;
        }

        public SubkeyChange(long keyId, Integer flags, Long expiry) {
            mKeyId = keyId;
            mFlags = flags;
            mExpiry = expiry;
        }

        @Override
        public String toString() {
            String out = "mKeyId: " + mKeyId + ", ";
            out += "mFlags: " + mFlags + ", ";
            out += "mExpiry: " + mExpiry;

            return out;
        }
    }

    public SubkeyChange getSubkeyChange(long keyId) {
        for (SubkeyChange subkeyChange : mChangeSubKeys) {
            if (subkeyChange.mKeyId == keyId) {
                return subkeyChange;
            }
        }
        return null;
    }

    public SubkeyChange getOrCreateSubkeyChange(long keyId) {
        SubkeyChange foundSubkeyChange = getSubkeyChange(keyId);
        if (foundSubkeyChange != null) {
            return foundSubkeyChange;
        } else {
            // else, create a new one
            SubkeyChange newSubkeyChange = new SubkeyChange(keyId);
            mChangeSubKeys.add(newSubkeyChange);
            return newSubkeyChange;
        }
    }

    public SaveKeyringParcel(Parcel source) {
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
        mFingerprint = source.createByteArray();

        mNewPassphrase = source.readString();

        mAddUserIds = source.createStringArrayList();
        mAddSubKeys = (ArrayList<SubkeyAdd>) source.readSerializable();

        mChangeSubKeys = (ArrayList<SubkeyChange>) source.readSerializable();
        mChangePrimaryUserId = source.readString();

        mRevokeUserIds = source.createStringArrayList();
        mRevokeSubKeys = (ArrayList<Long>) source.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(mMasterKeyId == null ? 0 : 1);
        if (mMasterKeyId != null) {
            destination.writeLong(mMasterKeyId);
        }
        destination.writeByteArray(mFingerprint);

        destination.writeString(mNewPassphrase);

        destination.writeStringList(mAddUserIds);
        destination.writeSerializable(mAddSubKeys);

        destination.writeSerializable(mChangeSubKeys);
        destination.writeString(mChangePrimaryUserId);

        destination.writeStringList(mRevokeUserIds);
        destination.writeSerializable(mRevokeSubKeys);
    }

    public static final Creator<SaveKeyringParcel> CREATOR = new Creator<SaveKeyringParcel>() {
        public SaveKeyringParcel createFromParcel(final Parcel source) {
            return new SaveKeyringParcel(source);
        }

        public SaveKeyringParcel[] newArray(final int size) {
            return new SaveKeyringParcel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        String out = "mMasterKeyId: " + mMasterKeyId + "\n";
        out += "mNewPassphrase: " + mNewPassphrase + "\n";
        out += "mAddUserIds: " + mAddUserIds + "\n";
        out += "mAddSubKeys: " + mAddSubKeys + "\n";
        out += "mChangeSubKeys: " + mChangeSubKeys + "\n";
        out += "mChangePrimaryUserId: " + mChangePrimaryUserId + "\n";
        out += "mRevokeUserIds: " + mRevokeUserIds + "\n";
        out += "mRevokeSubKeys: " + mRevokeSubKeys;

        return out;
    }

    // All supported algorithms
    public enum Algorithm {
        RSA, DSA, ELGAMAL, ECDSA, ECDH
    }

    // All curves defined in the standard
    // http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
    public enum Curve {
        NIST_P256, NIST_P384, NIST_P521,

        // these are supported by gpg, but they are not in rfc6637 and not supported by BouncyCastle yet
        // (adding support would be trivial though -> JcaPGPKeyConverter.java:190)
        // BRAINPOOL_P256, BRAINPOOL_P384, BRAINPOOL_P512
    }

}
