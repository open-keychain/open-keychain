package org.sufficientlysecure.keychain.util;

import android.os.Parcel;
import android.os.Parcelable;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;

import java.util.Collection;
import java.util.HashMap;

public class KeyringPassphrases implements Parcelable {

    public final long mMasterKeyId;
    public final HashMap<Long, Passphrase> mSubkeyPassphrases;
    public final Passphrase mKeyringPassphrase;


    public KeyringPassphrases(long masterKeyId, Passphrase keyringPassphrase) {
        mMasterKeyId = masterKeyId;
        mSubkeyPassphrases = new HashMap<>();
        mKeyringPassphrase = keyringPassphrase;
    }

    /**
     * Returns a subkey passphrase, with no guarantees on consistency
     */
    public Passphrase getSingleSubkeyPassphrase() {
        if (mSubkeyPassphrases.size() > 0) {
            return mSubkeyPassphrases.values().iterator().next();
        } else {
            return null;
        }
    }

    public boolean subKeysHaveSamePassphrase() {
        if (mSubkeyPassphrases.size() < 2) {
            return true;
        } else {
            Passphrase previous = null;
            for(Passphrase current : mSubkeyPassphrases.values()) {
                if(previous != null && !current.equals(previous)) {
                    return false;
                }
                previous = current;
            }
            return true;
        }
    }

    public void removeFromMemory() {
        if (mKeyringPassphrase != null) {
            mKeyringPassphrase.removeFromMemory();
        }

        for (Passphrase passphrase : mSubkeyPassphrases.values()) {
            passphrase.removeFromMemory();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mMasterKeyId);
        parcel.writeParcelable(mKeyringPassphrase, flags);
        parcel.writeParcelable(ParcelableHashMap.toParcelableHashMap(mSubkeyPassphrases), flags);
    }

    private KeyringPassphrases(Parcel source) {
        mMasterKeyId = source.readLong();
        mKeyringPassphrase = source.readParcelable(Passphrase.class.getClassLoader());
        ParcelableHashMap<ParcelableLong, Passphrase> parcelableHashMap =
                source.readParcelable(ParcelableHashMap.class.getClassLoader());
        mSubkeyPassphrases = ParcelableHashMap.toHashMap(parcelableHashMap);
    }

    public static final Creator<KeyringPassphrases> CREATOR =
            new Creator<KeyringPassphrases>() {
                @Override
                public KeyringPassphrases createFromParcel(Parcel parcel) {
                    return new KeyringPassphrases(parcel);
                }

                @Override
                public KeyringPassphrases[] newArray(int i) {
                    return new KeyringPassphrases[i];
                }
            };

    public static KeyringPassphrases getKeyringPassphrases(Collection<KeyringPassphrases> collection,
                                                           long masterKeyId) {
        for (KeyringPassphrases keyringPassphrases : collection) {
            if(keyringPassphrases.mMasterKeyId == masterKeyId) {
                return keyringPassphrases;
            }
        }

        return null;
    }

    public static class SubKeyInfo {
        public final long mMasterKeyId;
        public final long mSubKeyId;
        public final ParcelableKeyRing mKeyRing;

        public SubKeyInfo(long masterKeyId, long subKeyId, ParcelableKeyRing keyRing) {
            mMasterKeyId = masterKeyId;
            mSubKeyId = subKeyId;
            mKeyRing = keyRing;
        }
    }
}
