package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;

public interface ImportKeysListener {

    void loadKeys(LoaderState loaderState);
    void importKeys();
    void handleResult(ImportKeyResult result);

}
