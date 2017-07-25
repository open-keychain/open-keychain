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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsAllowedKeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAppsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeerColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.CertsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.OverriddenWarnings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPacketsColumns;
import org.sufficientlysecure.keychain.ui.ConsolidateDialogActivity;
import org.sufficientlysecure.keychain.util.Log;

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
    private static final int DATABASE_VERSION = 23;
    private Context mContext;

    public interface Tables {
        String KEY_RINGS_PUBLIC = "keyrings_public";
        String KEY_RINGS_SECRET = "keyrings_secret";
        String KEYS = "keys";
        String UPDATED_KEYS = "updated_keys";
        String USER_PACKETS = "user_packets";
        String CERTS = "certs";
        String API_APPS = "api_apps";
        String API_ALLOWED_KEYS = "api_allowed_keys";
        String OVERRIDDEN_WARNINGS = "overridden_warnings";
        String API_AUTOCRYPT_PEERS = "api_autocrypt_peers";
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
                + KeysColumns.IS_SECURE + " INTEGER, "

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
                    + UpdatedKeysColumns.SEEN_ON_KEYSERVERS + " INTEGER, "
                    + "FOREIGN KEY(" + UpdatedKeysColumns.MASTER_KEY_ID + ") REFERENCES "
                    + Tables.KEY_RINGS_PUBLIC + "(" + KeyRingsColumns.MASTER_KEY_ID + ") ON DELETE CASCADE"
                    + ")";

    private static final String CREATE_API_AUTOCRYPT_PEERS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_AUTOCRYPT_PEERS + " ("
                    + ApiAutocryptPeerColumns.PACKAGE_NAME + " TEXT NOT NULL, "
                    + ApiAutocryptPeerColumns.IDENTIFIER + " TEXT NOT NULL, "
                    + ApiAutocryptPeerColumns.LAST_SEEN + " INTEGER NOT NULL, "
                    + ApiAutocryptPeerColumns.LAST_SEEN_KEY + " INTEGER NOT NULL, "
                    + ApiAutocryptPeerColumns.STATE + " INTEGER NOT NULL, "
                    + ApiAutocryptPeerColumns.MASTER_KEY_ID + " INTEGER NULL, "
                    + "PRIMARY KEY(" + ApiAutocryptPeerColumns.PACKAGE_NAME + ", "
                        + ApiAutocryptPeerColumns.IDENTIFIER + "), "
                    + "FOREIGN KEY(" + ApiAutocryptPeerColumns.PACKAGE_NAME + ") REFERENCES "
                        + Tables.API_APPS + "(" + ApiAppsColumns.PACKAGE_NAME + ") ON DELETE CASCADE"
                + ")";

    private static final String CREATE_API_APPS =
            "CREATE TABLE IF NOT EXISTS " + Tables.API_APPS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ApiAppsColumns.PACKAGE_NAME + " TEXT NOT NULL UNIQUE, "
                + ApiAppsColumns.PACKAGE_CERTIFICATE + " BLOB"
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

    private static final String CREATE_OVERRIDDEN_WARNINGS =
            "CREATE TABLE IF NOT EXISTS " + Tables.OVERRIDDEN_WARNINGS + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + OverriddenWarnings.IDENTIFIER + " TEXT NOT NULL UNIQUE "
                + ")";

    public KeychainDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
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
        db.execSQL(CREATE_API_APPS_ALLOWED_KEYS);
        db.execSQL(CREATE_OVERRIDDEN_WARNINGS);
        db.execSQL(CREATE_API_AUTOCRYPT_PEERS);

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
                // no longer needed, api_accounts is deprecated
                // db.execSQL("DELETE FROM api_accounts WHERE key_id BETWEEN 0 AND 3");
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
                // splitUserId changed: Execute consolidate for new parsing of name, email
            case 17:
                // splitUserId changed: Execute consolidate for new parsing of name, email
            case 18:
                db.execSQL("ALTER TABLE keys ADD COLUMN is_secure INTEGER");
            case 19:
                // emergency fix for crashing consolidate
                db.execSQL("UPDATE keys SET is_secure = 1;");
            /* TODO actually drop this table. leaving it around for now!
            case 20:
                db.execSQL("DROP TABLE api_accounts");
                if (oldVersion == 20) {
                    // no need to consolidate
                    return;
                }
            */
            case 20:
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS overridden_warnings ("
                                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "identifier TEXT NOT NULL UNIQUE "
                                    + ")");

            case 21:
                db.execSQL("ALTER TABLE updated_keys ADD COLUMN seen_on_keyservers INTEGER;");

            case 22:
                db.execSQL("CREATE TABLE IF NOT EXISTS api_autocrypt_peers ("
                        + "package_name TEXT NOT NULL, "
                        + "identifier TEXT NOT NULL, "
                        + "last_updated INTEGER NOT NULL, "
                        + "last_seen_key INTEGER NOT NULL, "
                        + "state INTEGER NOT NULL, "
                        + "master_key_id INTEGER NULL, "
                        + "PRIMARY KEY(package_name, identifier), "
                        + "FOREIGN KEY(package_name) REFERENCES api_apps(package_name) ON DELETE CASCADE"
                    + ")");

                if (oldVersion == 18 || oldVersion == 19 || oldVersion == 20 || oldVersion == 21 || oldVersion == 22) {
                    return;
                }
        }

        // TODO: don't depend on consolidate! make migrations inline!
        // consolidate after upgrade
        Intent consolidateIntent = new Intent(mContext.getApplicationContext(), ConsolidateDialogActivity.class);
        consolidateIntent.putExtra(ConsolidateDialogActivity.EXTRA_CONSOLIDATE_RECOVERY, false);
        consolidateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.getApplicationContext().startActivity(consolidateIntent);
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
        getWritableDatabase().execSQL("delete from " + Tables.API_ALLOWED_KEYS);
        getWritableDatabase().execSQL("delete from " + Tables.API_APPS);
    }

}
