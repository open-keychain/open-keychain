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


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeySignatures;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPacketsColumns;
import org.sufficientlysecure.keychain.KeychainDatabase.Tables;
import timber.log.Timber;


public class KeychainProvider extends ContentProvider {
    private static final int KEY_RING_KEYS = 201;
    private static final int KEY_RING_USER_IDS = 202;
    private static final int KEY_RING_PUBLIC = 203;
    private static final int KEY_RING_CERTS = 205;

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
         * list key_ring specifics
         *
         * <pre>
         * key_rings/_/keys
         * key_rings/_/user_ids
         * key_rings/_/public
         * key_rings/_/certs
         * </pre>
         */
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

    @Override
    public boolean onCreate() {
        mUriMatcher = buildUriMatcher();
        return true;
    }

    public KeychainDatabase getDb() {
        if(mKeychainDatabase == null) {
            mKeychainDatabase = KeychainDatabase.getInstance(getContext());
        }
        return mKeychainDatabase;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor query(
            @NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Timber.d("insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SupportSQLiteDatabase db = getDb().getWritableDatabase();

        Uri rowUri = null;
        Long keyId = null;
        try {
            final int match = mUriMatcher.match(uri);

            switch (match) {
                case KEY_RING_PUBLIC: {
                    db.insert(Tables.KEY_RINGS_PUBLIC, SQLiteDatabase.CONFLICT_FAIL, values);
                    keyId = values.getAsLong(Keys.MASTER_KEY_ID);
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
                uri = DatabaseNotifyManager.getNotifyUriMasterKeyId(keyId);
                rowUri = uri;
            }

        } catch (SQLiteConstraintException e) {
            Timber.d(e, "Constraint exception on insert! Entry already existing?");
        }

        return rowUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String additionalSelection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
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
