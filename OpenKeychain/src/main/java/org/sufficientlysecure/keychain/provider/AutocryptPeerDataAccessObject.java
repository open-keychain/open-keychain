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


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;

import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeer;


public class AutocryptPeerDataAccessObject {
    private static final long AUTOCRYPT_DISCOURAGE_THRESHOLD_MILLIS = 35 * DateUtils.DAY_IN_MILLIS;

    private static final String[] PROJECTION_AUTOCRYPT_QUERY = {
            ApiAutocryptPeer.IDENTIFIER,
            ApiAutocryptPeer.LAST_SEEN,
            ApiAutocryptPeer.MASTER_KEY_ID,
            ApiAutocryptPeer.LAST_SEEN_KEY,
            ApiAutocryptPeer.IS_MUTUAL,
            ApiAutocryptPeer.KEY_IS_REVOKED,
            ApiAutocryptPeer.KEY_IS_EXPIRED,
            ApiAutocryptPeer.KEY_IS_VERIFIED,
            ApiAutocryptPeer.GOSSIP_MASTER_KEY_ID,
            ApiAutocryptPeer.GOSSIP_LAST_SEEN_KEY,
            ApiAutocryptPeer.GOSSIP_KEY_IS_REVOKED,
            ApiAutocryptPeer.GOSSIP_KEY_IS_EXPIRED,
            ApiAutocryptPeer.GOSSIP_KEY_IS_VERIFIED,
    };
    private static final int INDEX_IDENTIFIER = 0;
    private static final int INDEX_LAST_SEEN = 1;
    private static final int INDEX_MASTER_KEY_ID = 2;
    private static final int INDEX_LAST_SEEN_KEY = 3;
    private static final int INDEX_STATE = 4;
    private static final int INDEX_KEY_IS_REVOKED = 5;
    private static final int INDEX_KEY_IS_EXPIRED = 6;
    private static final int INDEX_KEY_IS_VERIFIED = 7;
    private static final int INDEX_GOSSIP_MASTER_KEY_ID = 8;
    private static final int INDEX_GOSSIP_LAST_SEEN_KEY = 9;
    private static final int INDEX_GOSSIP_KEY_IS_REVOKED = 10;
    private static final int INDEX_GOSSIP_KEY_IS_EXPIRED = 11;
    private static final int INDEX_GOSSIP_KEY_IS_VERIFIED = 12;

    private final SimpleContentResolverInterface queryInterface;
    private final String packageName;


    public AutocryptPeerDataAccessObject(Context context, String packageName) {
        this.packageName = packageName;

        final ContentResolver contentResolver = context.getContentResolver();
        queryInterface = new SimpleContentResolverInterface() {
            @Override
            public Cursor query(Uri contentUri, String[] projection, String selection, String[] selectionArgs,
                    String sortOrder) {
                return contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
            }

            @Override
            public Uri insert(Uri contentUri, ContentValues values) {
                return contentResolver.insert(contentUri, values);
            }

            @Override
            public int update(Uri contentUri, ContentValues values, String where, String[] selectionArgs) {
                return contentResolver.update(contentUri, values, where, selectionArgs);
            }

            @Override
            public int delete(Uri contentUri, String where, String[] selectionArgs) {
                return contentResolver.delete(contentUri, where, selectionArgs);
            }
        };
    }

