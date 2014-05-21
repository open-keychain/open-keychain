package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

public abstract class WrappedKeyRing extends KeyRing {

    private final boolean mHasAnySecret;
    private final int mVerified;

    WrappedKeyRing(boolean hasAnySecret, int verified) {
        mHasAnySecret = hasAnySecret;
        mVerified = verified;
    }

    public long getMasterKeyId() {
        return getRing().getPublicKey().getKeyID();
    }

    public boolean hasAnySecret() {
        return mHasAnySecret;
    }

    public int getVerified() {
        return mVerified;
    }

    public String getPrimaryUserId() throws PgpGeneralException {
        return (String) getRing().getPublicKey().getUserIDs().next();
    };

    public boolean isRevoked() throws PgpGeneralException {
        // Is the master key revoked?
        return getRing().getPublicKey().isRevoked();
    }

    public boolean canCertify() throws PgpGeneralException {
        return getRing().getPublicKey().isEncryptionKey();
    }

    public long getEncryptId() throws PgpGeneralException {
        for(PGPPublicKey key : new IterableIterator<PGPPublicKey>(getRing().getPublicKeys())) {
            if(PgpKeyHelper.isEncryptionKey(key)) {
                return key.getKeyID();
            }
        }
        throw new PgpGeneralException("No valid encryption key found!");
    }

    public boolean hasEncrypt() throws PgpGeneralException {
        try {
            getEncryptId();
            return true;
        } catch(PgpGeneralException e) {
            return false;
        }
    }

    public long getSignId() throws PgpGeneralException {
        for(PGPPublicKey key : new IterableIterator<PGPPublicKey>(getRing().getPublicKeys())) {
            if(PgpKeyHelper.isSigningKey(key)) {
                return key.getKeyID();
            }
        }
        throw new PgpGeneralException("No valid signing key found!");
    }

    public boolean hasSign() throws PgpGeneralException {
        try {
            getSignId();
            return true;
        } catch (PgpGeneralException e) {
            return false;
        }
    }

    abstract PGPKeyRing getRing();

}
