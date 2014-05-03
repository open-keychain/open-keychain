package org.sufficientlysecure.keychain.pgp;

public abstract class CachedKeyRing {

    private final long mMasterKeyId;
    private final boolean mCanCertify;
    private final byte[] mFingerprint;
    private final String mUserId;
    private final int mVerified;
    private final boolean mHasSecret;

    protected CachedKeyRing(long masterKeyId, boolean canCertify,
            byte[] fingerprint, String userId, int verified, boolean hasSecret)
    {
        mMasterKeyId = masterKeyId;
        mCanCertify = canCertify;
        mFingerprint = fingerprint;
        mUserId = userId;
        mVerified = verified;
        mHasSecret = hasSecret;
    }

    public byte[] getFingerprint() {
        return mFingerprint;
    }

    public String getPrimaryUserId() {
        return mUserId;
    }

    public long getMasterKeyId() {
        return mMasterKeyId;
    }

    public int getVerified() {
        return mVerified;
    }

    public boolean canCertify() {
        return mCanCertify;
    }

    public boolean hasSecret() {
        return mHasSecret;
    }

}
