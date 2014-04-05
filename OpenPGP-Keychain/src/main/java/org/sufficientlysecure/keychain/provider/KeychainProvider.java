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
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAccounts;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Arrays;
import java.util.HashMap;

public class KeychainProvider extends ContentProvider {

    private static final int KEY_RINGS_UNIFIED = 101;
    private static final int KEY_RINGS_PUBLIC = 102;
    private static final int KEY_RINGS_SECRET = 103;

    private static final int KEY_RING_UNIFIED = 200;
    private static final int KEY_RING_KEYS = 201;
    private static final int KEY_RING_USER_IDS = 202;
    private static final int KEY_RING_PUBLIC = 203;
    private static final int KEY_RING_SECRET = 204;
    private static final int KEY_RING_CERTS = 205;

    private static final int API_APPS = 301;
    private static final int API_APPS_BY_PACKAGE_NAME = 303;
    private static final int API_ACCOUNTS = 304;
    private static final int API_ACCOUNTS_BY_ACCOUNT_NAME = 306;

    private static final int KEY_RINGS_FIND_BY_EMAIL = 400;
    private static final int KEY_RINGS_FIND_BY_SUBKEY = 401;

    private static final int CERTS_FIND_BY_CERTIFIER_ID = 501;

    // private static final int DATA_STREAM = 501;

    protected UriMatcher mUriMatcher;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainContract.CONTENT_AUTHORITY;

