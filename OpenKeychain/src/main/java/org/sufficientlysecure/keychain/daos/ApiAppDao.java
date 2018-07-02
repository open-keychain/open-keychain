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

package org.sufficientlysecure.keychain.daos;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.ApiAllowedKeysModel.InsertAllowedKey;
import org.sufficientlysecure.keychain.ApiAppsModel;
import org.sufficientlysecure.keychain.ApiAppsModel.DeleteByPackageName;
import org.sufficientlysecure.keychain.ApiAppsModel.InsertApiApp;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.ApiAllowedKey;
import org.sufficientlysecure.keychain.model.ApiApp;


public class ApiAppDao extends AbstractDao {
    public static ApiAppDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new ApiAppDao(keychainDatabase, databaseNotifyManager);
    }

    private ApiAppDao(KeychainDatabase keychainDatabase, DatabaseNotifyManager databaseNotifyManager) {
        super(keychainDatabase, databaseNotifyManager);
    }

    public ApiApp getApiApp(String packageName) {
        try (Cursor cursor = getReadableDb().query(ApiApp.FACTORY.selectByPackageName(packageName))) {
            if (cursor.moveToFirst()) {
                return ApiApp.FACTORY.selectByPackageNameMapper().map(cursor);
            }
            return null;
        }
    }

    public byte[] getApiAppCertificate(String packageName) {
        try (Cursor cursor = getReadableDb().query(ApiApp.FACTORY.getCertificate(packageName))) {
            if (cursor.moveToFirst()) {
                return ApiApp.FACTORY.getCertificateMapper().map(cursor);
            }
            return null;
        }
    }

    public void insertApiApp(ApiApp apiApp) {
        ApiApp existingApiApp = getApiApp(apiApp.package_name());
        if (existingApiApp != null) {
            if (!Arrays.equals(existingApiApp.package_signature(), apiApp.package_signature())) {
                throw new IllegalStateException("Inserting existing api with different signature?!");
            }
            return;
        }
        InsertApiApp statement = new ApiAppsModel.InsertApiApp(getWritableDb());
        statement.bind(apiApp.package_name(), apiApp.package_signature());
        statement.executeInsert();

        getDatabaseNotifyManager().notifyApiAppChange(apiApp.package_name());
    }

    public void deleteApiApp(String packageName) {
        DeleteByPackageName deleteByPackageName = new DeleteByPackageName(getWritableDb());
        deleteByPackageName.bind(packageName);
        deleteByPackageName.executeUpdateDelete();

        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public HashSet<Long> getAllowedKeyIdsForApp(String packageName) {
        SqlDelightQuery allowedKeys = ApiAllowedKey.FACTORY.getAllowedKeys(packageName);
        HashSet<Long> keyIds = new HashSet<>();
        try (Cursor cursor = getReadableDb().query(allowedKeys)) {
            while (cursor.moveToNext()) {
                long allowedKeyId = ApiAllowedKey.FACTORY.getAllowedKeysMapper().map(cursor);
                keyIds.add(allowedKeyId);
            }
        }
        return keyIds;
    }

    public void saveAllowedKeyIdsForApp(String packageName, Set<Long> allowedKeyIds) {
        ApiAllowedKey.DeleteByPackageName deleteByPackageName = new ApiAllowedKey.DeleteByPackageName(getWritableDb());
        deleteByPackageName.bind(packageName);
        deleteByPackageName.executeUpdateDelete();

        InsertAllowedKey statement = new InsertAllowedKey(getWritableDb());
        for (Long keyId : allowedKeyIds) {
            statement.bind(packageName, keyId);
            statement.execute();
        }

        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public void addAllowedKeyIdForApp(String packageName, long allowedKeyId) {
        InsertAllowedKey statement = new InsertAllowedKey(getWritableDb());
        statement.bind(packageName, allowedKeyId);
        statement.execute();

        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public List<ApiApp> getAllApiApps() {
        SqlDelightQuery query = ApiApp.FACTORY.selectAll();

        ArrayList<ApiApp> result = new ArrayList<>();
        try (Cursor cursor = getReadableDb().query(query)) {
            while (cursor.moveToNext()) {
                ApiApp apiApp = ApiApp.FACTORY.selectAllMapper().map(cursor);
                result.add(apiApp);
            }
        }
        return result;
    }
}
