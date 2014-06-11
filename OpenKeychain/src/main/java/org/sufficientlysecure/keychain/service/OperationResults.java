package org.sufficientlysecure.keychain.service;

import android.os.Parcel;

public abstract class OperationResults {

    public static class ImportResult extends OperationResultParcel {

        public final int mNewKeys, mUpdatedKeys, mBadKeys;

        // At least one new key
        public static final int RESULT_OK_NEWKEYS = 2;
        // At least one updated key
        public static final int RESULT_OK_UPDATED = 4;
        // At least one key failed (might still be an overall success)
        public static final int RESULT_WITH_ERRORS = 8;
        // There are warnings in the log
        public static final int RESULT_WITH_WARNINGS = 16;

        // No keys to import...
        public static final int RESULT_FAIL_NOTHING = 32 +1;

        public boolean isOkBoth() {
            return (mResult & (RESULT_OK_NEWKEYS | RESULT_OK_UPDATED))
                    == (RESULT_OK_NEWKEYS | RESULT_OK_UPDATED);
        }
        public boolean isOkNew() {
            return (mResult & RESULT_OK_NEWKEYS) == RESULT_OK_NEWKEYS;
        }
        public boolean isOkUpdated() {
            return (mResult & RESULT_OK_UPDATED) == RESULT_OK_UPDATED;
        }
        public boolean isFailNothing() {
            return (mResult & RESULT_FAIL_NOTHING) == RESULT_FAIL_NOTHING;
        }

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

    public static class SaveKeyringResult extends OperationResultParcel {

        public SaveKeyringResult(int result, OperationLog log) {
            super(result, log);
        }

        // Some old key was updated
        public static final int UPDATED = 2;

        // Public key was saved
        public static final int SAVED_PUBLIC = 8;
        // Secret key was saved (not exclusive with public!)
        public static final int SAVED_SECRET = 16;

        public boolean updated() {
            return (mResult & UPDATED) == UPDATED;
        }

    }

}
