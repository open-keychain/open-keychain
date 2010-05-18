/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class DataProvider extends ContentProvider {
    public static final String AUTHORITY = "org.thialfihar.android.apg.provider";

    private static final String DATABASE_NAME = "apg";
    private static final int DATABASE_VERSION = 1;

    private static final int PUBLIC_KEYS = 101;
    private static final int PUBLIC_KEY_ID = 102;
    private static final int PUBLIC_KEY_BY_KEY_ID = 103;

    private static final int SECRET_KEYS = 201;
    private static final int SECRET_KEY_ID = 202;
    private static final int SECRET_KEY_BY_KEY_ID = 203;

    private static final int ACCOUNTS = 301;
    private static final int ACCOUNT_ID = 302;

    private static final UriMatcher mUriMatcher;
    private static final HashMap<String, String> mPublicKeysProjectionMap;
    private static final HashMap<String, String> mSecretKeysProjectionMap;
    private static final HashMap<String, String> mAccountsProjectionMap;

    private DatabaseHelper mdbHelper;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "public_keys", PUBLIC_KEYS);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "public_keys/#", PUBLIC_KEY_ID);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "public_keys/key_id/*", PUBLIC_KEY_BY_KEY_ID);

        mUriMatcher.addURI(DataProvider.AUTHORITY, "secret_keys", SECRET_KEYS);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "secret_keys/#", SECRET_KEY_ID);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "secret_keys/key_id/*", SECRET_KEY_BY_KEY_ID);

        mUriMatcher.addURI(DataProvider.AUTHORITY, "accounts", ACCOUNTS);
        mUriMatcher.addURI(DataProvider.AUTHORITY, "accounts/#", ACCOUNT_ID);

        mPublicKeysProjectionMap = new HashMap<String, String>();
        mPublicKeysProjectionMap.put(PublicKeys._ID, PublicKeys._ID);
        mPublicKeysProjectionMap.put(PublicKeys.KEY_ID, PublicKeys.KEY_ID);
        mPublicKeysProjectionMap.put(PublicKeys.KEY_DATA, PublicKeys.KEY_DATA);
        mPublicKeysProjectionMap.put(PublicKeys.WHO_ID, PublicKeys.WHO_ID);

        mSecretKeysProjectionMap = new HashMap<String, String>();
        mSecretKeysProjectionMap.put(PublicKeys._ID, PublicKeys._ID);
        mSecretKeysProjectionMap.put(PublicKeys.KEY_ID, PublicKeys.KEY_ID);
        mSecretKeysProjectionMap.put(PublicKeys.KEY_DATA, PublicKeys.KEY_DATA);
        mSecretKeysProjectionMap.put(PublicKeys.WHO_ID, PublicKeys.WHO_ID);

        mAccountsProjectionMap = new HashMap<String, String>();
        mAccountsProjectionMap.put(Accounts._ID, Accounts._ID);
        mAccountsProjectionMap.put(Accounts.NAME, Accounts.NAME);
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PublicKeys.TABLE_NAME + " (" +
                       PublicKeys._ID + " " + PublicKeys._ID_type + "," +
                       PublicKeys.KEY_ID + " " + PublicKeys.KEY_ID_type + ", " +
                       PublicKeys.KEY_DATA + " " + PublicKeys.KEY_DATA_type + ", " +
                       PublicKeys.WHO_ID + " " + PublicKeys.WHO_ID_type + ");");

            db.execSQL("CREATE TABLE " + SecretKeys.TABLE_NAME + " (" +
                       SecretKeys._ID + " " + SecretKeys._ID_type + "," +
                       SecretKeys.KEY_ID + " " + SecretKeys.KEY_ID_type + ", " +
                       SecretKeys.KEY_DATA + " " + SecretKeys.KEY_DATA_type + ", " +
                       SecretKeys.WHO_ID + " " + SecretKeys.WHO_ID_type + ");");

            db.execSQL("CREATE TABLE " + Accounts.TABLE_NAME + " (" +
                       Accounts._ID + " " + Accounts._ID_type + "," +
                       Accounts.NAME + " " + Accounts.NAME_type + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: upgrade db if necessary, and do that in a clever way
        }
    }

    @Override
    public boolean onCreate() {
        mdbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEYS: {
                qb.setTables(PublicKeys.TABLE_NAME);
                qb.setProjectionMap(mPublicKeysProjectionMap);
                break;
            }

            case PUBLIC_KEY_ID: {
                qb.setTables(PublicKeys.TABLE_NAME);
                qb.setProjectionMap(mPublicKeysProjectionMap);
                qb.appendWhere(PublicKeys._ID + "=" + uri.getPathSegments().get(1));
                break;
            }

            case PUBLIC_KEY_BY_KEY_ID: {
                qb.setTables(PublicKeys.TABLE_NAME);
                qb.setProjectionMap(mPublicKeysProjectionMap);
                qb.appendWhere(PublicKeys.KEY_ID + "=" + uri.getPathSegments().get(2));
                break;
            }

            case SECRET_KEYS: {
                qb.setTables(SecretKeys.TABLE_NAME);
                qb.setProjectionMap(mSecretKeysProjectionMap);
                break;
            }

            case SECRET_KEY_ID: {
                qb.setTables(SecretKeys.TABLE_NAME);
                qb.setProjectionMap(mSecretKeysProjectionMap);
                qb.appendWhere(SecretKeys._ID + "=" + uri.getPathSegments().get(1));
                break;
            }

            case SECRET_KEY_BY_KEY_ID: {
                qb.setTables(SecretKeys.TABLE_NAME);
                qb.setProjectionMap(mSecretKeysProjectionMap);
                qb.appendWhere(SecretKeys.KEY_ID + "=" + uri.getPathSegments().get(2));
                break;
            }

            case ACCOUNTS: {
                qb.setTables(Accounts.TABLE_NAME);
                qb.setProjectionMap(mAccountsProjectionMap);
                break;
            }

            case ACCOUNT_ID: {
                qb.setTables(Accounts.TABLE_NAME);
                qb.setProjectionMap(mAccountsProjectionMap);
                qb.appendWhere(Accounts._ID + "=" + uri.getPathSegments().get(1));
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = PublicKeys.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mdbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEYS: {
                return PublicKeys.CONTENT_TYPE;
            }

            case PUBLIC_KEY_ID: {
                return PublicKeys.CONTENT_ITEM_TYPE;
            }

            case PUBLIC_KEY_BY_KEY_ID: {
                return PublicKeys.CONTENT_ITEM_TYPE;
            }

            case SECRET_KEYS: {
                return SecretKeys.CONTENT_TYPE;
            }

            case SECRET_KEY_ID: {
                return SecretKeys.CONTENT_ITEM_TYPE;
            }

            case SECRET_KEY_BY_KEY_ID: {
                return SecretKeys.CONTENT_ITEM_TYPE;
            }

            case ACCOUNTS: {
                return Accounts.CONTENT_TYPE;
            }

            case ACCOUNT_ID: {
                return Accounts.CONTENT_ITEM_TYPE;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEYS: {
                ContentValues values;
                if (initialValues != null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }

                if (!values.containsKey(PublicKeys.WHO_ID)) {
                    values.put(PublicKeys.WHO_ID, "");
                }

                SQLiteDatabase db = mdbHelper.getWritableDatabase();
                long rowId = db.insert(PublicKeys.TABLE_NAME, PublicKeys.WHO_ID, values);
                if (rowId > 0) {
                    Uri transferUri = ContentUris.withAppendedId(PublicKeys.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(transferUri, null);
                    return transferUri;
                }

                throw new SQLException("Failed to insert row into " + uri);
            }

            case SECRET_KEYS: {
                ContentValues values;
                if (initialValues != null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }

                if (!values.containsKey(SecretKeys.WHO_ID)) {
                    values.put(SecretKeys.WHO_ID, "");
                }

                SQLiteDatabase db = mdbHelper.getWritableDatabase();
                long rowId = db.insert(SecretKeys.TABLE_NAME, SecretKeys.WHO_ID, values);
                if (rowId > 0) {
                    Uri transferUri = ContentUris.withAppendedId(SecretKeys.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(transferUri, null);
                    return transferUri;
                }

                throw new SQLException("Failed to insert row into " + uri);
            }

            case ACCOUNTS: {
                ContentValues values;
                if (initialValues != null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }

                SQLiteDatabase db = mdbHelper.getWritableDatabase();
                long rowId = db.insert(Accounts.TABLE_NAME, null, values);
                if (rowId > 0) {
                    Uri transferUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(transferUri, null);
                    return transferUri;
                }

                throw new SQLException("Failed to insert row into " + uri);
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mdbHelper.getWritableDatabase();
        int count;
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEYS: {
                count = db.delete(PublicKeys.TABLE_NAME, where, whereArgs);
                break;
            }

            case PUBLIC_KEY_ID: {
                String publicKeyId = uri.getPathSegments().get(1);
                count = db.delete(PublicKeys.TABLE_NAME,
                                  PublicKeys._ID + "=" + publicKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case PUBLIC_KEY_BY_KEY_ID: {
                String publicKeyKeyId = uri.getPathSegments().get(2);
                count = db.delete(PublicKeys.TABLE_NAME,
                                  PublicKeys.KEY_ID + "=" + publicKeyKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case SECRET_KEYS: {
                count = db.delete(SecretKeys.TABLE_NAME, where, whereArgs);
                break;
            }

            case SECRET_KEY_ID: {
                String secretKeyId = uri.getPathSegments().get(1);
                count = db.delete(SecretKeys.TABLE_NAME,
                                  SecretKeys._ID + "=" + secretKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case SECRET_KEY_BY_KEY_ID: {
                String secretKeyKeyId = uri.getPathSegments().get(2);
                count = db.delete(SecretKeys.TABLE_NAME,
                                  SecretKeys.KEY_ID + "=" + secretKeyKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case ACCOUNTS: {
                count = db.delete(Accounts.TABLE_NAME, where, whereArgs);
                break;
            }

            case ACCOUNT_ID: {
                String accountId = uri.getPathSegments().get(1);
                count = db.delete(Accounts.TABLE_NAME,
                                  Accounts._ID + "=" + accountId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mdbHelper.getWritableDatabase();
        int count;
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEYS: {
                count = db.update(PublicKeys.TABLE_NAME, values, where, whereArgs);
                break;
            }

            case PUBLIC_KEY_ID: {
                String publicKeyId = uri.getPathSegments().get(1);

                count = db.update(PublicKeys.TABLE_NAME, values,
                                  PublicKeys._ID + "=" + publicKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case PUBLIC_KEY_BY_KEY_ID: {
                String publicKeyKeyId = uri.getPathSegments().get(2);

                count = db.update(PublicKeys.TABLE_NAME, values,
                                  PublicKeys.KEY_ID + "=" + publicKeyKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case SECRET_KEYS: {
                count = db.update(SecretKeys.TABLE_NAME, values, where, whereArgs);
                break;
            }

            case SECRET_KEY_ID: {
                String secretKeyId = uri.getPathSegments().get(1);

                count = db.update(SecretKeys.TABLE_NAME, values,
                                  SecretKeys._ID + "=" + secretKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case SECRET_KEY_BY_KEY_ID: {
                String secretKeyKeyId = uri.getPathSegments().get(2);

                count = db.update(SecretKeys.TABLE_NAME, values,
                                  SecretKeys.KEY_ID + "=" + secretKeyKeyId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            case ACCOUNTS: {
                count = db.update(Accounts.TABLE_NAME, values, where, whereArgs);
                break;
            }

            case ACCOUNT_ID: {
                String accountId = uri.getPathSegments().get(1);

                count = db.update(Accounts.TABLE_NAME, values,
                                  Accounts._ID + "=" + accountId +
                                          (!TextUtils.isEmpty(where) ?
                                                   " AND (" + where + ')' : ""),
                                  whereArgs);
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
