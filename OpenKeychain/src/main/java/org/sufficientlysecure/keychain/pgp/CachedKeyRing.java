package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPSignature;

public abstract class CachedKeyRing {

    private final long mMasterKeyId;
    private final int mKeySize;
    private final boolean mIsRevoked;
    private final boolean mCanCertify;
    private final long mCreation;
    private final long mExpiry;
    private final int mAlgorithm;
    private final byte[] mFingerprint;
    private final String mUserId;
    private final int mVerified;
    private final boolean mHasSecret;

    protected CachedKeyRing(long masterKeyId, int keySize, boolean isRevoked,
            boolean canCertify, long creation, long expiry, int algorithm,
            byte[] fingerprint, String userId, int verified, boolean hasSecret)
    {
        mMasterKeyId = masterKeyId;
        mKeySize = keySize;
        mIsRevoked = isRevoked;
        mCanCertify = canCertify;
        mCreation = creation;
        mExpiry = expiry;
        mAlgorithm = algorithm;
        mFingerprint = fingerprint;
        mUserId = userId;
        mVerified = verified;
        mHasSecret = hasSecret;
    }

    public boolean isRevoked() {
        return mIsRevoked;
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

    public void initSignature(PGPSignature sig) {

    }

}
