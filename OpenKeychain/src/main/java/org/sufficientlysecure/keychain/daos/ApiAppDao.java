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


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;

import org.sufficientlysecure.keychain.ApiAllowedKeysQueries;
import org.sufficientlysecure.keychain.ApiAppsQueries;
import org.sufficientlysecure.keychain.Api_apps;
import org.sufficientlysecure.keychain.GetCertificate;
import org.sufficientlysecure.keychain.KeychainDatabase;


public class ApiAppDao extends AbstractDao {
    private final ApiAppsQueries apiAppsQueries = getDatabase().getApiAppsQueries();
    private final ApiAllowedKeysQueries apiAllowedKeysQueries =
            getDatabase().getApiAllowedKeysQueries();

    public static ApiAppDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new ApiAppDao(keychainDatabase, databaseNotifyManager);
    }

    private ApiAppDao(KeychainDatabase keychainDatabase,
            DatabaseNotifyManager databaseNotifyManager) {
        super(keychainDatabase, databaseNotifyManager);
    }

    public Api_apps getApiApp(String packageName) {
        return apiAppsQueries.selectByPackageName(packageName).executeAsOneOrNull();
    }

    public byte[] getApiAppCertificate(String packageName) {
        GetCertificate getCertificate =
                apiAppsQueries.getCertificate(packageName).executeAsOneOrNull();
        if (getCertificate == null) {
            return null;
        }
        return getCertificate.getPackage_signature();
    }

    public void insertApiApp(String packageName, byte[] signature) {
        Api_apps existingApiApp = getApiApp(packageName);
        if (existingApiApp != null) {
            if (!Arrays.equals(existingApiApp.getPackage_signature(),
                    signature)) {
                throw new IllegalStateException(
                        "Inserting existing api with different signature?!");
            }
            return;
        }
        apiAppsQueries.insertApiApp(packageName, signature);
        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public void insertApiApp(Api_apps apiApp) {
        Api_apps existingApiApp = getApiApp(apiApp.getPackage_name());
        if (existingApiApp != null) {
            if (!Arrays.equals(existingApiApp.getPackage_signature(),
                    apiApp.getPackage_signature())) {
                throw new IllegalStateException(
                        "Inserting existing api with different signature?!");
            }
            return;
        }
        apiAppsQueries.insertApiApp(apiApp.getPackage_name(), apiApp.getPackage_signature());
        getDatabaseNotifyManager().notifyApiAppChange(apiApp.getPackage_name());
    }

    public void deleteApiApp(String packageName) {
        apiAppsQueries.deleteByPackageName(packageName);
        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public HashSet<Long> getAllowedKeyIdsForApp(String packageName) {
        return new HashSet<>(apiAllowedKeysQueries.getAllowedKeys(packageName).executeAsList());
    }

    public void saveAllowedKeyIdsForApp(String packageName, Set<Long> allowedKeyIds) {
        apiAllowedKeysQueries.deleteByPackageName(packageName);
        for (Long keyId : allowedKeyIds) {
            apiAllowedKeysQueries.insertAllowedKey(packageName, keyId);
        }
        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public void addAllowedKeyIdForApp(String packageName, long allowedKeyId) {
        apiAllowedKeysQueries.insertAllowedKey(packageName, allowedKeyId);
        getDatabaseNotifyManager().notifyApiAppChange(packageName);
    }

    public List<Api_apps> getAllApiApps() {
        return apiAppsQueries.selectAll().executeAsList();
    }
}
