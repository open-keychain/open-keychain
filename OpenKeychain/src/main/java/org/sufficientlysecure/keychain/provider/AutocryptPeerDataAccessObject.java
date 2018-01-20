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


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
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

    public void updateKeyGossipFromAutocrypt(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateKeyGossip(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.GOSSIP_ORIGIN_AUTOCRYPT);
    }

    public void updateKeyGossipFromSignature(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateKeyGossip(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.GOSSIP_ORIGIN_SIGNATURE);
    }

    public void updateKeyGossipFromDedup(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateKeyGossip(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.GOSSIP_ORIGIN_DEDUP);
    }

    private void updateKeyGossip(String autocryptId, Date effectiveDate, long masterKeyId, int origin) {
        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.GOSSIP_MASTER_KEY_ID, masterKeyId);
        cv.put(ApiAutocryptPeer.GOSSIP_LAST_SEEN_KEY, effectiveDate.getTime());
        cv.put(ApiAutocryptPeer.GOSSIP_ORIGIN, origin);
        queryInterface
                .update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }

    public void delete(String autocryptId) {
        queryInterface.delete(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), null, null);
    }

    public List<AutocryptRecommendationResult> determineAutocryptRecommendations(String... autocryptIds) {
        List<AutocryptRecommendationResult> result = new ArrayList<>(autocryptIds.length);

        Cursor cursor = queryAutocryptPeerData(autocryptIds);
        try {
            while (cursor.moveToNext()) {
                AutocryptRecommendationResult peerResult = determineAutocryptRecommendation(cursor);
                result.add(peerResult);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /** Determines Autocrypt "ui-recommendation", according to spec.
     * See https://autocrypt.org/level1.html#recommendations-for-single-recipient-messages
     */
    private AutocryptRecommendationResult determineAutocryptRecommendation(Cursor cursor) {
        String peerId = cursor.getString(INDEX_IDENTIFIER);

        AutocryptRecommendationResult keyRecommendation = determineAutocryptKeyRecommendation(peerId, cursor);
        if (keyRecommendation != null) return keyRecommendation;

        AutocryptRecommendationResult gossipRecommendation = determineAutocryptGossipRecommendation(peerId, cursor);
        if (gossipRecommendation != null) return gossipRecommendation;

        return new AutocryptRecommendationResult(peerId, AutocryptState.DISABLE, null, false);
    }

    @Nullable
    private AutocryptRecommendationResult determineAutocryptKeyRecommendation(String peerId, Cursor cursor) {
        boolean hasKey = !cursor.isNull(INDEX_MASTER_KEY_ID);
        boolean isRevoked = cursor.getInt(INDEX_KEY_IS_REVOKED) != 0;
        boolean isExpired = cursor.getInt(INDEX_KEY_IS_EXPIRED) != 0;
        if (!hasKey || isRevoked || isExpired) {
            return null;
        }

        long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
        long lastSeen = cursor.getLong(INDEX_LAST_SEEN);
        long lastSeenKey = cursor.getLong(INDEX_LAST_SEEN_KEY);
        boolean isVerified = cursor.getInt(INDEX_KEY_IS_VERIFIED) != 0;
        if (lastSeenKey < (lastSeen - AUTOCRYPT_DISCOURAGE_THRESHOLD_MILLIS)) {
            return new AutocryptRecommendationResult(peerId, AutocryptState.DISCOURAGED_OLD, masterKeyId, isVerified);
        }

        boolean isMutual = cursor.getInt(INDEX_STATE) != 0;
        if (isMutual) {
            return new AutocryptRecommendationResult(peerId, AutocryptState.MUTUAL, masterKeyId, isVerified);
        } else {
            return new AutocryptRecommendationResult(peerId, AutocryptState.AVAILABLE, masterKeyId, isVerified);
        }
    }

    @Nullable
    private AutocryptRecommendationResult determineAutocryptGossipRecommendation(String peerId, Cursor cursor) {
        boolean gossipHasKey = !cursor.isNull(INDEX_GOSSIP_MASTER_KEY_ID);
        boolean gossipIsRevoked = cursor.getInt(INDEX_GOSSIP_KEY_IS_REVOKED) != 0;
        boolean gossipIsExpired = cursor.getInt(INDEX_GOSSIP_KEY_IS_EXPIRED) != 0;
        boolean isVerified = cursor.getInt(INDEX_GOSSIP_KEY_IS_VERIFIED) != 0;

        if (!gossipHasKey || gossipIsRevoked || gossipIsExpired) {
            return null;
        }

        long masterKeyId = cursor.getLong(INDEX_GOSSIP_MASTER_KEY_ID);
        return new AutocryptRecommendationResult(peerId, AutocryptState.DISCOURAGED_GOSSIP, masterKeyId, isVerified);
    }

    private Cursor queryAutocryptPeerData(String[] autocryptIds) {
        StringBuilder selection = new StringBuilder(ApiAutocryptPeer.IDENTIFIER + " IN (?");
        for (int i = 1; i < autocryptIds.length; i++) {
            selection.append(",?");
        }
        selection.append(")");

        return queryInterface.query(ApiAutocryptPeer.buildByPackageName(packageName),
                PROJECTION_AUTOCRYPT_QUERY, selection.toString(), autocryptIds, null);
    }

    public static class AutocryptRecommendationResult {
        public final String peerId;
        public final Long masterKeyId;
        public final AutocryptState autocryptState;
        public final boolean isVerified;

        AutocryptRecommendationResult(String peerId, AutocryptState autocryptState, Long masterKeyId,
                boolean isVerified) {
            this.peerId = peerId;
            this.autocryptState = autocryptState;
            this.masterKeyId = masterKeyId;
            this.isVerified = isVerified;
        }

    }

    public enum AutocryptState {
        DISABLE, DISCOURAGED_OLD, DISCOURAGED_GOSSIP, AVAILABLE, MUTUAL
    }
}
