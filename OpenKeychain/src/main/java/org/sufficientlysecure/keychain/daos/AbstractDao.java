package org.sufficientlysecure.keychain.daos;


import androidx.sqlite.db.SupportSQLiteDatabase;
import org.sufficientlysecure.keychain.Database;
import org.sufficientlysecure.keychain.KeychainDatabase;


class AbstractDao {
    private final KeychainDatabase db;
    private final DatabaseNotifyManager databaseNotifyManager;

    AbstractDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        this.db = db;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    Database getDatabase() {
        return db.getSqlDelightDatabase();
    }

    SupportSQLiteDatabase getWritableDb() {
        return db.getWritableDatabase();
    }

    DatabaseNotifyManager getDatabaseNotifyManager() {
        return databaseNotifyManager;
    }
}
