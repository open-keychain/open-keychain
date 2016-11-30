package org.sufficientlysecure.keychain.util;

import java.util.Iterator;

/**
 * An extended iterator interface, which knows the total number of its entries beforehand.
 */
public interface IteratorWithSize<E> extends Iterator<E> {

    /**
     * Returns the total number of entries in this iterator.
     *
     * @return the number of entries in this iterator.
     */
    int getSize();

}
