package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;

public class UncachedKeyRing {

    final PGPPublicKeyRing mPublicRing;
    final PGPSecretKeyRing mSecretRing;

    UncachedKeyRing(PGPPublicKeyRing publicRing, PGPSecretKeyRing secretRing) {
        // this one must not be false!
        assert(publicRing != null);
        mPublicRing = publicRing;
        mSecretRing = secretRing;
    }

    UncachedKeyRing(PGPPublicKeyRing publicRing) {
        this(publicRing, null);
    }

    public PGPPublicKeyRing getPublicRing() {
        return mPublicRing;
    }

    public PGPSecretKeyRing getSecretRing() {
        return mSecretRing;
    }
}
