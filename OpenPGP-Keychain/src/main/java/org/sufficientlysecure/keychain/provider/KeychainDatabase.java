/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIdsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.CertsColumns;
import org.sufficientlysecure.keychain.util.Log;

public class KeychainDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "apg.db";
    private static final int DATABASE_VERSION = 8;

    public interface Tables {
        String KEY_RINGS = "key_rings";
        String KEYS = "keys";
        String USER_IDS = "user_ids";
        String API_APPS = "api_apps";
        String CERTS = "certs";
    }

    private static final String CREATE_KEY_RINGS = "CREATE TABLE IF NOT EXISTS " + Tables.KEY_RINGS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KeyRingsColumns.MASTER_KEY_ID + " INT64, "
            + KeyRingsColumns.TYPE + " INTEGER, "
            + KeyRingsColumns.KEY_RING_DATA + " BLOB)";

    private static final String CREATE_KEYS = "CREATE TABLE IF NOT EXISTS " + Tables.KEYS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KeysColumns.KEY_ID + " INT64, "
            + KeysColumns.TYPE + " INTEGER, "
            + KeysColumns.IS_MASTER_KEY + " INTEGER, "
            + KeysColumns.ALGORITHM + " INTEGER, "
            + KeysColumns.KEY_SIZE + " INTEGER, "
            + KeysColumns.CAN_CERTIFY + " INTEGER, "
            + KeysColumns.CAN_SIGN + " INTEGER, "
            + KeysColumns.CAN_ENCRYPT + " INTEGER, "
            + KeysColumns.IS_REVOKED + " INTEGER, "
            + KeysColumns.CREATION + " INTEGER, "
            + KeysColumns.EXPIRY + " INTEGER, "
            + KeysColumns.KEY_DATA + " BLOB,"
            + KeysColumns.RANK + " INTEGER, "
            + KeysColumns.FINGERPRINT + " BLOB, "
            + KeysColumns.KEY_RING_ROW_ID + " INTEGER NOT NULL, FOREIGN KEY("
            + KeysColumns.KEY_RING_ROW_ID + ") REFERENCES " + Tables.KEY_RINGS + "("
            + BaseColumns._ID + ") ON DELETE CASCADE)";

    private static final String CREATE_USER_IDS = "CREATE TABLE IF NOT EXISTS " + Tables.USER_IDS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + UserIdsColumns.USER_ID + " TEXT, "
            + UserIdsColumns.RANK + " INTEGER, "
            + UserIdsColumns.KEY_RING_ROW_ID + " INTEGER NOT NULL, FOREIGN KEY("
            + UserIdsColumns.KEY_RING_ROW_ID + ") REFERENCES " + Tables.KEY_RINGS + "("
            + BaseColumns._ID + ") ON DELETE CASCADE)";

    private static final String CREATE_API_APPS = "CREATE TABLE IF NOT EXISTS " + Tables.API_APPS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ApiAppsColumns.PACKAGE_NAME + " TEXT UNIQUE, "
            + ApiAppsColumns.PACKAGE_SIGNATURE + " BLOB, "
            + ApiAppsColumns.KEY_ID + " INT64, "
            + ApiAppsColumns.ENCRYPTION_ALGORITHM + " INTEGER, "
            + ApiAppsColumns.HASH_ALORITHM + " INTEGER, "
            + ApiAppsColumns.COMPRESSION + " INTEGER)";

    private static final String CREATE_CERTS = "CREATE TABLE IF NOT EXISTS " + Tables.CERTS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + CertsColumns.KEY_RING_ROW_ID + " INTEGER NOT NULL "
                + " REFERENCES " + Tables.KEY_RINGS + "(" + BaseColumns._ID + ") ON DELETE CASCADE, "
            + CertsColumns.KEY_ID + " INTEGER, " // certified key
            + CertsColumns.RANK + " INTEGER, " // key rank of certified uid
            + CertsColumns.KEY_ID_CERTIFIER + " INTEGER, " // certifying key
            + CertsColumns.CREATION + " INTEGER, "
            + CertsColumns.VERIFIED + " INTEGER, "
            + CertsColumns.KEY_DATA + " BLOB)";


    KeychainDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.w(Constants.TAG, "Creating database...");

        db.execSQL(CREATE_KEY_RINGS);
        db.execSQL(CREATE_KEYS);
        db.execSQL(CREATE_USER_IDS);
        db.execSQL(CREATE_API_APPS);
        db.execSQL(CREATE_CERTS);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(Constants.TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Upgrade from oldVersion through all cases to newest one
        for (int version = oldVersion; version < newVersion; ++version) {
            Log.w(Constants.TAG, "Upgrading database to version " + version);

            switch (version) {
                case 3:
                    db.execSQL("ALTER TABLE " + Tables.KEYS + " ADD COLUMN " + KeysColumns.CAN_CERTIFY
                            + " INTEGER DEFAULT 0;");
                    db.execSQL("UPDATE " + Tables.KEYS + " SET " + KeysColumns.CAN_CERTIFY
                            + " = 1 WHERE " + KeysColumns.IS_MASTER_KEY + "= 1;");
                    break;
                case 4:
                    db.execSQL(CREATE_API_APPS);
                    break;
                case 5:
                    // new column: package_signature
                    db.execSQL("DROP TABLE IF EXISTS " + Tables.API_APPS);
                    db.execSQL(CREATE_API_APPS);
                    break;
                case 6:
                    // new column: fingerprint
                    db.execSQL("ALTER TABLE " + Tables.KEYS + " ADD COLUMN " + KeysColumns.FINGERPRINT
                            + " BLOB;");
                    break;
                case 7:
                    // new table: certs
                    db.execSQL(CREATE_CERTS);

                    break;
                default:
                    break;

            }
        }
    }

}
