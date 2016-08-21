package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

public class ChangePassphraseWorkflowResult extends OperationResult {
    public ChangePassphraseWorkflowResult(int result, OperationLog log) {
        super(result, log);
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public ChangePassphraseWorkflowResult(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static Creator<ChangePassphraseWorkflowResult> CREATOR = new Creator<ChangePassphraseWorkflowResult>() {
        public ChangePassphraseWorkflowResult createFromParcel(final Parcel source) {
            return new ChangePassphraseWorkflowResult(source);
        }

        public ChangePassphraseWorkflowResult[] newArray(final int size) {
            return new ChangePassphraseWorkflowResult[size];
        }
    };

}
