package org.sufficientlysecure.keychain.operations.results;


import android.os.Parcel;


public class GenericOperationResult extends OperationResult {
    public GenericOperationResult(int result, OperationLog log) {
        super(result, log);
    }

    public GenericOperationResult(Parcel source) {
        super(source);
    }

    public static final Creator<GenericOperationResult> CREATOR = new Creator<GenericOperationResult>() {
        public GenericOperationResult createFromParcel(final Parcel source) {
            return new GenericOperationResult(source);
        }

        public GenericOperationResult[] newArray(final int size) {
            return new GenericOperationResult[size];
        }
    };
}
