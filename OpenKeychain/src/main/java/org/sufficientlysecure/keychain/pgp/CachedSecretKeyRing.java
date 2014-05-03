package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.util.Iterator;

public class CachedSecretKeyRing extends CachedKeyRing {

    private PGPSecretKeyRing mRing;

    public CachedSecretKeyRing(long masterKeyId, int keySize, boolean isRevoked,
                               boolean canCertify, long creation, long expiry, int algorithm,
                               byte[] fingerprint, String userId, int verified, boolean hasSecret,
                               byte[] blob)
    {
        super(masterKeyId, canCertify, fingerprint, userId, verified, hasSecret);

        mRing = (PGPSecretKeyRing) PgpConversionHelper.BytesToPGPKeyRing(blob);
    }

    PGPSecretKeyRing getRing() {
        return mRing;
    }

    public CachedSecretKey getSubKey() {
        return new CachedSecretKey(this, mRing.getSecretKey());
    }

    public CachedSecretKey getSubKey(long id) {
        return new CachedSecretKey(this, mRing.getSecretKey(id));
    }

    public IterableIterator<CachedSecretKey> iterator() {
        return new IterableIterator<CachedSecretKey>(mRing.getSecretKeys());
    }

    public boolean hasPassphrase() {
        PGPSecretKey secretKey = null;
        boolean foundValidKey = false;
        for (Iterator keys = mRing.getSecretKeys(); keys.hasNext(); ) {
            secretKey = (PGPSecretKey) keys.next();
            if (!secretKey.isPrivateKeyEmpty()) {
                foundValidKey = true;
                break;
            }
        }
        if(!foundValidKey) {
            return false;
        }

        try {
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                    .setProvider("SC").build("".toCharArray());
            PGPPrivateKey testKey = secretKey.extractPrivateKey(keyDecryptor);
            return testKey == null;
        } catch(PGPException e) {
            // this means the crc check failed -> passphrase required
            return true;
        }
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

    public UncachedSecretKeyRing changeSecretKeyPassphrase(String oldPassphrase,
                                                     String newPassphrase)
            throws IOException, PGPException, NoSuchProviderException {

        if (oldPassphrase == null) {
            oldPassphrase = "";
        }
        if (newPassphrase == null) {
            newPassphrase = "";
        }

        PGPSecretKeyRing newKeyRing = PGPSecretKeyRing.copyWithNewPassword(
                mRing,
                new JcePBESecretKeyDecryptorBuilder(new JcaPGPDigestCalculatorProviderBuilder()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build()).setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(oldPassphrase.toCharArray()),
                new JcePBESecretKeyEncryptorBuilder(mRing.getSecretKey()
                        .getKeyEncryptionAlgorithm()).build(newPassphrase.toCharArray()));

        return new UncachedSecretKeyRing(newKeyRing);

    }

}
