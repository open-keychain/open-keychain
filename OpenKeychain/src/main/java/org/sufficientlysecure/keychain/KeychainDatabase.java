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

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.squareup.sqldelight.android.AndroidSqliteDriver;
import org.sufficientlysecure.keychain.model.CustomColumnAdapters;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


/**
 * SQLite Datatypes (from http://www.sqlite.org/datatype3.html)
 * - NULL. The value is a NULL value.
 * - INTEGER. The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the
 * magnitude of the value.
 * - REAL. The value is a floating point value, stored as an 8-byte IEEE floating point number.
 * - TEXT. The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or
 * UTF-16LE).
 * - BLOB. The value is a blob of data, stored exactly as it was input.
 */
public class KeychainDatabase {
    private static final String DATABASE_NAME = "openkeychain.db";
    private static final int DATABASE_VERSION = 36;
    private final SupportSQLiteOpenHelper supportSQLiteOpenHelper;
    private final Database sqldelightDatabase;

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
                                    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                            int newVersion) {
                                        KeychainDatabase.this.onUpgrade(db, context, oldVersion,
                                                newVersion);
                                    }

                                    @Override
                                    public void onDowngrade(SupportSQLiteDatabase db,
                                            int oldVersion, int newVersion) {
                                        KeychainDatabase.this.onDowngrade();
                                    }

                                    @Override
                                    public void onOpen(SupportSQLiteDatabase db) {
                                        super.onOpen(db);
                                        if (!db.isReadOnly()) {
                                            // Enable foreign key constraints
                                            db.execSQL("PRAGMA foreign_keys=ON;");
                                        }
                                    }
                                }).build());
        AndroidSqliteDriver driver = new AndroidSqliteDriver(supportSQLiteOpenHelper);
        sqldelightDatabase = Database.Companion.invoke(driver,
                new Autocrypt_peers.Adapter(CustomColumnAdapters.DATE_ADAPTER,
                        CustomColumnAdapters.DATE_ADAPTER,
                        CustomColumnAdapters.DATE_ADAPTER,
                        CustomColumnAdapters.GOSSIP_ORIGIN_ADAPTER),
                new Certs.Adapter(CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER),
                new Key_metadata.Adapter(CustomColumnAdapters.DATE_ADAPTER),
                new Keys.Adapter(CustomColumnAdapters.SECRET_KEY_TYPE_ADAPTER)
        );
    }

    public SupportSQLiteDatabase getReadableDatabase() {
        return supportSQLiteOpenHelper.getReadableDatabase();
    }

    public SupportSQLiteDatabase getWritableDatabase() {
        return supportSQLiteOpenHelper.getWritableDatabase();
    }

    public Database getSqlDelightDatabase() {
        return sqldelightDatabase;
    }

    @SuppressWarnings("deprecation") // using some sqldelight constants
    private void onCreate(SupportSQLiteDatabase db, Context context) {
        Timber.w("Creating database...");

        AndroidSqliteDriver sqlDriver = new AndroidSqliteDriver(db);
        Database.Companion.getSchema().create(sqlDriver);
        recreateDatabaseViews(db);

        db.execSQL("CREATE INDEX keys_by_rank ON keys (rank, master_key_id);");
        db.execSQL("CREATE INDEX uids_by_rank ON user_packets (rank, user_id, master_key_id);");
        db.execSQL("CREATE INDEX verified_certs ON certs (verified, master_key_id);");
        db.execSQL("CREATE INDEX uids_by_email ON user_packets (email);");

        Preferences.getPreferences(context).setKeySignaturesTableInitialized();
    }

    private void onUpgrade(SupportSQLiteDatabase db, Context context, int oldVersion,
            int newVersion) {
        Timber.d("Upgrading db from " + oldVersion + " to " + newVersion);
        if (oldVersion < 34) {
            throw new IllegalStateException("upgrades from older versions not supported");
        }
        switch (oldVersion) {
            case 34:
            case 35:
                // nothing
        }
        // recreate the unified key view on any upgrade
        recreateDatabaseViews(db);
    }

    private static void recreateDatabaseViews(SupportSQLiteDatabase db) {
        // for some reason those aren't created as part of the schema. so we do it here.
        db.execSQL("DROP VIEW IF EXISTS unifiedKeyView");
        db.execSQL(
                """
                        CREATE VIEW unifiedKeyView AS
                            SELECT keys.master_key_id, keys.fingerprint, MIN(user_packets.rank), user_packets.user_id, user_packets.name, user_packets.email, user_packets.comment, keys.creation, keys.expiry, keys.is_revoked, keys.is_secure, keys.can_certify, certs.verified,
                                (EXISTS (SELECT * FROM user_packets AS dups WHERE dups.master_key_id != keys.master_key_id AND dups.rank = 0 AND dups.name = user_packets.name COLLATE NOCASE AND dups.email = user_packets.email COLLATE NOCASE )) AS has_duplicate,
                                (EXISTS (SELECT * FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.has_secret != 0)) AS has_any_secret,
                                (EXISTS (SELECT * FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_encrypt != 0)) AS has_encrypt_key,
                                (EXISTS (SELECT * FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_sign != 0)) AS has_sign_key,
                                (EXISTS (SELECT * FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_authenticate != 0)) AS has_auth_key,
                                GROUP_CONCAT(DISTINCT aTI.package_name) AS autocrypt_package_names_csv,
                                GROUP_CONCAT(user_packets.user_id, '|||') AS user_id_list
                            FROM keys
                                 INNER JOIN user_packets ON ( keys.master_key_id = user_packets.master_key_id AND user_packets.type IS NULL AND (user_packets.rank = 0 OR user_packets.is_revoked = 0))
                                 LEFT JOIN certs ON ( keys.master_key_id = certs.master_key_id AND certs.verified = 1 )
                                 LEFT JOIN autocrypt_peers AS aTI ON ( aTI.master_key_id = keys.master_key_id )
                            WHERE keys.rank = 0
                            GROUP BY keys.master_key_id;""");
        db.execSQL("DROP VIEW IF EXISTS validKeys");
        db.execSQL("""
                        CREATE VIEW validKeys AS
                            SELECT master_key_id, rank, key_id, key_size, key_curve_oid, algorithm, fingerprint, can_certify, can_sign, can_encrypt, can_authenticate, is_revoked, has_secret, is_secure, creation, expiry
                                FROM keys
                                WHERE is_revoked = 0 AND is_secure = 1 AND (expiry IS NULL OR expiry >= strftime('%s', 'now')) AND validFrom <= strftime('%s', 'now');
                """);
        db.execSQL("DROP VIEW IF EXISTS uidStatus");
        db.execSQL("""
                        CREATE VIEW uidStatus AS
                            SELECT user_packets.email, MIN(certs.verified) AS key_status_int, user_packets.user_id, user_packets.master_key_id, COUNT(DISTINCT user_packets.master_key_id) AS candidates
                            FROM user_packets
                                JOIN validMasterKeys USING (master_key_id)
                                LEFT JOIN certs ON (certs.master_key_id = user_packets.master_key_id AND certs.rank = user_packets.rank AND certs.verified > 0)
                            WHERE user_packets.email IS NOT NULL
                            GROUP BY user_packets.email;
                """);
        db.execSQL("DROP VIEW IF EXISTS validMasterKeys");
        db.execSQL("""
                        CREATE VIEW validMasterKeys AS
                        SELECT *
                                FROM validKeys
                        WHERE rank = 0;
                """);
        db.execSQL("DROP VIEW IF EXISTS autocryptKeyStatus");
        db.execSQL("""
                        CREATE VIEW autocryptKeyStatus AS
                            SELECT autocryptPeer.*,
                                    (CASE WHEN ac_key.expiry IS NULL THEN 0 WHEN ac_key.expiry > strftime('%s', 'now') THEN 0 ELSE 1 END) AS key_is_expired_int,
                                    (CASE WHEN gossip_key.expiry IS NULL THEN 0 WHEN gossip_key.expiry > strftime('%s', 'now') THEN 0 ELSE 1 END) AS gossip_key_is_expired_int,
                                    ac_key.is_revoked AS key_is_revoked,
                                    gossip_key.is_revoked AS gossip_key_is_revoked,
                                    EXISTS (SELECT * FROM certs WHERE certs.master_key_id = autocryptPeer.master_key_id AND verified = 1) AS key_is_verified,
                                    EXISTS (SELECT * FROM certs WHERE certs.master_key_id = autocryptPeer.gossip_master_key_id AND verified = 1) AS gossip_key_is_verified
                                FROM autocrypt_peers AS autocryptPeer
                                    LEFT JOIN keys AS ac_key ON (ac_key.master_key_id = autocryptPeer.master_key_id AND ac_key.rank = 0)
                                    LEFT JOIN keys AS gossip_key ON (gossip_key.master_key_id = gossip_master_key_id AND gossip_key.rank = 0);
                """);
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
            throw new IOException("Cannot read " + in.getName());
        }
        if (!out.canWrite()) {
            throw new IOException("Cannot write " + out.getName());
        }
        copy(in, out);
    }

}
