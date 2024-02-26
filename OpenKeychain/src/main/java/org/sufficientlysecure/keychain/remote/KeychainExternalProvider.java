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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.UidStatus;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.daos.UserIdDao;
import org.sufficientlysecure.keychain.model.CustomColumnAdapters;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.remote.AutocryptInteractor.AutocryptRecommendationResult;
import org.sufficientlysecure.keychain.remote.AutocryptInteractor.AutocryptState;
import timber.log.Timber;


public class KeychainExternalProvider extends ContentProvider {
    private static final int EMAIL_STATUS = 101;

    private static final int AUTOCRYPT_STATUS = 201;
    private static final int AUTOCRYPT_STATUS_INTERNAL = 202;


    private UriMatcher uriMatcher;
    private ApiPermissionHelper apiPermissionHelper;


    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeychainExternalContract.CONTENT_AUTHORITY_EXTERNAL;

        matcher.addURI(authority, KeychainExternalContract.BASE_EMAIL_STATUS, EMAIL_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS, AUTOCRYPT_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS + "/*",
                AUTOCRYPT_STATUS_INTERNAL);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        uriMatcher = buildUriMatcher();

        Context context = getContext();
        if (context == null) {
            throw new NullPointerException("Context can't be null during onCreate!");
        }

        apiPermissionHelper = new ApiPermissionHelper(context, ApiAppDao.getInstance(getContext()));
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs,
            String sortOrder) {
        Timber.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException();
        }

        String callingPackageName = apiPermissionHelper.getCurrentCallingPackage();

        int match = uriMatcher.match(uri);
        switch (match) {
            case EMAIL_STATUS: {
                Toast.makeText(context, "This API is no longer supported by OpenKeychain!",
                        Toast.LENGTH_SHORT).show();
                return new MatrixCursor(projection);
            }

            case AUTOCRYPT_STATUS_INTERNAL:
                if (!BuildConfig.APPLICATION_ID.equals(callingPackageName)) {
                    throw new AccessControlException("This URI can only be called internally!");
                }

                // override package name to use any external
                callingPackageName = uri.getLastPathSegment();

            case AUTOCRYPT_STATUS: {
                boolean callerIsAllowed = (match == AUTOCRYPT_STATUS_INTERNAL) ||
                        apiPermissionHelper.isAllowedIgnoreErrors();
                if (!callerIsAllowed) {
                    throw new AccessControlException(
                            "An application must register before use of KeychainExternalProvider!");
                }

                if (projection == null) {
                    throw new IllegalArgumentException("Please provide a projection!");
                }

                List<String> plist = Arrays.asList(projection);
                boolean isWildcardSelector =
                        selectionArgs.length == 1 && selectionArgs[0].contains("%");

                UserIdDao userIdDao = UserIdDao.getInstance(getContext());
                AutocryptInteractor autocryptInteractor =
                        AutocryptInteractor.getInstance(getContext(), callingPackageName);

                Map<String, UidStatus> uidStatuses;
                Map<String, AutocryptRecommendationResult> autocryptStates;
                String[] emails;
                if (isWildcardSelector) {
                    uidStatuses = userIdDao.getUidStatusByEmailLike(selectionArgs[0]);
                    autocryptStates = autocryptInteractor.determineAutocryptRecommendationsLike(selectionArgs[0]);
                    // If this was a wildcard query, use the found email addresses in the result set.
                    Set<String> emailsSet = new HashSet<>();
                    emailsSet.addAll(uidStatuses.keySet());
                    emailsSet.addAll(autocryptStates.keySet());
                    emails = emailsSet.toArray(new String[0]);
                } else {
                    uidStatuses = userIdDao.getUidStatusByEmail(selectionArgs);
                    autocryptStates = autocryptInteractor.determineAutocryptRecommendations(selectionArgs);
                    // Otherwise, map exactly the selection args to results.
                    emails = selectionArgs;
                }

                MatrixCursor cursor = mapResultsToProjectedMatrixCursor(
                        projection, emails, uidStatuses, autocryptStates);

                uri = DatabaseNotifyManager.getNotifyUriAllKeys();
                cursor.setNotificationUri(context.getContentResolver(), uri);

                return cursor;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri + " (" + match + ")");
            }
        }
    }

    @NonNull
    private MatrixCursor mapResultsToProjectedMatrixCursor(
            String[] projection,
            String[] addresses,
            Map<String, UidStatus> uidStatuses,
            Map<String, AutocryptRecommendationResult> autocryptStates
        ) {
        MatrixCursor cursor = new MatrixCursor(projection);
        for (String address : addresses) {
            AutocryptRecommendationResult autocryptResult = autocryptStates.get(address);
            UidStatus uidStatus = uidStatuses.get(address);

            Object[] row = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                if (AutocryptStatus.ADDRESS.equals(projection[i]) ||
                        AutocryptStatus._ID.equals(projection[i])) {
                    row[i] = address;
                } else {
                    row[i] = columnNameToRowContent(projection[i], autocryptResult, uidStatus);
                }
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    private Object columnNameToRowContent(
            String columnName, AutocryptRecommendationResult autocryptResult, UidStatus uidStatus) {
        switch (columnName) {
            case AutocryptStatus.UID_KEY_STATUS: {
                if (uidStatus == null) {
                    return null;
                }
                return CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER.decode(
                        uidStatus.getKey_status_int()) == VerificationStatus.VERIFIED_SECRET ?
                        KeychainExternalContract.KEY_STATUS_VERIFIED :
                        KeychainExternalContract.KEY_STATUS_UNVERIFIED;
            }
            case AutocryptStatus.UID_ADDRESS:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.getUser_id();

            case AutocryptStatus.UID_MASTER_KEY_ID:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.getMaster_key_id();

            case AutocryptStatus.UID_CANDIDATES:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.getCandidates();

            case AutocryptStatus.AUTOCRYPT_PEER_STATE:
                if (autocryptResult == null) {
                    return null;
                }
                return getPeerStateValue(autocryptResult.autocryptState);

            case AutocryptStatus.AUTOCRYPT_KEY_STATUS:
                if (autocryptResult == null) {
                    return null;
                }
                return autocryptResult.isVerified ?
                        KeychainExternalContract.KEY_STATUS_VERIFIED :
                        KeychainExternalContract.KEY_STATUS_UNVERIFIED;

            case AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID:
                if (autocryptResult == null) {
                    return null;
                }
                return autocryptResult.masterKeyId;

            default:
                throw new IllegalArgumentException("Unhandled case " + columnName);
        }
    }

    private int getPeerStateValue(AutocryptState autocryptState) {
        switch (autocryptState) {
            case DISABLE:
                return AutocryptStatus.AUTOCRYPT_PEER_DISABLED;
            case DISCOURAGED_OLD:
                return AutocryptStatus.AUTOCRYPT_PEER_DISCOURAGED_OLD;
            case DISCOURAGED_GOSSIP:
                return AutocryptStatus.AUTOCRYPT_PEER_GOSSIP;
            case AVAILABLE:
                return AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE;
            case MUTUAL:
                return AutocryptStatus.AUTOCRYPT_PEER_MUTUAL;
        }
        throw new IllegalStateException("Unhandled case!");
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Unknown uri: " + uri);
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
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
