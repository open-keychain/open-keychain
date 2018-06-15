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

package org.sufficientlysecure.keychain.util;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.text.TextUtils;

import timber.log.Timber;


/**
 * Shamelessly copied from android.database.DatabaseUtils
 */
public class DatabaseUtil {
    /**
     * Concatenates two SQL WHERE clauses, handling empty or null values.
     */
    public static String concatenateWhere(String a, String b) {
        if (TextUtils.isEmpty(a)) {
            return b;
        }
        if (TextUtils.isEmpty(b)) {
            return a;
        }

        return "(" + a + ") AND (" + b + ")";
    }

    /**
     * Appends one set of selection args to another. This is useful when adding a selection
     * argument to a user provided set.
     */
    public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
        if (originalValues == null || originalValues.length == 0) {
            return newValues;
        }
        String[] result = new String[originalValues.length + newValues.length ];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length);
        System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
        return result;
    }

    public static void explainQuery(SupportSQLiteDatabase db, String sql) {
        Cursor explainCursor = db.query("EXPLAIN QUERY PLAN " + sql, new String[0]);

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
