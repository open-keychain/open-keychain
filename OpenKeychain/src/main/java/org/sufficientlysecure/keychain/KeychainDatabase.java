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

package org.sufficientlysecure.keychain;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Callback;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;

import org.sufficientlysecure.keychain.daos.LocalSecretKeyStorage;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


/**
 * SQLite Datatypes (from http://www.sqlite.org/datatype3.html)
 * - NULL. The value is a NULL value.
 * - INTEGER. The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
 * - REAL. The value is a floating point value, stored as an 8-byte IEEE floating point number.
 * - TEXT. The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
 * - BLOB. The value is a blob of data, stored exactly as it was input.
 */
public class KeychainDatabase {
    private static final String DATABASE_NAME = "openkeychain.db";
    private static final int DATABASE_VERSION = 32;
    private final SupportSQLiteOpenHelper supportSQLiteOpenHelper;

    private static KeychainDatabase sInstance;

    public static KeychainDatabase getInstance(Context context) {
        if (sInstance == null || Constants.IS_RUNNING_UNITTEST) {
            sInstance = new KeychainDatabase(context.getApplicationContext());
        }
        return sInstance;
    }

    private KeychainDatabase(Context context) {
        supportSQLiteOpenHelper =
                new FrameworkSQLiteOpenHelperFactory()
                        .create(Configuration.builder(context).name(DATABASE_NAME).callback(
                                new Callback(DATABASE_VERSION) {
                                    @Override
                                    public void onCreate(SupportSQLiteDatabase db) {
                                        KeychainDatabase.this.onCreate(db, context);
                                    }

                                    @Override
                                    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                        KeychainDatabase.this.onUpgrade(db, context, oldVersion, newVersion);
                                    }

                                    @Override
                                    public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                        KeychainDatabase.this.onDowngrade();
                                    }

                                    @Override
                                    public void onOpen(SupportSQLiteDatabase db) {
                                        super.onOpen(db);
                                        if (!db.isReadOnly()) {
                                            // Enable foreign key constraints
                                            db.execSQL("PRAGMA foreign_keys=ON;");
                                            if (Constants.DEBUG) {
                                                recreateUnifiedKeyView(db);
                                            }
                                        }
                                    }
                                }).build());
    }

    public SupportSQLiteDatabase getReadableDatabase() {
        return supportSQLiteOpenHelper.getReadableDatabase();
    }

    public SupportSQLiteDatabase getWritableDatabase() {
        return supportSQLiteOpenHelper.getWritableDatabase();
    }

    @SuppressWarnings("deprecation") // using some sqldelight constants
    private void onCreate(SupportSQLiteDatabase db, Context context) {
        Timber.w("Creating database...");

        db.execSQL(KeyRingsPublicModel.CREATE_TABLE);
        db.execSQL(KeysModel.CREATE_TABLE);
        db.execSQL(UserPacketsModel.CREATE_TABLE);
        db.execSQL(CertsModel.CREATE_TABLE);
        db.execSQL(KeyMetadataModel.CREATE_TABLE);
        db.execSQL(KeySignaturesModel.CREATE_TABLE);
        db.execSQL(ApiAppsModel.CREATE_TABLE);
        db.execSQL(OverriddenWarningsModel.CREATE_TABLE);
        db.execSQL(AutocryptPeersModel.CREATE_TABLE);
        db.execSQL(ApiAllowedKeysModel.CREATE_TABLE);
        db.execSQL(KeysModel.UNIFIEDKEYVIEW);
        db.execSQL(KeysModel.VALIDKEYSVIEW);
        db.execSQL(UserPacketsModel.UIDSTATUS);

        db.execSQL("CREATE INDEX keys_by_rank ON keys (" + KeysModel.RANK + ", " + KeysModel.MASTER_KEY_ID + ");");
        db.execSQL("CREATE INDEX uids_by_rank ON user_packets (" + UserPacketsModel.RANK + ", "
                + UserPacketsModel.USER_ID + ", " + UserPacketsModel.MASTER_KEY_ID + ");");
        db.execSQL("CREATE INDEX verified_certs ON certs ("
                + CertsModel.VERIFIED + ", " + CertsModel.MASTER_KEY_ID + ");");
        db.execSQL("CREATE INDEX uids_by_email ON user_packets ("
                + UserPacketsModel.EMAIL + ");");

        Preferences.getPreferences(context).setKeySignaturesTableInitialized();
    }

    private void onUpgrade(SupportSQLiteDatabase db, Context context, int oldVersion, int newVersion) {
        Timber.d("Upgrading db from " + oldVersion + " to " + newVersion);

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
                    db.execSQL(ApiAppsModel.CREATE_TABLE);
                } catch (Exception e) {
                    // never mind, the column probably already existed
                }
            case 8:
                // tbale name for user_ids changed to user_packets
                db.execSQL("DROP TABLE IF EXISTS certs");
                db.execSQL("DROP TABLE IF EXISTS user_ids");
                db.execSQL("CREATE TABLE IF NOT EXISTS user_packets("
                        + "master_key_id INTEGER, "
                        + "type INT, "
                        + "user_id TEXT, "
                        + "attribute_data BLOB, "

                        + "is_primary INTEGER, "
                        + "is_revoked INTEGER, "
                        + "rank INTEGER, "

                        + "PRIMARY KEY(master_key_id, rank), "
                        + "FOREIGN KEY(master_key_id) REFERENCES "
                        + "keyrings_public(master_key_id) ON DELETE CASCADE"
                        + ")");
                db.execSQL("CREATE TABLE IF NOT EXISTS certs("
                        + "master_key_id INTEGER,"
                        + "rank INTEGER, " // rank of certified uid

                        + "key_id_certifier INTEGER, " // certifying key
                        + "type INTEGER, "
                        + "verified INTEGER, "
                        + "creation INTEGER, "

                        + "data BLOB, "

                        + "PRIMARY KEY(master_key_id, rank, "
                        + "key_id_certifier), "
                        + "FOREIGN KEY(master_key_id) REFERENCES "
                        + "keyrings_public(master_key_id) ON DELETE CASCADE,"
                        + "FOREIGN KEY(master_key_id, rank) REFERENCES "
                        + "user_packets(master_key_id, rank) ON DELETE CASCADE"
                        + ")");
            case 9:
                // do nothing here, just consolidate
            case 10:
                // fix problems in database, see #1402 for details
                // https://github.com/open-keychain/open-keychain/issues/1402
                // no longer needed, api_accounts is deprecated
                // db.execSQL("DELETE FROM api_accounts WHERE key_id BETWEEN 0 AND 3");
            case 11:
                db.execSQL("CREATE TABLE IF NOT EXISTS updated_keys ("
                        + "master_key_id INTEGER PRIMARY KEY, "
                        + "last_updated INTEGER, "
                        + "FOREIGN KEY(master_key_id) REFERENCES "
                        + "keyrings_public(master_key_id) ON DELETE CASCADE"
                        + ")");
            case 12:
                // do nothing here, just consolidate
            case 13:
                db.execSQL("CREATE INDEX keys_by_rank ON keys (rank);");
                db.execSQL("CREATE INDEX uids_by_rank ON user_packets (rank, user_id, master_key_id);");
                db.execSQL("CREATE INDEX verified_certs ON certs (verified, master_key_id);");
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
            case 20:
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS overridden_warnings ("
                                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "identifier TEXT NOT NULL UNIQUE "
                                + ")");

            case 21:
                try {
                    db.execSQL("ALTER TABLE updated_keys ADD COLUMN seen_on_keyservers INTEGER;");
                } catch (SQLiteException e) {
                    // don't bother, the column probably already existed
                }

            case 22:
                db.execSQL("CREATE TABLE IF NOT EXISTS api_autocrypt_peers ("
                        + "package_name TEXT NOT NULL, "
                        + "identifier TEXT NOT NULL, "
                        + "last_updated INTEGER NOT NULL, "
                        + "last_seen_key INTEGER NOT NULL, "
                        + "state INTEGER NOT NULL, "
                        + "master_key_id INTEGER, "
                        + "PRIMARY KEY(package_name, identifier), "
                        + "FOREIGN KEY(package_name) REFERENCES api_apps(package_name) ON DELETE CASCADE"
                        + ")");

            case 23:
                db.execSQL("CREATE TABLE IF NOT EXISTS key_signatures ("
                        + "master_key_id INTEGER NOT NULL, "
                        + "signer_key_id INTEGER NOT NULL, "
                        + "PRIMARY KEY(master_key_id, signer_key_id), "
                        + "FOREIGN KEY(master_key_id) REFERENCES keyrings_public(master_key_id) ON DELETE CASCADE"
                        + ")");

            case 24: {
                try {
                    db.beginTransaction();
                    db.execSQL("ALTER TABLE api_autocrypt_peers RENAME TO tmp");
                    db.execSQL("CREATE TABLE api_autocrypt_peers ("
                            + "package_name TEXT NOT NULL, "
                            + "identifier TEXT NOT NULL, "
                            + "last_seen INTEGER, "
                            + "last_seen_key INTEGER, "
                            + "is_mutual INTEGER, "
                            + "master_key_id INTEGER, "
                            + "gossip_master_key_id INTEGER, "
                            + "gossip_last_seen_key INTEGER, "
                            + "gossip_origin INTEGER, "
                            + "PRIMARY KEY(package_name, identifier), "
                            + "FOREIGN KEY(package_name) REFERENCES api_apps (package_name) ON DELETE CASCADE"
                            + ")");
                    // Note: Keys from Autocrypt 0.X with state == "reset" (0) are dropped
                    db.execSQL("INSERT INTO api_autocrypt_peers " +
                            "(package_name, identifier, last_seen, gossip_last_seen_key, gossip_master_key_id, gossip_origin) " +
                            "SELECT package_name, identifier, last_updated, last_seen_key, master_key_id, 0 " +
                            "FROM tmp WHERE state = 1"); // Autocrypt 0.X, "gossip" -> now origin=autocrypt
                    db.execSQL("INSERT INTO api_autocrypt_peers " +
                            "(package_name, identifier, last_seen, gossip_last_seen_key, gossip_master_key_id, gossip_origin) " +
                            "SELECT package_name, identifier, last_updated, last_seen_key, master_key_id, 20 " +
                            "FROM tmp WHERE state = 2"); // "selected" keys -> now origin=dedup
                    db.execSQL("INSERT INTO api_autocrypt_peers " +
                            "(package_name, identifier, last_seen, last_seen_key, master_key_id, is_mutual) " +
                            "SELECT package_name, identifier, last_updated, last_seen_key, master_key_id, 0 " +
                            "FROM tmp WHERE state = 3"); // Autocrypt 0.X, state = "available"
                    db.execSQL("INSERT INTO api_autocrypt_peers " +
                            "(package_name, identifier, last_seen, last_seen_key, master_key_id, is_mutual) " +
                            "SELECT package_name, identifier, last_updated, last_seen_key, master_key_id, 1 " +
                            "FROM tmp WHERE state = 4"); // from Autocrypt 0.X, state = "mutual"
                    db.execSQL("DROP TABLE tmp");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS uids_by_email ON user_packets (email);");
                db.execSQL("DROP INDEX keys_by_rank");
                db.execSQL("CREATE INDEX keys_by_rank ON keys(rank, master_key_id);");
            }

            case 25: {
                try {
                    migrateSecretKeysFromDbToLocalStorage(db, context);
                } catch (IOException e) {
                    throw new IllegalStateException("Error migrating secret keys! This is bad!!");
                }
            }

            case 26:
                migrateUpdatedKeysToKeyMetadataTable(db);

            case 27:
                renameApiAutocryptPeersTable(db);

            case 28:
                // drop old table from version 20
                db.execSQL("DROP TABLE IF EXISTS api_accounts");

            case 29:
                recreateUnifiedKeyView(db);

            case 30:
                // ignore. this case only came up in an unreleased beta.

            case 31:
                addSubkeyValidFromField(db);
        }
    }

    private void addSubkeyValidFromField(SupportSQLiteDatabase db) {
        try {
            db.beginTransaction();
            db.execSQL("ALTER TABLE keys ADD COLUMN validFrom INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("UPDATE keys SET validFrom = creation");
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            // column probably already existed, nvm this
            if (!Constants.DEBUG) {
                throw e;
            }
        } finally {
            db.endTransaction();
        }
    }

    private void recreateUnifiedKeyView(SupportSQLiteDatabase db) {
        try {
            db.beginTransaction();

            // noinspection deprecation
            db.execSQL("DROP VIEW IF EXISTS " + KeysModel.UNIFIEDKEYVIEW_VIEW_NAME);
            db.execSQL(KeysModel.UNIFIEDKEYVIEW);
            // noinspection deprecation
            db.execSQL("DROP VIEW IF EXISTS " + KeysModel.VALIDMASTERKEYS_VIEW_NAME);
            db.execSQL(KeysModel.VALIDKEYSVIEW);
            // noinspection deprecation
            db.execSQL("DROP VIEW IF EXISTS " + UserPacketsModel.UIDSTATUS_VIEW_NAME);
            db.execSQL(UserPacketsModel.UIDSTATUS);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void migrateSecretKeysFromDbToLocalStorage(SupportSQLiteDatabase db, Context context) throws IOException {
        LocalSecretKeyStorage localSecretKeyStorage = LocalSecretKeyStorage.getInstance(context);
        Cursor cursor = db.query("SELECT master_key_id, key_ring_data FROM keyrings_secret");
        while (cursor.moveToNext()) {
            long masterKeyId = cursor.getLong(0);
            byte[] secretKeyBlob = cursor.getBlob(1);
            localSecretKeyStorage.writeSecretKey(masterKeyId, secretKeyBlob);
        }
        cursor.close();

        // we'll keep this around for now, but make sure to delete when migration looks ok!!
        // db.execSQL("DROP TABLE keyrings_secret");
    }

    private void migrateUpdatedKeysToKeyMetadataTable(SupportSQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE updated_keys RENAME TO key_metadata;");
        } catch (SQLException e) {
            if (Constants.DEBUG) {
                Timber.e(e, "Ignoring migration exception, this probably happened before");
                return;
            }
            throw e;
        }
    }

    private void renameApiAutocryptPeersTable(SupportSQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE api_autocrypt_peers RENAME TO autocrypt_peers;");
        } catch (SQLException e) {
            if (Constants.DEBUG) {
                Timber.e(e, "Ignoring migration exception, this probably happened before");
                return;
            }
            throw e;
        }
    }

    private void onDowngrade() {
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

}
