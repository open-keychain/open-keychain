/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQueryBuilder;
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
        SupportSQLiteDatabase db = getDb().getReadableDatabase();
        SupportSQLiteQuery query = SupportSQLiteQueryBuilder
                .builder(Tables.OVERRIDDEN_WARNINGS)
                .columns(new String[] { "COUNT(*) FROM " })
                .selection(OverriddenWarnings.IDENTIFIER + " = ?", new String[] { identifier })
                .create();
        Cursor cursor = db.query(query);

        try {
            cursor.moveToFirst();
            return cursor.getInt(0) > 0;
        } finally {
            cursor.close();
        }
    }

    public void putOverride(String identifier) {
        SupportSQLiteDatabase db = getDb().getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(OverriddenWarnings.IDENTIFIER, identifier);
        db.insert(Tables.OVERRIDDEN_WARNINGS, SQLiteDatabase.CONFLICT_REPLACE, cv);
    }

    public void deleteOverride(String identifier) {
        SupportSQLiteDatabase db = getDb().getWritableDatabase();
        db.delete(Tables.OVERRIDDEN_WARNINGS, OverriddenWarnings.IDENTIFIER + " = ?", new String[] { identifier });
    }
}
