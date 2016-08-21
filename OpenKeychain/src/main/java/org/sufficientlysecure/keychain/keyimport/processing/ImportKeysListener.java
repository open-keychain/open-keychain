package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;

import java.util.List;

public interface ImportKeysListener extends ImportKeysResultListener {

    void loadKeys(LoaderState loaderState);

    void importKeys(List<ImportKeysListEntry> entries);

}
