package org.sufficientlysecure.keychain.operations.results;


import android.os.Parcel;

import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;


public class InputPendingResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    final RequiredInputParcel mRequiredInput;

    public InputPendingResult(int result, OperationLog log) {
        super(result, log);
        mRequiredInput = null;
    }

    public InputPendingResult(OperationLog log, RequiredInputParcel requiredInput) {
        super(RESULT_PENDING, log);
        mRequiredInput = requiredInput;
    }

    public InputPendingResult(Parcel source) {
        super(source);
        mRequiredInput = source.readParcelable(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mRequiredInput, 0);
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public RequiredInputParcel getRequiredInputParcel() {
        return mRequiredInput;
    }

}
