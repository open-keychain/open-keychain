/*
 * Copyright (C) 2016 Vincent Breitmoser <look@my.amazin.horse>
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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.EmailStatus;
import org.sufficientlysecure.keychain.provider.SimpleContentResolverInterface;
import org.sufficientlysecure.keychain.util.Log;

public class KeychainExternalProvider extends ContentProvider implements SimpleContentResolverInterface {
    private static final int EMAIL_STATUS = 101;
    private static final int EMAIL_STATUS_INTERNAL = 102;
    private static final int API_APPS = 301;
    private static final int API_APPS_BY_PACKAGE_NAME = 302;

    public static final String TEMP_TABLE_QUERIED_ADDRESSES = "queried_addresses";
    public static final String TEMP_TABLE_COLUMN_ADDRES = "address";


    private UriMatcher mUriMatcher;
    private ApiPermissionHelper mApiPermissionHelper;


    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainExternalContract.CONTENT_AUTHORITY_EXTERNAL;

        /**
         * list email_status
         *
         * <pre>
         * email_status/
         * </pre>
         */
        matcher.addURI(authority, KeychainExternalContract.BASE_EMAIL_STATUS, EMAIL_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_EMAIL_STATUS + "/*", EMAIL_STATUS_INTERNAL);

        // can only query status of calling app - for internal use only!
        matcher.addURI(KeychainContract.CONTENT_AUTHORITY, KeychainContract.BASE_API_APPS + "/*", API_APPS_BY_PACKAGE_NAME);

        return matcher;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        mUriMatcher = buildUriMatcher();
        mApiPermissionHelper = new ApiPermissionHelper(getContext(), new ApiDataAccessObject(this));
        return true;
    }

    public KeychainDatabase getDb() {
        return new KeychainDatabase(getContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = mUriMatcher.match(uri);
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
        Log.v(Constants.TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = mUriMatcher.match(uri);

        String groupBy = null;

        SQLiteDatabase db = getDb().getReadableDatabase();

        String callingPackageName = mApiPermissionHelper.getCurrentCallingPackage();

        switch (match) {
            case EMAIL_STATUS_INTERNAL:
                if (!BuildConfig.APPLICATION_ID.equals(callingPackageName)) {
                    throw new AccessControlException("This URI can only be called internally!");
                }

                // override package name to use any external
                // callingPackageName = uri.getLastPathSegment();

            case EMAIL_STATUS: {
                boolean callerIsAllowed = (match == EMAIL_STATUS_INTERNAL) || mApiPermissionHelper.isAllowedIgnoreErrors();
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
                        + " WHEN NULL THEN 1"
                        + " WHEN " + Certs.VERIFIED_SELF + " THEN 1"
                        + " WHEN " + Certs.VERIFIED_SECRET + " THEN 2"
                        + " WHEN NULL THEN NULL"
                        + " END AS " + EmailStatus.USER_ID_STATUS);
                projectionMap.put(EmailStatus.USER_ID, Tables.USER_PACKETS + "." + UserPackets.USER_ID + " AS " + EmailStatus.USER_ID);
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

            case API_APPS_BY_PACKAGE_NAME: {
                String requestedPackageName = uri.getLastPathSegment();
                checkIfPackageBelongsToCaller(getContext(), requestedPackageName);

                qb.setTables(Tables.API_APPS);
                qb.appendWhere(ApiApps.PACKAGE_NAME + " = ");
                qb.appendWhereEscapeString(requestedPackageName);

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

        Cursor cursor = qb.query(db, projection, selection, null, groupBy, null, orderBy);
        if (cursor != null) {
            // Tell the cursor what uri to watch, so it knows when its source data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        Log.d(Constants.TAG,
                "Query: " + qb.buildQuery(projection, selection, groupBy, null, orderBy, null));

        return cursor;
    }

    private void checkIfPackageBelongsToCaller(Context context, String requestedPackageName) {
        int callerUid = Binder.getCallingUid();
        String[] callerPackageNames = context.getPackageManager().getPackagesForUid(callerUid);
        if (callerPackageNames == null) {
            throw new IllegalStateException("Failed to retrieve caller package name, this is an error!");
        }

        boolean packageBelongsToCaller = false;
        for (String p : callerPackageNames) {
            if (p.equals(requestedPackageName)) {
                packageBelongsToCaller = true;
                break;
            }
        }
        if (!packageBelongsToCaller) {
            throw new SecurityException("ExternalProvider may only check status of caller package!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, String additionalSelection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
