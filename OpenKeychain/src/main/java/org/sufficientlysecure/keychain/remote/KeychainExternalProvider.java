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

package org.sufficientlysecure.keychain.remote;


import java.security.AccessControlException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject.AutocryptRecommendationResult;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject.AutocryptState;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.EmailStatus;
import org.sufficientlysecure.keychain.provider.KeychainProvider;
import org.sufficientlysecure.keychain.provider.SimpleContentResolverInterface;
import org.sufficientlysecure.keychain.util.CloseDatabaseCursorFactory;
import timber.log.Timber;


public class KeychainExternalProvider extends ContentProvider implements SimpleContentResolverInterface {
    private static final int EMAIL_STATUS = 101;

    private static final int AUTOCRYPT_STATUS = 201;
    private static final int AUTOCRYPT_STATUS_INTERNAL = 202;

    private static final int API_APPS = 301;
    private static final int API_APPS_BY_PACKAGE_NAME = 302;

    public static final String TEMP_TABLE_QUERIED_ADDRESSES = "queried_addresses";
    public static final String TEMP_TABLE_COLUMN_ADDRES = "address";


    private UriMatcher uriMatcher;
    private ApiPermissionHelper apiPermissionHelper;
    private KeychainProvider internalKeychainProvider;


    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainExternalContract.CONTENT_AUTHORITY_EXTERNAL;