        /**
         * list key_rings
         *
         * <pre>
         * key_rings/unified
         * key_rings/public
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                + "/" + KeychainContract.PATH_UNIFIED,
                KEY_RINGS_UNIFIED);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                + "/" + KeychainContract.PATH_PUBLIC,
                KEY_RINGS_PUBLIC);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                + "/" + KeychainContract.PATH_SECRET,
                KEY_RINGS_SECRET);

        /**
         * find by criteria other than master key id
         *
         * key_rings/find/email/_
         * key_rings/find/subkey/_
         *
         */
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/"
                + KeychainContract.PATH_FIND + "/" + KeychainContract.PATH_BY_EMAIL + "/*",
                KEY_RINGS_FIND_BY_EMAIL);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/"
                + KeychainContract.PATH_FIND + "/" + KeychainContract.PATH_BY_SUBKEY + "/*",
                KEY_RINGS_FIND_BY_SUBKEY);

        /**
         * list key_ring specifics
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
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_CERTS,
                KEY_RING_CERTS);

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
        return true;
    }

    public KeychainDatabase getDb() {
        if(mKeychainDatabase == null)
            mKeychainDatabase = new KeychainDatabase(getContext());
        return mKeychainDatabase;
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

        int match = mUriMatcher.match(uri);

        // all query() parameters, for good measure
        String groupBy = null, having = null;

        switch (match) {
            case KEY_RING_UNIFIED:
            case KEY_RINGS_UNIFIED:
            case KEY_RINGS_FIND_BY_EMAIL:
            case KEY_RINGS_FIND_BY_SUBKEY: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(KeyRings._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(KeyRings.MASTER_KEY_ID, Tables.KEYS + "." + Keys.MASTER_KEY_ID);
                projectionMap.put(KeyRings.KEY_ID, Keys.KEY_ID);
                projectionMap.put(KeyRings.KEY_SIZE, Keys.KEY_SIZE);
                projectionMap.put(KeyRings.IS_REVOKED, Keys.IS_REVOKED);
                projectionMap.put(KeyRings.CAN_CERTIFY, Keys.CAN_CERTIFY);
                projectionMap.put(KeyRings.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
                projectionMap.put(KeyRings.CAN_SIGN, Keys.CAN_SIGN);
                projectionMap.put(KeyRings.CREATION, Keys.CREATION);
                projectionMap.put(KeyRings.EXPIRY, Keys.EXPIRY);
                projectionMap.put(KeyRings.ALGORITHM, Keys.ALGORITHM);
                projectionMap.put(KeyRings.FINGERPRINT, Keys.FINGERPRINT);
                projectionMap.put(KeyRings.USER_ID, UserIds.USER_ID);
                projectionMap.put(KeyRings.HAS_SECRET, "(" + Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID + " IS NOT NULL) AS " + KeyRings.HAS_SECRET);
                qb.setProjectionMap(projectionMap);

                qb.setTables(
                    Tables.KEYS
                        + " INNER JOIN " + Tables.USER_IDS + " ON ("
                                    + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                                    + Tables.USER_IDS + "." + UserIds.MASTER_KEY_ID
                            + " AND " + Tables.USER_IDS + "." + UserIds.RANK + " = 0"
                        + ") LEFT JOIN " + Tables.KEY_RINGS_SECRET + " ON ("
                            + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                            + Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID
                        + ")"
                    );
                qb.appendWhere(Tables.KEYS + "." + Keys.RANK + " = 0");

                switch(match) {
                    case KEY_RING_UNIFIED: {
                        qb.appendWhere(" AND " + Tables.KEYS + "." + Keys.MASTER_KEY_ID + " = ");
                        qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                        break;
                    }
                    case KEY_RINGS_FIND_BY_SUBKEY: {
                        try {
                            String subkey = Long.valueOf(uri.getLastPathSegment()).toString();
                            qb.appendWhere(" AND EXISTS ("
                                    + " SELECT 1 FROM " + Tables.KEYS + " AS tmp"
                                    + " WHERE tmp." + UserIds.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                    + " AND tmp." + Keys.KEY_ID + " = " + subkey + ""
                                    + ")");
                        } catch(NumberFormatException e) {
                            Log.e(Constants.TAG, "Malformed find by subkey query!", e);
                            qb.appendWhere(" AND 0");
                        }
                        break;
                    }
                    case KEY_RINGS_FIND_BY_EMAIL: {
                        String chunks[] = uri.getLastPathSegment().split(" *, *");
                        boolean gotCondition = false;
                        String emailWhere = "";
                        // JAVA ♥
                        for (int i = 0; i < chunks.length; ++i) {
                            if (chunks[i].length() == 0) {
                                continue;
                            }
                            if (i != 0) {
                                emailWhere += " OR ";
                            }
                            emailWhere += "tmp." + UserIds.USER_ID + " LIKE ";
                            // match '*<email>', so it has to be at the *end* of the user id
                            emailWhere += DatabaseUtils.sqlEscapeString("%<" + chunks[i] + ">");
                            gotCondition = true;
                        }
                        if(gotCondition) {
                            qb.appendWhere(" AND EXISTS ("
                                + " SELECT 1 FROM " + Tables.USER_IDS + " AS tmp"
                                    + " WHERE tmp." + UserIds.MASTER_KEY_ID
                                            + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                        + " AND (" + emailWhere + ")"
                                + ")");
                        } else {
                            // TODO better way to do this?
                            Log.e(Constants.TAG, "Malformed find by email query!");
                            qb.appendWhere(" AND 0");
                        }
                        break;
                    }
                }

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder =
                            Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID + " IS NULL ASC, "
                                    + Tables.USER_IDS + "." + UserIds.USER_ID + " ASC";
                }

                // uri to watch is all /key_rings/
                uri = KeyRings.CONTENT_URI;

                break;
            }

            case KEY_RING_KEYS: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(Keys._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(Keys.MASTER_KEY_ID, Tables.KEYS + "." + Keys.MASTER_KEY_ID);
                projectionMap.put(Keys.RANK, Tables.KEYS + "." + Keys.RANK);
                projectionMap.put(Keys.KEY_ID, Keys.KEY_ID);
                projectionMap.put(Keys.KEY_SIZE, Keys.KEY_SIZE);
                projectionMap.put(Keys.IS_REVOKED, Keys.IS_REVOKED);
                projectionMap.put(Keys.CAN_CERTIFY, Keys.CAN_CERTIFY);
                projectionMap.put(Keys.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
                projectionMap.put(Keys.CAN_SIGN, Keys.CAN_SIGN);
                projectionMap.put(Keys.CREATION, Keys.CREATION);
                projectionMap.put(Keys.EXPIRY, Keys.EXPIRY);
                projectionMap.put(Keys.ALGORITHM, Keys.ALGORITHM);
                projectionMap.put(Keys.FINGERPRINT, Keys.FINGERPRINT);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEYS);
                qb.appendWhere(Keys.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            }

            case KEY_RING_USER_IDS: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(UserIds._ID, Tables.USER_IDS + ".oid AS _id");
                projectionMap.put(UserIds.MASTER_KEY_ID, Tables.USER_IDS + "." + UserIds.MASTER_KEY_ID);
                projectionMap.put(UserIds.USER_ID, Tables.USER_IDS + "." + UserIds.USER_ID);
                projectionMap.put(UserIds.RANK, Tables.USER_IDS + "." + UserIds.RANK);
                projectionMap.put(UserIds.IS_PRIMARY, Tables.USER_IDS + "." + UserIds.IS_PRIMARY);
                // we take the minimum (>0) here, where "1" is "verified by known secret key"
                projectionMap.put(UserIds.VERIFIED, "MIN(" + Certs.VERIFIED + ") AS " + UserIds.VERIFIED);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.USER_IDS
                        + " LEFT JOIN " + Tables.CERTS + " ON ("
                            + Tables.USER_IDS + "." + UserIds.MASTER_KEY_ID + " = "
                                + Tables.CERTS + "." + Certs.MASTER_KEY_ID
                            + " AND " + Tables.USER_IDS + "." + UserIds.RANK + " = "
                                + Tables.CERTS + "." + Certs.RANK
                            + " AND " + Tables.CERTS + "." + Certs.VERIFIED + " > 0"
                        + ")");
                groupBy = Tables.USER_IDS + "." + UserIds.RANK;

                qb.appendWhere(Tables.USER_IDS + "." + UserIds.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Tables.USER_IDS + "." + UserIds.RANK + " ASC";
                }

                break;

            }

            case KEY_RINGS_PUBLIC:
            case KEY_RING_PUBLIC: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(KeyRingData._ID, Tables.KEY_RINGS_PUBLIC + ".oid AS _id");
                projectionMap.put(KeyRingData.MASTER_KEY_ID, KeyRingData.MASTER_KEY_ID);
                projectionMap.put(KeyRingData.KEY_RING_DATA, KeyRingData.KEY_RING_DATA);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEY_RINGS_PUBLIC);

                if(match == KEY_RING_PUBLIC) {
                    qb.appendWhere(KeyRings.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                }

                break;
            }

            case KEY_RINGS_SECRET:
            case KEY_RING_SECRET: {
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(KeyRingData._ID, Tables.KEY_RINGS_SECRET + ".oid AS _id");
                projectionMap.put(KeyRingData.MASTER_KEY_ID, KeyRingData.MASTER_KEY_ID);
                projectionMap.put(KeyRingData.KEY_RING_DATA, KeyRingData.KEY_RING_DATA);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEY_RINGS_SECRET);

                if(match == KEY_RING_SECRET) {
                    qb.appendWhere(KeyRings.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                }

                break;
            }

            case KEY_RING_CERTS:
                HashMap<String, String> projectionMap = new HashMap<String, String>();
                projectionMap.put(Certs._ID, Tables.CERTS + ".oid AS " + Certs._ID);
                projectionMap.put(Certs.MASTER_KEY_ID, Tables.CERTS + "." + Certs.MASTER_KEY_ID);
                projectionMap.put(Certs.RANK, Tables.CERTS + "." + Certs.RANK);
                projectionMap.put(Certs.CREATION, Tables.CERTS + "." + Certs.CREATION);
                projectionMap.put(Certs.KEY_ID_CERTIFIER, Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER);
                projectionMap.put(Certs.VERIFIED, Tables.CERTS + "." + Certs.VERIFIED);
                projectionMap.put(Certs.KEY_DATA, Tables.CERTS + "." + Certs.KEY_DATA);
                projectionMap.put(Certs.USER_ID, Tables.USER_IDS + "." + UserIds.USER_ID);
                projectionMap.put(Certs.SIGNER_UID, "signer." + UserIds.USER_ID + " AS " + Certs.SIGNER_UID);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.CERTS
                    + " JOIN " + Tables.USER_IDS + " ON ("
                            + Tables.CERTS + "." + Certs.MASTER_KEY_ID  + " = "
                            + Tables.USER_IDS + "." + UserIds.MASTER_KEY_ID
                        + " AND "
                            + Tables.CERTS + "." + Certs.RANK + " = "
                            + Tables.USER_IDS + "." + UserIds.RANK
                    + ") LEFT JOIN " + Tables.USER_IDS + " AS signer ON ("
                            + Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER + " = "
                            + "signer." + UserIds.MASTER_KEY_ID
                        + " AND "
                            + "signer." + Keys.RANK + " = 0"
                    + ")");

                groupBy = Tables.CERTS + "." + Certs.RANK + ", "
                        + Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER;

                qb.appendWhere(Tables.CERTS + "." + KeyRings.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;

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
                throw new IllegalArgumentException("Unknown URI " + uri + " (" + match + ")");

        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = getDb().getReadableDatabase();
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

        final SQLiteDatabase db = getDb().getWritableDatabase();

        Uri rowUri = null;
        Long keyId = null;
        try {
            final int match = mUriMatcher.match(uri);

            switch (match) {
                case KEY_RING_PUBLIC:
                    db.insertOrThrow(Tables.KEY_RINGS_PUBLIC, null, values);
                    keyId = values.getAsLong(KeyRings.MASTER_KEY_ID);
                    break;

                case KEY_RING_SECRET:
                    db.insertOrThrow(Tables.KEY_RINGS_SECRET, null, values);
                    keyId = values.getAsLong(KeyRings.MASTER_KEY_ID);
                    break;

                case KEY_RING_KEYS:
                    Log.d(Constants.TAG, "keys");
                    db.insertOrThrow(Tables.KEYS, null, values);
                    keyId = values.getAsLong(Keys.MASTER_KEY_ID);
                    break;

                case KEY_RING_USER_IDS:
                    db.insertOrThrow(Tables.USER_IDS, null, values);
                    keyId = values.getAsLong(UserIds.MASTER_KEY_ID);
                    break;

                case KEY_RING_CERTS:
                    // we replace here, keeping only the latest signature
                    // TODO this would be better handled in saveKeyRing directly!
                    db.replaceOrThrow(Tables.CERTS, null, values);
                    keyId = values.getAsLong(Certs.MASTER_KEY_ID);
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

        final SQLiteDatabase db = getDb().getWritableDatabase();

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
