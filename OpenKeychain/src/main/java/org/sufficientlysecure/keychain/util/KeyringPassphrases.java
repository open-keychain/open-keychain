package org.sufficientlysecure.keychain.util;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class KeyringPassphrases implements Parcelable {

    public final long mMasterKeyId;
    public final HashMap<Long, Passphrase> mSubkeyPassphrases;
    public final Passphrase mNewKeyringPassphrase;

    // default new passphrase is an empty one
    public KeyringPassphrases(long masterKeyId) {
        mMasterKeyId = masterKeyId;
        mSubkeyPassphrases = new HashMap<>();
        mNewKeyringPassphrase = new Passphrase();
    }

    public KeyringPassphrases(long masterKeyId, Passphrase newKeyringPassphrase) {
        if (newKeyringPassphrase == null) {
            throw new IllegalArgumentException("newKeyringPassphrase cannot be null!");
        }
        mMasterKeyId = masterKeyId;
        mSubkeyPassphrases = new HashMap<>();
        mNewKeyringPassphrase = newKeyringPassphrase;
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

    private static ParcelableHashMap<ParcelableLong, Passphrase> toParcelableHashMap(HashMap<Long, Passphrase> hashMap) {
        HashMap<ParcelableLong, Passphrase> forParceling = new HashMap<>();
        Set<Long> keys = hashMap.keySet();
        for (Long key : keys) {
            forParceling.put(new ParcelableLong(key), hashMap.get(key));
        }
        return new ParcelableHashMap<>(forParceling);
    }

    private static HashMap<Long, Passphrase> fromParcelableHashMap(ParcelableHashMap<ParcelableLong, Passphrase> parcelableHashMap) {
        HashMap<ParcelableLong, Passphrase> toProcess = parcelableHashMap.getMap();
        HashMap<Long, Passphrase> result = new HashMap<>();
        Set<ParcelableLong> keys = toProcess.keySet();
        for (ParcelableLong key : keys) {
            result.put(key.mValue, toProcess.get(key));
        }
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mMasterKeyId);
        parcel.writeParcelable(mNewKeyringPassphrase, flags);
        parcel.writeParcelable(toParcelableHashMap(mSubkeyPassphrases), flags);
    }

    private KeyringPassphrases(Parcel source) {
        mMasterKeyId = source.readLong();
        mNewKeyringPassphrase = source.readParcelable(Passphrase.class.getClassLoader());
        ParcelableHashMap<ParcelableLong, Passphrase> parcelableHashMap =
                source.readParcelable(ParcelableHashMap.class.getClassLoader());
        mSubkeyPassphrases = fromParcelableHashMap(parcelableHashMap);
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

}
