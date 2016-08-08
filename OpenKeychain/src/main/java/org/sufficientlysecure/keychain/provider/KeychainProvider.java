/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAccounts;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAllowedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPacketsColumns;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class KeychainProvider extends ContentProvider {

    private static final int KEY_RINGS_UNIFIED = 101;
    private static final int KEY_RINGS_PUBLIC = 102;
    private static final int KEY_RINGS_SECRET = 103;
    private static final int KEY_RINGS_USER_IDS = 104;

    private static final int KEY_RING_UNIFIED = 200;
    private static final int KEY_RING_KEYS = 201;
    private static final int KEY_RING_USER_IDS = 202;
    private static final int KEY_RING_PUBLIC = 203;
    private static final int KEY_RING_SECRET = 204;
    private static final int KEY_RING_CERTS = 205;
    private static final int KEY_RING_CERTS_SPECIFIC = 206;
    private static final int KEY_RING_LINKED_IDS = 207;
    private static final int KEY_RING_LINKED_ID_CERTS = 208;

    private static final int API_APPS = 301;
    private static final int API_APPS_BY_PACKAGE_NAME = 302;
    private static final int API_ACCOUNTS = 303;
    private static final int API_ACCOUNTS_BY_ACCOUNT_NAME = 304;
    private static final int API_ALLOWED_KEYS = 305;

    private static final int KEY_RINGS_FIND_BY_EMAIL = 400;
    private static final int KEY_RINGS_FIND_BY_SUBKEY = 401;
    private static final int KEY_RINGS_FIND_BY_USER_ID = 402;

    private static final int UPDATED_KEYS = 500;
    private static final int UPDATED_KEYS_SPECIFIC = 501;

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
         * key_rings/secret
         * key_rings/user_ids
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
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS
                        + "/" + KeychainContract.PATH_USER_IDS,
                KEY_RINGS_USER_IDS);

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
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/"
                        + KeychainContract.PATH_FIND + "/" + KeychainContract.PATH_BY_USER_ID + "/*",
                KEY_RINGS_FIND_BY_USER_ID);

        /**
         * list key_ring specifics
         *
         * <pre>
         * key_rings/_/unified
         * key_rings/_/keys
         * key_rings/_/user_ids
         * key_rings/_/linked_ids
         * key_rings/_/linked_ids/_
         * key_rings/_/linked_ids/_/certs
         * key_rings/_/public
         * key_rings/_/secret
         * key_rings/_/certs
         * key_rings/_/certs/_/_
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
                        + KeychainContract.PATH_LINKED_IDS,
                KEY_RING_LINKED_IDS);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                        + KeychainContract.PATH_LINKED_IDS + "/*/"
                        + KeychainContract.PATH_CERTS,
                KEY_RING_LINKED_ID_CERTS);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_PUBLIC,
                KEY_RING_PUBLIC);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_SECRET,
                KEY_RING_SECRET);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                + KeychainContract.PATH_CERTS,
                KEY_RING_CERTS);
        matcher.addURI(authority, KeychainContract.BASE_KEY_RINGS + "/*/"
                        + KeychainContract.PATH_CERTS + "/*/*",
                KEY_RING_CERTS_SPECIFIC);

        /**
         * API apps
         *
         * <pre>
         * api_apps
         * api_apps/_ (package name)
         *
         * api_apps/_/accounts
         * api_apps/_/accounts/_ (account name)
         *
         * api_apps/_/allowed_keys
         * </pre>
         */
        matcher.addURI(authority, KeychainContract.BASE_API_APPS, API_APPS);
        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*", API_APPS_BY_PACKAGE_NAME);

        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*/"
                + KeychainContract.PATH_ACCOUNTS, API_ACCOUNTS);
        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*/"
                + KeychainContract.PATH_ACCOUNTS + "/*", API_ACCOUNTS_BY_ACCOUNT_NAME);

        matcher.addURI(authority, KeychainContract.BASE_API_APPS + "/*/"
                + KeychainContract.PATH_ALLOWED_KEYS, API_ALLOWED_KEYS);

        /**
         * to access table containing last updated dates of keys
         */
        matcher.addURI(authority, KeychainContract.BASE_UPDATED_KEYS, UPDATED_KEYS);
        matcher.addURI(authority, KeychainContract.BASE_UPDATED_KEYS + "/*", UPDATED_KEYS_SPECIFIC);

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
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case KEY_RING_PUBLIC:
                return KeyRings.CONTENT_ITEM_TYPE;

            case KEY_RING_KEYS:
                return Keys.CONTENT_TYPE;

            case KEY_RING_USER_IDS:
                return UserPackets.CONTENT_TYPE;

            case KEY_RING_SECRET:
                return KeyRings.CONTENT_ITEM_TYPE;

            case UPDATED_KEYS:
                return UpdatedKeys.CONTENT_TYPE;
            case UPDATED_KEYS_SPECIFIC:
                return UpdatedKeys.CONTENT_ITEM_TYPE;

            case API_APPS:
                return ApiApps.CONTENT_TYPE;

            case API_APPS_BY_PACKAGE_NAME:
                return ApiApps.CONTENT_ITEM_TYPE;

            case API_ACCOUNTS:
                return ApiAccounts.CONTENT_TYPE;

            case API_ACCOUNTS_BY_ACCOUNT_NAME:
                return ApiAccounts.CONTENT_ITEM_TYPE;

            case API_ALLOWED_KEYS:
                return ApiAllowedKeys.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
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
            case KEY_RINGS_FIND_BY_SUBKEY:
            case KEY_RINGS_FIND_BY_USER_ID: {
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(KeyRings._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(KeyRings.MASTER_KEY_ID, Tables.KEYS + "." + Keys.MASTER_KEY_ID);
                projectionMap.put(KeyRings.KEY_ID, Tables.KEYS + "." + Keys.KEY_ID);
                projectionMap.put(KeyRings.KEY_SIZE, Tables.KEYS + "." + Keys.KEY_SIZE);
                projectionMap.put(KeyRings.KEY_CURVE_OID, Tables.KEYS + "." + Keys.KEY_CURVE_OID);
                projectionMap.put(KeyRings.IS_REVOKED, Tables.KEYS + "." + Keys.IS_REVOKED);
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
                projectionMap.put(KeyRings.PUBKEY_DATA,
                        Tables.KEY_RINGS_PUBLIC + "." + KeyRingData.KEY_RING_DATA
                                + " AS " + KeyRings.PUBKEY_DATA);
                projectionMap.put(KeyRings.PRIVKEY_DATA,
                        Tables.KEY_RINGS_SECRET + "." + KeyRingData.KEY_RING_DATA
                                + " AS " + KeyRings.PRIVKEY_DATA);
                projectionMap.put(KeyRings.SECRET_RING_TYPE,
                        Tables.KEY_RINGS_SECRET + "." + KeyRingData.SECRET_RING_TYPE
                                + " AS " + KeyRings.SECRET_RING_TYPE);
                projectionMap.put(KeyRings.AWAITING_MERGE,
                        Tables.KEY_RINGS_SECRET + "." + KeyRingData.AWAITING_MERGE
                                + " AS " + KeyRings.AWAITING_MERGE);
                projectionMap.put(KeyRings.HAS_SECRET, Tables.KEYS + "." + KeyRings.HAS_SECRET);
                projectionMap.put(KeyRings.HAS_ANY_SECRET,
                        "(" + Tables.KEY_RINGS_SECRET + "." + KeyRings.MASTER_KEY_ID + " IS NOT NULL)" +
                                " AS " + KeyRings.HAS_ANY_SECRET);
                projectionMap.put(KeyRings.HAS_ENCRYPT,
                        "kE." + Keys.KEY_ID + " AS " + KeyRings.HAS_ENCRYPT);
                projectionMap.put(KeyRings.HAS_SIGN,
                        "kS." + Keys.KEY_ID + " AS " + KeyRings.HAS_SIGN);
                projectionMap.put(KeyRings.HAS_CERTIFY,
                        "kC." + Keys.KEY_ID + " AS " + KeyRings.HAS_CERTIFY);
                projectionMap.put(KeyRings.IS_EXPIRED,
                        "(" + Tables.KEYS + "." + Keys.EXPIRY + " IS NOT NULL AND " + Tables.KEYS + "." + Keys.EXPIRY
                                + " < " + new Date().getTime() / 1000 + ") AS " + KeyRings.IS_EXPIRED);
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
                        + (plist.contains(KeyRings.PUBKEY_DATA) ?
                            " INNER JOIN " + Tables.KEY_RINGS_PUBLIC + " ON ("
                                    + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                                    + Tables.KEY_RINGS_PUBLIC + "." + KeyRingData.MASTER_KEY_ID
                                + ")" : "")
                        + (plist.contains(KeyRings.PRIVKEY_DATA)
                            || plist.contains(KeyRings.HAS_ANY_SECRET)
                            || plist.contains(KeyRings.AWAITING_MERGE)
                            || plist.contains(KeyRings.SECRET_RING_TYPE) ?
                            " LEFT JOIN " + Tables.KEY_RINGS_SECRET + " ON ("
                                    + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " = "
                                    + Tables.KEY_RINGS_SECRET + "." + KeyRingData.MASTER_KEY_ID
                                + ")" : "")
                        + (plist.contains(KeyRings.HAS_ENCRYPT) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kE ON ("
                                +"kE." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kE." + Keys.IS_REVOKED + " = 0"
                                + " AND kE." + Keys.CAN_ENCRYPT + " = 1"
                                + " AND ( kE." + Keys.EXPIRY + " IS NULL OR kE." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                            + ")" : "")
                        + (plist.contains(KeyRings.HAS_SIGN) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kS ON ("
                                +"kS." + Keys.MASTER_KEY_ID
                                    + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kS." + Keys.IS_REVOKED + " = 0"
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
                                    + " AND kA." + Keys.CAN_AUTHENTICATE + " = 1"
                                    + " AND kA." + Keys.HAS_SECRET + " > 1"
                                    + " AND ( kA." + Keys.EXPIRY + " IS NULL OR kA." + Keys.EXPIRY
                                    + " >= " + new Date().getTime() / 1000 + " )"
                                    + ")" : "")
                        + (plist.contains(KeyRings.HAS_CERTIFY) ?
                            " LEFT JOIN " + Tables.KEYS + " AS kC ON ("
                                +"kC." + Keys.MASTER_KEY_ID
                                + " = " + Tables.KEYS + "." + Keys.MASTER_KEY_ID
                                + " AND kC." + Keys.IS_REVOKED + " = 0"
                                + " AND kC." + Keys.CAN_CERTIFY + " = 1"
                                + " AND kC." + Keys.HAS_SECRET + " > 1"
                                + " AND ( kC." + Keys.EXPIRY + " IS NULL OR kC." + Keys.EXPIRY
                                + " >= " + new Date().getTime() / 1000 + " )"
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
                            Log.e(Constants.TAG, "Malformed find by subkey query!", e);
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
                            Log.e(Constants.TAG, "Malformed find by email query!");
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

            case KEY_RING_KEYS: {
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(Keys._ID, Tables.KEYS + ".oid AS _id");
                projectionMap.put(Keys.MASTER_KEY_ID, Tables.KEYS + "." + Keys.MASTER_KEY_ID);
                projectionMap.put(Keys.RANK, Tables.KEYS + "." + Keys.RANK);
                projectionMap.put(Keys.KEY_ID, Keys.KEY_ID);
                projectionMap.put(Keys.KEY_SIZE, Keys.KEY_SIZE);
                projectionMap.put(Keys.KEY_CURVE_OID, Keys.KEY_CURVE_OID);
                projectionMap.put(Keys.IS_REVOKED, Tables.KEYS + "." + Keys.IS_REVOKED);
                projectionMap.put(Keys.CAN_CERTIFY, Keys.CAN_CERTIFY);
                projectionMap.put(Keys.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
                projectionMap.put(Keys.CAN_SIGN, Keys.CAN_SIGN);
                projectionMap.put(Keys.CAN_AUTHENTICATE, Keys.CAN_AUTHENTICATE);
                projectionMap.put(Keys.HAS_SECRET, Keys.HAS_SECRET);
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

            case KEY_RINGS_USER_IDS:
            case KEY_RING_USER_IDS:
            case KEY_RING_LINKED_IDS: {
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(UserPackets._ID, Tables.USER_PACKETS + ".oid AS _id");
                projectionMap.put(UserPackets.MASTER_KEY_ID, Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID);
                projectionMap.put(UserPackets.TYPE, Tables.USER_PACKETS + "." + UserPackets.TYPE);
                projectionMap.put(UserPackets.USER_ID, Tables.USER_PACKETS + "." + UserPackets.USER_ID);
                projectionMap.put(UserPackets.ATTRIBUTE_DATA, Tables.USER_PACKETS + "." + UserPackets.ATTRIBUTE_DATA);
                projectionMap.put(UserPackets.RANK, Tables.USER_PACKETS + "." + UserPackets.RANK);
                projectionMap.put(UserPackets.IS_PRIMARY, Tables.USER_PACKETS + "." + UserPackets.IS_PRIMARY);
                projectionMap.put(UserPackets.IS_REVOKED, Tables.USER_PACKETS + "." + UserPackets.IS_REVOKED);
                // we take the minimum (>0) here, where "1" is "verified by known secret key"
                projectionMap.put(UserPackets.VERIFIED, "MIN(" + Certs.VERIFIED + ") AS " + UserPackets.VERIFIED);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.USER_PACKETS
                        + " LEFT JOIN " + Tables.CERTS + " ON ("
                            + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " = "
                                + Tables.CERTS + "." + Certs.MASTER_KEY_ID
                            + " AND " + Tables.USER_PACKETS + "." + UserPackets.RANK + " = "
                                + Tables.CERTS + "." + Certs.RANK
                            + " AND " + Tables.CERTS + "." + Certs.VERIFIED + " > 0"
                        + ")");
                groupBy = Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID
                        + ", " + Tables.USER_PACKETS + "." + UserPackets.RANK;

                if (match == KEY_RING_LINKED_IDS) {
                    qb.appendWhere(Tables.USER_PACKETS + "." + UserPackets.TYPE + " = "
                            + WrappedUserAttribute.UAT_URI_ATTRIBUTE);
                } else {
                    qb.appendWhere(Tables.USER_PACKETS + "." + UserPackets.TYPE + " IS NULL");
                }

                // If we are searching for a particular keyring's ids, add where
                if (match == KEY_RING_USER_IDS || match == KEY_RING_LINKED_IDS) {
                    qb.appendWhere(" AND ");
                    qb.appendWhere(Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                }

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " ASC"
                            + "," + Tables.USER_PACKETS + "." + UserPackets.RANK + " ASC";
                }

                break;
            }

            case KEY_RINGS_PUBLIC:
            case KEY_RING_PUBLIC: {
                HashMap<String, String> projectionMap = new HashMap<>();
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
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(KeyRingData._ID, Tables.KEY_RINGS_SECRET + ".oid AS _id");
                projectionMap.put(KeyRingData.MASTER_KEY_ID, KeyRingData.MASTER_KEY_ID);
                projectionMap.put(KeyRingData.KEY_RING_DATA, KeyRingData.KEY_RING_DATA);
                projectionMap.put(KeyRingData.SECRET_RING_TYPE, KeyRingData.SECRET_RING_TYPE);
                projectionMap.put(KeyRingData.AWAITING_MERGE, KeyRingData.AWAITING_MERGE);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.KEY_RINGS_SECRET);

                if(match == KEY_RING_SECRET) {
                    qb.appendWhere(KeyRings.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                }

                break;
            }

            case KEY_RING_CERTS:
            case KEY_RING_CERTS_SPECIFIC:
            case KEY_RING_LINKED_ID_CERTS: {
                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(Certs._ID, Tables.CERTS + ".oid AS " + Certs._ID);
                projectionMap.put(Certs.MASTER_KEY_ID, Tables.CERTS + "." + Certs.MASTER_KEY_ID);
                projectionMap.put(Certs.RANK, Tables.CERTS + "." + Certs.RANK);
                projectionMap.put(Certs.VERIFIED, Tables.CERTS + "." + Certs.VERIFIED);
                projectionMap.put(Certs.TYPE, Tables.CERTS + "." + Certs.TYPE);
                projectionMap.put(Certs.CREATION, Tables.CERTS + "." + Certs.CREATION);
                projectionMap.put(Certs.KEY_ID_CERTIFIER, Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER);
                projectionMap.put(Certs.DATA, Tables.CERTS + "." + Certs.DATA);
                projectionMap.put(Certs.USER_ID, Tables.USER_PACKETS + "." + UserPackets.USER_ID);
                projectionMap.put(Certs.SIGNER_UID, "signer." + UserPackets.USER_ID + " AS " + Certs.SIGNER_UID);
                qb.setProjectionMap(projectionMap);

                qb.setTables(Tables.CERTS
                    + " JOIN " + Tables.USER_PACKETS + " ON ("
                            + Tables.CERTS + "." + Certs.MASTER_KEY_ID + " = "
                            + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID
                        + " AND "
                            + Tables.CERTS + "." + Certs.RANK + " = "
                            + Tables.USER_PACKETS + "." + UserPackets.RANK
                    + ") LEFT JOIN " + Tables.USER_PACKETS + " AS signer ON ("
                            + Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER + " = "
                            + "signer." + UserPackets.MASTER_KEY_ID
                        + " AND "
                            + "signer." + Keys.RANK + " = 0"
                    + ")");

                groupBy = Tables.CERTS + "." + Certs.RANK + ", "
                        + Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER;

                qb.appendWhere(Tables.CERTS + "." + Certs.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                if(match == KEY_RING_CERTS_SPECIFIC) {
                    qb.appendWhere(" AND " + Tables.CERTS + "." + Certs.RANK + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(3));
                    qb.appendWhere(" AND " + Tables.CERTS + "." + Certs.KEY_ID_CERTIFIER+ " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(4));
                }

                if (match == KEY_RING_LINKED_ID_CERTS) {
                    qb.appendWhere(" AND " + Tables.USER_PACKETS + "."
                            + UserPackets.TYPE + " IS NOT NULL");

                    qb.appendWhere(" AND " + Tables.USER_PACKETS + "."
                            + UserPackets.RANK + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(3));
                } else {
                    qb.appendWhere(" AND " + Tables.USER_PACKETS + "." + UserPackets.TYPE + " IS NULL");
                }

                break;
            }

            case UPDATED_KEYS:
            case UPDATED_KEYS_SPECIFIC: {
                HashMap<String, String> projectionMap = new HashMap<>();
                qb.setTables(Tables.UPDATED_KEYS);
                projectionMap.put(UpdatedKeys.MASTER_KEY_ID, Tables.UPDATED_KEYS + "."
                        + UpdatedKeys.MASTER_KEY_ID);
                projectionMap.put(UpdatedKeys.LAST_UPDATED, Tables.UPDATED_KEYS + "."
                        + UpdatedKeys.LAST_UPDATED);
                qb.setProjectionMap(projectionMap);
                if (match == UPDATED_KEYS_SPECIFIC) {
                    qb.appendWhere(UpdatedKeys.MASTER_KEY_ID + " = ");
                    qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                }
                break;
            }

            case API_APPS: {
                qb.setTables(Tables.API_APPS);

                break;
            }
            case API_APPS_BY_PACKAGE_NAME: {
                qb.setTables(Tables.API_APPS);
                qb.appendWhere(ApiApps.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getLastPathSegment());

                break;
            }
            case API_ACCOUNTS: {
                qb.setTables(Tables.API_ACCOUNTS);
                qb.appendWhere(Tables.API_ACCOUNTS + "." + ApiAccounts.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                break;
            }
            case API_ACCOUNTS_BY_ACCOUNT_NAME: {
                qb.setTables(Tables.API_ACCOUNTS);
                qb.appendWhere(Tables.API_ACCOUNTS + "." + ApiAccounts.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

                qb.appendWhere(" AND " + Tables.API_ACCOUNTS + "." + ApiAccounts.ACCOUNT_NAME + " = ");
                qb.appendWhereEscapeString(uri.getLastPathSegment());

                break;
            }
            case API_ALLOWED_KEYS: {
                qb.setTables(Tables.API_ALLOWED_KEYS);
                qb.appendWhere(Tables.API_ALLOWED_KEYS + "." + ApiAccounts.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));

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

        SQLiteDatabase db = getDb().getReadableDatabase();

        Cursor cursor = qb.query(db, projection, selection, selectionArgs, groupBy, having, orderBy);
        if (cursor != null) {
            // Tell the cursor what uri to watch, so it knows when its source data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        Log.d(Constants.TAG,
                "Query: " + qb.buildQuery(projection, selection, null, null, orderBy, null));

        if (Constants.DEBUG && Constants.DEBUG_LOG_DB_QUERIES) {
            Log.d(Constants.TAG, "Cursor: " + DatabaseUtils.dumpCursorToString(cursor));
        }

        if (Constants.DEBUG && Constants.DEBUG_EXPLAIN_QUERIES) {
            String rawQuery = qb.buildQuery(projection, selection, groupBy, having, orderBy, null);
            Cursor explainCursor = db.rawQuery("EXPLAIN QUERY PLAN " + rawQuery, selectionArgs);

            // this is a debugging feature, we can be a little careless
            explainCursor.moveToFirst();

            StringBuilder line = new StringBuilder();
            for (int i = 0; i < explainCursor.getColumnCount(); i++) {
                line.append(explainCursor.getColumnName(i)).append(", ");
            }
            Log.d(Constants.TAG, line.toString());

            while (!explainCursor.isAfterLast()) {
                line = new StringBuilder();
                for (int i = 0; i < explainCursor.getColumnCount(); i++) {
                    line.append(explainCursor.getString(i)).append(", ");
                }
                Log.d(Constants.TAG, line.toString());
                explainCursor.moveToNext();
            }

            explainCursor.close();
        }

        return cursor;
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
                case KEY_RING_PUBLIC: {
                    db.insertOrThrow(Tables.KEY_RINGS_PUBLIC, null, values);
                    keyId = values.getAsLong(KeyRings.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_SECRET: {
                    db.insertOrThrow(Tables.KEY_RINGS_SECRET, null, values);
                    keyId = values.getAsLong(KeyRings.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_KEYS: {
                    db.insertOrThrow(Tables.KEYS, null, values);
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
                    db.insertOrThrow(Tables.USER_PACKETS, null, values);
                    keyId = values.getAsLong(UserPackets.MASTER_KEY_ID);
                    break;
                }
                case KEY_RING_CERTS: {
                    // we replace here, keeping only the latest signature
                    // TODO this would be better handled in savePublicKeyRing directly!
                    db.replaceOrThrow(Tables.CERTS, null, values);
                    keyId = values.getAsLong(Certs.MASTER_KEY_ID);
                    break;
                }
                case UPDATED_KEYS: {
                    long updatedKeyId = db.replace(Tables.UPDATED_KEYS, null, values);
                    rowUri = UpdatedKeys.CONTENT_URI.buildUpon().appendPath("" + updatedKeyId)
                            .build();
                    break;
                }
                case API_APPS: {
                    db.insertOrThrow(Tables.API_APPS, null, values);
                    break;
                }
                case API_ACCOUNTS: {
                    // set foreign key automatically based on given uri
                    // e.g., api_apps/com.example.app/accounts/
                    String packageName = uri.getPathSegments().get(1);
                    values.put(ApiAccounts.PACKAGE_NAME, packageName);

                    db.insertOrThrow(Tables.API_ACCOUNTS, null, values);
                    break;
                }
                case API_ALLOWED_KEYS: {
                    // set foreign key automatically based on given uri
                    // e.g., api_apps/com.example.app/allowed_keys/
                    String packageName = uri.getPathSegments().get(1);
                    values.put(ApiAllowedKeys.PACKAGE_NAME, packageName);

                    db.insertOrThrow(Tables.API_ALLOWED_KEYS, null, values);
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

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.d(Constants.TAG, "Constraint exception on insert! Entry already existing?", e);
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
            // dangerous
            case KEY_RINGS_UNIFIED: {
                count = db.delete(Tables.KEY_RINGS_PUBLIC, null, null);
                break;
            }
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

            case API_APPS_BY_PACKAGE_NAME: {
                count = db.delete(Tables.API_APPS, buildDefaultApiAppsSelection(uri, additionalSelection),
                        selectionArgs);
                break;
            }
            case API_ACCOUNTS_BY_ACCOUNT_NAME: {
                count = db.delete(Tables.API_ACCOUNTS, buildDefaultApiAccountsSelection(uri, additionalSelection),
                        selectionArgs);
                break;
            }
            case API_ALLOWED_KEYS: {
                count = db.delete(Tables.API_ALLOWED_KEYS, buildDefaultApiAllowedKeysSelection(uri, additionalSelection),
                        selectionArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
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

        final SQLiteDatabase db = getDb().getWritableDatabase();

        int count = 0;
        try {
            final int match = mUriMatcher.match(uri);
            switch (match) {
                case KEY_RING_SECRET: {
                    if (values.size() != 1 ||
                            !(values.containsKey(KeyRingData.AWAITING_MERGE) || values.containsKey(KeyRingData.SECRET_RING_TYPE))) {
                        throw new UnsupportedOperationException(
                                "Only awaiting_merge || has_secret_ring column may be updated, one at at time!");
                    }
                    Long mkid = Long.parseLong(uri.getPathSegments().get(1));
                    String actualSelection = Keys.MASTER_KEY_ID + " = " + Long.toString(mkid);
                    if (!TextUtils.isEmpty(selection)) {
                        actualSelection += " AND (" + selection + ")";
                    }
                    count = db.update(Tables.KEY_RINGS_SECRET, values, actualSelection, selectionArgs);
                    break;
                }
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
                    count = db.update(Tables.KEYS, values, actualSelection, selectionArgs);
                    break;
                }
                case API_APPS_BY_PACKAGE_NAME: {
                    count = db.update(Tables.API_APPS, values,
                            buildDefaultApiAppsSelection(uri, selection), selectionArgs);
                    break;
                }
                case API_ACCOUNTS_BY_ACCOUNT_NAME: {
                    count = db.update(Tables.API_ACCOUNTS, values,
                            buildDefaultApiAccountsSelection(uri, selection), selectionArgs);
                    break;
                }
                case KEY_RING_CERTS: {
                    if (values.size() != 1 || !values.containsKey(Certs.VERIFIED)) {
                        throw new UnsupportedOperationException(
                                "Only verified column may be updated");
                    }
                    // make sure we get a long value here
                    Long mkid = Long.parseLong(uri.getPathSegments().get(1));
                    String actualSelection = Keys.MASTER_KEY_ID + " = " + Long.toString(mkid);
                    if (!TextUtils.isEmpty(selection)) {
                        actualSelection += " AND (" + selection + ")";
                    }
                    count = db.update(Tables.CERTS, values, actualSelection, selectionArgs);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
                }
            }

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.d(Constants.TAG, "Constraint exception on update! Entry already existing?", e);
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

    private String buildDefaultApiAllowedKeysSelection(Uri uri, String selection) {
        String packageName = DatabaseUtils.sqlEscapeString(uri.getPathSegments().get(1));

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return ApiAllowedKeys.PACKAGE_NAME + "=" + packageName + andSelection;
    }

}
