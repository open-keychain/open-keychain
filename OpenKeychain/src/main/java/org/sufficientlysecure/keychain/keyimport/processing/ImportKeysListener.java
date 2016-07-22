package org.sufficientlysecure.keychain.keyimport.processing;

public interface ImportKeysListener extends ImportKeysResultListener {

    void loadKeys(LoaderState loaderState);

    void importKeys();

}
