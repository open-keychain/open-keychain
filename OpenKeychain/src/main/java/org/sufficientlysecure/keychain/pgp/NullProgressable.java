package org.sufficientlysecure.keychain.pgp;

/**
 * No-op implementation of Progressable
 */
public class NullProgressable implements Progressable {

    @Override
    public void setProgress(String message, int current, int total) {
    }

    @Override
    public void setProgress(int resourceId, int current, int total) {
    }

    @Override
    public void setProgress(int current, int total) {
    }
}
