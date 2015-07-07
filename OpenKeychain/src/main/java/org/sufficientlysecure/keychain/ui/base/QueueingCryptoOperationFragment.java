package org.sufficientlysecure.keychain.ui.base;


import android.os.Bundle;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.operations.results.OperationResult;


public abstract class QueueingCryptoOperationFragment<T extends Parcelable, S extends OperationResult>
        extends CryptoOperationFragment<T,S> {

    public static final String ARG_QUEUED_RESULT = "queued_result";
    private S mQueuedResult;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mQueuedResult != null) {
            try {
                if (mQueuedResult.success()) {
                    onQueuedOperationSuccess(mQueuedResult);
                } else {
                    onQueuedOperationError(mQueuedResult);
                }
            } finally {
                mQueuedResult = null;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_QUEUED_RESULT, mQueuedResult);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mQueuedResult = savedInstanceState.getParcelable(ARG_QUEUED_RESULT);
        }
    }

    public abstract void onQueuedOperationSuccess(S result);

    public void onQueuedOperationError(S result) {
        super.onCryptoOperationError(result);
    }

    @Override
    final public void onCryptoOperationSuccess(S result) {
        if (getActivity() == null) {
            mQueuedResult = result;
            return;
        }
        onQueuedOperationSuccess(result);
    }

    @Override
    final public void onCryptoOperationError(S result) {
        if (getActivity() == null) {
            mQueuedResult = result;
            return;
        }
        onQueuedOperationError(result);
    }
}
