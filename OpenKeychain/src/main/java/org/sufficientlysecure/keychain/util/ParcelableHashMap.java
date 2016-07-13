package org.sufficientlysecure.keychain.util;


import java.util.HashMap;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.KeychainApplication;


public class ParcelableHashMap <K extends Parcelable, V extends Parcelable> implements Parcelable {

    HashMap<K,V> mInner;

    public ParcelableHashMap(HashMap<K,V> inner) {
        mInner = inner;
    }

    protected ParcelableHashMap(@NonNull Parcel in) {
        mInner = new HashMap<>();
        ClassLoader loader = KeychainApplication.class.getClassLoader();

        int num = in.readInt();
        while (num-- > 0) {
            K key = in.readParcelable(loader);
            V val = in.readParcelable(loader);
            mInner.put(key, val);
        }
    }

    public HashMap<K,V> getMap() {
        return mInner;
    }

    public static ParcelableHashMap<ParcelableLong, Passphrase> toParcelableHashMap(HashMap<Long, Passphrase> hashMap) {
        if (hashMap == null) {
            return null;
        }
        HashMap<ParcelableLong, Passphrase> forParceling = new HashMap<>();
        Set<Long> keys = hashMap.keySet();
        for (Long key : keys) {
            forParceling.put(new ParcelableLong(key), hashMap.get(key));
        }
        return new ParcelableHashMap<>(forParceling);
    }

    public static HashMap<Long, Passphrase> toHashMap(ParcelableHashMap<ParcelableLong, Passphrase> parcelableHashMap) {
        if (parcelableHashMap == null) {
            return null;
        }
        HashMap<ParcelableLong, Passphrase> toProcess = parcelableHashMap.getMap();
        HashMap<Long, Passphrase> result = new HashMap<>();
        Set<ParcelableLong> keys = toProcess.keySet();
        for (ParcelableLong key : keys) {
            result.put(key.mValue, toProcess.get(key));
        }
        return result;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mInner.size());
        for (HashMap.Entry<K,V> entry : mInner.entrySet()) {
            parcel.writeParcelable(entry.getKey(), 0);
            parcel.writeParcelable(entry.getValue(), 0);
        }
    }

    public static final Creator<ParcelableHashMap> CREATOR = new Creator<ParcelableHashMap>() {
        @Override
        public ParcelableHashMap createFromParcel(Parcel in) {
            return new ParcelableHashMap(in);
        }

        @Override
        public ParcelableHashMap[] newArray(int size) {
            return new ParcelableHashMap[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
