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
import org.thialfihar.android.apg.provider.ApgContract.KeyRingsColumns;
import org.thialfihar.android.apg.provider.ApgContract.KeyTypes;
import org.thialfihar.android.apg.provider.ApgContract.KeysColumns;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.PublicUserIds;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeys;
import org.thialfihar.android.apg.provider.ApgContract.SecretUserIds;
import org.thialfihar.android.apg.provider.ApgContract.UserIdsColumns;
import org.thialfihar.android.apg.provider.ApgDatabase.Tables;
import org.thialfihar.android.apg.util.Log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
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
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int PUBLIC_KEY_RING = 101;
    private static final int PUBLIC_KEY_RING_ID = 102;
    private static final int PUBLIC_KEY_RING_BY_KEY_ID = 103;
    private static final int PUBLIC_KEY_RING_BY_EMAILS = 104;
    private static final int PUBLIC_KEY_RING_KEY = 111;
    private static final int PUBLIC_KEY_RING_KEY_RANK = 112;
    private static final int PUBLIC_KEY_RING_USER_ID = 121;
    private static final int PUBLIC_KEY_RING_USER_ID_RANK = 122;

    private static final int SECRET_KEY_RING = 201;
    private static final int SECRET_KEY_RING_ID = 202;
    private static final int SECRET_KEY_RING_BY_KEY_ID = 203;
    private static final int SECRET_KEY_RING_BY_EMAILS = 204;
    private static final int SECRET_KEY_RING_KEY = 211;
    private static final int SECRET_KEY_RING_KEY_RANK = 212;
    private static final int SECRET_KEY_RING_USER_ID = 221;
    private static final int SECRET_KEY_RING_USER_ID_RANK = 222;

    private static final int DATA_STREAM = 301;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ApgContract.CONTENT_AUTHORITY;

        /**
         * public key rings
         * 
         * <pre>
         * key_rings/public
         * key_rings/public/_
         * key_rings/public/key_id/_
         * key_rings/public/emails/_
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC,
                PUBLIC_KEY_RING);
        matcher.addURI(authority,
                ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/*",
                PUBLIC_KEY_RING_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/"
                + ApgContract.PATH_BY_KEY_ID + "/*", PUBLIC_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC + "/"
                + ApgContract.PATH_BY_EMAILS + "/*", PUBLIC_KEY_RING_BY_EMAILS);

        /**
         * public keys
         * 
         * <pre>
         * key_rings/public/_/keys
         * key_rings/public/_/keys/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/*/" + ApgContract.PATH_KEYS, PUBLIC_KEY_RING_KEY);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/*/" + ApgContract.PATH_KEYS + "/" + ApgContract.PATH_RANK,
                PUBLIC_KEY_RING_KEY_RANK);

        /**
         * public user ids
         * 
         * <pre>
         * key_rings/public/_/user_ids
         * key_rings/public/_/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/*/" + ApgContract.PATH_USER_IDS, PUBLIC_KEY_RING_USER_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_PUBLIC
                + "/*/" + ApgContract.PATH_USER_IDS + "/" + ApgContract.PATH_RANK,
                PUBLIC_KEY_RING_USER_ID_RANK);

        /**
         * secret key rings
         * 
         * <pre>
         * key_rings/secret
         * key_rings/secret/*
         * key_rings/secret/key_id/*
         * key_rings/secret/emails/*
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET,
                SECRET_KEY_RING);
        matcher.addURI(authority,
                ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/*",
                SECRET_KEY_RING_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/"
                + ApgContract.PATH_BY_KEY_ID + "/*", SECRET_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET + "/"
                + ApgContract.PATH_BY_EMAILS + "/*", SECRET_KEY_RING_BY_EMAILS);

        /**
         * secret keys
         * 
         * <pre>
         * key_rings/secret/_/keys
         * key_rings/secret/_/keys/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/*/" + ApgContract.PATH_KEYS, SECRET_KEY_RING_KEY);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/*/" + ApgContract.PATH_KEYS + "/" + ApgContract.PATH_RANK,
                SECRET_KEY_RING_KEY_RANK);

        /**
         * secret user ids
         * 
         * <pre>
         * key_rings/secret/_/user_ids
         * key_rings/secret/_/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/*/" + ApgContract.PATH_USER_IDS, SECRET_KEY_RING_USER_ID);
        matcher.addURI(authority, ApgContract.BASE_KEY_RINGS + "/" + ApgContract.PATH_SECRET
                + "/*/" + ApgContract.PATH_USER_IDS + "/" + ApgContract.PATH_RANK,
                SECRET_KEY_RING_USER_ID_RANK);

        /**
         * data stream
         * 
         * <pre>
         * data/*
         * </pre>
         */
        matcher.addURI(authority, ApgContract.BASE_DATA + "/*", DATA_STREAM);

        return matcher;
    }

    private ApgDatabase mApgDatabase;

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mApgDatabase = new ApgDatabase(context);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_BY_EMAILS:
            return PublicKeyRings.CONTENT_TYPE;

        case PUBLIC_KEY_RING_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
            return PublicKeyRings.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_KEY:
            return PublicKeys.CONTENT_TYPE;

        case PUBLIC_KEY_RING_KEY_RANK:
            return PublicKeys.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_USER_ID:
            return PublicUserIds.CONTENT_TYPE;

        case PUBLIC_KEY_RING_USER_ID_RANK:
            return PublicUserIds.CONTENT_ITEM_TYPE;

        case SECRET_KEY_RING:
        case SECRET_KEY_RING_BY_EMAILS:
            return SecretKeyRings.CONTENT_TYPE;

        case SECRET_KEY_RING_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
            return SecretKeyRings.CONTENT_ITEM_TYPE;

        case SECRET_KEY_RING_KEY:
            return SecretKeys.CONTENT_TYPE;

        case SECRET_KEY_RING_KEY_RANK:
            return SecretKeys.CONTENT_ITEM_TYPE;

        case SECRET_KEY_RING_USER_ID:
            return SecretUserIds.CONTENT_TYPE;

        case SECRET_KEY_RING_USER_ID_RANK:
            return SecretUserIds.CONTENT_ITEM_TYPE;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Returns weather the key is a public or secret one
     * 
     * @param uri
     * @return
     */
    private int getKeyType(int match) {
        int type;
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_KEY:
        case PUBLIC_KEY_RING_KEY_RANK:
        case PUBLIC_KEY_RING_USER_ID:
        case PUBLIC_KEY_RING_USER_ID_RANK:
            type = KeyTypes.PUBLIC;
            break;

        case SECRET_KEY_RING:
        case SECRET_KEY_RING_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
        case SECRET_KEY_RING_BY_EMAILS:
        case SECRET_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY_RANK:
        case SECRET_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID_RANK:
            type = KeyTypes.SECRET;
            break;

        default:
            throw new IllegalArgumentException("Unknown match " + match);

        }

        return type;
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v(Constants.TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mApgDatabase.getReadableDatabase();

        HashMap<String, String> projectionMap = new HashMap<String, String>();

        int match = sUriMatcher.match(uri);

        qb.appendWhere(Tables.KEY_RINGS + "." + KeyRingsColumns.TYPE + " = " + getKeyType(match));

        switch (match) {
        case PUBLIC_KEY_RING_ID:
        case SECRET_KEY_RING_ID:
            qb.appendWhere(" AND " + Tables.KEY_RINGS + "." + KeyRingsColumns.MASTER_KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            // break omitted intentionally

        case PUBLIC_KEY_RING:
        case SECRET_KEY_RING:
            qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.KEYS + " ON " + "("
                    + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                    + KeysColumns.KEY_RING_ID + " AND " + Tables.KEYS + "."
                    + KeysColumns.IS_MASTER_KEY + " = '1'" + ") " + " INNER JOIN "
                    + Tables.USER_IDS + " ON " + "(" + Tables.KEYS + "." + BaseColumns._ID + " = "
                    + Tables.USER_IDS + "." + UserIdsColumns.KEY_ID + " AND " + Tables.USER_IDS
                    + "." + UserIdsColumns.RANK + " = '0') ");

            projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS + "." + BaseColumns._ID);
            projectionMap.put(KeyRingsColumns.MASTER_KEY_ID, Tables.KEY_RINGS + "."
                    + KeyRingsColumns.MASTER_KEY_ID);
            projectionMap.put(UserIdsColumns.USER_ID, Tables.USER_IDS + "."
                    + UserIdsColumns.USER_ID);

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case SECRET_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
            qb.setTables(Tables.KEYS + " AS tmp INNER JOIN " + Tables.KEY_RINGS + " ON ("
                    + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + "tmp."
                    + KeysColumns.KEY_RING_ID + ")" + " INNER JOIN " + Tables.KEYS + " ON " + "("
                    + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                    + KeysColumns.KEY_RING_ID + " AND " + Tables.KEYS + "."
                    + KeysColumns.IS_MASTER_KEY + " = '1'" + ") " + " INNER JOIN "
                    + Tables.USER_IDS + " ON " + "(" + Tables.KEYS + "." + BaseColumns._ID + " = "
                    + Tables.USER_IDS + "." + UserIdsColumns.KEY_ID + " AND " + Tables.USER_IDS
                    + "." + UserIdsColumns.RANK + " = '0') ");

            projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS + "." + BaseColumns._ID);
            projectionMap.put(KeyRingsColumns.MASTER_KEY_ID, Tables.KEY_RINGS + "."
                    + KeyRingsColumns.MASTER_KEY_ID);
            projectionMap.put(UserIdsColumns.USER_ID, Tables.USER_IDS + "."
                    + UserIdsColumns.USER_ID);

            qb.appendWhere(" AND tmp." + KeysColumns.KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(3));

            break;

        case SECRET_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_BY_EMAILS:
            qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.KEYS + " ON " + "("
                    + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                    + KeysColumns.KEY_RING_ID + " AND " + Tables.KEYS + "."
                    + KeysColumns.IS_MASTER_KEY + " = '1'" + ") " + " INNER JOIN "
                    + Tables.USER_IDS + " ON " + "(" + Tables.KEYS + "." + BaseColumns._ID + " = "
                    + Tables.USER_IDS + "." + UserIdsColumns.KEY_ID + " AND " + Tables.USER_IDS
                    + "." + UserIdsColumns.RANK + " = '0') ");

            projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS + "." + BaseColumns._ID);
            projectionMap.put(KeyRingsColumns.MASTER_KEY_ID, Tables.KEY_RINGS + "."
                    + KeyRingsColumns.MASTER_KEY_ID);
            projectionMap.put(UserIdsColumns.USER_ID, Tables.USER_IDS + "."
                    + UserIdsColumns.USER_ID);

            String emails = uri.getPathSegments().get(3);
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
                        + Tables.USER_IDS + " AS tmp WHERE tmp." + UserIdsColumns.KEY_ID + " = "
                        + Tables.KEYS + "." + BaseColumns._ID + " AND (" + emailWhere + "))");
            }

            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);

        }

        qb.setProjectionMap(projectionMap);

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        Log.d(Constants.TAG,
                qb.buildQuery(projection, selection, selectionArgs, null, null, orderBy, null)
                        .replace("WHERE", "WHERE\n"));
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(Constants.TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        Uri rowUri = null;
        try {
            final int match = sUriMatcher.match(uri);

            switch (match) {
            case PUBLIC_KEY_RING:
                values.put(PublicKeyRings.TYPE, KeyTypes.PUBLIC);

                db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = PublicKeyRings.buildPublicKeyRingsUri(values
                        .getAsString(PublicKeyRings._ID));

                break;
            case PUBLIC_KEY_RING_KEY:
                values.put(PublicKeys.TYPE, KeyTypes.PUBLIC);

                db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = PublicKeys.buildPublicKeysUri(values.getAsString(PublicKeys._ID));

                break;
            case PUBLIC_KEY_RING_USER_ID:
                db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = PublicUserIds.buildPublicUserIdsUri(values.getAsString(PublicUserIds._ID));

                break;
            case SECRET_KEY_RING:
                values.put(SecretKeyRings.TYPE, KeyTypes.SECRET);

                db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = SecretKeyRings.buildSecretKeyRingsUri(values
                        .getAsString(SecretKeyRings._ID));

                break;
            case SECRET_KEY_RING_KEY:
                values.put(SecretKeys.TYPE, KeyTypes.SECRET);

                db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = SecretKeys.buildSecretKeysUri(values.getAsString(SecretKeys._ID));

                break;
            case SECRET_KEY_RING_USER_ID:
                db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = SecretUserIds.buildSecretUserIdsUri(values.getAsString(SecretUserIds._ID));

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

        switch (match) {
        case PUBLIC_KEY_RING_ID:
            // delete corresponding keys and userids
            // db.delete(Tables.KEYS, whereClause, whereArgs)
            // TODO
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultSelection(uri, KeyTypes.PUBLIC, selection), selectionArgs);
            break;
        case SECRET_KEY_RING_ID:
            // delete corresponding keys and userids
            // TODO
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultSelection(uri, KeyTypes.SECRET, selection), selectionArgs);
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

        int count = 0;
        try {
            final int match = sUriMatcher.match(uri);
            switch (match) {
            case PUBLIC_KEY_RING_ID:
                count = db.update(Tables.KEY_RINGS, values,
                        buildDefaultSelection(uri, KeyTypes.PUBLIC, selection), selectionArgs);
                break;
            case SECRET_KEY_RING_ID:
                count = db.update(Tables.KEY_RINGS, values,
                        buildDefaultSelection(uri, KeyTypes.PUBLIC, selection), selectionArgs);
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
     * Build default selection statement. If no extra selection is specified only build where clause
     * with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultSelection(Uri uri, Integer keyType, String selection) {
        String rowId = uri.getPathSegments().get(1);
        String andWhere = "";
        if (!TextUtils.isEmpty(selection)) {
            andWhere = " AND (" + selection + ")";
        }

        String andType = "";
        if (keyType != null) {
            andType = " AND " + KeyRingsColumns.TYPE + "=" + keyType;
        }

        return BaseColumns._ID + "=" + rowId + andType + andWhere;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int match = sUriMatcher.match(uri);
        if (match != DATA_STREAM) {
            throw new FileNotFoundException();
        }
        String fileName = uri.getPathSegments().get(1);
        File file = new File(getContext().getFilesDir().getAbsolutePath(), fileName);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
