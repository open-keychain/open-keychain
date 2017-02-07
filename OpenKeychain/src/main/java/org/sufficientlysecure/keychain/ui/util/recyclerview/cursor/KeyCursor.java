package org.sufficientlysecure.keychain.ui.util.recyclerview.cursor;

import android.database.Cursor;

import org.sufficientlysecure.keychain.provider.KeychainContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by daquexian on 17-2-7.
 */

public class KeyCursor extends AbstractCursor {
    public static final String[] PROJECTION;

    static {
        ArrayList<String> arr = new ArrayList<>();
        arr.addAll(Arrays.asList(AbstractCursor.PROJECTION));
        arr.addAll(Arrays.asList(
                KeychainContract.KeyRings.MASTER_KEY_ID,
                KeychainContract.KeyRings.USER_ID,
                KeychainContract.KeyRings.IS_REVOKED,
                KeychainContract.KeyRings.IS_EXPIRED,
                KeychainContract.KeyRings.IS_SECURE,
                KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID,
                KeychainContract.KeyRings.CREATION,
                KeychainContract.KeyRings.NAME,
                KeychainContract.KeyRings.EMAIL,
                KeychainContract.KeyRings.COMMENT
        ));

        PROJECTION = arr.toArray(new String[arr.size()]);
    }

    public static KeyCursor wrap(Cursor cursor) {
        if (cursor != null) {
            return new KeyCursor(cursor);
        } else {
            return null;
        }
    }

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    protected KeyCursor(Cursor cursor) {
        super(cursor);
    }

    public long getKeyId() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.MASTER_KEY_ID);
        return getLong(index);
    }

    public String getName() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.NAME);
        return getString(index);
    }

    public String getEmail() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.EMAIL);
        return getString(index);
    }

    public String getComment() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.COMMENT);
        return getString(index);
    }

    public boolean hasDuplicate() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID);
        return getLong(index) > 0L;
    }

    public boolean isRevoked() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.IS_REVOKED);
        return getInt(index) > 0;
    }

    public boolean isExpired() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.IS_EXPIRED);
        return getInt(index) > 0;
    }

    public boolean isSecure() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.IS_SECURE);
        return getInt(index) > 0;
    }

    public long getCreationTime() {
        int index = getColumnIndexOrThrow(KeychainContract.KeyRings.CREATION);
        return getLong(index) * 1000;
    }

    public Date getCreationDate() {
        return new Date(getCreationTime());
    }
}
