package org.sufficientlysecure.keychain.keyimport.processing;

public interface LoaderState {

    /**
     * Basic mode includes ability to import all keys retrieved from the selected source
     * This doesn't make sense for all sources (for example keyservers..)
     *
     * @return if currently selected source supports basic mode
     */
    boolean isBasicModeSupported();

}
