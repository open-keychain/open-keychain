package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPSecretKey;

import java.io.IOException;
import java.io.OutputStream;

public class UncachedSecretKey extends UncachedPublicKey {

    final PGPSecretKey mSecretKey;

    public UncachedSecretKey(PGPSecretKey secretKey) {
        super(secretKey.getPublicKey());
        mSecretKey = secretKey;
    }

    @Deprecated
    public PGPSecretKey getSecretKeyExternal() {
        return mSecretKey;
    }

    public void encodeSecretKey(OutputStream os) throws IOException {
        mSecretKey.encode(os);
    }

}
