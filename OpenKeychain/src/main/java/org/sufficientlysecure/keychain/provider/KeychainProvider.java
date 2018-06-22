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


import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.model.AutocryptPeer;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeySignatures;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPacketsColumns;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.util.DatabaseUtil;
import timber.log.Timber;

import static android.database.DatabaseUtils.dumpCursorToString;


public class KeychainProvider extends ContentProvider implements SimpleContentResolverInterface {

    private static final int KEY_RINGS_UNIFIED = 101;

    private static final int KEY_RING_UNIFIED = 200;
    private static final int KEY_RING_KEYS = 201;
    private static final int KEY_RING_USER_IDS = 202;
    private static final int KEY_RING_PUBLIC = 203;
    private static final int KEY_RING_CERTS = 205;

    private static final int KEY_RINGS_FIND_BY_EMAIL = 400;
    private static final int KEY_RINGS_FIND_BY_SUBKEY = 401;
    private static final int KEY_RINGS_FIND_BY_USER_ID = 402;

    private static final int KEY_SIGNATURES = 700;

    protected UriMatcher mUriMatcher;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainContract.CONTENT_AUTHORITY;

        /*
         * list key_rings
         *
         * <pre>
         * key_rings/unified
         * key_rings/user_ids
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                        + "/" + KeychainContract.PATH_UNIFIED,
                KEY_RINGS_UNIFIED);

        /*
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
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/"
                        + KeychainContract.PATH_FIND + "/" + KeychainContract.PATH_BY_USER_ID + "/*",
                KEY_RINGS_FIND_BY_USER_ID);

        /*
         * list key_ring specifics
         *
         * <pre>
         * key_rings/_/unified
         * key_rings/_/keys
         * key_rings/_/user_ids
         * key_rings/_/linked_ids
         * key_rings/_/public
         * key_rings/_/certs
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
                + KeychainContract.PATH_CERTS,
                KEY_RING_CERTS);

        matcher.addURI(authority, KeychainContract.BASE_KEY_SIGNATURES, KEY_SIGNATURES);


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
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Timber.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = mUriMatcher.match(uri);

        // all query() parameters, for good measure
        String groupBy;

        switch (match) {
            case KEY_RING_UNIFIED:
            case KEY_RINGS_UNIFIED:
            case KEY_RINGS_FIND_BY_EMAIL:
            case KEY_RINGS_FIND_BY_SUBKEY:
            case KEY_RINGS_FIND_BY_USER_ID: {
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(KeyRings._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(KeyRings.MASTER_KEY_ID, Tables.KEYS + "." + Keys.MASTER_KEY_ID);
                projectionMap.put(KeyRings.KEY_ID, Tables.KEYS + "." + Keys.KEY_ID);
                projectionMap.put(KeyRings.KEY_SIZE, Tables.KEYS + "." + Keys.KEY_SIZE);
                projectionMap.put(KeyRings.KEY_CURVE_OID, Tables.KEYS + "." + Keys.KEY_CURVE_OID);
                projectionMap.put(KeyRings.IS_REVOKED, Tables.KEYS + "." + Keys.IS_REVOKED);
                projectionMap.put(KeyRings.IS_SECURE, Tables.KEYS + "." + Keys.IS_SECURE);
                projectionMap.put(KeyRings.CAN_CERTIFY, Tables.KEYS + "." + Keys.CAN_CERTIFY);
                projectionMap.put(KeyRings.CAN_ENCRYPT, Tables.KEYS + "." + Keys.CAN_ENCRYPT);
                projectionMap.put(KeyRings.CAN_SIGN, Tables.KEYS + "." + Keys.CAN_SIGN);
                projectionMap.put(KeyRings.CAN_AUTHENTICATE, Tables.KEYS + "." + Keys.CAN_AUTHENTICATE);
                projectionMap.put(KeyRings.CREATION, Tables.KEYS + "." + Keys.CREATION);
                projectionMap.put(KeyRings.EXPIRY, Tables.KEYS + "." + Keys.EXPIRY);
                projectionMap.put(KeyRings.ALGORITHM, Tables.KEYS + "." + Keys.ALGORITHM);
                projectionMap.put(KeyRings.FINGERPRINT, Tables.KEYS + "." + Keys.FINGERPRINT);
                projectionMap.put(KeyRings.USER_ID, Tables.USER_PACKETS + "." + UserPackets.USER_ID);
                projectionMap.put(KeyRings.NAME, Tables.USER_PACKETS + "." + UserPackets.NAME);
                projectionMap.put(KeyRings.EMAIL, Tables.USER_PACKETS + "." + UserPackets.EMAIL);
                projectionMap.put(KeyRings.COMMENT, Tables.USER_PACKETS + "." + UserPackets.COMMENT);
                projectionMap.put(KeyRings.HAS_DUPLICATE_USER_ID,
                            "(EXISTS (SELECT * FROM " + Tables.USER_PACKETS + " AS dups"
                                + " WHERE dups." + UserPackets.MASTER_KEY_ID
                                    + " != " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND dups." + UserPackets.RANK + " = 0"
                                + " AND dups." + UserPackets.NAME
                                    + " = " + Tables.USER_PACKETS + "." + UserPackets.NAME + " COLLATE NOCASE"
                                + " AND dups." + UserPackets.EMAIL
                                    + " = " + Tables.USER_PACKETS + "." + UserPackets.EMAIL + " COLLATE NOCASE"
                                + ")) AS " + KeyRings.HAS_DUPLICATE_USER_ID);
                projectionMap.put(KeyRings.VERIFIED, Tables.CERTS + "." + Certs.VERIFIED);
                projectionMap.put(KeyRings.HAS_SECRET, Tables.KEYS + "." + KeyRings.HAS_SECRET);
                projectionMap.put(KeyRings.HAS_ANY_SECRET,
                        "(EXISTS (SELECT * FROM " + Tables.KEYS + " AS k WHERE "
                                + "k." + Keys.HAS_SECRET + " != 0"
                                + " AND k." + Keys.MASTER_KEY_ID + " = "
                                + Tables.KEYS + "." + KeyRingData.MASTER_KEY_ID
                                + ")) AS " + KeyRings.HAS_ANY_SECRET);
                projectionMap.put(KeyRings.HAS_ENCRYPT,
                        "kE." + Keys.KEY_ID + " AS " + KeyRings.HAS_ENCRYPT);
                projectionMap.put(KeyRings.HAS_SIGN_SECRET,
                        "kS." + Keys.KEY_ID + " AS " + KeyRings.HAS_SIGN_SECRET);
                projectionMap.put(KeyRings.HAS_AUTHENTICATE,
                        "kA." + Keys.KEY_ID + " AS " + KeyRings.HAS_AUTHENTICATE);
                projectionMap.put(KeyRings.HAS_AUTHENTICATE_SECRET,
                        "kA." + Keys.KEY_ID + " AS " + KeyRings.HAS_AUTHENTICATE_SECRET);
                projectionMap.put(KeyRings.HAS_CERTIFY_SECRET,
                        "kC." + Keys.KEY_ID + " AS " + KeyRings.HAS_CERTIFY_SECRET);
                projectionMap.put(KeyRings.IS_EXPIRED,
                        "(" + Tables.KEYS + "." + Keys.EXPIRY + " IS NOT NULL AND " + Tables.KEYS + "." + Keys.EXPIRY
                                + " < " + new Date().getTime() / 1000 + ") AS " + KeyRings.IS_EXPIRED);
                projectionMap.put(KeyRings.API_KNOWN_TO_PACKAGE_NAMES,
                        "GROUP_CONCAT(DISTINCT aTI." + AutocryptPeer.PACKAGE_NAME + ") AS "
                        + KeyRings.API_KNOWN_TO_PACKAGE_NAMES);
                qb.setProjectionMap(projectionMap);

                if (projection == null) {
                    throw new IllegalArgumentException("Please provide a projection!");
                }

                // Need this as list so we can search in it
                List<String> plist = Arrays.asList(projection);

                qb.setTables(
                    Tables.KEYS
                        + " INNER JOIN " + Tables.USER_PACKETS + " ON ("
                                    + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                                    + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID
                            // we KNOW that the rank zero user packet is a user id!
                            + " AND " + Tables.USER_PACKETS + "." + UserPackets.RANK + " = 0"
                        + ") LEFT JOIN " + Tables.CERTS + " ON ("
                            + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                            + Tables.CERTS + "." + KeyRings.MASTER_KEY_ID
                            + " AND " + Tables.CERTS + "." + Certs.VERIFIED
                                + " = " + Certs.VERIFIED_SECRET
                        + ")"
                        // fairly expensive joins following, only do when requested
                        + (plist.contains(KeyRings.HAS_ENCRYPT) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kE ON ("
                                +"kE." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kE." + Keys.IS_REVOKED + " = 0"
                                + " AND kE." + Keys.IS_SECURE + " = 1"
                                + " AND kE." + Keys.CAN_ENCRYPT + " = 1"
                                + " AND ( kE." + Keys.EXPIRY + " IS NULL OR kE." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                            + ")" : "")
                        + (plist.contains(KeyRings.HAS_SIGN_SECRET) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kS ON ("
                                +"kS." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kS." + Keys.IS_REVOKED + " = 0"
                                + " AND kS." + Keys.IS_SECURE + " = 1"
                                + " AND kS." + Keys.CAN_SIGN + " = 1"
                                + " AND kS." + Keys.HAS_SECRET + " > 1"
                                + " AND ( kS." + Keys.EXPIRY + " IS NULL OR kS." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                            + ")" : "")
                        + (plist.contains(KeyRings.HAS_AUTHENTICATE) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kA ON ("
                                    +"kA." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                    + " AND kA." + Keys.IS_REVOKED + " = 0"
                                    + " AND kA." + Keys.IS_SECURE + " = 1"
                                    + " AND kA." + Keys.CAN_AUTHENTICATE + " = 1"
                                    + " AND ( kA." + Keys.EXPIRY + " IS NULL OR kA." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                                    + ")" : "")
                        + (plist.contains(KeyRings.HAS_AUTHENTICATE_SECRET) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kA ON ("
                                    +"kA." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                    + " AND kA." + Keys.IS_REVOKED + " = 0"
                                    + " AND kA." + Keys.IS_SECURE + " = 1"
                                    + " AND kA." + Keys.CAN_AUTHENTICATE + " = 1"
                                    + " AND kA." + Keys.HAS_SECRET + " > 1"
                                    + " AND ( kA." + Keys.EXPIRY + " IS NULL OR kA." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                                    + ")" : "")
                        + (plist.contains(KeyRings.HAS_CERTIFY_SECRET) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kC ON ("
                                +"kC." + Keys.MASTER_KEY_ID
                                + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kC." + Keys.IS_REVOKED + " = 0"
                                + " AND kC." + Keys.IS_SECURE + " = 1"
                                + " AND kC." + Keys.CAN_CERTIFY + " = 1"
                                + " AND kC." + Keys.HAS_SECRET + " > 1"
                                + " AND ( kC." + Keys.EXPIRY + " IS NULL OR kC." + Keys.EXPIRY
                                + " >= " + new Date().getTime() / 1000 + " )"
                                + ")" : "")
                        + (plist.contains(KeyRings.API_KNOWN_TO_PACKAGE_NAMES) ?
                            " LEFT JOIN " + AutocryptPeer.TABLE_NAME + " AS aTI ON ("
                                    +"aTI." + AutocryptPeer.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                    + ")" : "")
                    );
                qb.appendWhere(Tables.KEYS + "." + Keys.RANK + " = 0");
                // in case there are multiple verifying certificates
                groupBy = Tables.KEYS + "." + Keys.MASTER_KEY_ID;

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
                                    + " WHERE tmp." + UserPackets.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                    + " AND tmp." + Keys.KEY_ID + " = " + subkey + ""
                                    + ")");
                        } catch(NumberFormatException e) {
                            Timber.e(e, "Malformed find by subkey query!");
                            qb.appendWhere(" AND 0");
                        }
                        break;
                    }
                    case KEY_RINGS_FIND_BY_EMAIL:
                    case KEY_RINGS_FIND_BY_USER_ID: {
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
                            if (match == KEY_RINGS_FIND_BY_EMAIL) {
                                emailWhere += "tmp." + UserPackets.EMAIL + " LIKE "
                                        + DatabaseUtils.sqlEscapeString(chunks[i]);
                            } else {
                                emailWhere += "tmp." + UserPackets.USER_ID + " LIKE "
                                        + DatabaseUtils.sqlEscapeString("%" + chunks[i] + "%");
                            }
                            gotCondition = true;
                        }
                        if(gotCondition) {
                            qb.appendWhere(" AND EXISTS ("
                                + " SELECT 1 FROM " + Tables.USER_PACKETS + " AS tmp"
                                    + " WHERE tmp." + UserPackets.MASTER_KEY_ID
                                            + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                        + " AND (" + emailWhere + ")"
                                + ")");
                        } else {
                            // TODO better way to do this?
                            Timber.e("Malformed find by email query!");
                            qb.appendWhere(" AND 0");
                        }
                        break;
                    }
                }

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Tables.USER_PACKETS + "." + UserPackets.USER_ID + " ASC";
                }

                // uri to watch is all /key_rings/
                uri = KeyRings.CONTENT_URI;

                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri + " (" + match + ")");
            }

        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        SupportSQLiteDatabase db = getDb().getReadableDatabase();

        String query = qb.buildQuery(projection, selection, groupBy, null, orderBy, null);
        Cursor cursor = db.query(query, selectionArgs);
        if (cursor != null) {
            // Tell the cursor what uri to watch, so it knows when its source data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        Timber.d("Query: " + qb.buildQuery(projection, selection, null, null, orderBy, null));

        if (Constants.DEBUG && Constants.DEBUG_LOG_DB_QUERIES) {
            Timber.d("Cursor: " + dumpCursorToString(cursor));
        }

        if (Constants.DEBUG && Constants.DEBUG_EXPLAIN_QUERIES) {
            String rawQuery = qb.buildQuery(projection, selection, groupBy, null, orderBy, null);
            DatabaseUtil.explainQuery(db, rawQuery);
        }

        return cursor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Timber.d("insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SupportSQLiteDatabase db = getDb().getWritableDatabase();

        Uri rowUri = null;
        Long keyId = null;
        try {
            final int match = mUriMatcher.match(uri);

            switch (match) {
                case KEY_RING_PUBLIC: {
                    db.insert(Tables.KEY_RINGS_PUBLIC, SQLiteDatabase.CONFLICT_FAIL, values);
                    keyId = values.getAsLong(KeyRings.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_KEYS: {
                    db.insert(Tables.KEYS, SQLiteDatabase.CONFLICT_FAIL, values);
                    keyId = values.getAsLong(Keys.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_USER_IDS: {
                    // iff TYPE is null, user_id MUST be null as well
                    if (!(values.get(UserPacketsColumns.TYPE) == null
                            ? (values.get(UserPacketsColumns.USER_ID) != null && values.get(UserPacketsColumns.ATTRIBUTE_DATA) == null)
                            : (values.get(UserPacketsColumns.ATTRIBUTE_DATA) != null && values.get(UserPacketsColumns.USER_ID) == null)
                    )) {
                        throw new AssertionError("Incorrect type for user packet! This is a bug!");
                    }
                    if (((Number) values.get(UserPacketsColumns.RANK)).intValue() == 0 && values.get(UserPacketsColumns.USER_ID) == null) {
                        throw new AssertionError("Rank 0 user packet must be a user id!");
                    }
                    db.insert(Tables.USER_PACKETS, SQLiteDatabase.CONFLICT_FAIL, values);
                    keyId = values.getAsLong(UserPackets.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_CERTS: {
                    // we replace here, keeping only the latest signature
                    // TODO this would be better handled in savePublicKeyRing directly!
                    db.insert(Tables.CERTS, SQLiteDatabase.CONFLICT_FAIL, values);
                    keyId = values.getAsLong(Certs.MASTER_KEY_ID);
                    break;
                }
                case KEY_SIGNATURES: {
                    db.insert(Tables.KEY_SIGNATURES, SQLiteDatabase.CONFLICT_FAIL, values);
                    rowUri = KeySignatures.CONTENT_URI;
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
                }
            }

            if (keyId != null) {
                uri = KeyRings.buildGenericKeyRingUri(keyId);
                rowUri = uri;
            }

        } catch (SQLiteConstraintException e) {
            Timber.d(e, "Constraint exception on insert! Entry already existing?");
        }

        return rowUri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(@NonNull Uri uri, String additionalSelection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Timber.v("update(uri=" + uri + ", values=" + values.toString() + ")");

        final SupportSQLiteDatabase db = getDb().getWritableDatabase();

        int count = 0;
        try {
            final int match = mUriMatcher.match(uri);
            switch (match) {
                case KEY_RING_KEYS: {
                    if (values.size() != 1 || !values.containsKey(Keys.HAS_SECRET)) {
                        throw new UnsupportedOperationException(
                                "Only has_secret column may be updated!");
                    }
                    // make sure we get a long value here
                    Long mkid = Long.parseLong(uri.getPathSegments().get(1));
                    String actualSelection = Keys.MASTER_KEY_ID + " = " + Long.toString(mkid);
                    if (!TextUtils.isEmpty(selection)) {
                        actualSelection += " AND (" + selection + ")";
                    }
                    count = db.update(Tables.KEYS, SQLiteDatabase.CONFLICT_FAIL, values, actualSelection, selectionArgs);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
                }
            }
        } catch (SQLiteConstraintException e) {
            Timber.d(e, "Constraint exception on update! Entry already existing?");
        }

        return count;
    }
}
