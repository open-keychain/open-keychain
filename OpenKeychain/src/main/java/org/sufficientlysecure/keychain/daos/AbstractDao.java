package org.sufficientlysecure.keychain.daos;


import java.util.ArrayList;
import java.util.List;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.database.Cursor;

import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;


class AbstractDao {
    private final KeychainDatabase db;
    private final DatabaseNotifyManager databaseNotifyManager;

    AbstractDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        this.db = db;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    SupportSQLiteDatabase getReadableDb() {
        return db.getReadableDatabase();
    }

    SupportSQLiteDatabase getWritableDb() {
        return db.getWritableDatabase();
    }

    DatabaseNotifyManager getDatabaseNotifyManager() {
        return databaseNotifyManager;
    }

    <T> List<T> mapAllRows(SupportSQLiteQuery query, Mapper<T> mapper) {
        ArrayList<T> result = new ArrayList<>();
        try (Cursor cursor = getReadableDb().query(query)) {
            while (cursor.moveToNext()) {
                T item = mapper.map(cursor);
                result.add(item);
            }
        }
        return result;
    }

    <T> T mapSingleRowOrThrow(SupportSQLiteQuery query, Mapper<T> mapper) throws NotFoundException {
        T result = mapSingleRow(query, mapper);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    <T> T mapSingleRow(SupportSQLiteQuery query, Mapper<T> mapper) {
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToNext()) {
                return mapper.map(cursor);
            }
        }
        return null;
    }

    interface Mapper<T> {
        T map(Cursor cursor);
    }
}
