package org.sufficientlysecure.keychain.service;


import java.util.Iterator;

import android.support.v4.util.LongSparseArray;


class PassphraseCache<T> {
    private final LongSparseArray<T> passphraseCache = new LongSparseArray<>();
    private final OnDeleteListener<T> onDeleteListener;

    interface OnDeleteListener<T> {
        void onDelete(T cachedPassphrase);
    }

    PassphraseCache(OnDeleteListener<T> onDeleteListener) {
        this.onDeleteListener = onDeleteListener;
    }

    T get(long keyId) {
        return passphraseCache.get(keyId);
    }

    void put(long referenceKeyId, T cachedPassphrase) {
        passphraseCache.put(referenceKeyId, cachedPassphrase);
    }

    void removeByReferenceId(long keyId) {
        int index = passphraseCache.indexOfKey(keyId);
        if (index > 0) {
            removeFromCacheAt(index);
        }
    }

    int count() {
        return passphraseCache.size();
    }

    void clearAll() {
        Iterator<T> it = iterateCachedPassphrases();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    Iterator<T> iterateCachedPassphrases() {
        return new Iterator<T>() {
            int position = 0;

            @Override
            public boolean hasNext() {
                return position < passphraseCache.size();
            }

            @Override
            public T next() {
                return passphraseCache.valueAt(position++);
            }

            @Override
            public void remove() {
                if (position == 0) {
                    throw new IllegalStateException();
                }

                position -= 1;
                removeFromCacheAt(position);
            }
        };
    }

    private void removeFromCacheAt(int index) {
        T cachedPassphrase = passphraseCache.valueAt(index);
        passphraseCache.removeAt(index);

        if (onDeleteListener != null) {
            onDeleteListener.onDelete(cachedPassphrase);
        }
    }
}
