package org.sufficientlysecure.keychain.ui.util.recyclerview.cursor;

import android.database.Cursor;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by daquexian on 17-2-7.
 */

public class CertCursor extends AbstractCursor {
    public static final String[] CERTS_PROJECTION;
    static {
        ArrayList<String> projection = new ArrayList<>();
        projection.addAll(Arrays.asList(AbstractCursor.PROJECTION));
        projection.addAll(Arrays.asList(
                KeychainContract.Certs.MASTER_KEY_ID,
                KeychainContract.Certs.VERIFIED,
                KeychainContract.Certs.TYPE,
                KeychainContract.Certs.RANK,
                KeychainContract.Certs.KEY_ID_CERTIFIER,
                KeychainContract.Certs.USER_ID,
                KeychainContract.Certs.SIGNER_UID
        ));

        CERTS_PROJECTION = projection.toArray(new String[projection.size()]);
    }

    public static final String CERTS_SORT_ORDER =
            KeychainDatabase.Tables.CERTS + "." + KeychainContract.Certs.RANK + " ASC, "
                    + KeychainContract.Certs.VERIFIED + " DESC, "
                    + KeychainDatabase.Tables.CERTS + "." + KeychainContract.Certs.TYPE + " DESC, "
                    + KeychainContract.Certs.SIGNER_UID + " ASC";

    public static CertCursor wrap(Cursor cursor) {
        if(cursor != null) {
            return new CertCursor(cursor);
        } else {
            return null;
        }
    }

    private CertCursor(Cursor cursor) {
        super(cursor);
    }

    public long getKeyId() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.MASTER_KEY_ID);
        return getLong(index);
    }

    public boolean isVerified() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.VERIFIED);
        return getInt(index) > 0;
    }

    public int getType() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.TYPE);
        return getInt(index);
    }

    public long getRank() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.RANK);
        return getLong(index);
    }

    public long getCertifierKeyId() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.KEY_ID_CERTIFIER);
        return getLong(index);
    }

    public String getRawUserId() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.USER_ID);
        return getString(index);
    }

    public String getRawSignerUserId() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.SIGNER_UID);
        return getString(index);
    }

    public String getName() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.NAME);
        return getString(index);
    }

    public String getEmail() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.EMAIL);
        return getString(index);
    }

    public String getComment() {
        int index = getColumnIndexOrThrow(KeychainContract.Certs.COMMENT);
        return getString(index);
    }

    public OpenPgpUtils.UserId getSignerUserId() {
        return KeyRing.splitUserId(getRawSignerUserId());
    }
}
