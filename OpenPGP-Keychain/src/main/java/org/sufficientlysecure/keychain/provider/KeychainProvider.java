/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAccounts;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingsColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyTypes;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeysColumns;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIdsColumns;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Arrays;
import java.util.HashMap;

public class KeychainProvider extends ContentProvider {
    // public static final String ACTION_BROADCAST_DATABASE_CHANGE = Constants.PACKAGE_NAME
    // + ".action.DATABASE_CHANGE";
    //
    // public static final String EXTRA_BROADCAST_KEY_TYPE = "key_type";
    // public static final String EXTRA_BROADCAST_CONTENT_ITEM_TYPE = "contentItemType";

    private static final int KEY_RINGS_UNIFIED = 101;

    private static final int KEY_RING_UNIFIED = 200;
    private static final int KEY_RING_KEYS = 201;
    private static final int KEY_RING_USER_IDS = 202;
    private static final int KEY_RING_PUBLIC = 203;
    private static final int KEY_RING_SECRET = 204;

    private static final int API_APPS = 301;
    private static final int API_APPS_BY_PACKAGE_NAME = 303;
    private static final int API_ACCOUNTS = 304;
    private static final int API_ACCOUNTS_BY_ACCOUNT_NAME = 306;

    // private static final int DATA_STREAM = 401;

    protected UriMatcher mUriMatcher;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainContract.CONTENT_AUTHORITY;

