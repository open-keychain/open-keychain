package org.sufficientlysecure.keychain.operations;


import android.os.Parcelable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class KeySyncParcel implements Parcelable {
    public abstract boolean getRefreshAll();

    public static KeySyncParcel createRefreshAll() {
        return new AutoValue_KeySyncParcel(true);
    }

    public static KeySyncParcel createRefreshOutdated() {
        return new AutoValue_KeySyncParcel(false);
    }
}
