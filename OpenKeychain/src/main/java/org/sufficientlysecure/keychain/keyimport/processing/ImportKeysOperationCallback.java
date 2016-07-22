package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

public class ImportKeysOperationCallback implements
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    private ImportKeysResultListener mResultListener;
    private ImportKeyringParcel mKeyringParcel;

    public ImportKeysOperationCallback(
            ImportKeysResultListener resultListener,
            ImportKeyringParcel inputParcel
    ) {
        this.mResultListener = resultListener;
        this.mKeyringParcel = inputParcel;
    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        return mKeyringParcel;
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        mResultListener.handleResult(result);
    }

    @Override
    public void onCryptoOperationCancelled() {
        // do nothing
    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        mResultListener.handleResult(result);
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

}
