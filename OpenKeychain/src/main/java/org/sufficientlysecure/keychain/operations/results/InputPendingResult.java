package org.sufficientlysecure.keychain.operations.results;


import android.os.Parcel;

import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;


public class InputPendingResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    public static final int RESULT_PENDING_PASSPHRASE = RESULT_PENDING + 16;
    public static final int RESULT_PENDING_NFC = RESULT_PENDING + 32;

    final RequiredInputParcel mRequiredInput;
    final Long mKeyIdPassphraseNeeded;

    public InputPendingResult(int result, OperationLog log) {
        super(result, log);
        mRequiredInput = null;
        mKeyIdPassphraseNeeded = null;
    }

    public InputPendingResult(OperationLog log, RequiredInputParcel requiredInput) {
        super(RESULT_PENDING_NFC, log);
        mRequiredInput = requiredInput;
        mKeyIdPassphraseNeeded = null;
    }

    public InputPendingResult(OperationLog log, long keyIdPassphraseNeeded) {
        super(RESULT_PENDING_PASSPHRASE, log);
        mRequiredInput = null;
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public InputPendingResult(Parcel source) {
        super(source);
        mRequiredInput = source.readParcelable(getClass().getClassLoader());
        mKeyIdPassphraseNeeded = source.readInt() != 0 ? source.readLong() : null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mRequiredInput, 0);
        if (mKeyIdPassphraseNeeded != null) {
            dest.writeInt(1);
            dest.writeLong(mKeyIdPassphraseNeeded);
        } else {
            dest.writeInt(0);
        }
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public boolean isNfcPending() {
        return (mResult & RESULT_PENDING_NFC) == RESULT_PENDING_NFC;
    }

    public boolean isPassphrasePending() {
        return (mResult & RESULT_PENDING_PASSPHRASE) == RESULT_PENDING_PASSPHRASE;
    }

    public RequiredInputParcel getRequiredInputParcel() {
        return mRequiredInput;
    }

    public long getPassphraseKeyId() {
        return mKeyIdPassphraseNeeded;
    }

}
