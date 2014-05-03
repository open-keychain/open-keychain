package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UncachedKeyRing {

    final PGPPublicKeyRing mPublicRing;
    final PGPSecretKeyRing mSecretRing;

    UncachedKeyRing(PGPPublicKeyRing publicRing, PGPSecretKeyRing secretRing) {
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

    public byte[] getFingerprint() {
        return mPublicRing.getPublicKey().getFingerprint();
    }

    public static UncachedKeyRing decodePubkeyFromData(byte[] data)
            throws PgpGeneralException, IOException {
        BufferedInputStream bufferedInput =
                new BufferedInputStream(new ByteArrayInputStream(data));
        if (bufferedInput.available() > 0) {
            InputStream in = PGPUtil.getDecoderStream(bufferedInput);
            PGPObjectFactory objectFactory = new PGPObjectFactory(in);

            // get first object in block
            Object obj;
            if ((obj = objectFactory.nextObject()) != null && obj instanceof PGPPublicKeyRing) {
                return new UncachedKeyRing((PGPPublicKeyRing) obj);
            } else {
                throw new PgpGeneralException("Object not recognized as PGPPublicKeyRing!");
            }
        } else {
            throw new PgpGeneralException("Object not recognized as PGPPublicKeyRing!");
        }
    }

}
