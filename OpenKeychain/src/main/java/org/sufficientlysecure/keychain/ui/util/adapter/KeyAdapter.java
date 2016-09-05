package org.sufficientlysecure.keychain.ui.util.adapter;

import android.database.Cursor;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.io.Serializable;
import java.util.Date;

public interface KeyAdapter {
    // These are the rows that we will retrieve.
    String[] PROJECTION = new String[] {
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.CREATION,
            KeychainContract.KeyRings.HAS_ENCRYPT
    };

    // projection indices
    int INDEX_MASTER_KEY_ID = 1;
    int INDEX_USER_ID = 2;
    int INDEX_IS_REVOKED = 3;
    int INDEX_IS_EXPIRED = 4;
    int INDEX_VERIFIED = 5;
    int INDEX_HAS_ANY_SECRET = 6;
    int INDEX_HAS_DUPLICATE_USER_ID = 7;
    int INDEX_FINGERPRINT = 8;
    int INDEX_CREATION = 9;
    int INDEX_HAS_ENCRYPT = 10;

    // adapter functionality
    void setSearchQuery(String query);
    boolean isEnabled(Cursor cursor);

    KeyItem getItem(int position);
    long getMasterKeyId(int position);
    boolean isSecretAvailable(int position);

    class KeyItem implements Serializable {
        public final String mUserIdFull;
        public final OpenPgpUtils.UserId mUserId;
        public final long mKeyId;
        public final boolean mHasDuplicate;
        public final boolean mHasEncrypt;
        public final Date mCreation;
        public final String mFingerprint;
        public final boolean mIsSecret, mIsRevoked, mIsExpired, mIsVerified;

        public KeyItem(Cursor cursor) {
            String userId = cursor.getString(INDEX_USER_ID);
            mUserId = KeyRing.splitUserId(userId);
            mUserIdFull = userId;
            mKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
            mHasDuplicate = cursor.getLong(INDEX_HAS_DUPLICATE_USER_ID) > 0;
            mHasEncrypt = cursor.getInt(INDEX_HAS_ENCRYPT) != 0;
            mCreation = new Date(cursor.getLong(INDEX_CREATION) * 1000);
            mFingerprint = KeyFormattingUtils.convertFingerprintToHex(
                    cursor.getBlob(INDEX_FINGERPRINT));
            mIsSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
            mIsRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
            mIsExpired = cursor.getInt(INDEX_IS_EXPIRED) > 0;
            mIsVerified = cursor.getInt(INDEX_VERIFIED) > 0;
        }

        public KeyItem(CanonicalizedPublicKeyRing ring) {
            CanonicalizedPublicKey key = ring.getPublicKey();
            String userId = key.getPrimaryUserIdWithFallback();
            mUserId = KeyRing.splitUserId(userId);
            mUserIdFull = userId;
            mKeyId = ring.getMasterKeyId();
            mHasDuplicate = false;
            mHasEncrypt = key.getKeyRing().getEncryptIds().size() > 0;
            mCreation = key.getCreationTime();
            mFingerprint = KeyFormattingUtils.convertFingerprintToHex(
                    ring.getFingerprint());
            mIsRevoked = key.isRevoked();
            mIsExpired = key.isExpired();

            // these two are actually "don't know"s
            mIsSecret = false;
            mIsVerified = false;
        }

        public String getReadableName() {
            if (mUserId.name != null) {
                return mUserId.name;
            } else {
                return mUserId.email;
            }
        }
    }
}