        /*
         * list email_status
         *
         * <pre>
         * email_status/
         * </pre>
         */
        matcher.addURI(authority, KeychainExternalContract.BASE_EMAIL_STATUS, EMAIL_STATUS);

        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS, AUTOCRYPT_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS + "/*", AUTOCRYPT_STATUS_INTERNAL);

        return matcher;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        uriMatcher = buildUriMatcher();

        internalKeychainProvider = new KeychainProvider();
        internalKeychainProvider.attachInfo(getContext(), null);
        apiPermissionHelper = new ApiPermissionHelper(getContext(), new ApiDataAccessObject(internalKeychainProvider));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case EMAIL_STATUS:
                return EmailStatus.CONTENT_TYPE;

            case API_APPS:
                return ApiApps.CONTENT_TYPE;

            case API_APPS_BY_PACKAGE_NAME:
                return ApiApps.CONTENT_ITEM_TYPE;

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
        Timber.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        long startTime = System.currentTimeMillis();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = uriMatcher.match(uri);

        String groupBy = null;

        SQLiteDatabase db = new KeychainDatabase(getContext()).getReadableDatabase();

        String callingPackageName = apiPermissionHelper.getCurrentCallingPackage();

        switch (match) {
            case EMAIL_STATUS: {
                boolean callerIsAllowed = apiPermissionHelper.isAllowedIgnoreErrors();
                if (!callerIsAllowed) {
                    throw new AccessControlException("An application must register before use of KeychainExternalProvider!");
                }

                db.execSQL("CREATE TEMPORARY TABLE " + TEMP_TABLE_QUERIED_ADDRESSES + " (" + TEMP_TABLE_COLUMN_ADDRES + " TEXT);");
                ContentValues cv = new ContentValues();
                for (String address : selectionArgs) {
                    cv.put(TEMP_TABLE_COLUMN_ADDRES, address);
                    db.insert(TEMP_TABLE_QUERIED_ADDRESSES, null, cv);
                }

                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(EmailStatus._ID, "email AS _id");
                projectionMap.put(EmailStatus.EMAIL_ADDRESS, // this is actually the queried address
                        TEMP_TABLE_QUERIED_ADDRESSES + "." + TEMP_TABLE_COLUMN_ADDRES + " AS " + EmailStatus.EMAIL_ADDRESS);
                projectionMap.put(EmailStatus.USER_ID,
                        Tables.USER_PACKETS + "." + UserPackets.USER_ID + " AS " + EmailStatus.USER_ID);
                // we take the minimum (>0) here, where "1" is "verified by known secret key", "2" is "self-certified"
                projectionMap.put(EmailStatus.USER_ID_STATUS, "CASE ( MIN (" + Certs.VERIFIED + " ) ) "
                        // remap to keep this provider contract independent from our internal representation
                        + " WHEN " + Certs.VERIFIED_SELF + " THEN " + KeychainExternalContract.KEY_STATUS_UNVERIFIED
                        + " WHEN " + Certs.VERIFIED_SECRET + " THEN " + KeychainExternalContract.KEY_STATUS_VERIFIED
                        + " WHEN NULL THEN " + KeychainExternalContract.KEY_STATUS_UNVERIFIED
                        + " END AS " + EmailStatus.USER_ID_STATUS);
                projectionMap.put(EmailStatus.MASTER_KEY_ID,
                        Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " AS " + EmailStatus.MASTER_KEY_ID);
                qb.setProjectionMap(projectionMap);

                if (projection == null) {
                    throw new IllegalArgumentException("Please provide a projection!");
                }

                qb.setTables(
                        TEMP_TABLE_QUERIED_ADDRESSES
                                + " LEFT JOIN " + Tables.USER_PACKETS + " ON ("
                                + Tables.USER_PACKETS + "." + UserPackets.USER_ID + " IS NOT NULL"
                                + " AND " + Tables.USER_PACKETS + "." + UserPackets.EMAIL + " LIKE " + TEMP_TABLE_QUERIED_ADDRESSES + "." + TEMP_TABLE_COLUMN_ADDRES
                                + ")"
                                + " LEFT JOIN " + Tables.CERTS + " ON ("
                                + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " = " + Tables.CERTS + "." + Certs.MASTER_KEY_ID
                                + " AND " + Tables.USER_PACKETS + "." + UserPackets.RANK + " = " + Tables.CERTS + "." + Certs.RANK
                                + ")"
                );
                // in case there are multiple verifying certificates
                groupBy = TEMP_TABLE_QUERIED_ADDRESSES + "." + TEMP_TABLE_COLUMN_ADDRES;
                List<String> plist = Arrays.asList(projection);
                if (plist.contains(EmailStatus.USER_ID)) {
                    groupBy += ", " + Tables.USER_PACKETS + "." + UserPackets.USER_ID;
                }

                // verified == 0 has no self-cert, which is basically an error case. never return that!
                // verified == null is fine, because it means there was no join partner
                qb.appendWhere(Tables.CERTS + "." + Certs.VERIFIED + " IS NULL OR " + Tables.CERTS + "." + Certs.VERIFIED + " > 0");

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = EmailStatus.EMAIL_ADDRESS;
                }

                // uri to watch is all /key_rings/
                uri = KeyRings.CONTENT_URI;

                break;
            }

            case AUTOCRYPT_STATUS_INTERNAL:
                if (!BuildConfig.APPLICATION_ID.equals(callingPackageName)) {
                    throw new AccessControlException("This URI can only be called internally!");
                }

                // override package name to use any external
                 callingPackageName = uri.getLastPathSegment();

            case AUTOCRYPT_STATUS: {
                boolean callerIsAllowed = (match == AUTOCRYPT_STATUS_INTERNAL) || apiPermissionHelper.isAllowedIgnoreErrors();
                if (!callerIsAllowed) {
                    throw new AccessControlException("An application must register before use of KeychainExternalProvider!");
                }

                if (projection == null) {
                    throw new IllegalArgumentException("Please provide a projection!");
                }

                db.execSQL("CREATE TEMPORARY TABLE " + TEMP_TABLE_QUERIED_ADDRESSES + " (" +
                        TEMP_TABLE_COLUMN_ADDRES + " TEXT NOT NULL PRIMARY KEY, " +
                        AutocryptStatus.UID_KEY_STATUS + " INT, " +
                        AutocryptStatus.UID_ADDRESS + " TEXT, " +
                        AutocryptStatus.UID_MASTER_KEY_ID + " INT, " +
                        AutocryptStatus.UID_CANDIDATES + " INT, " +
                        AutocryptStatus.AUTOCRYPT_PEER_STATE + " INT DEFAULT " + AutocryptStatus.AUTOCRYPT_PEER_DISABLED + ", " +
                        AutocryptStatus.AUTOCRYPT_KEY_STATUS + " INT, " +
                        AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID + " INT" +
                        ");");
                ContentValues cv = new ContentValues();
                for (String address : selectionArgs) {
                    cv.put(TEMP_TABLE_COLUMN_ADDRES, address);
                    db.insert(TEMP_TABLE_QUERIED_ADDRESSES, null, cv);
                }

                boolean isWildcardSelector = selectionArgs.length == 1 && selectionArgs[0].contains("%");

                List<String> plist = Arrays.asList(projection);

                boolean queriesUidResult = plist.contains(AutocryptStatus.UID_KEY_STATUS) ||
                        plist.contains(AutocryptStatus.UID_ADDRESS) ||
                        plist.contains(AutocryptStatus.UID_MASTER_KEY_ID) ||
                        plist.contains(AutocryptStatus.UID_CANDIDATES);
                if (queriesUidResult) {
                    fillTempTableWithUidResult(db, isWildcardSelector);
                }

                boolean queriesAutocryptResult = plist.contains(AutocryptStatus.AUTOCRYPT_PEER_STATE) ||
                        plist.contains(AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID) ||
                        plist.contains(AutocryptStatus.AUTOCRYPT_KEY_STATUS);
                if (isWildcardSelector && queriesAutocryptResult) {
                    throw new UnsupportedOperationException("Cannot wildcard-query autocrypt results!");
                }
                if (!isWildcardSelector && queriesAutocryptResult) {
                    AutocryptPeerDataAccessObject autocryptPeerDao =
                            new AutocryptPeerDataAccessObject(internalKeychainProvider, callingPackageName);
                    fillTempTableWithAutocryptRecommendations(db, autocryptPeerDao, selectionArgs);
                }

                HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(AutocryptStatus._ID, AutocryptStatus._ID);
                projectionMap.put(AutocryptStatus.ADDRESS, AutocryptStatus.ADDRESS);
                projectionMap.put(AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_KEY_STATUS);
                projectionMap.put(AutocryptStatus.UID_ADDRESS, AutocryptStatus.UID_ADDRESS);
                projectionMap.put(AutocryptStatus.UID_MASTER_KEY_ID, AutocryptStatus.UID_MASTER_KEY_ID);
                projectionMap.put(AutocryptStatus.UID_CANDIDATES, AutocryptStatus.UID_CANDIDATES);
                projectionMap.put(AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_PEER_STATE);
                projectionMap.put(AutocryptStatus.AUTOCRYPT_KEY_STATUS, AutocryptStatus.AUTOCRYPT_KEY_STATUS);
                projectionMap.put(AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID, AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID);
                qb.setProjectionMap(projectionMap);
                qb.setTables(TEMP_TABLE_QUERIED_ADDRESSES);

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = AutocryptStatus.ADDRESS;
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

        qb.setStrict(true);
        qb.setCursorFactory(new CloseDatabaseCursorFactory());
        Cursor cursor = qb.query(db, projection, null, null, groupBy, null, orderBy);
        if (cursor != null) {
            // Tell the cursor what uri to watch, so it knows when its source data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            if (Constants.DEBUG_LOG_DB_QUERIES) {
                DatabaseUtils.dumpCursor(cursor);
            }
        }

        Timber.d("Query: " + qb.buildQuery(projection, selection, groupBy, null, orderBy, null));
        Timber.d(Constants.TAG, "Query took %s ms", (System.currentTimeMillis() - startTime));

        return cursor;
    }

    private void fillTempTableWithAutocryptRecommendations(SQLiteDatabase db,
            AutocryptPeerDataAccessObject autocryptPeerDao, String[] peerIds) {
        List<AutocryptRecommendationResult> autocryptStates =
                autocryptPeerDao.determineAutocryptRecommendations(peerIds);

        fillTempTableWithAutocryptRecommendations(db, autocryptStates);
    }

    private void fillTempTableWithAutocryptRecommendations(SQLiteDatabase db,
            List<AutocryptRecommendationResult> autocryptRecommendations) {
        ContentValues cv = new ContentValues();
        for (AutocryptRecommendationResult peerResult : autocryptRecommendations) {
            cv.clear();

            cv.put(AutocryptStatus.AUTOCRYPT_PEER_STATE, getPeerStateValue(peerResult.autocryptState));
            if (peerResult.masterKeyId != null) {
                cv.put(AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID, peerResult.masterKeyId);
                cv.put(AutocryptStatus.AUTOCRYPT_KEY_STATUS, peerResult.isVerified ?
                        KeychainExternalContract.KEY_STATUS_VERIFIED :
                        KeychainExternalContract.KEY_STATUS_UNVERIFIED);
            }

            db.update(TEMP_TABLE_QUERIED_ADDRESSES, cv,TEMP_TABLE_COLUMN_ADDRES + "=?",
                    new String[] { peerResult.peerId });
        }
    }

    private void fillTempTableWithUidResult(SQLiteDatabase db, boolean isWildcardSelector) {
        String cmpOperator = isWildcardSelector ? " LIKE " : " = ";
        long unixSeconds = System.currentTimeMillis() / 1000;
        db.execSQL("REPLACE INTO " + TEMP_TABLE_QUERIED_ADDRESSES +
                "(" + TEMP_TABLE_COLUMN_ADDRES + ", " + AutocryptStatus.UID_KEY_STATUS + ", " +
                AutocryptStatus.UID_ADDRESS + ", " + AutocryptStatus.UID_MASTER_KEY_ID
                + ", " + AutocryptStatus.UID_CANDIDATES + ")" +
                " SELECT " + TEMP_TABLE_COLUMN_ADDRES + ", " +
                "CASE ( MIN (" + Tables.CERTS + "." + Certs.VERIFIED + " ) ) "
                // remap to keep this provider contract independent from our internal representation
                + " WHEN " + Certs.VERIFIED_SELF + " THEN " + KeychainExternalContract.KEY_STATUS_UNVERIFIED
                + " WHEN " + Certs.VERIFIED_SECRET + " THEN " + KeychainExternalContract.KEY_STATUS_VERIFIED
                + " END AS " + AutocryptStatus.UID_KEY_STATUS
                + ", " + Tables.USER_PACKETS + "." + UserPackets.USER_ID
                + ", " + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID
                + ", COUNT(DISTINCT " + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + ")"
                + " FROM " + TEMP_TABLE_QUERIED_ADDRESSES
                + " LEFT JOIN " + Tables.USER_PACKETS + " ON ("
                + Tables.USER_PACKETS + "." + UserPackets.EMAIL + cmpOperator +
                TEMP_TABLE_QUERIED_ADDRESSES + "." + TEMP_TABLE_COLUMN_ADDRES
                + ")"
                + " LEFT JOIN " + Tables.CERTS + " ON ("
                + Tables.CERTS + "." + Certs.MASTER_KEY_ID + " = " + Tables.USER_PACKETS + "." +
                UserPackets.MASTER_KEY_ID
                + " AND " + Tables.CERTS + "." + Certs.RANK + " = " + Tables.USER_PACKETS + "." +
                UserPackets.RANK
                + " AND " + Tables.CERTS + "." + Certs.VERIFIED + " > 0"
                + ")"
                + " WHERE (EXISTS (SELECT 1 FROM " + Tables.KEYS + " WHERE "
                + Tables.KEYS + "." + Keys.KEY_ID + " = " + Tables.USER_PACKETS + "." +
                UserPackets.MASTER_KEY_ID
                + " AND " + Tables.KEYS + "." + Keys.RANK + " = 0"
                + " AND " + Tables.KEYS + "." + Keys.IS_REVOKED + " = 0"
                + " AND NOT " + "(" + Tables.KEYS + "." + Keys.EXPIRY + " IS NOT NULL AND " + Tables.KEYS +
                "." + Keys.EXPIRY
                + " < " + unixSeconds + ")"
                + ")) OR " + Tables.USER_PACKETS + "." + UserPackets.MASTER_KEY_ID + " IS NULL"
                + " GROUP BY " + TEMP_TABLE_QUERIED_ADDRESSES + "." + TEMP_TABLE_COLUMN_ADDRES);
    }

    private int getPeerStateValue(AutocryptState autocryptState) {
        switch (autocryptState) {
            case DISABLE: return AutocryptStatus.AUTOCRYPT_PEER_DISABLED;
            case DISCOURAGED_OLD: return AutocryptStatus.AUTOCRYPT_PEER_DISCOURAGED_OLD;
            case DISCOURAGED_GOSSIP: return AutocryptStatus.AUTOCRYPT_PEER_GOSSIP;
            case AVAILABLE: return AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE;
            case MUTUAL: return AutocryptStatus.AUTOCRYPT_PEER_MUTUAL;
        }
        throw new IllegalStateException("Unhandled case!");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
