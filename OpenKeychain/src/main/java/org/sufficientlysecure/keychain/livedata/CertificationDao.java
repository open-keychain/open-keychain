package org.sufficientlysecure.keychain.livedata;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.model.Certification;
import org.sufficientlysecure.keychain.model.Certification.CertDetails;
import org.sufficientlysecure.keychain.provider.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;


public class CertificationDao {
    private final SupportSQLiteDatabase db;
    private final DatabaseNotifyManager databaseNotifyManager;

    public static CertificationDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = new KeychainDatabase(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new CertificationDao(keychainDatabase.getWritableDatabase(), databaseNotifyManager);
    }

    private CertificationDao(SupportSQLiteDatabase writableDatabase, DatabaseNotifyManager databaseNotifyManager) {
        this.db = writableDatabase;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    public CertDetails getVerifyingCertDetails(long masterKeyId, int userPacketRank) {
        SqlDelightQuery query = Certification.FACTORY.selectVerifyingCertDetails(masterKeyId, userPacketRank);
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToFirst()) {
                return Certification.CERT_DETAILS_MAPPER.map(cursor);
            }
        }
        return null;
    }

}
