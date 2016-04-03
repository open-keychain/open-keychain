package org.sufficientlysecure.keychain.javacard;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;

public class CachingBaseJavacardDevice extends BaseJavacardDevice {
    private byte[] mFingerprintsCache;
    private String mUserIdCache;
    private byte[] mAidCache;

    public CachingBaseJavacardDevice(final Transport mTransport) {
        super(mTransport);
    }

    @Override
    public byte[] getFingerprints() throws IOException {
        if (mFingerprintsCache == null) {
            mFingerprintsCache = super.getFingerprints();
        }
        return mFingerprintsCache;
    }

    @Override
    public String getUserId() throws IOException {
        if (mUserIdCache == null) {
            mUserIdCache = super.getUserId();
        }
        return mUserIdCache;
    }

    @Override
    public byte[] getAid() throws IOException {
        if (mAidCache == null) {
            mAidCache = super.getAid();
        }
        return mAidCache;
    }

    @Override
    public void changeKey(final CanonicalizedSecretKey secretKey, final Passphrase passphrase) throws IOException {
        super.changeKey(secretKey, passphrase);
        mFingerprintsCache = null;
    }
}
