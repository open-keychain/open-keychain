package org.sufficientlysecure.keychain.pgp;

public interface PassphraseCacheInterface {
    public static class NoSecretKeyException extends Exception {
        public NoSecretKeyException() {
        }
    }

    public String getCachedPassphrase(long masterKeyId) throws NoSecretKeyException;

}
