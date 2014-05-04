package org.sufficientlysecure.keychain.pgp;

public abstract class CachedKeyRing {

    private final long mMasterKeyId;
    private final String mUserId;
    private final boolean mHasAnySecret;
    private final boolean mIsRevoked;
    private final boolean mCanCertify;
    private final long mHasEncryptId;
    private final long mHasSignId;
    private final int mVerified;

    protected CachedKeyRing(long masterKeyId, String userId, boolean hasAnySecret,
                            boolean isRevoked, boolean canCertify, long hasEncryptId, long hasSignId,
                            int verified)
    {
        mMasterKeyId = masterKeyId;
        mUserId = userId;
        mHasAnySecret = hasAnySecret;
        mIsRevoked = isRevoked;
        mCanCertify = canCertify;
        mHasEncryptId = hasEncryptId;
        mHasSignId = hasSignId;
        mVerified = verified;
    }

    public long getMasterKeyId() {
        return mMasterKeyId;
    }

    public String getPrimaryUserId() {
        return mUserId;
    }

    public boolean hasAnySecret() {
        return mHasAnySecret;
    }

    public boolean isRevoked() {
        return mIsRevoked;
    }

    public boolean canCertify() {
        return mCanCertify;
    }

    public long getEncryptId() {
        return mHasEncryptId;
    }

    public boolean hasEncrypt() {
        return mHasEncryptId != 0;
    }

    public long getSignId() {
        return mHasSignId;
    }

    public boolean hasSign() {
        return mHasSignId != 0;
    }

    public int getVerified() {
        return mVerified;
    }

}
