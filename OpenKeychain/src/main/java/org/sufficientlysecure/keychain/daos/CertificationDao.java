package org.sufficientlysecure.keychain.daos;


import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.Certification;
import org.sufficientlysecure.keychain.model.Certification.CertDetails;


public class CertificationDao extends AbstractDao {
    public static CertificationDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new CertificationDao(keychainDatabase, databaseNotifyManager);
    }

    private CertificationDao(KeychainDatabase keychainDatabase, DatabaseNotifyManager databaseNotifyManager) {
        super(keychainDatabase, databaseNotifyManager);
    }

    public CertDetails getVerifyingCertDetails(long masterKeyId, int userPacketRank) {
        SqlDelightQuery query = Certification.FACTORY.selectVerifyingCertDetails(masterKeyId, userPacketRank);
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToFirst()) {
                return Certification.CERT_DETAILS_MAPPER.map(cursor);
            }
        }
        return null;
    }

}
