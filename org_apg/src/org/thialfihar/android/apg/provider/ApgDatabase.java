/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.provider;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.provider.ApgContract.KeyRingsColumns;
import org.thialfihar.android.apg.provider.ApgContract.KeysColumns;
import org.thialfihar.android.apg.provider.ApgContract.UserIdsColumns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.thialfihar.android.apg.util.Log;

public class ApgDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "apg.db";
    // Last APG 1 db version was 2
    private static final int DATABASE_VERSION = 3;

    public interface Tables {
        String KEY_RINGS = "key_rings";
        String KEYS = "keys";
        String USER_IDS = "user_ids";
    }

    private static final String CREATE_KEY_RINGS = "CREATE TABLE IF NOT EXISTS " + Tables.KEY_RINGS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY, " + KeyRingsColumns.MASTER_KEY_ID
            + " INT64, " + KeyRingsColumns.TYPE + " INTEGER, " + KeyRingsColumns.WHO_ID
            + " INTEGER, " + KeyRingsColumns.KEY_RING_DATA + " BLOB)";

    private static final String CREATE_KEYS = "CREATE TABLE IF NOT EXISTS " + Tables.KEYS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY, " + KeysColumns.KEY_ID + " INT64, "
            + KeysColumns.TYPE + " INTEGER, " + KeysColumns.IS_MASTER_KEY + " INTEGER, "
            + KeysColumns.ALGORITHM + " INTEGER, " + KeysColumns.KEY_SIZE + " INTEGER, "
            + KeysColumns.CAN_SIGN + " INTEGER, " + KeysColumns.CAN_ENCRYPT + " INTEGER, "
            + KeysColumns.IS_REVOKED + " INTEGER, " + KeysColumns.CREATION + " INTEGER, "
            + KeysColumns.EXPIRY + " INTEGER, " + KeysColumns.KEY_RING_ROW_ID
            + " INTEGER REFERENCES " + Tables.KEY_RINGS + " ON DELETE CASCADE, "
            + KeysColumns.KEY_DATA + " BLOB," + KeysColumns.RANK + " INTEGER)";

    private static final String CREATE_USER_IDS = "CREATE TABLE IF NOT EXISTS " + Tables.USER_IDS
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY, " + UserIdsColumns.KEY_ROW_ID
            + " INTEGER REFERENCES " + Tables.KEYS + " ON DELETE CASCADE, "
            + UserIdsColumns.USER_ID + " TEXT, " + UserIdsColumns.RANK + " INTEGER)";

    ApgDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //
    // public static HashMap<String, String> sKeyRingsProjection;
    // public static HashMap<String, String> sKeysProjection;
    // public static HashMap<String, String> sUserIdsProjection;
    //
    // private SQLiteDatabase mDb = null;
    // private int mStatus = 0;
    //
    // static {
    // sKeyRingsProjection = new HashMap<String, String>();
    // sKeyRingsProjection.put(KeyRings._ID, KeyRings._ID);
    // sKeyRingsProjection.put(KeyRings.MASTER_KEY_ID, KeyRings.MASTER_KEY_ID);
    // sKeyRingsProjection.put(KeyRings.TYPE, KeyRings.TYPE);
    // sKeyRingsProjection.put(KeyRings.WHO_ID, KeyRings.WHO_ID);
    // sKeyRingsProjection.put(KeyRings.KEY_RING_DATA, KeyRings.KEY_RING_DATA);
    //
    // sKeysProjection = new HashMap<String, String>();
    // sKeysProjection.put(Keys._ID, Keys._ID);
    // sKeysProjection.put(Keys.KEY_ID, Keys.KEY_ID);
    // sKeysProjection.put(Keys.TYPE, Keys.TYPE);
    // sKeysProjection.put(Keys.IS_MASTER_KEY, Keys.IS_MASTER_KEY);
    // sKeysProjection.put(Keys.ALGORITHM, Keys.ALGORITHM);
    // sKeysProjection.put(Keys.KEY_SIZE, Keys.KEY_SIZE);
    // sKeysProjection.put(Keys.CAN_SIGN, Keys.CAN_SIGN);
    // sKeysProjection.put(Keys.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
    // sKeysProjection.put(Keys.IS_REVOKED, Keys.IS_REVOKED);
    // sKeysProjection.put(Keys.CREATION, Keys.CREATION);
    // sKeysProjection.put(Keys.EXPIRY, Keys.EXPIRY);
    // sKeysProjection.put(Keys.KEY_DATA, Keys.KEY_DATA);
    // sKeysProjection.put(Keys.RANK, Keys.RANK);
    //
    // sUserIdsProjection = new HashMap<String, String>();
    // sUserIdsProjection.put(UserIds._ID, UserIds._ID);
    // sUserIdsProjection.put(UserIds.KEY_ID, UserIds.KEY_ID);
    // sUserIdsProjection.put(UserIds.USER_ID, UserIds.USER_ID);
    // sUserIdsProjection.put(UserIds.RANK, UserIds.RANK);
    // }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.w(Constants.TAG, "Creating database...");

        db.execSQL(CREATE_KEY_RINGS);
        db.execSQL(CREATE_KEYS);
        db.execSQL(CREATE_USER_IDS);
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

        // Upgrade from oldVersion through all methods to newest one
        for (int version = oldVersion; version < newVersion; ++version) {
            Log.w(Constants.TAG, "Upgrading database to version " + version);

            switch (version) {
            case 1:

                break;

            default:
                break;

            }
        }
    }

}
