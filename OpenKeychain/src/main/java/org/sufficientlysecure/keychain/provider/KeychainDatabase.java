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
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsAccountsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsAllowedKeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.CertsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPacketsColumns;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.ConsolidateDialogActivity;
import org.sufficientlysecure.keychain.ui.MigrateSymmetricActivity;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

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
 */
public class KeychainDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "openkeychain.db";
    private static final int DATABASE_VERSION = 18;
    static Boolean apgHack = false;
    private Context mContext;

    public interface Tables {
        String KEY_RINGS_PUBLIC = "keyrings_public";
        String KEY_RINGS_SECRET = "keyrings_secret";
        String KEYS = "keys";
        String UPDATED_KEYS = "updated_keys";
        String USER_PACKETS = "user_packets";
        String CERTS = "certs";
        String API_APPS = "api_apps";
        String API_ACCOUNTS = "api_accounts";
        String API_ALLOWED_KEYS = "api_allowed_keys";
    }

    private static final String CREATE_KEYRINGS_PUBLIC =
            "CREATE TABLE IF NOT EXISTS keyrings_public ("
                + KeyRingsColumns.MASTER_KEY_ID + " INTEGER PRIMARY KEY,"
                + KeyRingsColumns.KEY_RING_DATA + " BLOB"
            + ")";

    private static final String CREATE_KEYRINGS_SECRET =
            "CREATE TABLE IF NOT EXISTS keyrings_secret ("
                    + KeyRingsColumns.MASTER_KEY_ID + " INTEGER PRIMARY KEY,"
                    + KeyRingsColumns.KEY_RING_DATA + " BLOB, "
                    + KeyRingsColumns.AWAITING_MERGE + " INTEGER, "
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

                + KeysColumns.CAN_CERTIFY + " INTEGER, "
                + KeysColumns.CAN_SIGN + " INTEGER, "
                + KeysColumns.CAN_ENCRYPT + " INTEGER, "
                + KeysColumns.CAN_AUTHENTICATE + " INTEGER, "
                + KeysColumns.IS_REVOKED + " INTEGER, "
                + KeysColumns.HAS_SECRET + " INTEGER, "

                + KeysColumns.CREATION + " INTEGER, "
                + KeysColumns.EXPIRY + " INTEGER, "

                + "PRIMARY KEY(" + KeysColumns.MASTER_KEY_ID + ", " + KeysColumns.RANK + "),"
                + "FOREIGN KEY(" + KeysColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_USER_PACKETS =
            "CREATE TABLE IF NOT EXISTS " + Tables.USER_PACKETS + "("
                + UserPacketsColumns.MASTER_KEY_ID + " INTEGER, "
                + UserPacketsColumns.TYPE + " INT, "
                + UserPacketsColumns.USER_ID + " TEXT, "
                + UserPacketsColumns.NAME + " TEXT, "
                + UserPacketsColumns.EMAIL + " TEXT, "
                + UserPacketsColumns.COMMENT + " TEXT, "
                + UserPacketsColumns.ATTRIBUTE_DATA + " BLOB, "

                + UserPacketsColumns.IS_PRIMARY + " INTEGER, "
                + UserPacketsColumns.IS_REVOKED + " INTEGER, "
                + UserPacketsColumns.RANK+ " INTEGER, "

                + "PRIMARY KEY(" + UserPacketsColumns.MASTER_KEY_ID + ", " + UserPacketsColumns.RANK + "), "
                + "FOREIGN KEY(" + UserPacketsColumns.MASTER_KEY_ID + ") REFERENCES "
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
                    + Tables.USER_PACKETS + "(" + UserPacketsColumns.MASTER_KEY_ID + ", " + UserPacketsColumns.RANK + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_UPDATE_KEYS =
            "CREATE TABLE IF NOT EXISTS " + Tables.UPDATED_KEYS + " ("
                    + UpdatedKeysColumns.MASTER_KEY_ID + " INTEGER PRIMARY KEY, "
                    + UpdatedKeysColumns.LAST_UPDATED + " INTEGER, "
                    + "FOREIGN KEY(" + UpdatedKeysColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
                    + ")";

    private static final String CREATE_API_APPS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_APPS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ApiAppsColumns.PACKAGE_NAME + " TEXT NOT NULL UNIQUE, "
                + ApiAppsColumns.PACKAGE_CERTIFICATE + " BLOB"
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

    private static final String CREATE_API_APPS_ALLOWED_KEYS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_ALLOWED_KEYS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ApiAppsAllowedKeysColumns.KEY_ID + " INTEGER, "
                + ApiAppsAllowedKeysColumns.PACKAGE_NAME + " TEXT NOT NULL, "

                + "UNIQUE(" + ApiAppsAllowedKeysColumns.KEY_ID + ", "
                + ApiAppsAllowedKeysColumns.PACKAGE_NAME + "), "
                + "FOREIGN KEY(" + ApiAppsAllowedKeysColumns.PACKAGE_NAME + ") REFERENCES "
                + Tables.API_APPS + "(" + ApiAppsAllowedKeysColumns.PACKAGE_NAME + ") ON DELETE CASCADE"
                + ")";

    public KeychainDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;

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
        db.execSQL(CREATE_USER_PACKETS);
        db.execSQL(CREATE_CERTS);
        db.execSQL(CREATE_UPDATE_KEYS);
        db.execSQL(CREATE_API_APPS);
        db.execSQL(CREATE_API_APPS_ACCOUNTS);
        db.execSQL(CREATE_API_APPS_ALLOWED_KEYS);

        db.execSQL("CREATE INDEX keys_by_rank ON keys (" + KeysColumns.RANK + ");");
        db.execSQL("CREATE INDEX uids_by_rank ON user_packets (" + UserPacketsColumns.RANK + ", "
                + UserPacketsColumns.USER_ID + ", " + UserPacketsColumns.MASTER_KEY_ID + ");");
        db.execSQL("CREATE INDEX verified_certs ON certs ("
                + CertsColumns.VERIFIED + ", " + CertsColumns.MASTER_KEY_ID + ");");

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
        Log.d(Constants.TAG, "Upgrading db from " + oldVersion + " to " + newVersion);

        switch (oldVersion) {
            case 1:
                // add has_secret for all who are upgrading from a beta version
                try {
                    db.execSQL("ALTER TABLE keys ADD COLUMN has_secret INTEGER");
                } catch (Exception e) {
                    // never mind, the column probably already existed
                }
                // fall through
            case 2:
                // ECC support
                try {
                    db.execSQL("ALTER TABLE keys ADD COLUMN key_curve_oid TEXT");
                } catch (Exception e) {
                    // never mind, the column probably already existed
                }
                // fall through
            case 3:
                // better s2k detection, we need consolidate
                // fall through
            case 4:
                try {
                    db.execSQL("ALTER TABLE keys ADD COLUMN can_authenticate INTEGER");
                } catch (Exception e) {
                    // never mind, the column probably already existed
                }
                // fall through
            case 5:
                // do consolidate for 3.0 beta3
                // fall through
            case 6:
                db.execSQL("ALTER TABLE user_ids ADD COLUMN type INTEGER");
                db.execSQL("ALTER TABLE user_ids ADD COLUMN attribute_data BLOB");
            case 7:
                // new table for allowed key ids in API
                try {
                    db.execSQL(CREATE_API_APPS_ALLOWED_KEYS);
                } catch (Exception e) {
                    // never mind, the column probably already existed
                }
            case 8:
                // tbale name for user_ids changed to user_packets
                db.execSQL("DROP TABLE IF EXISTS certs");
                db.execSQL("DROP TABLE IF EXISTS user_ids");
                db.execSQL(CREATE_USER_PACKETS);
                db.execSQL(CREATE_CERTS);
            case 9:
                // do nothing here, just consolidate
            case 10:
                // fix problems in database, see #1402 for details
                // https://github.com/open-keychain/open-keychain/issues/1402
                db.execSQL("DELETE FROM api_accounts WHERE key_id BETWEEN 0 AND 3");
            case 11:
                db.execSQL(CREATE_UPDATE_KEYS);
            case 12:
                // do nothing here, just consolidate
            case 13:
                db.execSQL("CREATE INDEX keys_by_rank ON keys (" + KeysColumns.RANK + ");");
                db.execSQL("CREATE INDEX uids_by_rank ON user_packets (" + UserPacketsColumns.RANK + ", "
                        + UserPacketsColumns.USER_ID + ", " + UserPacketsColumns.MASTER_KEY_ID + ");");
                db.execSQL("CREATE INDEX verified_certs ON certs ("
                        + CertsColumns.VERIFIED + ", " + CertsColumns.MASTER_KEY_ID + ");");
            case 14:
                db.execSQL("ALTER TABLE user_packets ADD COLUMN name TEXT");
                db.execSQL("ALTER TABLE user_packets ADD COLUMN email TEXT");
                db.execSQL("ALTER TABLE user_packets ADD COLUMN comment TEXT");
            case 15:
                db.execSQL("CREATE INDEX uids_by_name ON user_packets (name COLLATE NOCASE)");
                db.execSQL("CREATE INDEX uids_by_email ON user_packets (email COLLATE NOCASE)");
            case 16:
                // fall through for migrate
            case 17:
                PassphraseCacheService.clearAllCachedPassphrases(mContext);
                Preferences.getPreferences(mContext).setUsingS2k(true);
                db.execSQL("ALTER TABLE keyrings_secret ADD COLUMN awaiting_merge INTEGER");
                // TODO: wip, db to set all unmigrated keyring type to passphrase by default
        }


        if (oldVersion <= 17) {
            // migrate to symmetrically encrypted keyring blocks
            // consolidate is handled by migration to prevent progress bar bugs
            Intent migrateIntent = new Intent(mContext.getApplicationContext(), MigrateSymmetricActivity.class);
            migrateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.getApplicationContext().startActivity(migrateIntent);

        } else {

            // always do consolidate after upgrade
            Intent consolidateIntent = new Intent(mContext.getApplicationContext(), ConsolidateDialogActivity.class);
            consolidateIntent.putExtra(ConsolidateDialogActivity.EXTRA_CONSOLIDATE_RECOVERY, false);
            consolidateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.getApplicationContext().startActivity(consolidateIntent);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Downgrade is ok for the debug version, makes it easier to work with branches
        if (Constants.DEBUG) {
            return;
        }
        // NOTE: downgrading the database is explicitly not allowed to prevent
        // someone from exploiting old bugs to export the database
        throw new RuntimeException("Downgrading the database is not allowed!");
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
                if ("apg.db".equals(db)) {
                    hasApgDb = true;
                } else if ("apg_old.db".equals(db)) {
                    Log.d(Constants.TAG, "Found apg_old.db, delete it!");
                    // noinspection ResultOfMethodCallIgnored - if it doesn't happen, it doesn't happen.
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

        // noinspection ResultOfMethodCallIgnored - not much we can do if this doesn't work
        context.getDatabasePath("apg.db").delete();
    }

    private static void copy(File in, File out) throws IOException {
        FileInputStream is = new FileInputStream(in);
        FileOutputStream os = new FileOutputStream(out);
        try {
            byte[] buf = new byte[512];
            while (is.available() > 0) {
                int count = is.read(buf, 0, 512);
                os.write(buf, 0, count);
            }
        } finally {
            is.close();
            os.close();
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
            // noinspection ResultOfMethodCallIgnored - this is a pure debug feature, anyways
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
        getWritableDatabase().execSQL("delete from " + Tables.API_ACCOUNTS);
        getWritableDatabase().execSQL("delete from " + Tables.API_ALLOWED_KEYS);
        getWritableDatabase().execSQL("delete from " + Tables.API_APPS);
    }

}
