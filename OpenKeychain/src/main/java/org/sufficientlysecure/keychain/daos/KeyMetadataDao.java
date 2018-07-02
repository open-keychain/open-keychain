package org.sufficientlysecure.keychain.daos;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.KeyMetadataModel.ReplaceKeyMetadata;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.KeyMetadata;


public class KeyMetadataDao extends AbstractDao {
    public static KeyMetadataDao create(Context context) {
        KeychainDatabase database = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new KeyMetadataDao(database, databaseNotifyManager);
    }

    private KeyMetadataDao(KeychainDatabase database, DatabaseNotifyManager databaseNotifyManager) {
        super(database, databaseNotifyManager);
    }

    public KeyMetadata getKeyMetadata(long masterKeyId) {
        SqlDelightQuery query = KeyMetadata.FACTORY.selectByMasterKeyId(masterKeyId);
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToFirst()) {
                return KeyMetadata.FACTORY.selectByMasterKeyIdMapper().map(cursor);
            }
        }
        return null;
    }

    public void resetAllLastUpdatedTimes() {
        new KeyMetadata.DeleteAllLastUpdatedTimes(getWritableDb()).execute();
    }

    public void renewKeyLastUpdatedTime(long masterKeyId, boolean seenOnKeyservers) {
        ReplaceKeyMetadata replaceStatement = new ReplaceKeyMetadata(getWritableDb(), KeyMetadata.FACTORY);
        replaceStatement.bind(masterKeyId, new Date(), seenOnKeyservers);
        replaceStatement.executeInsert();

        getDatabaseNotifyManager().notifyKeyMetadataChange(masterKeyId);
    }

    public List<byte[]> getFingerprintsForKeysOlderThan(long olderThan, TimeUnit timeUnit) {
        SqlDelightQuery query = KeyMetadata.FACTORY.selectFingerprintsForKeysOlderThan(new Date(timeUnit.toMillis(olderThan)));

        List<byte[]> fingerprintList = new ArrayList<>();
        try (Cursor cursor = getReadableDb().query(query)) {
            while (cursor.moveToNext()) {
                byte[] fingerprint = KeyMetadata.FACTORY.selectFingerprintsForKeysOlderThanMapper().map(cursor);
                fingerprintList.add(fingerprint);
            }
        }
        return fingerprintList;
    }
}