        /**
         * select from key_ring
         *
         * <pre>
         * key_rings/unified
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                + "/" + KeychainContract.PATH_UNIFIED,
                KEY_RINGS_UNIFIED);

        /**
         * select from key_ring
         *
         * <pre>
         * key_rings/_/unified
         * key_rings/_/keys
         * key_rings/_/user_ids
         * key_rings/_/public
         * key_rings/_/secret
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_UNIFIED,
                KEY_RING_UNIFIED);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_KEYS,
                KEY_RING_KEYS);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_USER_IDS,
                KEY_RING_USER_IDS);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_PUBLIC,
                KEY_RING_PUBLIC);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_SECRET,
                KEY_RING_SECRET);

        /**
         * API apps
         *
         * <pre>
         * api_apps
         * api_apps/_ (package name)
         *
         * api_apps/_/accounts
         * api_apps/_/accounts/_ (account name)
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_API_APPS, API_APPS);
        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*", API_APPS_BY_PACKAGE_NAME);

        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*/"
                + KeychainContract.PATH_ACCOUNTS, API_ACCOUNTS);
        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*/"
                + KeychainContract.PATH_ACCOUNTS + "/*", API_ACCOUNTS_BY_ACCOUNT_NAME);

        /**
         * data stream
         *
         * <pre>
         * data / _
         * </pre>
         */
        // matcher.addURI(authority, KeychainContract.BASE_DATA + "/*", DATA_STREAM);

        return matcher;
    }

    private KeychainDatabase mKeychainDatabase;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreate() {
        mUriMatcher = buildUriMatcher();
        mKeychainDatabase = new KeychainDatabase(getContext());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Uri uri) {
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case KEY_RING_PUBLIC:
                return KeyRings.CONTENT_ITEM_TYPE;

            case KEY_RING_KEYS:
                return Keys.CONTENT_TYPE;

            case KEY_RING_USER_IDS:
                return UserIds.CONTENT_TYPE;

            case KEY_RING_SECRET:
                return KeyRings.CONTENT_ITEM_TYPE;

            case API_APPS:
                return ApiApps.CONTENT_TYPE;

            case API_APPS_BY_PACKAGE_NAME:
                return ApiApps.CONTENT_ITEM_TYPE;

            case API_ACCOUNTS:
                return ApiAccounts.CONTENT_TYPE;

            case API_ACCOUNTS_BY_ACCOUNT_NAME:
                return ApiAccounts.CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.v(Constants.TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mKeychainDatabase.getReadableDatabase();

        int match = mUriMatcher.match(uri);

        // all query() parameters, for good measure
        String groupBy = null, having = null;

        switch (match) {
            case KEY_RING_UNIFIED:
            case KEY_RINGS_UNIFIED: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(BaseColumns._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(KeysColumns.MASTER_KEY_ID, Tables.KEYS + "." + KeysColumns.MASTER_KEY_ID);
                projectionMap.put(KeysColumns.RANK, Tables.KEYS + "." + KeysColumns.RANK);
                projectionMap.put(KeysColumns.KEY_ID, KeysColumns.KEY_ID);
                projectionMap.put(KeysColumns.KEY_SIZE, KeysColumns.KEY_SIZE);
                projectionMap.put(KeysColumns.IS_REVOKED, KeysColumns.IS_REVOKED);
                projectionMap.put(KeysColumns.CAN_CERTIFY, KeysColumns.CAN_CERTIFY);
                projectionMap.put(KeysColumns.CAN_ENCRYPT, KeysColumns.CAN_ENCRYPT);
                projectionMap.put(KeysColumns.CAN_SIGN, KeysColumns.CAN_SIGN);
                projectionMap.put(KeysColumns.CREATION, KeysColumns.CREATION);
                projectionMap.put(KeysColumns.EXPIRY, KeysColumns.EXPIRY);
                projectionMap.put(KeysColumns.ALGORITHM, KeysColumns.ALGORITHM);
                projectionMap.put(KeysColumns.FINGERPRINT, KeysColumns.FINGERPRINT);
                projectionMap.put(UserIdsColumns.USER_ID, UserIdsColumns.USER_ID);
                projectionMap.put(Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID, Tables.KEY_RINGS_SECRET + "." + KeyRingsColumns.MASTER_KEY_ID);
                qb.setProjectionMap(projectionMap);

                qb.setTables(
                    Tables.KEYS
                        + " INNER JOIN " + Tables.USER_IDS + " ON ("
                                    + Tables.KEYS + "." + KeysColumns.MASTER_KEY_ID
                                + " = "
                                    + Tables.USER_IDS + "." + UserIdsColumns.MASTER_KEY_ID
                            + " AND " + Tables.USER_IDS + "." + UserIdsColumns.RANK + " = 0"
                        + ") LEFT JOIN " + Tables.KEY_RINGS_SECRET + " ON ("
                            + Tables.KEYS + "." + KeysColumns.MASTER_KEY_ID
                                + " = "
                            + Tables.KEY_RINGS_SECRET + "." + KeyRingsColumns.MASTER_KEY_ID
                        + ")"
                    );
                qb.appendWhere(Tables.KEYS + "." + KeysColumns.RANK + " = 0");

                if(match == KEY_RING_UNIFIED) {
                    qb.appendWhere(" AND " + Tables.KEYS + "." + KeysColumns.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                } else if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder =
                            Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID + " IS NULL DESC"
                            + Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
                }

                // uri to watch is all /key_rings/
                uri = KeyRings.CONTENT_URI;

                break;
            }
            /*case SECRET_KEY_RING_BY_EMAILS:
            case PUBLIC_KEY_RING_BY_EMAILS:
                qb = buildKeyRingQuery(qb, match);

                String emails = uri.getLastPathSegment();
                String chunks[] = emails.split(" *, *");
                boolean gotCondition = false;
                String emailWhere = "";
                for (int i = 0; i < chunks.length; ++i) {
                    if (chunks[i].length() == 0) {
                        continue;
                    }
                    if (i != 0) {
                        emailWhere += " OR ";
                    }
                    emailWhere += "tmp." + UserIdsColumns.USER_ID + " LIKE ";
                    // match '*<email>', so it has to be at the *end* of the user id
                    emailWhere += DatabaseUtils.sqlEscapeString("%<" + chunks[i] + ">");
                    gotCondition = true;
                }

                if (gotCondition) {
                    qb.appendWhere(" AND EXISTS (SELECT tmp." + BaseColumns._ID + " FROM "
                            + Tables.USER_IDS + " AS tmp WHERE tmp." + UserIdsColumns.KEY_RING_ROW_ID
                            + " = " + Tables.KEY_RINGS + "." + BaseColumns._ID + " AND (" + emailWhere
                            + "))");
                }*/

            case KEY_RING_KEYS: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(BaseColumns._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(KeysColumns.MASTER_KEY_ID, Tables.KEYS + "." + KeysColumns.MASTER_KEY_ID);
                projectionMap.put(KeysColumns.RANK, Tables.KEYS + "." + KeysColumns.RANK);
                projectionMap.put(KeysColumns.KEY_ID, KeysColumns.KEY_ID);
                projectionMap.put(KeysColumns.KEY_SIZE, KeysColumns.KEY_SIZE);
                projectionMap.put(KeysColumns.IS_REVOKED, KeysColumns.IS_REVOKED);
                projectionMap.put(KeysColumns.CAN_CERTIFY, KeysColumns.CAN_CERTIFY);
                projectionMap.put(KeysColumns.CAN_ENCRYPT, KeysColumns.CAN_ENCRYPT);
                projectionMap.put(KeysColumns.CAN_SIGN, KeysColumns.CAN_SIGN);
                projectionMap.put(KeysColumns.CREATION, KeysColumns.CREATION);
                projectionMap.put(KeysColumns.EXPIRY, KeysColumns.EXPIRY);
                projectionMap.put(KeysColumns.ALGORITHM, KeysColumns.ALGORITHM);
                projectionMap.put(KeysColumns.FINGERPRINT, KeysColumns.FINGERPRINT);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEYS);
                qb.appendWhere(KeysColumns.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            }

            case KEY_RING_USER_IDS: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(BaseColumns._ID, Tables.USER_IDS + ".oid AS _id");
                projectionMap.put(UserIds.MASTER_KEY_ID, UserIds.MASTER_KEY_ID);
                projectionMap.put(UserIds.USER_ID, UserIds.USER_ID);
                projectionMap.put(UserIds.RANK, UserIds.RANK);
                projectionMap.put(UserIds.IS_PRIMARY, UserIds.IS_PRIMARY);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.USER_IDS);
                qb.appendWhere(UserIdsColumns.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;

            }

            case KEY_RING_PUBLIC: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS_PUBLIC + ".oid AS _id");
                projectionMap.put(KeyRings.MASTER_KEY_ID, KeyRings.MASTER_KEY_ID);
                projectionMap.put(KeyRings.KEY_RING_DATA, KeyRings.KEY_RING_DATA);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEY_RINGS_PUBLIC);
                qb.appendWhere(KeyRingsColumns.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            }

            case KEY_RING_SECRET: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS_SECRET + ".oid AS _id");
                projectionMap.put(KeyRings.MASTER_KEY_ID, KeyRings.MASTER_KEY_ID);
                projectionMap.put(KeyRings.KEY_RING_DATA, KeyRings.KEY_RING_DATA);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEY_RINGS_SECRET);
                qb.appendWhere(KeyRingsColumns.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            }

            case API_APPS:
                qb.setTables(Tables.API_APPS);

                break;
            case API_APPS_BY_PACKAGE_NAME:
                qb.setTables(Tables.API_APPS);
                qb.appendWhere(ApiApps.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getLastPathSegment());

                break;
            case API_ACCOUNTS:
                qb.setTables(Tables.API_ACCOUNTS);
                qb.appendWhere(Tables.API_ACCOUNTS + "." + ApiAccounts.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            case API_ACCOUNTS_BY_ACCOUNT_NAME:
                qb.setTables(Tables.API_ACCOUNTS);
                qb.appendWhere(Tables.API_ACCOUNTS + "." + ApiAccounts.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                qb.appendWhere(" AND " + Tables.API_ACCOUNTS + "." + ApiAccounts.ACCOUNT_NAME + " = ");
                qb.appendWhereEscapeString(uri.getLastPathSegment());

                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);

        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        if (Constants.DEBUG) {
            Log.d(Constants.TAG,
                    "Query: "
                            + qb.buildQuery(projection, selection, selectionArgs, null, null,
                            orderBy, null));
            Log.d(Constants.TAG, "Cursor: " + DatabaseUtils.dumpCursorToString(c));
        }

        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(Constants.TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mKeychainDatabase.getWritableDatabase();

        Uri rowUri = null;
        Long keyId = null;
        try {
            final int match = mUriMatcher.match(uri);

            switch (match) {
                case KEY_RING_PUBLIC:
                    db.insertOrThrow(Tables.KEY_RINGS_PUBLIC, null, values);
                    keyId = values.getAsLong(KeyRingsColumns.MASTER_KEY_ID);
                    break;

                case KEY_RING_SECRET:
                    db.insertOrThrow(Tables.KEY_RINGS_SECRET, null, values);
                    keyId = values.getAsLong(KeyRingsColumns.MASTER_KEY_ID);
                    break;

                case KEY_RING_KEYS:
                    Log.d(Constants.TAG, "keys");
                    db.insertOrThrow(Tables.KEYS, null, values);
                    keyId = values.getAsLong(KeysColumns.MASTER_KEY_ID);
                    break;

                case KEY_RING_USER_IDS:
                    db.insertOrThrow(Tables.USER_IDS, null, values);
                    keyId = values.getAsLong(UserIdsColumns.MASTER_KEY_ID);
                    break;

                case API_APPS:
                    db.insertOrThrow(Tables.API_APPS, null, values);
                    break;

                case API_ACCOUNTS:
                    // set foreign key automatically based on given uri
                    // e.g., api_apps/com.example.app/accounts/
                    String packageName = uri.getPathSegments().get(1);
                    values.put(ApiAccounts.PACKAGE_NAME, packageName);

                    Log.d(Constants.TAG, "provider packageName: " + packageName);

                    db.insertOrThrow(Tables.API_ACCOUNTS, null, values);
                    // TODO: this is wrong:
//                    rowUri = ApiAccounts.buildIdUri(Long.toString(rowId));

                    break;

                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }

            if(keyId != null) {
                uri = KeyRings.buildGenericKeyRingUri(keyId.toString());
                rowUri = uri;
            }

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.e(Constants.TAG, "Constraint exception on insert! Entry already existing?", e);
        }

        return rowUri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Uri uri, String additionalSelection, String[] selectionArgs) {
        Log.v(Constants.TAG, "delete(uri=" + uri + ")");

        final SQLiteDatabase db = mKeychainDatabase.getWritableDatabase();

        int count;
        final int match = mUriMatcher.match(uri);

        switch (match) {
            case KEY_RING_PUBLIC: {
                @SuppressWarnings("ConstantConditions") // ensured by uriMatcher above
                String selection = KeyRings.MASTER_KEY_ID + " = " + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(additionalSelection)) {
                    selection += " AND (" + additionalSelection + ")";
                }
                // corresponding keys and userIds are deleted by ON DELETE CASCADE
                count = db.delete(Tables.KEY_RINGS_PUBLIC, selection, selectionArgs);
                uri = KeyRings.buildGenericKeyRingUri(uri.getPathSegments().get(1));
                break;
            }
            case KEY_RING_SECRET: {
                @SuppressWarnings("ConstantConditions") // ensured by uriMatcher above
                String selection  = KeyRings.MASTER_KEY_ID + " = " + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(additionalSelection)) {
                    selection += " AND (" + additionalSelection + ")";
                }
                count = db.delete(Tables.KEY_RINGS_SECRET, selection, selectionArgs);
                uri = KeyRings.buildGenericKeyRingUri(uri.getPathSegments().get(1));
                break;
            }

            case API_APPS_BY_PACKAGE_NAME:
                count = db.delete(Tables.API_APPS, buildDefaultApiAppsSelection(uri, additionalSelection),
                        selectionArgs);
                break;
            case API_ACCOUNTS_BY_ACCOUNT_NAME:
                count = db.delete(Tables.API_ACCOUNTS, buildDefaultApiAccountsSelection(uri, additionalSelection),
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // notify of changes in db
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.v(Constants.TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mKeychainDatabase.getWritableDatabase();

        String defaultSelection = null;
        int count = 0;
        try {
            final int match = mUriMatcher.match(uri);
            switch (match) {
                case API_APPS_BY_PACKAGE_NAME:
                    count = db.update(Tables.API_APPS, values,
                            buildDefaultApiAppsSelection(uri, selection), selectionArgs);
                    break;
                case API_ACCOUNTS_BY_ACCOUNT_NAME:
                    count = db.update(Tables.API_ACCOUNTS, values,
                            buildDefaultApiAccountsSelection(uri, selection), selectionArgs);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.e(Constants.TAG, "Constraint exception on update! Entry already existing?");
        }

        return count;
    }

    /**
     * Build default selection statement for API apps. If no extra selection is specified only build
     * where clause with rowId
     *
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultApiAppsSelection(Uri uri, String selection) {
        String packageName = DatabaseUtils.sqlEscapeString(uri.getLastPathSegment());

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return ApiApps.PACKAGE_NAME + "=" + packageName + andSelection;
    }

    private String buildDefaultApiAccountsSelection(Uri uri, String selection) {
        String packageName = DatabaseUtils.sqlEscapeString(uri.getPathSegments().get(1));
        String accountName = DatabaseUtils.sqlEscapeString(uri.getLastPathSegment());

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return ApiAccounts.PACKAGE_NAME + "=" + packageName + " AND "
                + ApiAccounts.ACCOUNT_NAME + "=" + accountName
                + andSelection;
    }

}
