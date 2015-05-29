package org.sufficientlysecure.keychain.ui.base;


import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;


public abstract class CachingCryptoOperationFragment <T extends Parcelable> extends CryptoOperationFragment {

    public static final String ARG_CACHED_ACTIONS = "cached_actions";

    private T mCachedActionsParcel;

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        cryptoOperation(cryptoInput, mCachedActionsParcel);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_CACHED_ACTIONS, mCachedActionsParcel);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCachedActionsParcel = savedInstanceState.getParcelable(ARG_CACHED_ACTIONS);
        }
    }

    @Override
    public boolean handlePendingMessage(Message message) {
        // see if it's an InputPendingResult, and if so don't care
        if (super.handlePendingMessage(message)) {
            return true;
        }

        // if it's a non-input-pending OKAY message, always clear the cached actions parcel
        if (message.arg1 == ServiceProgressHandler.MessageStatus.OKAY.ordinal()) {
            mCachedActionsParcel = null;
        }

        return false;
    }

    protected abstract void cryptoOperation(CryptoInputParcel cryptoInput, T cachedActionsParcel);

    protected void cacheActionsParcel(T cachedActionsParcel) {
        mCachedActionsParcel = cachedActionsParcel;
    }

    protected void onCryptoOperationCancelled() {
        mCachedActionsParcel = null;
    }

}
