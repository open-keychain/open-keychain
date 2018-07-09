package org.sufficientlysecure.keychain.daos;


import java.util.List;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.UserPacket;
import org.sufficientlysecure.keychain.model.UserPacket.UidStatus;


public class UserIdDao extends AbstractDao {
    public UserIdDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        super(db, databaseNotifyManager);
    }

    public List<UidStatus> getUidStatusByEmailLike(String emailLike) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdStatusByEmailLike(emailLike);
        return mapAllRows(query, UserPacket.UID_STATUS_MAPPER);
    }

    public List<UidStatus> getUidStatusByEmail(String... emails) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdStatusByEmail(emails);
        return mapAllRows(query, UserPacket.UID_STATUS_MAPPER);
    }
}
