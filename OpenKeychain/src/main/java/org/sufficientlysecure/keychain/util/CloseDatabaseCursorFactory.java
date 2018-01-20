package org.sufficientlysecure.keychain.util;


import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;

import timber.log.Timber;


public class CloseDatabaseCursorFactory implements CursorFactory {
    private static class CloseDatabaseCursor extends SQLiteCursor {
        CloseDatabaseCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
            super(db, driver, editTable, query);
        }

        @Override
        public void close() {
            final SQLiteDatabase db = getDatabase();
            super.close();
            if (db != null) {
                Timber.d("Closing cursor: " + db.getPath());
                try {
                    db.close();
                } catch (Exception e) {
                    Timber.e(e, "Error closing db");
                }
            }
        }
    }

    @Override
    public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
            String editTable, SQLiteQuery query) {
        return new CloseDatabaseCursor(db, masterQuery, editTable, query);
    }
}
