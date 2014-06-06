package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** Wrapper around PGPKeyRing class, to be constructed from bytes.
 *
 * This class and its relatives UncachedPublicKey and UncachedSecretKey are
 * used to move around pgp key rings in non crypto related (UI, mostly) code.
 * It should be used for simple inspection only until it saved in the database,
 * all actual crypto operations should work with WrappedKeyRings exclusively.
 *
 * This class is also special in that it can hold either the PGPPublicKeyRing
 * or PGPSecretKeyRing derivate of the PGPKeyRing class, since these are
 * treated equally for most purposes in UI code. It is up to the programmer to
 * take care of the differences.
 *
 * @see org.sufficientlysecure.keychain.pgp.WrappedKeyRing
 * @see org.sufficientlysecure.keychain.pgp.UncachedPublicKey
 * @see org.sufficientlysecure.keychain.pgp.UncachedSecretKey
 *
 */
public class UncachedKeyRing {

    final PGPKeyRing mRing;
    final boolean mIsSecret;

    UncachedKeyRing(PGPKeyRing ring) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
    }

    public long getMasterKeyId() {
        return mRing.getPublicKey().getKeyID();
    }

    /* TODO don't use this */
    @Deprecated
    public PGPKeyRing getRing() {
        return mRing;
    }

    public UncachedPublicKey getPublicKey() {
        return new UncachedPublicKey(mRing.getPublicKey());
    }

    public Iterator<UncachedPublicKey> getPublicKeys() {
        final Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        return new Iterator<UncachedPublicKey>() {
            public void remove() {
                it.remove();
            }
            public UncachedPublicKey next() {
                return new UncachedPublicKey(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

    /** Returns the dynamic (though final) property if this is a secret keyring or not. */
    public boolean isSecret() {
        return mIsSecret;
    }

    public byte[] getEncoded() throws IOException {
        return mRing.getEncoded();
    }

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
    }

    public static UncachedKeyRing decodePublicFromData(byte[] data)
            throws PgpGeneralException, IOException {
        UncachedKeyRing ring = decodeFromData(data);
        if(ring.isSecret()) {
            throw new PgpGeneralException("Object not recognized as PGPPublicKeyRing!");
        }
        return ring;
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {
        BufferedInputStream bufferedInput =
                new BufferedInputStream(new ByteArrayInputStream(data));
        if (bufferedInput.available() > 0) {
            InputStream in = PGPUtil.getDecoderStream(bufferedInput);
            PGPObjectFactory objectFactory = new PGPObjectFactory(in);

            // get first object in block
            Object obj;
            if ((obj = objectFactory.nextObject()) != null && obj instanceof PGPKeyRing) {
                return new UncachedKeyRing((PGPKeyRing) obj);
            } else {
                throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
            }
        } else {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
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

    public void encodeArmored(OutputStream out, String version) throws IOException {
        ArmoredOutputStream aos = new ArmoredOutputStream(out);
        aos.setHeader("Version", version);
        aos.write(mRing.getEncoded());
        aos.close();
    }

    public HashSet<Long> getAvailableSubkeys() {
        if(!isSecret()) {
            throw new RuntimeException("Tried to find available subkeys from non-secret keys. " +
                    "This is a programming error and should never happen!");
        }

        HashSet<Long> result = new HashSet<Long>();
        // then, mark exactly the keys we have available
        for (PGPSecretKey sub : new IterableIterator<PGPSecretKey>(
                ((PGPSecretKeyRing) mRing).getSecretKeys())) {
            S2K s2k = sub.getS2K();
            // Set to 1, except if the encryption type is GNU_DUMMY_S2K
            if(s2k == null || s2k.getType() != S2K.GNU_DUMMY_S2K) {
                result.add(sub.getKeyID());
            }
        }
        return result;
    }

}
