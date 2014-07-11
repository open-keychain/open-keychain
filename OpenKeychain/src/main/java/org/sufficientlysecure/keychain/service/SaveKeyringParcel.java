package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

/** This class is a a transferable representation for a collection of changes
 * to be done on a keyring.
 *
 * This class should include all types of operations supported in the backend.
 *
 * All changes are done in a differential manner. Besides the two key
 * identification attributes, all attributes may be null, which indicates no
 * change to the keyring. This is also the reason why boxed values are used
 * instead of primitives in the subclasses.
 *
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
        public int mAlgorithm;
        public int mKeysize;
        public int mFlags;
        public Long mExpiry;
        public SubkeyAdd(int algorithm, int keysize, int flags, Long expiry) {
            mAlgorithm = algorithm;
            mKeysize = keysize;
            mFlags = flags;
            mExpiry = expiry;
        }
    }

    public static class SubkeyChange implements Serializable {
        public long mKeyId;
        public Integer mFlags;
        public Long mExpiry;
        public SubkeyChange(long keyId, Integer flags, Long expiry) {
            mKeyId = keyId;
            mFlags = flags;
            mExpiry = expiry;
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
        if(mMasterKeyId != null) {
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

}