    public AutocryptPeerDataAccessObject(SimpleContentResolverInterface queryInterface, String packageName) {
        this.queryInterface = queryInterface;
        this.packageName = packageName;
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        Cursor cursor = queryInterface.query(
                ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int masterKeyIdColumn = cursor.getColumnIndex(ApiAutocryptPeer.MASTER_KEY_ID);
                return cursor.getLong(masterKeyIdColumn);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public Date getLastSeen(String autocryptId) {
        Cursor cursor = queryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
                null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                long lastUpdated = cursor.getColumnIndex(ApiAutocryptPeer.LAST_SEEN);
                return new Date(lastUpdated);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public Date getLastSeenKey(String autocryptId) {
        Cursor cursor = queryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
                null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                long lastUpdated = cursor.getColumnIndex(ApiAutocryptPeer.LAST_SEEN_KEY);
                return new Date(lastUpdated);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public Date getLastSeenGossip(String autocryptId) {
        Cursor cursor = queryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
                null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                long lastUpdated = cursor.getColumnIndex(ApiAutocryptPeer.GOSSIP_LAST_SEEN_KEY);
                return new Date(lastUpdated);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public void updateLastSeen(String autocryptId, Date date) {
        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.LAST_SEEN, date.getTime());
        queryInterface
                .update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }

    public void updateKey(String autocryptId, Date effectiveDate, long masterKeyId, boolean isMutual) {
        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.MASTER_KEY_ID, masterKeyId);
        cv.put(ApiAutocryptPeer.LAST_SEEN_KEY, effectiveDate.getTime());
        cv.put(ApiAutocryptPeer.IS_MUTUAL, isMutual ? 1 : 0);
        queryInterface
                .update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }

    public void updateKeyGossip(String autocryptId, Date effectiveDate, long masterKeyId) {
        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.GOSSIP_MASTER_KEY_ID, masterKeyId);
        cv.put(ApiAutocryptPeer.GOSSIP_LAST_SEEN_KEY, effectiveDate.getTime());
        queryInterface
                .update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }

    public void delete(String autocryptId) {
        queryInterface.delete(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), null, null);
    }

    public Map<String,AutocryptPeerStateResult> getAutocryptState(String... autocryptIds) {
                /*
Determine if encryption is possible
If there is no peers[to-addr], then set ui-recommendation to disable, and terminate.

For the purposes of the rest of this recommendation, if either public_key or gossip_key is revoked, expired, or otherwise known to be unusable for encryption, then treat that key as though it were null (not present).

If both public_key and gossip_key are null, then set ui-recommendation to disable and terminate.

Otherwise, we derive the recommendation using a two-phase algorithm. The first phase computes the preliminary-recommendation.

Preliminary Recommendation
If public_key is null, then set target-keys[to-addr] to gossip_key and set preliminary-recommendation to discourage and skip to the Deciding to Encrypt by Default.

Otherwise, set target-keys[to-addr] to public_key.

If autocrypt_timestamp is more than 35 days older than last_seen, set preliminary-recommendation to discourage.

Otherwise, set preliminary-recommendation to available.
                 */

        Map<String,AutocryptPeerStateResult> result = new HashMap<>();
        StringBuilder selection = new StringBuilder(ApiAutocryptPeer.IDENTIFIER + " IN (?");
        for (int i = 1; i < autocryptIds.length; i++) {
            selection.append(",?");
        }
        selection.append(")");

        Cursor cursor = queryInterface.query(ApiAutocryptPeer.buildByPackageName(packageName),
                PROJECTION_AUTOCRYPT_QUERY, selection.toString(), autocryptIds, null);
        try {
            while (cursor.moveToNext()) {
                String autocryptId = cursor.getString(INDEX_IDENTIFIER);

                boolean hasKey = !cursor.isNull(INDEX_MASTER_KEY_ID);
                boolean isRevoked = cursor.getInt(INDEX_KEY_IS_REVOKED) != 0;
                boolean isExpired = cursor.getInt(INDEX_KEY_IS_EXPIRED) != 0;
                if (hasKey && !isRevoked && !isExpired) {
                    long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                    long lastSeen = cursor.getLong(INDEX_LAST_SEEN);
                    long lastSeenKey = cursor.getLong(INDEX_LAST_SEEN_KEY);
                    boolean isVerified = cursor.getInt(INDEX_KEY_IS_VERIFIED) != 0;
                    if (lastSeenKey < (lastSeen - AUTOCRYPT_DISCOURAGE_THRESHOLD_MILLIS)) {
                        AutocryptPeerStateResult peerResult = new AutocryptPeerStateResult(
                                AutocryptState.DISCOURAGED_OLD, masterKeyId, isVerified);
                        result.put(autocryptId, peerResult);
                        continue;
                    }

                    boolean isMutual = cursor.getInt(INDEX_STATE) != 0;
                    if (isMutual) {
                        AutocryptPeerStateResult peerResult = new AutocryptPeerStateResult(
                                AutocryptState.MUTUAL, masterKeyId, isVerified);
                        result.put(autocryptId, peerResult);
                        continue;
                    } else {
                        AutocryptPeerStateResult peerResult = new AutocryptPeerStateResult(
                                AutocryptState.AVAILABLE, masterKeyId, isVerified);
                        result.put(autocryptId, peerResult);
                        continue;
                    }
                }

                boolean gossipHasKey = !cursor.isNull(INDEX_GOSSIP_MASTER_KEY_ID);
                boolean gossipIsRevoked = cursor.getInt(INDEX_GOSSIP_KEY_IS_REVOKED) != 0;
                boolean gossipIsExpired = cursor.getInt(INDEX_GOSSIP_KEY_IS_EXPIRED) != 0;
                boolean isVerified = cursor.getInt(INDEX_GOSSIP_KEY_IS_VERIFIED) != 0;
                if (gossipHasKey && !gossipIsRevoked && !gossipIsExpired) {
                    long masterKeyId = cursor.getLong(INDEX_GOSSIP_MASTER_KEY_ID);
                    AutocryptPeerStateResult peerResult =
                            new AutocryptPeerStateResult(
                                    AutocryptState.DISCOURAGED_GOSSIP, masterKeyId, isVerified);
                    result.put(autocryptId, peerResult);
                    continue;
                }

                AutocryptPeerStateResult peerResult = new AutocryptPeerStateResult(
                        AutocryptState.DISABLE, null, false);
                result.put(autocryptId, peerResult);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static class AutocryptPeerStateResult {
        public final Long masterKeyId;
        public final AutocryptState autocryptState;
        public final boolean isVerified;

        AutocryptPeerStateResult(AutocryptState autocryptState, Long masterKeyId, boolean isVerified) {
            this.autocryptState = autocryptState;
            this.masterKeyId = masterKeyId;
            this.isVerified = isVerified;
        }

    }

    public enum AutocryptState {
        DISABLE, DISCOURAGED_OLD, DISCOURAGED_GOSSIP, AVAILABLE, MUTUAL
    }
}
