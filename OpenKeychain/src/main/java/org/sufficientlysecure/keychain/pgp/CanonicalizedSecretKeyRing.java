package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class CanonicalizedSecretKeyRing extends CanonicalizedKeyRing {

    private PGPSecretKeyRing mRing;

    CanonicalizedSecretKeyRing(PGPSecretKeyRing ring, int verified) {
        super(verified);
        mRing = ring;
    }

    public CanonicalizedSecretKeyRing(byte[] blob, boolean isRevoked, int verified)
    {
        super(verified);
        PGPObjectFactory factory = new PGPObjectFactory(blob);
        PGPKeyRing keyRing = null;
        try {
            if ((keyRing = (PGPKeyRing) factory.nextObject()) == null) {
                Log.e(Constants.TAG, "No keys given!");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while converting to PGPKeyRing!", e);
        }

        mRing = (PGPSecretKeyRing) keyRing;
    }

    PGPSecretKeyRing getRing() {
        return mRing;
    }

    public CanonicalizedSecretKey getSecretKey() {
        return new CanonicalizedSecretKey(this, mRing.getSecretKey());
    }

    public CanonicalizedSecretKey getSecretKey(long id) {
        return new CanonicalizedSecretKey(this, mRing.getSecretKey(id));
    }

    public HashSet<Long> getAvailableSubkeys() {
        HashSet<Long> result = new HashSet<Long>();
        // then, mark exactly the keys we have available
        for (PGPSecretKey sub : new IterableIterator<PGPSecretKey>(getRing().getSecretKeys())) {
            S2K s2k = sub.getS2K();
            // add key, except if the private key has been stripped (GNU extension)
            if(s2k == null || (s2k.getProtectionMode() != S2K.GNU_PROTECTION_MODE_NO_PRIVATE_KEY)) {
                result.add(sub.getKeyID());
            }
        }
        return result;
    }

    /** Getter that returns the subkey that should be used for signing. */
    CanonicalizedSecretKey getSigningSubKey() throws PgpGeneralException {
        PGPSecretKey key = mRing.getSecretKey(getSignId());
        if(key != null) {
            CanonicalizedSecretKey cKey = new CanonicalizedSecretKey(this, key);
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

    public IterableIterator<CanonicalizedSecretKey> secretKeyIterator() {
        final Iterator<PGPSecretKey> it = mRing.getSecretKeys();
        return new IterableIterator<CanonicalizedSecretKey>(new Iterator<CanonicalizedSecretKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedSecretKey next() {
                return new CanonicalizedSecretKey(CanonicalizedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

    public IterableIterator<CanonicalizedPublicKey> publicKeyIterator() {
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<CanonicalizedPublicKey>(new Iterator<CanonicalizedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedPublicKey next() {
                return new CanonicalizedPublicKey(CanonicalizedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}
