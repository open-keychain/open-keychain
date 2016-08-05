package org.sufficientlysecure.keychain.operations.results;


import android.os.Parcel;

public class MigrateSymmetricResult extends OperationResult {

    public MigrateSymmetricResult(int result, OperationLog log) {
        super(result, log);
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public MigrateSymmetricResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<MigrateSymmetricResult> CREATOR = new Creator<MigrateSymmetricResult>() {
        public MigrateSymmetricResult createFromParcel(final Parcel source) {
            return new MigrateSymmetricResult(source);
        }

        public MigrateSymmetricResult[] newArray(final int size) {
            return new MigrateSymmetricResult[size];
        }
    };

}
