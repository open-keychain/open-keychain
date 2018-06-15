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

package org.sufficientlysecure.keychain.provider;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.ApiAllowedKeysModel.InsertAllowedKey;
import org.sufficientlysecure.keychain.ApiAppsModel;
import org.sufficientlysecure.keychain.ApiAppsModel.DeleteByPackageName;
import org.sufficientlysecure.keychain.ApiAppsModel.InsertApiApp;
import org.sufficientlysecure.keychain.model.ApiAllowedKey;
import org.sufficientlysecure.keychain.model.ApiApp;


public class ApiDataAccessObject {

    private final SupportSQLiteDatabase db;

    public ApiDataAccessObject(Context context) {
        KeychainDatabase keychainDatabase = new KeychainDatabase(context);
        db = keychainDatabase.getWritableDatabase();
    }

    public ApiApp getApiApp(String packageName) {
        try (Cursor cursor = db.query(ApiApp.FACTORY.selectByPackageName(packageName))) {
            if (cursor.moveToFirst()) {
                return ApiApp.FACTORY.selectByPackageNameMapper().map(cursor);
            }
            return null;
        }
    }

    public byte[] getApiAppCertificate(String packageName) {
        Cursor cursor = db.query(ApiApp.FACTORY.getCertificate(packageName));
        return ApiApp.FACTORY.getCertificateMapper().map(cursor);
    }

    public void insertApiApp(ApiApp apiApp) {
        InsertApiApp statement = new ApiAppsModel.InsertApiApp(db);
        statement.bind(apiApp.package_name(), apiApp.package_signature());
        statement.execute();
    }

    public void deleteApiApp(String packageName) {
        DeleteByPackageName deleteByPackageName = new DeleteByPackageName(db);
        deleteByPackageName.bind(packageName);
        deleteByPackageName.executeUpdateDelete();
    }

    public HashSet<Long> getAllowedKeyIdsForApp(String packageName) {
        SqlDelightQuery allowedKeys = ApiAllowedKey.FACTORY.getAllowedKeys(packageName);
        HashSet<Long> keyIds = new HashSet<>();
        try (Cursor cursor = db.query(allowedKeys)) {
            while (cursor.moveToNext()) {
                long allowedKeyId = ApiAllowedKey.FACTORY.getAllowedKeysMapper().map(cursor);
                keyIds.add(allowedKeyId);
            }
        }
        return keyIds;
    }

    public void saveAllowedKeyIdsForApp(String packageName, Set<Long> allowedKeyIds) {
        ApiAllowedKey.DeleteByPackageName deleteByPackageName = new ApiAllowedKey.DeleteByPackageName(db);
        deleteByPackageName.bind(packageName);
        deleteByPackageName.executeUpdateDelete();

        InsertAllowedKey statement = new InsertAllowedKey(db);
        for (Long keyId : allowedKeyIds) {
            statement.bind(packageName, keyId);
            statement.execute();
        }
    }

    public void addAllowedKeyIdForApp(String packageName, long allowedKeyId) {
        InsertAllowedKey statement = new InsertAllowedKey(db);
        statement.bind(packageName, allowedKeyId);
        statement.execute();
    }

    public List<ApiApp> getAllApiApps() {
        SqlDelightQuery query = ApiApp.FACTORY.selectAll();

        ArrayList<ApiApp> result = new ArrayList<>();
        try (Cursor cursor = db.query(query)) {
            while (cursor.moveToNext()) {
                ApiApp apiApp = ApiApp.FACTORY.selectAllMapper().map(cursor);
                result.add(apiApp);
            }
        }
        return result;
    }
}
