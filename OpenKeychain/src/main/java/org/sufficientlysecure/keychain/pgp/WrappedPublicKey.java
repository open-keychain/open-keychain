package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.util.IterableIterator;

/** Wrapper for a PGPPublicKey.
 *
 * The methods implemented in this class are a thin layer over
 * UncachedPublicKey. The difference between the two classes is that objects of
 * this class can only be obtained from a WrappedKeyRing, and that it stores a
 * back reference to its parent as well. A method which works with
 * WrappedPublicKey is therefore guaranteed to work on a KeyRing which is
 * stored in the database.
 *
 */
public class WrappedPublicKey extends UncachedPublicKey {

    // this is the parent key ring
    final KeyRing mRing;

    WrappedPublicKey(KeyRing ring, PGPPublicKey key) {
        super(key);
        mRing = ring;
    }

    public IterableIterator<String> getUserIds() {
        return new IterableIterator<String>(mPublicKey.getUserIDs());
    }

    public KeyRing getKeyRing() {
        return mRing;
    }

    JcePublicKeyKeyEncryptionMethodGenerator getPubKeyEncryptionGenerator() {
        return  new JcePublicKeyKeyEncryptionMethodGenerator(mPublicKey);
    }

}
