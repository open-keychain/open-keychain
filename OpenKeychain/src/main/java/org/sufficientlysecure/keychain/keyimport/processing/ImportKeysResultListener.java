package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;

public interface ImportKeysResultListener {

    void handleResult(ImportKeyResult result);

}
