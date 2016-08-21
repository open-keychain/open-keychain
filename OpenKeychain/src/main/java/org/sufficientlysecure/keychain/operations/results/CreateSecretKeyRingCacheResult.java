package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

public class CreateSecretKeyRingCacheResult extends OperationResult {

    public CreateSecretKeyRingCacheResult(int result, OperationLog log) {
        super(result, log);
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public CreateSecretKeyRingCacheResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<CreateSecretKeyRingCacheResult> CREATOR = new Creator<CreateSecretKeyRingCacheResult>() {
        public CreateSecretKeyRingCacheResult createFromParcel(final Parcel source) {
            return new CreateSecretKeyRingCacheResult(source);
        }

        public CreateSecretKeyRingCacheResult[] newArray(final int size) {
            return new CreateSecretKeyRingCacheResult[size];
        }
    };

}
