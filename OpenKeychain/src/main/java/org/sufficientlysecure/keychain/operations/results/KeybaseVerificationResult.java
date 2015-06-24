package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;
import android.os.Parcelable;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.prover.Prover;

public class KeybaseVerificationResult extends OperationResult implements Parcelable {
    public final String mProofUrl;
    public final String mPresenceUrl;
    public final String mPresenceLabel;

    public KeybaseVerificationResult(int result, OperationLog log) {
        super(result, log);
        mProofUrl = null;
        mPresenceLabel = null;
        mPresenceUrl = null;
    }

    public KeybaseVerificationResult(int result, OperationLog log, Prover prover)
            throws KeybaseException {
        super(result, log);
        mProofUrl = prover.getProofUrl();
        mPresenceUrl = prover.getPresenceUrl();
        mPresenceLabel = prover.getPresenceLabel();
    }

    protected KeybaseVerificationResult(Parcel in) {
        super(in);
        mProofUrl = in.readString();
        mPresenceUrl = in.readString();
        mPresenceLabel = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mProofUrl);
        dest.writeString(mPresenceUrl);
        dest.writeString(mPresenceLabel);
    }

    public static final Parcelable.Creator<KeybaseVerificationResult> CREATOR = new Parcelable.Creator<KeybaseVerificationResult>() {
        @Override
        public KeybaseVerificationResult createFromParcel(Parcel in) {
            return new KeybaseVerificationResult(in);
        }

        @Override
        public KeybaseVerificationResult[] newArray(int size) {
            return new KeybaseVerificationResult[size];
        }
    };
}