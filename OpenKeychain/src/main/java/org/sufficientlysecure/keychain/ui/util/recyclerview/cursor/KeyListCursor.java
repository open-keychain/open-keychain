package org.sufficientlysecure.keychain.ui.util.recyclerview.cursor;

import android.database.Cursor;

import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
public class KeyListCursor extends KeyCursor {
    public static final String ORDER = KeychainContract.KeyRings.HAS_ANY_SECRET
            + " DESC, " + KeychainContract.KeyRings.USER_ID + " COLLATE NOCASE ASC";

    public static final String[] PROJECTION;

    static {
        ArrayList<String> arr = new ArrayList<>();
        arr.addAll(Arrays.asList(KeyCursor.PROJECTION));
        arr.addAll(Arrays.asList(
                KeychainContract.KeyRings.VERIFIED,
                KeychainContract.KeyRings.HAS_ANY_SECRET,
                KeychainContract.KeyRings.FINGERPRINT,
                KeychainContract.KeyRings.HAS_ENCRYPT
        ));

        PROJECTION = arr.toArray(new String[arr.size()]);
    }

    public static KeyListCursor wrap(Cursor cursor) {
        if (cursor != null) {
            return new KeyListCursor(cursor);
        } else {
            return null;
        }
    }

    private KeyListCursor(Cursor cursor) {
        super(cursor);
    }

    public boolean hasEncrypt() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ENCRYPT);
        return getInt(index) != 0;
    }

    public byte[] getRawFingerprint() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.FINGERPRINT);
        return getBlob(index);
    }

    public String getFingerprint() {
        return KeyFormattingUtils.convertFingerprintToHex(getRawFingerprint());
    }

    public boolean isSecret() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ANY_SECRET);
        return getInt(index) != 0;
    }

    public boolean isVerified() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.VERIFIED);
        return getInt(index) > 0;
    }
}
