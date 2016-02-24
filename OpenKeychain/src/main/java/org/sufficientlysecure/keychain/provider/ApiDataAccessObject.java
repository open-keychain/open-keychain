/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014-2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAllowedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;


public class ApiDataAccessObject {

    private final SimpleContentResolverInterface mQueryInterface;

    public ApiDataAccessObject(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        mQueryInterface = new SimpleContentResolverInterface() {
            @Override
            public Cursor query(Uri contentUri, String[] projection, String selection, String[] selectionArgs,
                    String sortOrder) {
                return contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
            }

            @Override
            public Uri insert(Uri contentUri, ContentValues values) {
                return contentResolver.insert(contentUri, values);
            }

            @Override
            public int update(Uri contentUri, ContentValues values, String where, String[] selectionArgs) {
                return contentResolver.update(contentUri, values, where, selectionArgs);
            }

            @Override
            public int delete(Uri contentUri, String where, String[] selectionArgs) {
                return contentResolver.delete(contentUri, where, selectionArgs);
            }
        };
    }

    public ApiDataAccessObject(SimpleContentResolverInterface queryInterface) {
        mQueryInterface = queryInterface;
    }

    public ArrayList<String> getRegisteredApiApps() {
        Cursor cursor = mQueryInterface.query(ApiApps.CONTENT_URI, null, null, null, null);

        ArrayList<String> packageNames = new ArrayList<>();
        try {
            if (cursor != null) {
                int packageNameCol = cursor.getColumnIndex(ApiApps.PACKAGE_NAME);
                if (cursor.moveToFirst()) {
                    do {
                        packageNames.add(cursor.getString(packageNameCol));
                    } while (cursor.moveToNext());
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return packageNames;
    }

    private ContentValues contentValueForApiApps(AppSettings appSettings) {
        ContentValues values = new ContentValues();
        values.put(ApiApps.PACKAGE_NAME, appSettings.getPackageName());
        values.put(ApiApps.PACKAGE_CERTIFICATE, appSettings.getPackageCertificate());
        return values;
    }

    private ContentValues contentValueForApiAccounts(AccountSettings accSettings) {
        ContentValues values = new ContentValues();
        values.put(KeychainContract.ApiAccounts.ACCOUNT_NAME, accSettings.getAccountName());
        values.put(KeychainContract.ApiAccounts.KEY_ID, accSettings.getKeyId());

        // DEPRECATED and thus hardcoded
        values.put(KeychainContract.ApiAccounts.COMPRESSION, CompressionAlgorithmTags.ZLIB);
        values.put(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM,
                PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_DEFAULT);
        values.put(KeychainContract.ApiAccounts.HASH_ALORITHM,
                PgpSecurityConstants.OpenKeychainHashAlgorithmTags.USE_DEFAULT);
        return values;
    }

    public void insertApiApp(AppSettings appSettings) {
        mQueryInterface.insert(ApiApps.CONTENT_URI,
                contentValueForApiApps(appSettings));
    }

    public void insertApiAccount(Uri uri, AccountSettings accSettings) {
        mQueryInterface.insert(uri, contentValueForApiAccounts(accSettings));
    }

    public void updateApiAccount(Uri uri, AccountSettings accSettings) {
        if (mQueryInterface.update(uri, contentValueForApiAccounts(accSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    /**
     * Must be an uri pointing to an account
     */
    public AppSettings getApiAppSettings(Uri uri) {
        AppSettings settings = null;

        Cursor cursor = mQueryInterface.query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                settings = new AppSettings();
                settings.setPackageName(cursor.getString(
                        cursor.getColumnIndex(ApiApps.PACKAGE_NAME)));
                settings.setPackageCertificate(cursor.getBlob(
                        cursor.getColumnIndex(ApiApps.PACKAGE_CERTIFICATE)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return settings;
    }

    public AccountSettings getApiAccountSettings(Uri accountUri) {
        AccountSettings settings = null;

        Cursor cursor = mQueryInterface.query(accountUri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                settings = new AccountSettings();

                settings.setAccountName(cursor.getString(
                        cursor.getColumnIndex(KeychainContract.ApiAccounts.ACCOUNT_NAME)));
                settings.setKeyId(cursor.getLong(
                        cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return settings;
    }

    public Set<Long> getAllKeyIdsForApp(Uri uri) {
        Set<Long> keyIds = new HashSet<>();

        Cursor cursor = mQueryInterface.query(uri, null, null, null, null);
        try {
            if (cursor != null) {
                int keyIdColumn = cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID);
                while (cursor.moveToNext()) {
                    keyIds.add(cursor.getLong(keyIdColumn));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return keyIds;
    }

    public HashSet<Long> getAllowedKeyIdsForApp(Uri uri) {
        HashSet<Long> keyIds = new HashSet<>();

        Cursor cursor = mQueryInterface.query(uri, null, null, null, null);
        try {
            if (cursor != null) {
                int keyIdColumn = cursor.getColumnIndex(ApiAllowedKeys.KEY_ID);
                while (cursor.moveToNext()) {
                    keyIds.add(cursor.getLong(keyIdColumn));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return keyIds;
    }

    public void saveAllowedKeyIdsForApp(Uri uri, Set<Long> allowedKeyIds)
            throws RemoteException, OperationApplicationException {
        // wipe whole table of allowed keys for this account
        mQueryInterface.delete(uri, null, null);

        // re-insert allowed key ids
        for (Long keyId : allowedKeyIds) {
            ContentValues values = new ContentValues();
            values.put(ApiAllowedKeys.KEY_ID, keyId);
            mQueryInterface.insert(uri, values);
        }
    }

    public void addAllowedKeyIdForApp(Uri uri, long allowedKeyId) {
        ContentValues values = new ContentValues();
        values.put(ApiAllowedKeys.KEY_ID, allowedKeyId);
        mQueryInterface.insert(uri, values);
    }

    public byte[] getApiAppCertificate(String packageName) {
        Uri queryUri = ApiApps.buildByPackageNameUri(packageName);

        String[] projection = new String[]{ApiApps.PACKAGE_CERTIFICATE};

        Cursor cursor = mQueryInterface.query(queryUri, projection, null, null, null);
        try {
            byte[] signature = null;
            if (cursor != null && cursor.moveToFirst()) {
                int signatureCol = 0;

                signature = cursor.getBlob(signatureCol);
            }
            return signature;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
