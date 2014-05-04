package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.util.Iterator;

public class CachedSecretKeyRing extends CachedKeyRing {

    private PGPSecretKeyRing mRing;

    public CachedSecretKeyRing(long masterKeyId, String userId, boolean hasAnySecret,
                               boolean isRevoked, boolean canCertify, long hasEncryptId, long hasSignId,
                               int verified, byte[] blob)
    {
        super(masterKeyId, userId, hasAnySecret,
                isRevoked, canCertify, hasEncryptId, hasSignId,
                verified);

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

    /** Getter that returns the subkey that should be used for signing. */
    CachedSecretKey getSigningSubKey() throws PgpGeneralException {
        PGPSecretKey key = mRing.getSecretKey(getSignId());
        if(key != null) {
            CachedSecretKey cKey = new CachedSecretKey(this, key);
            if(!cKey.canSign()) {
                throw new PgpGeneralException("key error");
            }
            return cKey;
        }
        // TODO handle with proper exception
        throw new PgpGeneralException("no signing key available");
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

    public IterableIterator<CachedSecretKey> iterator() {
        final Iterator<PGPSecretKey> it = mRing.getSecretKeys();
        return new IterableIterator<CachedSecretKey>(new Iterator<CachedSecretKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CachedSecretKey next() {
                return new CachedSecretKey(CachedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}
