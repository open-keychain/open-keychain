package org.sufficientlysecure.keychain.daos;


import java.util.ArrayList;
import java.util.List;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import android.database.Cursor;

import com.squareup.sqldelight.RowMapper;
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

    <T> List<T> mapAllRows(SupportSQLiteQuery query, RowMapper<T> mapper) {
        ArrayList<T> result = new ArrayList<>();
        try (Cursor cursor = getReadableDb().query(query)) {
            while (cursor.moveToNext()) {
                T item = mapper.map(cursor);
                result.add(item);
            }
        }
        return result;
    }

    <T> T mapSingleRowOrThrow(SupportSQLiteQuery query, RowMapper<T> mapper) throws NotFoundException {
        T result = mapSingleRow(query, mapper);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    <T> T mapSingleRow(SupportSQLiteQuery query, RowMapper<T> mapper) {
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToNext()) {
                return mapper.map(cursor);
            }
        }
        return null;
    }
}
