package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.util.Iterator;

public class CanonicalizedPublicKeyRing extends CanonicalizedKeyRing {

    private PGPPublicKeyRing mRing;

    CanonicalizedPublicKeyRing(PGPPublicKeyRing ring, int verified) {
        super(verified);
        mRing = ring;
    }

    public CanonicalizedPublicKeyRing(byte[] blob, int verified) {
        super(verified);
        if(mRing == null) {
            // get first object in block
            PGPObjectFactory factory = new PGPObjectFactory(blob);
            try {
                Object obj = factory.nextObject();
                if (! (obj instanceof PGPPublicKeyRing)) {
                    throw new RuntimeException("Error constructing CanonicalizedPublicKeyRing, should never happen!");
                }
                mRing = (PGPPublicKeyRing) obj;
                if (factory.nextObject() != null) {
                    throw new RuntimeException("Encountered trailing data after keyring, should never happen!");
                }
            } catch (IOException e) {
                throw new RuntimeException("IO Error constructing CanonicalizedPublicKeyRing, should never happen!");
            }
        }
    }

    PGPPublicKeyRing getRing() {
        return mRing;
    }

    public void encode(ArmoredOutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    /** Getter that returns the subkey that should be used for signing. */
    CanonicalizedPublicKey getEncryptionSubKey() throws PgpGeneralException {
        PGPPublicKey key = getRing().getPublicKey(getEncryptId());
        if(key != null) {
            CanonicalizedPublicKey cKey = new CanonicalizedPublicKey(this, key);
            if(!cKey.canEncrypt()) {
                throw new PgpGeneralException("key error");
            }
            return cKey;
        }
        throw new PgpGeneralException("no encryption key available");
    }

    public IterableIterator<CanonicalizedPublicKey> publicKeyIterator() {
        @SuppressWarnings("unchecked")
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<CanonicalizedPublicKey>(new Iterator<CanonicalizedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedPublicKey next() {
                return new CanonicalizedPublicKey(CanonicalizedPublicKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}