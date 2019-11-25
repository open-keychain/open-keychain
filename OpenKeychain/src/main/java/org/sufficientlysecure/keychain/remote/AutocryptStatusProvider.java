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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.daos.UserIdDao;
import org.sufficientlysecure.keychain.model.UserPacket.UidStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.remote.AutocryptInteractor.AutocryptRecommendationResult;
import org.sufficientlysecure.keychain.remote.AutocryptInteractor.AutocryptState;
import timber.log.Timber;


public class AutocryptStatusProvider {
    private static final int EMAIL_STATUS = 101;

    private static final int AUTOCRYPT_STATUS = 201;
    private static final int AUTOCRYPT_STATUS_INTERNAL = 202;


    private final Context context;
    private final UriMatcher uriMatcher;
    private final ApiPermissionHelper apiPermissionHelper;

    private UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = "*";

        matcher.addURI(authority, KeychainExternalContract.BASE_EMAIL_STATUS, EMAIL_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS, AUTOCRYPT_STATUS);
        matcher.addURI(authority, KeychainExternalContract.BASE_AUTOCRYPT_STATUS + "/*", AUTOCRYPT_STATUS_INTERNAL);

        return matcher;
    }

    public AutocryptStatusProvider(Context context) {
        this.context = context;
        uriMatcher = buildUriMatcher();

        apiPermissionHelper = new ApiPermissionHelper(context);
    }

    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Timber.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        String callingPackageName = apiPermissionHelper.getCurrentCallingPackage();

        int match = uriMatcher.match(uri);
        switch (match) {
            case EMAIL_STATUS: {
                Toast.makeText(context, "This API is no longer supported by OpenKeychain!", Toast.LENGTH_SHORT).show();
                return new MatrixCursor(projection);
            }

            case AUTOCRYPT_STATUS_INTERNAL:

                // override package name to use any external
                 callingPackageName = uri.getLastPathSegment();

            case AUTOCRYPT_STATUS: {
                if (projection == null) {
                    throw new IllegalArgumentException("Please provide a projection!");
                }

                List<String> plist = Arrays.asList(projection);
                boolean isWildcardSelector = selectionArgs.length == 1 && selectionArgs[0].contains("%");
                boolean queriesUidResult = plist.contains(AutocryptStatus.UID_KEY_STATUS) ||
                        plist.contains(AutocryptStatus.UID_ADDRESS) ||
                        plist.contains(AutocryptStatus.UID_MASTER_KEY_ID) ||
                        plist.contains(AutocryptStatus.UID_CANDIDATES);
                boolean queriesAutocryptResult = plist.contains(AutocryptStatus.AUTOCRYPT_PEER_STATE) ||
                        plist.contains(AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID) ||
                        plist.contains(AutocryptStatus.AUTOCRYPT_KEY_STATUS);
                if (isWildcardSelector && queriesAutocryptResult) {
                    throw new UnsupportedOperationException("Cannot wildcard-query autocrypt results!");
                }

                Map<String, UidStatus> uidStatuses = queriesUidResult ?
                        loadUidStatusMap(selectionArgs, isWildcardSelector) : Collections.emptyMap();
                Map<String, AutocryptRecommendationResult> autocryptStates = queriesAutocryptResult ?
                        loadAutocryptRecommendationMap(selectionArgs, callingPackageName) : Collections.emptyMap();

                MatrixCursor cursor =
                        mapResultsToProjectedMatrixCursor(projection, selectionArgs, uidStatuses, autocryptStates);

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
    private MatrixCursor mapResultsToProjectedMatrixCursor(String[] projection, String[] selectionArgs,
            Map<String, UidStatus> uidStatuses, Map<String, AutocryptRecommendationResult> autocryptStates) {
        MatrixCursor cursor = new MatrixCursor(projection);
        for (String selectionArg : selectionArgs) {
            AutocryptRecommendationResult autocryptResult = autocryptStates.get(selectionArg);
            UidStatus uidStatus = uidStatuses.get(selectionArg);

            Object[] row = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                if (AutocryptStatus.ADDRESS.equals(projection[i]) || AutocryptStatus._ID.equals(projection[i])) {
                    row[i] = selectionArg;
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
                return uidStatus.keyStatus() == VerificationStatus.VERIFIED_SECRET ?
                        KeychainExternalContract.KEY_STATUS_VERIFIED :
                        KeychainExternalContract.KEY_STATUS_UNVERIFIED;
            }
            case AutocryptStatus.UID_ADDRESS:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.user_id();

            case AutocryptStatus.UID_MASTER_KEY_ID:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.master_key_id();

            case AutocryptStatus.UID_CANDIDATES:
                if (uidStatus == null) {
                    return null;
                }
                return uidStatus.candidates();

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
                        KeychainExternalContract.KEY_STATUS_VERIFIED : KeychainExternalContract.KEY_STATUS_UNVERIFIED;

            case AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID:
                if (autocryptResult == null) {
                    return null;
                }
                return autocryptResult.masterKeyId;

            default:
                throw new IllegalArgumentException("Unhandled case " + columnName);
        }
    }

    private Map<String, UidStatus> loadUidStatusMap(String[] selectionArgs, boolean isWildcardSelector) {
        UserIdDao userIdDao = UserIdDao.getInstance(context);
        if (isWildcardSelector) {
            UidStatus uidStatus = userIdDao.getUidStatusByEmailLike(selectionArgs[0]);
            return Collections.singletonMap(selectionArgs[0], uidStatus);
        } else {
            return userIdDao.getUidStatusByEmail(selectionArgs);
        }
    }

    private Map<String, AutocryptRecommendationResult> loadAutocryptRecommendationMap(
            String[] selectionArgs, String callingPackageName) {
        AutocryptInteractor autocryptInteractor = AutocryptInteractor.getInstance(context, callingPackageName);
        return autocryptInteractor.determineAutocryptRecommendations(selectionArgs);
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
}
