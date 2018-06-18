package org.sufficientlysecure.keychain.provider;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.KeyMetadataModel.ReplaceKeyMetadata;
import org.sufficientlysecure.keychain.model.KeyMetadata;


public class KeyMetadataDao {
    private final SupportSQLiteDatabase db;
    private DatabaseNotifyManager databaseNotifyManager;


    public static KeyMetadataDao create(Context context) {
        SupportSQLiteDatabase supportSQLiteDatabase = new KeychainDatabase(context).getWritableDatabase();
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new KeyMetadataDao(supportSQLiteDatabase, databaseNotifyManager);
    }

    private KeyMetadataDao(SupportSQLiteDatabase supportSQLiteDatabase, DatabaseNotifyManager databaseNotifyManager) {
        this.db = supportSQLiteDatabase;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    public KeyMetadata getKeyMetadata(long masterKeyId) {
        SqlDelightQuery query = KeyMetadata.FACTORY.selectByMasterKeyId(masterKeyId);
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToFirst()) {
                return KeyMetadata.FACTORY.selectByMasterKeyIdMapper().map(cursor);
            }
        }
        return null;
    }

    public void resetAllLastUpdatedTimes() {
        new KeyMetadata.DeleteAllLastUpdatedTimes(db).execute();
    }

    public void renewKeyLastUpdatedTime(long masterKeyId, boolean seenOnKeyservers) {
        ReplaceKeyMetadata replaceStatement = new ReplaceKeyMetadata(db, KeyMetadata.FACTORY);
        replaceStatement.bind(masterKeyId, new Date(), seenOnKeyservers);
        replaceStatement.executeInsert();

        databaseNotifyManager.notifyKeyMetadataChange(masterKeyId);
    }

    public List<byte[]> getFingerprintsForKeysOlderThan(long olderThan, TimeUnit timeUnit) {
        SqlDelightQuery query = KeyMetadata.FACTORY.selectFingerprintsForKeysOlderThan(new Date(timeUnit.toMillis(olderThan)));

        List<byte[]> fingerprintList = new ArrayList<>();
        try (Cursor cursor = db.query(query)) {
            while (cursor.moveToNext()) {
                byte[] fingerprint = KeyMetadata.FACTORY.selectFingerprintsForKeysOlderThanMapper().map(cursor);
                fingerprintList.add(fingerprint);
            }
        }
        return fingerprintList;
    }
}
