package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

public class UncachedKeyRing {

    final PGPKeyRing mRing;
    final boolean mIsSecret;

    UncachedKeyRing(PGPKeyRing ring) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
    }

    /* TODO don't use this */
    @Deprecated
    public PGPKeyRing getRing() {
        return mRing;
    }

    public UncachedPublicKey getPublicKey() {
        return new UncachedPublicKey(mRing.getPublicKey());
    }

    public boolean isSecret() {
        return mIsSecret;
    }

    public byte[] getEncoded() throws IOException {
        return mRing.getEncoded();
    }

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
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

    public static List<UncachedKeyRing> fromStream(InputStream stream)
            throws PgpGeneralException, IOException {

        PGPObjectFactory objectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream));

        List<UncachedKeyRing> result = new Vector<UncachedKeyRing>();

        // go through all objects in this block
        Object obj;
        while ((obj = objectFactory.nextObject()) != null) {
            Log.d(Constants.TAG, "Found class: " + obj.getClass());

            if (obj instanceof PGPKeyRing) {
                result.add(new UncachedKeyRing((PGPKeyRing) obj));
            } else {
                Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
            }
        }
        return result;
    }

}
