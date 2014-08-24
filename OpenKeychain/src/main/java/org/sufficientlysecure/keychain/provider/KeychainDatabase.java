/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsAccountsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.CertsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIdsColumns;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * SQLite Datatypes (from http://www.sqlite.org/datatype3.html)
 * - NULL. The value is a NULL value.
 * - INTEGER. The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
 * - REAL. The value is a floating point value, stored as an 8-byte IEEE floating point number.
 * - TEXT. The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
 * - BLOB. The value is a blob of data, stored exactly as it was input.
 *
 * Adding BOOLEAN results in an INTEGER, but we keep BOOLEAN to indicate the usage!
 */
public class KeychainDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "openkeychain.db";
    private static final int DATABASE_VERSION = 3;
    static Boolean apgHack = false;

    public interface Tables {
        String KEY_RINGS_PUBLIC = "keyrings_public";
        String KEY_RINGS_SECRET = "keyrings_secret";
        String KEYS = "keys";
        String USER_IDS = "user_ids";
        String CERTS = "certs";
        String API_APPS = "api_apps";
        String API_ACCOUNTS = "api_accounts";
    }

    private static final String CREATE_KEYRINGS_PUBLIC =
            "CREATE TABLE IF NOT EXISTS keyrings_public ("
                + KeyRingsColumns.MASTER_KEY_ID + " INTEGER PRIMARY KEY,"
                + KeyRingsColumns.KEY_RING_DATA + " BLOB"
            + ")";

    private static final String CREATE_KEYRINGS_SECRET =
            "CREATE TABLE IF NOT EXISTS keyrings_secret ("
                    + KeyRingsColumns.MASTER_KEY_ID + " INTEGER PRIMARY KEY,"
                    + KeyRingsColumns.KEY_RING_DATA + " BLOB,"
                    + "FOREIGN KEY(" + KeyRingsColumns.MASTER_KEY_ID + ") "
                        + "REFERENCES keyrings_public(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_KEYS =
            "CREATE TABLE IF NOT EXISTS " + Tables.KEYS + " ("
                + KeysColumns.MASTER_KEY_ID + " INTEGER, "
                + KeysColumns.RANK + " INTEGER, "

                + KeysColumns.KEY_ID + " INTEGER, "
                + KeysColumns.KEY_SIZE + " INTEGER, "
                + KeysColumns.KEY_CURVE_OID + " TEXT, "
                + KeysColumns.ALGORITHM + " INTEGER, "
                + KeysColumns.FINGERPRINT + " BLOB, "

                + KeysColumns.CAN_CERTIFY + " BOOLEAN, "
                + KeysColumns.CAN_SIGN + " BOOLEAN, "
                + KeysColumns.CAN_ENCRYPT + " BOOLEAN, "
                + KeysColumns.IS_REVOKED + " BOOLEAN, "
                + KeysColumns.HAS_SECRET + " BOOLEAN, "

                + KeysColumns.CREATION + " INTEGER, "
                + KeysColumns.EXPIRY + " INTEGER, "

                + "PRIMARY KEY(" + KeysColumns.MASTER_KEY_ID + ", " + KeysColumns.RANK + "),"
                + "FOREIGN KEY(" + KeysColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_USER_IDS =
            "CREATE TABLE IF NOT EXISTS " + Tables.USER_IDS + "("
                + UserIdsColumns.MASTER_KEY_ID + " INTEGER, "
                + UserIdsColumns.USER_ID + " TEXT, "

                + UserIdsColumns.IS_PRIMARY + " BOOLEAN, "
                + UserIdsColumns.IS_REVOKED + " BOOLEAN, "
                + UserIdsColumns.RANK+ " INTEGER, "

                + "PRIMARY KEY(" + UserIdsColumns.MASTER_KEY_ID + ", " + UserIdsColumns.USER_ID + "), "
                + "UNIQUE (" + UserIdsColumns.MASTER_KEY_ID + ", " + UserIdsColumns.RANK + "), "
                + "FOREIGN KEY(" + UserIdsColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_CERTS =
            "CREATE TABLE IF NOT EXISTS " + Tables.CERTS + "("
                + CertsColumns.MASTER_KEY_ID + " INTEGER,"
                + CertsColumns.RANK + " INTEGER, " // rank of certified uid

                + CertsColumns.KEY_ID_CERTIFIER + " INTEGER, " // certifying key
                + CertsColumns.TYPE + " INTEGER, "
                + CertsColumns.VERIFIED + " INTEGER, "
                + CertsColumns.CREATION + " INTEGER, "

                + CertsColumns.DATA + " BLOB, "

                + "PRIMARY KEY(" + CertsColumns.MASTER_KEY_ID + ", " + CertsColumns.RANK + ", "
                    + CertsColumns.KEY_ID_CERTIFIER + "), "
                + "FOREIGN KEY(" + CertsColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + CertsColumns.MASTER_KEY_ID + ", " + CertsColumns.RANK + ") REFERENCES "
                    + Tables.USER_IDS + "(" + UserIdsColumns.MASTER_KEY_ID + ", " + UserIdsColumns.RANK + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_API_APPS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_APPS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ApiAppsColumns.PACKAGE_NAME + " TEXT NOT NULL UNIQUE, "
                + ApiAppsColumns.PACKAGE_SIGNATURE + " BLOB"
            + ")";

    private static final String CREATE_API_APPS_ACCOUNTS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_ACCOUNTS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ApiAppsAccountsColumns.ACCOUNT_NAME + " TEXT NOT NULL, "
                + ApiAppsAccountsColumns.KEY_ID + " INTEGER, "
                + ApiAppsAccountsColumns.ENCRYPTION_ALGORITHM + " INTEGER, "
                + ApiAppsAccountsColumns.HASH_ALORITHM + " INTEGER, "
                + ApiAppsAccountsColumns.COMPRESSION + " INTEGER, "
                + ApiAppsAccountsColumns.PACKAGE_NAME + " TEXT NOT NULL, "

                + "UNIQUE(" + ApiAppsAccountsColumns.ACCOUNT_NAME + ", "
                    + ApiAppsAccountsColumns.PACKAGE_NAME + "), "
                + "FOREIGN KEY(" + ApiAppsAccountsColumns.PACKAGE_NAME + ") REFERENCES "
                    + Tables.API_APPS + "(" + ApiAppsColumns.PACKAGE_NAME + ") ON DELETE CASCADE"
            + ")";

    KeychainDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // make sure this is only done once, on the first instance!
        boolean iAmIt = false;
        synchronized (KeychainDatabase.class) {
            if (!KeychainDatabase.apgHack) {
                iAmIt = true;
                KeychainDatabase.apgHack = true;
            }
        }
        // if it's us, do the import
        if (iAmIt) {
            checkAndImportApg(context);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.w(Constants.TAG, "Creating database...");

        db.execSQL(CREATE_KEYRINGS_PUBLIC);
        db.execSQL(CREATE_KEYRINGS_SECRET);
        db.execSQL(CREATE_KEYS);
        db.execSQL(CREATE_USER_IDS);
        db.execSQL(CREATE_CERTS);
        db.execSQL(CREATE_API_APPS);
        db.execSQL(CREATE_API_APPS_ACCOUNTS);
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
        // add has_secret for all who are upgrading from a beta version
        switch (oldVersion) {
            case 1:
                try {
                    db.execSQL("ALTER TABLE keys ADD COLUMN has_secret BOOLEAN");
                } catch(Exception e){
                    // never mind, the column probably already existed
                }
            case 2:
                try {
                    db.execSQL("ALTER TABLE keys ADD COLUMN " + KeysColumns.KEY_CURVE_OID + " TEXT");
                } catch(Exception e){
                    // never mind, the column probably already existed
                }
        }
    }

    /** This method tries to import data from a provided database.
     *
     * The sole assumptions made on this db are that there is a key_rings table
     * with a key_ring_data, a master_key_id and a type column, the latter of
     * which should be 1 for secret keys and 0 for public keys.
     */
    public void checkAndImportApg(Context context) {

        boolean hasApgDb = false;
        {
            // It's the Java way =(
            String[] dbs = context.databaseList();
            for (String db : dbs) {
                if (db.equals("apg.db")) {
                    hasApgDb = true;
                } else if (db.equals("apg_old.db")) {
                    Log.d(Constants.TAG, "Found apg_old.db, delete it!");
                    context.getDatabasePath("apg_old.db").delete();
                }
            }
        }

        if (!hasApgDb) {
            return;
        }

        Log.d(Constants.TAG, "apg.db exists! Importing...");

        SQLiteDatabase db = new SQLiteOpenHelper(context, "apg.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                // should never happen
                throw new AssertionError();
            }
            @Override
            public void onDowngrade(SQLiteDatabase db, int old, int nu) {
                // don't care
            }
            @Override
            public void onUpgrade(SQLiteDatabase db, int old, int nu) {
                // don't care either
            }
        }.getReadableDatabase();

        Cursor cursor = null;
        ProviderHelper providerHelper = new ProviderHelper(context);

        try {
            // we insert in two steps: first, all public keys that have secret keys
            cursor = db.rawQuery("SELECT key_ring_data FROM key_rings WHERE type = 1 OR EXISTS ("
                    + " SELECT 1 FROM key_rings d2 WHERE key_rings.master_key_id = d2.master_key_id"
                    + " AND d2.type = 1) ORDER BY type ASC", null);
            if (cursor != null) {
                Log.d(Constants.TAG, "Importing " + cursor.getCount() + " secret keyrings from apg.db...");
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    byte[] data = cursor.getBlob(0);
                    try {
                        UncachedKeyRing ring = UncachedKeyRing.decodeFromData(data);
                        providerHelper.savePublicKeyRing(ring);
                    } catch(PgpGeneralException e) {
                        Log.e(Constants.TAG, "Error decoding keyring blob!");
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            // afterwards, insert all keys, starting with public keys that have secret keys, then
            // secret keys, then all others. this order is necessary to ensure all certifications
            // are recognized properly.
            cursor = db.rawQuery("SELECT key_ring_data FROM key_rings ORDER BY (type = 0 AND EXISTS ("
                    + " SELECT 1 FROM key_rings d2 WHERE key_rings.master_key_id = d2.master_key_id AND"
                    + " d2.type = 1)) DESC, type DESC", null);
            // import from old database
            if (cursor != null) {
                Log.d(Constants.TAG, "Importing " + cursor.getCount() + " keyrings from apg.db...");
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    byte[] data = cursor.getBlob(0);
                    try {
                        UncachedKeyRing ring = UncachedKeyRing.decodeFromData(data);
                        providerHelper.savePublicKeyRing(ring);
                    } catch(PgpGeneralException e) {
                        Log.e(Constants.TAG, "Error decoding keyring blob!");
                    }
                }
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error importing apg.db!", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        // delete old database
        context.getDatabasePath("apg.db").delete();
    }

    private static void copy(File in, File out) throws IOException {
        FileInputStream is = new FileInputStream(in);
        FileOutputStream os = new FileOutputStream(out);
        byte[] buf = new byte[512];
        while (is.available() > 0) {
            int count = is.read(buf, 0, 512);
            os.write(buf, 0, count);
        }
    }

    public static void debugBackup(Context context, boolean restore) throws IOException {
        if (!Constants.DEBUG) {
            return;
        }

        File in;
        File out;
        if (restore) {
            in = context.getDatabasePath("debug_backup.db");
            out = context.getDatabasePath(DATABASE_NAME);
        } else {
            in = context.getDatabasePath(DATABASE_NAME);
            out = context.getDatabasePath("debug_backup.db");
            out.createNewFile();
        }
        if (!in.canRead()) {
            throw new IOException("Cannot read " +  in.getName());
        }
        if (!out.canWrite()) {
            throw new IOException("Cannot write " + out.getName());
        }
        copy(in, out);
    }

    // DANGEROUS, use in test code ONLY!
    public void clearDatabase() {
        getWritableDatabase().execSQL("delete from " + Tables.KEY_RINGS_PUBLIC);
    }

}
