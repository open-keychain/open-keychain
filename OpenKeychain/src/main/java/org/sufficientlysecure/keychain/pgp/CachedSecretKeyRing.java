package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.sufficientlysecure.keychain.util.IterableIterator;

public class CachedSecretKeyRing extends CachedKeyRing {

    private PGPSecretKeyRing mRing;

    public CachedSecretKeyRing(long masterKeyId, int keySize, boolean isRevoked,
                               boolean canCertify, long creation, long expiry, int algorithm,
                               byte[] fingerprint, String userId, int verified, boolean hasSecret,
                               byte[] blob)
    {
        super(masterKeyId, keySize, isRevoked, canCertify, creation, expiry,
                algorithm, fingerprint, userId, verified, hasSecret);

        mRing = (PGPSecretKeyRing) PgpConversionHelper.BytesToPGPKeyRing(blob);
    }

    public CachedSecretKey getSubKey() {
        return new CachedSecretKey(this, mRing.getSecretKey());
    }

    public CachedSecretKey getSubKey(long id) {
        return new CachedSecretKey(this, mRing.getSecretKey(id));
    }

    /** This returns the subkey that should be used for signing.
     * At this point, this is simply the first suitable subkey.
     */
    CachedSecretKey getSigningSubKey() {
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(mRing.getSecretKeys())) {
            if (isSigningKey(key.getPublicKey())) {
                return new CachedSecretKey(this, key);
            }
        }
        // TODO exception
        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean isSigningKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        // special case
        if (key.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
        }

        return false;
    }

}
