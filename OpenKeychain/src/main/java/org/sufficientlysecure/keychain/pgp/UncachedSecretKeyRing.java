package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPSecretKeyRing;

public class UncachedSecretKeyRing {

    final PGPSecretKeyRing mSecretRing;

    UncachedSecretKeyRing(PGPSecretKeyRing secretRing) {
        mSecretRing = secretRing;
    }

    // Breaking the pattern here, for key import!
    // TODO reduce this from public to default visibility!
    public PGPSecretKeyRing getSecretKeyRing() {
        return mSecretRing;
    }

}
