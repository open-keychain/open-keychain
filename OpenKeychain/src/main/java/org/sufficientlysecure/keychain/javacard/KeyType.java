package org.sufficientlysecure.keychain.javacard;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

public enum KeyType {
    SIGN(0, 0xB6, 0xCE, 0xC7),
    ENCRYPT(1, 0xB8, 0xCF, 0xC8),
    AUTH(2, 0xA4, 0xD0, 0xC9),;

    private final int mIdx;
    private final int mSlot;
    private final int mTimestampObjectId;
    private final int mFingerprintObjectId;

    KeyType(final int idx, final int slot, final int timestampObjectId, final int fingerprintObjectId) {
        this.mIdx = idx;
        this.mSlot = slot;
        this.mTimestampObjectId = timestampObjectId;
        this.mFingerprintObjectId = fingerprintObjectId;
    }

    public static KeyType from(final CanonicalizedSecretKey key) {
        if (key.canSign() || key.canCertify()) {
            return SIGN;
        } else if (key.canEncrypt()) {
            return ENCRYPT;
        } else if (key.canAuthenticate()) {
            return AUTH;
        }
        return null;
    }

    public int getIdx() {
        return mIdx;
    }

    public int getmSlot() {
        return mSlot;
    }

    public int getTimestampObjectId() {
        return mTimestampObjectId;
    }

    public int getmFingerprintObjectId() {
        return mFingerprintObjectId;
    }
}
