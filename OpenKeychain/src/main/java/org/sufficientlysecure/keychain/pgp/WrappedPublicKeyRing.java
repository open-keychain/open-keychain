package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.util.Iterator;

public class WrappedPublicKeyRing extends WrappedKeyRing {

    private PGPPublicKeyRing mRing;
    private final byte[] mPubKey;

    public WrappedPublicKeyRing(byte[] blob, boolean hasAnySecret, int verified) {
        super(hasAnySecret, verified);
        mPubKey = blob;
    }

    PGPPublicKeyRing getRing() {
        if(mRing == null) {
            // get first object in block
            PGPObjectFactory factory = new PGPObjectFactory(mPubKey);
            try {
                Object obj = factory.nextObject();
                if (! (obj instanceof PGPPublicKeyRing)) {
                    throw new RuntimeException("Error constructing WrappedPublicKeyRing, should never happen!");
                }
                mRing = (PGPPublicKeyRing) obj;
                if (factory.nextObject() != null) {
                    throw new RuntimeException("Encountered trailing data after keyring, should never happen!");
                }
            } catch (IOException e) {
                throw new RuntimeException("IO Error constructing WrappedPublicKeyRing, should never happen!");
            }
        }
        return mRing;
    }

    public void encode(ArmoredOutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    /** Getter that returns the subkey that should be used for signing. */
    WrappedPublicKey getEncryptionSubKey() throws PgpGeneralException {
        PGPPublicKey key = getRing().getPublicKey(getEncryptId());
        if(key != null) {
            WrappedPublicKey cKey = new WrappedPublicKey(this, key);
            if(!cKey.canEncrypt()) {
                throw new PgpGeneralException("key error");
            }
            return cKey;
        }
        throw new PgpGeneralException("no encryption key available");
    }

    public IterableIterator<WrappedPublicKey> publicKeyIterator() {
        @SuppressWarnings("unchecked")
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<WrappedPublicKey>(new Iterator<WrappedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public WrappedPublicKey next() {
                return new WrappedPublicKey(WrappedPublicKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}