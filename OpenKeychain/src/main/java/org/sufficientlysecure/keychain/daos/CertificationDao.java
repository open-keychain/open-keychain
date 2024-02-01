package org.sufficientlysecure.keychain.daos;


import android.content.Context;

import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.SelectVerifyingCertDetails;


public class CertificationDao extends AbstractDao {
    public static CertificationDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new CertificationDao(keychainDatabase, databaseNotifyManager);
    }

    private CertificationDao(KeychainDatabase keychainDatabase, DatabaseNotifyManager databaseNotifyManager) {
        super(keychainDatabase, databaseNotifyManager);
    }

    public SelectVerifyingCertDetails getVerifyingCertDetails(long masterKeyId, int userPacketRank) {
        return getDatabase().getCertsQueries().selectVerifyingCertDetails(masterKeyId, userPacketRank).executeAsOneOrNull();
    }

}
