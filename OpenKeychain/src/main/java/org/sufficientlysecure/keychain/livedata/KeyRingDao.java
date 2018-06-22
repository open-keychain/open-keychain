package org.sufficientlysecure.keychain.livedata;


import java.util.ArrayList;
import java.util.List;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;


public class KeyRingDao {
    private final SupportSQLiteDatabase db;

    public static KeyRingDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = new KeychainDatabase(context);

        return new KeyRingDao(keychainDatabase.getWritableDatabase());
    }

    private KeyRingDao(SupportSQLiteDatabase writableDatabase) {
        this.db = writableDatabase;
    }

    public List<UnifiedKeyInfo> getUnifiedKeyInfo() {
        SqlDelightQuery query = SubKey.FACTORY.selectAllUnifiedKeyInfo();
        List<UnifiedKeyInfo> result = new ArrayList<>();
        try (Cursor cursor = db.query(query)) {
            while (cursor.moveToNext()) {
                UnifiedKeyInfo unifiedKeyInfo = SubKey.UNIFIED_KEY_INFO_MAPPER.map(cursor);
                result.add(unifiedKeyInfo);
            }
        }
        return result;
    }
}
