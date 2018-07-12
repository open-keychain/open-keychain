package org.sufficientlysecure.keychain.daos;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.UserPacket;
import org.sufficientlysecure.keychain.model.UserPacket.UidStatus;


public class UserIdDao extends AbstractDao {
    public static UserIdDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new UserIdDao(keychainDatabase, databaseNotifyManager);
    }

    private UserIdDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        super(db, databaseNotifyManager);
    }

    public UidStatus getUidStatusByEmailLike(String emailLike) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdStatusByEmailLike(emailLike);
        return mapSingleRow(query, UserPacket.UID_STATUS_MAPPER);
    }

    public Map<String,UidStatus> getUidStatusByEmail(String... emails) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdStatusByEmail(emails);
        Map<String,UidStatus> result = new HashMap<>();
        try (Cursor cursor = getReadableDb().query(query)) {
            while (cursor.moveToNext()) {
                UidStatus item = UserPacket.UID_STATUS_MAPPER.map(cursor);
                result.put(item.email(), item);
            }
        }
        return result;
    }
}
