package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPSecretKey;

import java.io.IOException;
import java.io.OutputStream;

public class UncachedSecretKey extends UncachedPublicKey {

    public static final int CERTIFY_OTHER = KeyFlags.CERTIFY_OTHER;
    public static final int SIGN_DATA = KeyFlags.SIGN_DATA;
    public static final int ENCRYPT_COMMS = KeyFlags.ENCRYPT_COMMS;
    public static final int ENCRYPT_STORAGE = KeyFlags.ENCRYPT_STORAGE;
    public static final int AUTHENTICATION = KeyFlags.AUTHENTICATION;

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
