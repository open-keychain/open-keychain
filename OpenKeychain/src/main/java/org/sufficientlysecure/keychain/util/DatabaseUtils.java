package org.sufficientlysecure.keychain.util;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import timber.log.Timber;


public class DatabaseUtils {
    public static void explainQuery(SQLiteDatabase db, String sql) {
        Cursor explainCursor = db.rawQuery("EXPLAIN QUERY PLAN " + sql, new String[0]);

        // this is a debugging feature, we can be a little careless
        explainCursor.moveToFirst();

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < explainCursor.getColumnCount(); i++) {
            line.append(explainCursor.getColumnName(i)).append(", ");
        }
        Timber.d(line.toString());

        while (!explainCursor.isAfterLast()) {
            line = new StringBuilder();
            for (int i = 0; i < explainCursor.getColumnCount(); i++) {
                line.append(explainCursor.getString(i)).append(", ");
            }
            Timber.d(line.toString());
            explainCursor.moveToNext();
        }

        explainCursor.close();
    }
}
