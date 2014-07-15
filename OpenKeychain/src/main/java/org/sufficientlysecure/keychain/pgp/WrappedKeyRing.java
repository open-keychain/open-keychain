package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.io.OutputStream;

/** A generic wrapped PGPKeyRing object.
 *
 * This class provides implementations for all basic getters which both
 * PublicKeyRing and SecretKeyRing have in common. To make the wrapped keyring
 * class typesafe in implementing subclasses, the field is stored in the
 * implementing class, providing properly typed access through the getRing
 * getter method.
 *
 */
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
        return getPublicKey().getPrimaryUserId();
    }

    public String getPrimaryUserIdWithFallback() throws PgpGeneralException {
        return getPublicKey().getPrimaryUserIdWithFallback();
    }

    public boolean isRevoked() throws PgpGeneralException {
        // Is the master key revoked?
        return getRing().getPublicKey().isRevoked();
    }

    public boolean canCertify() throws PgpGeneralException {
        return getRing().getPublicKey().isEncryptionKey();
    }

    public long getEncryptId() throws PgpGeneralException {
        for(WrappedPublicKey key : publicKeyIterator()) {
            if(key.canEncrypt()) {
                return key.getKeyId();
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
        for(WrappedPublicKey key : publicKeyIterator()) {
            if(key.canSign()) {
                return key.getKeyId();
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

    public void encode(OutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    /** Returns an UncachedKeyRing which wraps the same data as this ring. This method should
     * only be used */
    public UncachedKeyRing getUncachedKeyRing() {
        return new UncachedKeyRing(getRing());
    }

    abstract PGPKeyRing getRing();

    abstract public IterableIterator<WrappedPublicKey> publicKeyIterator();

    public WrappedPublicKey getPublicKey() {
        return new WrappedPublicKey(this, getRing().getPublicKey());
    }

    public WrappedPublicKey getPublicKey(long id) {
        return new WrappedPublicKey(this, getRing().getPublicKey(id));
    }

    public byte[] getEncoded() throws IOException {
        return getRing().getEncoded();
    }

}
