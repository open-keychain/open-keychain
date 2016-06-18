package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

import java.util.ArrayList;

public class ImportKeysOperationCallback implements
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    private ImportKeysListener mResultListener;
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;

    public ImportKeysOperationCallback(
            ImportKeysListener resultListener,
            String keyserver,
            ArrayList<ParcelableKeyRing> keyList
    ) {
        this.mResultListener = resultListener;
        this.mKeyserver = keyserver;
        this.mKeyList = keyList;
    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
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
