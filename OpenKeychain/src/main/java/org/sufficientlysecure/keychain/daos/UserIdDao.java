package org.sufficientlysecure.keychain.daos;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.squareup.sqldelight.Query;
import com.squareup.sqldelight.db.SqlCursor;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.UidStatus;
import org.sufficientlysecure.keychain.model.UserId;


public class UserIdDao extends AbstractDao {
    public static UserIdDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new UserIdDao(keychainDatabase, databaseNotifyManager);
    }

    private UserIdDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        super(db, databaseNotifyManager);
    }

    public List<UserId> getUserIdsByMasterKeyIds(long... masterKeyIds) {
        return getDatabase().getUserPacketsQueries()
                .selectUserIdsByMasterKeyId(getLongArrayAsList(masterKeyIds), UserId::create)
                .executeAsList();
    }

    public UidStatus getUidStatusByEmailLike(String emailLike) {
        return getDatabase().getUserPacketsQueries().selectUserIdStatusByEmailLike(emailLike)
                .executeAsOne();
    }

    public Map<String, UidStatus> getUidStatusByEmail(String... emails) {
        Query<UidStatus> q = getDatabase().getUserPacketsQueries()
                .selectUserIdStatusByEmail(Arrays.asList(emails));
        Map<String, UidStatus> result = new HashMap<>();
        try (SqlCursor cursor = q.execute()) {
            while (cursor.next()) {
                UidStatus item = q.getMapper().invoke(cursor);
                result.put(item.getEmail(), item);
            }
        } catch (IOException e) {
            // oops
        }
        return result;
    }

    private List<Long> getLongArrayAsList(long[] longList) {
        Long[] longs = new Long[longList.length];
        int i = 0;
        for (Long aLong : longList) {
            longs[i++] = aLong;
        }
        return Arrays.asList(longs);
    }
}