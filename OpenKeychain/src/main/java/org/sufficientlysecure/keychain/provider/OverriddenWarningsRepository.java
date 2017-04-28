package org.sufficientlysecure.keychain.provider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.sufficientlysecure.keychain.provider.KeychainContract.OverriddenWarnings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;


public class OverriddenWarningsRepository {
    private final Context context;
    private KeychainDatabase keychainDatabase;

    public static OverriddenWarningsRepository createOverriddenWarningsRepository(Context context) {
        return new OverriddenWarningsRepository(context);
    }

    private OverriddenWarningsRepository(Context context) {
        this.context = context;
    }

    private KeychainDatabase getDb() {
        if (keychainDatabase == null) {
            keychainDatabase = new KeychainDatabase(context);
        }
        return keychainDatabase;
    }

    public boolean isWarningOverridden(String identifier) {
        SQLiteDatabase db = getDb().getReadableDatabase();
        Cursor cursor = db.query(
                Tables.OVERRIDDEN_WARNINGS,
                new String[] { "COUNT(*)" },
                OverriddenWarnings.IDENTIFIER + " = ?",
                new String[] { identifier },
                null, null, null);

        try {
            cursor.moveToFirst();
            return cursor.getInt(0) > 0;
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void putOverride(String identifier) {
        SQLiteDatabase db = getDb().getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(OverriddenWarnings.IDENTIFIER, identifier);
        db.replace(Tables.OVERRIDDEN_WARNINGS, null, cv);
        db.close();
    }

    public void deleteOverride(String identifier) {
        SQLiteDatabase db = getDb().getWritableDatabase();
        db.delete(Tables.OVERRIDDEN_WARNINGS, OverriddenWarnings.IDENTIFIER + " = ?", new String[] { identifier });
        db.close();
    }
}
