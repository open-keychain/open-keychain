package org.sufficientlysecure.keychain.ui.base;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainNewService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;


public abstract class CachingCryptoOperationFragment <T extends Parcelable, S extends OperationResult>
        extends CryptoOperationFragment<T, S> {

    public static final String ARG_CACHED_ACTIONS = "cached_actions";

    private T mCachedActionsParcel;

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
    protected void onCryptoOperationResult(S result) {
        super.onCryptoOperationResult(result);
        mCachedActionsParcel = null;
    }

    protected abstract T createOperationInput();

    protected void cryptoOperation(CryptoInputParcel cryptoInput) {

        if (mCachedActionsParcel == null) {

            mCachedActionsParcel = createOperationInput();
            // this is null if invalid, just return in that case
            if (mCachedActionsParcel == null) {
                // Notify was created by createCryptoInput.
                return;
            }
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainNewService.class);

        intent.putExtra(KeychainNewService.EXTRA_OPERATION_INPUT, mCachedActionsParcel);
        intent.putExtra(KeychainNewService.EXTRA_CRYPTO_INPUT, cryptoInput);

        showProgressFragment(
                getString(R.string.progress_start),
                ProgressDialog.STYLE_HORIZONTAL,
                false);

        // start service with intent
        getActivity().startService(intent);

    }

    protected T getCachedActionsParcel() {
        return mCachedActionsParcel;
    }

    protected void cacheActionsParcel(T cachedActionsParcel) {
        mCachedActionsParcel = cachedActionsParcel;
    }

    protected void onCryptoOperationCancelled() {
        mCachedActionsParcel = null;
    }

}
