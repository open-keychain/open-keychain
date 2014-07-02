package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.Iterator;

public class WrappedSecretKeyRing extends WrappedKeyRing {

    private PGPSecretKeyRing mRing;

    public WrappedSecretKeyRing(byte[] blob, boolean isRevoked, int verified)
    {
        super(isRevoked, verified);
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

    public WrappedSecretKey getSubKey() {
        return new WrappedSecretKey(this, mRing.getSecretKey());
    }

    public WrappedSecretKey getSubKey(long id) {
        return new WrappedSecretKey(this, mRing.getSecretKey(id));
    }

    /** Getter that returns the subkey that should be used for signing. */
    WrappedSecretKey getSigningSubKey() throws PgpGeneralException {
        PGPSecretKey key = mRing.getSecretKey(getSignId());
        if(key != null) {
            WrappedSecretKey cKey = new WrappedSecretKey(this, key);
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

    public IterableIterator<WrappedSecretKey> secretKeyIterator() {
        final Iterator<PGPSecretKey> it = mRing.getSecretKeys();
        return new IterableIterator<WrappedSecretKey>(new Iterator<WrappedSecretKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public WrappedSecretKey next() {
                return new WrappedSecretKey(WrappedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

    public IterableIterator<WrappedPublicKey> publicKeyIterator() {
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<WrappedPublicKey>(new Iterator<WrappedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public WrappedPublicKey next() {
                return new WrappedPublicKey(WrappedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}
