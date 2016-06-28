package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;

public interface ImportKeysListener {

    void loadKeys(LoaderState loaderState);

    void importKey(ParcelableKeyRing keyRing);

    void importKeys();

    void handleResult(ImportKeyResult result);

}
