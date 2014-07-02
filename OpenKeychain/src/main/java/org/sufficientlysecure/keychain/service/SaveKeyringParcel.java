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

    public String newPassphrase;

    public ArrayList<String> addUserIds;
    public ArrayList<SubkeyAdd> addSubKeys;

    public ArrayList<SubkeyChange> changeSubKeys;
    public String changePrimaryUserId;

    public ArrayList<String> revokeUserIds;
    public ArrayList<Long> revokeSubKeys;

    public SaveKeyringParcel() {
        addUserIds = new ArrayList<String>();
        addSubKeys = new ArrayList<SubkeyAdd>();
        changeSubKeys = new ArrayList<SubkeyChange>();
        revokeUserIds = new ArrayList<String>();
        revokeSubKeys = new ArrayList<Long>();
    }

    public SaveKeyringParcel(long masterKeyId, byte[] fingerprint) {
        this();
        mMasterKeyId = masterKeyId;
        mFingerprint = fingerprint;
    }

    // performance gain for using Parcelable here would probably be negligible,
    // use Serializable instead.
    public static class SubkeyAdd implements Serializable {
        public final int mAlgorithm;
        public final int mKeysize;
        public final int mFlags;
        public final Long mExpiry;
        public SubkeyAdd(int algorithm, int keysize, int flags, Long expiry) {
            mAlgorithm = algorithm;
            mKeysize = keysize;
            mFlags = flags;
            mExpiry = expiry;
        }
    }

    public static class SubkeyChange implements Serializable {
        public final long mKeyId;
        public final Integer mFlags;
        public final Long mExpiry;
        public SubkeyChange(long keyId, Integer flags, Long expiry) {
            mKeyId = keyId;
            mFlags = flags;
            mExpiry = expiry;
        }
    }

    public SaveKeyringParcel(Parcel source) {
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
        mFingerprint = source.createByteArray();

        addUserIds = source.createStringArrayList();
        addSubKeys = (ArrayList<SubkeyAdd>) source.readSerializable();

        changeSubKeys = (ArrayList<SubkeyChange>) source.readSerializable();
        changePrimaryUserId = source.readString();

        revokeUserIds = source.createStringArrayList();
        revokeSubKeys = (ArrayList<Long>) source.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(mMasterKeyId == null ? 0 : 1);
        if(mMasterKeyId != null) {
            destination.writeLong(mMasterKeyId);
        }
        destination.writeByteArray(mFingerprint);

        destination.writeStringList(addUserIds);
        destination.writeSerializable(addSubKeys);

        destination.writeSerializable(changeSubKeys);
        destination.writeString(changePrimaryUserId);

        destination.writeStringList(revokeUserIds);
        destination.writeSerializable(revokeSubKeys);
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
