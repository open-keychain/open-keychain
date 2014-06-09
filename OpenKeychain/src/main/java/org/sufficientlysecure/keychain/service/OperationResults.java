package org.sufficientlysecure.keychain.service;

import android.os.Parcel;

public abstract class OperationResults {

    public static class ImportResult extends OperationResultParcel {

        public final int mNewKeys, mUpdatedKeys, mBadKeys;

        // Operation ok, at least one new key (no warnings)
        public static final int RESULT_OK_NEWKEYS = 1;
        // Operation ok, at least one new and one updated key (no warnings)
        public static final int RESULT_OK_BOTHKEYS = 2;
        // Operation ok, no new keys but upated ones (no warnings)
        public static final int RESULT_OK_UPDATED = 3;
        // Operation ok, but with warnings
        public static final int RESULT_OK_WITH_WARNINGS = 4;

        // Operation partially ok, but at least one key failed!
        public static final int RESULT_PARTIAL_WITH_ERRORS = 50;

        // Operation failed, errors thrown and no new keys imported
        public static final int RESULT_FAIL_ERROR = 100;
        // Operation failed, no keys to import...
        public static final int RESULT_FAIL_NOTHING = 101;

        public ImportResult(Parcel source) {
            super(source);
            mNewKeys = source.readInt();
            mUpdatedKeys = source.readInt();
            mBadKeys = source.readInt();
        }

        public ImportResult(int result, OperationLog log,
                            int newKeys, int updatedKeys, int badKeys) {
            super(result, log);
            mNewKeys = newKeys;
            mUpdatedKeys = updatedKeys;
            mBadKeys = badKeys;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mNewKeys);
            dest.writeInt(mUpdatedKeys);
            dest.writeInt(mBadKeys);
        }

        public static Creator<ImportResult> CREATOR = new Creator<ImportResult>() {
            public ImportResult createFromParcel(final Parcel source) {
                return new ImportResult(source);
            }

            public ImportResult[] newArray(final int size) {
                return new ImportResult[size];
            }
        };

    }

}
