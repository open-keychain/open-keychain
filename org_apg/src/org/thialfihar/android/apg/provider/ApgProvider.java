/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.provider.ApgContract.KeyRings;
import org.thialfihar.android.apg.provider.ApgContract.KeyRingsColumns;
import org.thialfihar.android.apg.provider.ApgContract.KeyTypes;
import org.thialfihar.android.apg.provider.ApgContract.KeysColumns;
import org.thialfihar.android.apg.provider.ApgContract.UserIds;
import org.thialfihar.android.apg.provider.ApgContract.Keys;
import org.thialfihar.android.apg.provider.ApgContract.UserIdsColumns;
import org.thialfihar.android.apg.provider.ApgDatabase.Tables;
import org.thialfihar.android.apg.util.Log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class ApgProvider extends ContentProvider {
    private static final int PUBLIC_KEY_RING = 101;
    private static final int PUBLIC_KEY_RING_BY_ROW_ID = 102;
    private static final int PUBLIC_KEY_RING_BY_MASTER_KEY_ID = 103;
    private static final int PUBLIC_KEY_RING_BY_KEY_ID = 104;
    private static final int PUBLIC_KEY_RING_BY_EMAILS = 105;

    private static final int PUBLIC_KEY_RING_KEY = 111;
    private static final int PUBLIC_KEY_RING_KEY_BY_ROW_ID = 112;

    private static final int PUBLIC_KEY_RING_USER_ID = 121;
    private static final int PUBLIC_KEY_RING_USER_ID_BY_ROW_ID = 122;

    private static final int SECRET_KEY_RING = 201;
    private static final int SECRET_KEY_RING_BY_ROW_ID = 202;
    private static final int SECRET_KEY_RING_BY_MASTER_KEY_ID = 203;
    private static final int SECRET_KEY_RING_BY_KEY_ID = 204;
    private static final int SECRET_KEY_RING_BY_EMAILS = 205;

    private static final int SECRET_KEY_RING_KEY = 211;
    private static final int SECRET_KEY_RING_KEY_BY_ROW_ID = 212;

    private static final int SECRET_KEY_RING_USER_ID = 221;
    private static final int SECRET_KEY_RING_USER_ID_BY_ROW_ID = 222;

    private static final int DATA_STREAM = 301;

    protected static boolean sInternalProvider;
    protected static UriMatcher sUriMatcher;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected static UriMatcher buildUriMatcher(boolean internalProvider) {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority;
        if (internalProvider) {
            authority = ApgContract.CONTENT_AUTHORITY_INTERNAL;
        } else {
            authority = ApgContract.CONTENT_AUTHORITY_EXTERNAL;
        }

        /**
         * public key rings
         * 
         * <pre>
         * key_rings/public
         * key_rings/public/#
         * key_rings/public/master_key_id/_
         * key_rings/public/key_id/_
         * key_rings/public/emails/_
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC,
                PUBLIC_KEY_RING);
        matcher.addURI(authority,
                ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/#",
                PUBLIC_KEY_RING_BY_ROW_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/"
                + ApgContract.PATH_BY_MASTER_KEY_ID + "/*", PUBLIC_KEY_RING_BY_MASTER_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/"
                + ApgContract.PATH_BY_KEY_ID + "/*", PUBLIC_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/"
                + ApgContract.PATH_BY_EMAILS + "/*", PUBLIC_KEY_RING_BY_EMAILS);

        /**
         * public keys
         * 
         * <pre>
         * key_rings/public/#/keys
         * key_rings/public/#/keys/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/#/" + ApgContract.PATH_KEYS, PUBLIC_KEY_RING_KEY);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/#/" + ApgContract.PATH_KEYS + "/#", PUBLIC_KEY_RING_KEY_BY_ROW_ID);

        /**
         * public user ids
         * 
         * <pre>
         * key_rings/public/#/user_ids
         * key_rings/public/#/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/#/" + ApgContract.PATH_USER_IDS, PUBLIC_KEY_RING_USER_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/#/" + ApgContract.PATH_USER_IDS + "/#", PUBLIC_KEY_RING_USER_ID_BY_ROW_ID);

        /**
         * secret key rings
         * 
         * <pre>
         * key_rings/secret
         * key_rings/secret/#
         * key_rings/secret/master_key_id/_
         * key_rings/secret/key_id/_
         * key_rings/secret/emails/_
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET,
                SECRET_KEY_RING);
        matcher.addURI(authority,
                ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/#",
                SECRET_KEY_RING_BY_ROW_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/"
                + ApgContract.PATH_BY_MASTER_KEY_ID + "/*", SECRET_KEY_RING_BY_MASTER_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/"
                + ApgContract.PATH_BY_KEY_ID + "/*", SECRET_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/"
                + ApgContract.PATH_BY_EMAILS + "/*", SECRET_KEY_RING_BY_EMAILS);

        /**
         * secret keys
         * 
         * <pre>
         * key_rings/secret/#/keys
         * key_rings/secret/#/keys/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/#/" + ApgContract.PATH_KEYS, SECRET_KEY_RING_KEY);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/#/" + ApgContract.PATH_KEYS + "/#", SECRET_KEY_RING_KEY_BY_ROW_ID);

        /**
         * secret user ids
         * 
         * <pre>
         * key_rings/secret/#/user_ids
         * key_rings/secret/#/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/#/" + ApgContract.PATH_USER_IDS, SECRET_KEY_RING_USER_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/#/" + ApgContract.PATH_USER_IDS + "/#", SECRET_KEY_RING_USER_ID_BY_ROW_ID);

        /**
         * data stream
         * 
         * <pre>
         * data / _
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_DATA + "/*", DATA_STREAM);

        return matcher;
    }

    private ApgDatabase mApgDatabase;

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        mApgDatabase = new ApgDatabase(getContext());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_BY_EMAILS:
        case SECRET_KEY_RING:
        case SECRET_KEY_RING_BY_EMAILS:
            return KeyRings.CONTENT_TYPE;

        case PUBLIC_KEY_RING_BY_ROW_ID:
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
            return KeyRings.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY:
            return Keys.CONTENT_TYPE;

        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            return Keys.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID:
            return UserIds.CONTENT_TYPE;

        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            return UserIds.CONTENT_ITEM_TYPE;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Returns type of the query (secret/public)
     * 
     * @param uri
     * @return
     */
    private int getKeyType(int match) {
        int type;
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_BY_ROW_ID:
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_KEY:
        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case PUBLIC_KEY_RING_USER_ID:
        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
            type = KeyTypes.PUBLIC;
            break;

        case SECRET_KEY_RING:
        case SECRET_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
        case SECRET_KEY_RING_BY_EMAILS:
        case SECRET_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            type = KeyTypes.SECRET;
            break;

        default:
            throw new IllegalArgumentException("Unknown match " + match);

        }

        return type;
    }

    /**
     * Set result of query to specific columns, don't show blob column for external content provider
     * 
     * @return
     */
    private HashMap<String, String> getProjectionMapForKeyRings() {
        HashMap<String, String> projectionMap = new HashMap<String, String>();

        projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS + "." + BaseColumns._ID);
        projectionMap.put(KeyRingsColumns.MASTER_KEY_ID, Tables.KEY_RINGS + "."
                + KeyRingsColumns.MASTER_KEY_ID);
        // only give out keyRing blob when we are using the internal content provider
        if (sInternalProvider) {
            projectionMap.put(KeyRingsColumns.KEY_RING_DATA, Tables.KEY_RINGS + "."
                    + KeyRingsColumns.KEY_RING_DATA);
        }
        projectionMap.put(UserIdsColumns.USER_ID, Tables.USER_IDS + "." + UserIdsColumns.USER_ID);

        return projectionMap;
    }

    /**
     * Set result of query to specific columns, don't show blob column for external content provider
     * 
     * @return
     */
    private HashMap<String, String> getProjectionMapForKeys() {
        HashMap<String, String> projectionMap = new HashMap<String, String>();

        projectionMap.put(BaseColumns._ID, BaseColumns._ID);
        projectionMap.put(KeysColumns.KEY_ID, KeysColumns.KEY_ID);
        projectionMap.put(KeysColumns.IS_MASTER_KEY, KeysColumns.IS_MASTER_KEY);
        projectionMap.put(KeysColumns.ALGORITHM, KeysColumns.ALGORITHM);
        projectionMap.put(KeysColumns.KEY_SIZE, KeysColumns.KEY_SIZE);
        projectionMap.put(KeysColumns.CAN_SIGN, KeysColumns.CAN_SIGN);
        projectionMap.put(KeysColumns.CAN_ENCRYPT, KeysColumns.CAN_ENCRYPT);
        projectionMap.put(KeysColumns.IS_REVOKED, KeysColumns.IS_REVOKED);
        projectionMap.put(KeysColumns.CREATION, KeysColumns.CREATION);
        projectionMap.put(KeysColumns.EXPIRY, KeysColumns.EXPIRY);
        projectionMap.put(KeysColumns.KEY_RING_ROW_ID, KeysColumns.KEY_RING_ROW_ID);
        // only give out keyRing blob when we are using the internal content provider
        if (sInternalProvider) {
            projectionMap.put(KeysColumns.KEY_DATA, KeysColumns.KEY_DATA);
        }
        projectionMap.put(KeysColumns.RANK, KeysColumns.RANK);

        return projectionMap;
    }

    /**
     * Builds default query for keyRings: KeyRings table is joined with Keys and UserIds
     * 
     * @param qb
     * @param match
     * @param isMasterKey
     * @param sortOrder
     * @return
     */
    private SQLiteQueryBuilder buildKeyRingQuery(SQLiteQueryBuilder qb, int match,
            boolean isMasterKey, String sortOrder) {
        qb.appendWhere(Tables.KEY_RINGS + "." + KeyRingsColumns.TYPE + " = ");
        qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

        String isMasterKeyQuery = "";
        if (isMasterKey) {
            isMasterKeyQuery = " AND " + Tables.KEYS + "." + KeysColumns.IS_MASTER_KEY + " = '1'";
        }

        qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.KEYS + " ON " + "("
                + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                + KeysColumns.KEY_RING_ROW_ID + isMasterKeyQuery + ") " + " INNER JOIN "
                + Tables.USER_IDS + " ON " + "(" + Tables.KEY_RINGS + "." + BaseColumns._ID + " = "
                + Tables.USER_IDS + "." + UserIdsColumns.KEY_RING_ROW_ID + " AND "
                + Tables.USER_IDS + "." + UserIdsColumns.RANK + " = '0')");

        qb.setProjectionMap(getProjectionMapForKeyRings());

        return qb;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v(Constants.TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mApgDatabase.getReadableDatabase();

        int match = sUriMatcher.match(uri);

        switch (match) {
        case PUBLIC_KEY_RING:
        case SECRET_KEY_RING:
            qb = buildKeyRingQuery(qb, match, true, sortOrder);

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case PUBLIC_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
            qb = buildKeyRingQuery(qb, match, true, sortOrder);

            qb.appendWhere(" AND " + Tables.KEY_RINGS + "." + BaseColumns._ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
            qb = buildKeyRingQuery(qb, match, true, sortOrder);

            qb.appendWhere(" AND " + Tables.KEY_RINGS + "." + KeyRingsColumns.MASTER_KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case SECRET_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
            qb = buildKeyRingQuery(qb, match, false, sortOrder);

            qb.appendWhere(" AND " + Tables.KEYS + "." + KeysColumns.KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case SECRET_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_BY_EMAILS:
            qb.appendWhere(Tables.KEY_RINGS + "." + KeyRingsColumns.TYPE + " = ");
            qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

            qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.KEYS + " ON " + "("
                    + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                    + KeysColumns.KEY_RING_ROW_ID + " AND " + Tables.KEYS + "."
                    + KeysColumns.IS_MASTER_KEY + " = '1'" + ") " + " INNER JOIN "
                    + Tables.USER_IDS + " ON " + "(" + Tables.KEYS + "." + BaseColumns._ID + " = "
                    + Tables.USER_IDS + "." + UserIdsColumns.KEY_RING_ROW_ID + " AND "
                    + Tables.USER_IDS + "." + UserIdsColumns.RANK + " = '0')");

            qb.setProjectionMap(getProjectionMapForKeyRings());

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
                        + " = " + Tables.KEYS + "." + BaseColumns._ID + " AND (" + emailWhere
                        + "))");
            }

            break;

        case PUBLIC_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY:
            qb.setTables(Tables.KEYS);
            qb.appendWhere(KeysColumns.TYPE + " = ");
            qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

            qb.appendWhere(" AND " + KeysColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.setProjectionMap(getProjectionMapForKeys());

            break;

        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            qb.setTables(Tables.KEYS);
            qb.appendWhere(KeysColumns.TYPE + " = ");
            qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

            qb.appendWhere(" AND " + KeysColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.appendWhere(" AND " + BaseColumns._ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            qb.setProjectionMap(getProjectionMapForKeys());

            break;

        case PUBLIC_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID:
            qb.setTables(Tables.USER_IDS);
            qb.appendWhere(UserIdsColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            break;

        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            qb.setTables(Tables.USER_IDS);
            qb.appendWhere(UserIdsColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.appendWhere(" AND " + BaseColumns._ID + " = ");
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

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

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

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(Constants.TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        Uri rowUri = null;
        long rowId = -1;
        try {
            final int match = sUriMatcher.match(uri);

            switch (match) {
            case PUBLIC_KEY_RING:
                values.put(KeyRings.TYPE, KeyTypes.PUBLIC);

                rowId = db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = KeyRings.buildPublicKeyRingsUri(Long.toString(rowId));

                break;
            case PUBLIC_KEY_RING_KEY:
                values.put(Keys.TYPE, KeyTypes.PUBLIC);

                rowId = db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = Keys.buildPublicKeysUri(Long.toString(rowId));

                break;
            case PUBLIC_KEY_RING_USER_ID:
                rowId = db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = UserIds.buildPublicUserIdsUri(Long.toString(rowId));

                break;
            case SECRET_KEY_RING:
                values.put(KeyRings.TYPE, KeyTypes.SECRET);

                rowId = db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = KeyRings.buildSecretKeyRingsUri(Long.toString(rowId));

                break;
            case SECRET_KEY_RING_KEY:
                values.put(Keys.TYPE, KeyTypes.SECRET);

                rowId = db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = Keys.buildSecretKeysUri(Long.toString(rowId));

                break;
            case SECRET_KEY_RING_USER_ID:
                rowId = db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = UserIds.buildSecretUserIdsUri(Long.toString(rowId));

                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(Constants.TAG, "Constraint exception on insert! Entry already existing?");
        }

        // notify of changes in db
        getContext().getContentResolver().notifyChange(uri, null);

        return rowUri;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v(Constants.TAG, "delete(uri=" + uri + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        int count;
        final int match = sUriMatcher.match(uri);

        String defaultSelection = null;
        switch (match) {
        case PUBLIC_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
            defaultSelection = BaseColumns._ID + "=" + uri.getLastPathSegment();
            // corresponding keys and userIds are deleted by ON DELETE CASCADE
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match), selection),
                    selectionArgs);
            break;
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
            defaultSelection = KeyRings.MASTER_KEY_ID + "=" + uri.getLastPathSegment();
            // corresponding keys and userIds are deleted by ON DELETE CASCADE
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match), selection),
                    selectionArgs);
            break;
        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            count = db.delete(Tables.KEYS,
                    buildDefaultKeysSelection(uri, getKeyType(match), selection), selectionArgs);
            break;
        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            count = db.delete(Tables.KEYS, buildDefaultUserIdsSelection(uri, selection),
                    selectionArgs);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // notify of changes in db
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.v(Constants.TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        String defaultSelection = null;
        int count = 0;
        try {
            final int match = sUriMatcher.match(uri);
            switch (match) {
            case PUBLIC_KEY_RING_BY_ROW_ID:
            case SECRET_KEY_RING_BY_ROW_ID:
                defaultSelection = BaseColumns._ID + "=" + uri.getLastPathSegment();

                count = db.update(
                        Tables.KEY_RINGS,
                        values,
                        buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match),
                                selection), selectionArgs);
                break;
            case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
            case SECRET_KEY_RING_BY_MASTER_KEY_ID:
                defaultSelection = KeyRings.MASTER_KEY_ID + "=" + uri.getLastPathSegment();

                count = db.update(
                        Tables.KEY_RINGS,
                        values,
                        buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match),
                                selection), selectionArgs);
                break;
            case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
            case SECRET_KEY_RING_KEY_BY_ROW_ID:
                count = db
                        .update(Tables.KEYS, values,
                                buildDefaultKeysSelection(uri, getKeyType(match), selection),
                                selectionArgs);
                break;
            case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
            case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
                count = db.update(Tables.USER_IDS, values,
                        buildDefaultUserIdsSelection(uri, selection), selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(Constants.TAG, "Constraint exception on update! Entry already existing?");
        }

        // notify of changes in db
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /**
     * Build default selection statement for KeyRings. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultKeyRingsSelection(String defaultSelection, Integer keyType,
            String selection) {
        String andType = "";
        if (keyType != null) {
            andType = " AND " + KeyRingsColumns.TYPE + "=" + keyType;
        }

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return defaultSelection + andType + andSelection;
    }

    /**
     * Build default selection statement for Keys. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultKeysSelection(Uri uri, Integer keyType, String selection) {
        String rowId = uri.getLastPathSegment();

        String foreignKeyRingRowId = uri.getPathSegments().get(2);
        String andForeignKeyRing = " AND " + KeysColumns.KEY_RING_ROW_ID + " = "
                + foreignKeyRingRowId;

        String andType = "";
        if (keyType != null) {
            andType = " AND " + KeysColumns.TYPE + "=" + keyType;
        }

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return BaseColumns._ID + "=" + rowId + andForeignKeyRing + andType + andSelection;
    }

    /**
     * Build default selection statement for UserIds. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultUserIdsSelection(Uri uri, String selection) {
        String rowId = uri.getLastPathSegment();

        String foreignKeyRingRowId = uri.getPathSegments().get(2);
        String andForeignKeyRing = " AND " + KeysColumns.KEY_RING_ROW_ID + " = "
                + foreignKeyRingRowId;

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return BaseColumns._ID + "=" + rowId + andForeignKeyRing + andSelection;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int match = sUriMatcher.match(uri);
        if (match != DATA_STREAM) {
            throw new FileNotFoundException();
        }
        String fileName = uri.getLastPathSegment();
        File file = new File(getContext().getFilesDir().getAbsolutePath(), fileName);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
