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

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
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
    private static final int DATABASE_VERSION = 34;
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
        db.execSQL(OverriddenWarningsModel.CREATE_TABLE);
        db.execSQL(AutocryptPeersModel.CREATE_TABLE);
        db.execSQL(KeysModel.UNIFIEDKEYVIEW);
        db.execSQL(KeysModel.VALIDKEYSVIEW);
        db.execSQL(KeysModel.VALIDMASTERKEYSVIEW);
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
            case 34:
                // NEXT migration
        }
    }

    private void addSubkeyValidFromField(SupportSQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE keys ADD COLUMN validFrom INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("UPDATE keys SET validFrom = creation");
        } catch (SQLiteException e) {
            // column probably already existed, nvm this
        }
    }

    private void recreateUnifiedKeyView(SupportSQLiteDatabase db) {
        // noinspection deprecation
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.UNIFIEDKEYVIEW_VIEW_NAME);
        db.execSQL(KeysModel.UNIFIEDKEYVIEW);
        // noinspection deprecation
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.VALIDKEYS_VIEW_NAME);
        db.execSQL(KeysModel.VALIDKEYSVIEW);
        // noinspection deprecation
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.VALIDMASTERKEYS_VIEW_NAME);
        db.execSQL(KeysModel.VALIDMASTERKEYSVIEW);
        // noinspection deprecation
        db.execSQL("DROP VIEW IF EXISTS " + UserPacketsModel.UIDSTATUS_VIEW_NAME);
        db.execSQL(UserPacketsModel.UIDSTATUS);
    }

    private void dropKeyMetadataForeignKey(SupportSQLiteDatabase db) {
        // noinspection deprecation
        db.execSQL("ALTER TABLE " + KeyMetadataModel.TABLE_NAME + " RENAME TO metadata_tmp");
        db.execSQL(KeyMetadataModel.CREATE_TABLE);
        // noinspection deprecation
        db.execSQL("INSERT INTO " + KeyMetadataModel.TABLE_NAME + " SELECT * FROM metadata_tmp");
        db.execSQL("DROP TABLE metadata_tmp");
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
