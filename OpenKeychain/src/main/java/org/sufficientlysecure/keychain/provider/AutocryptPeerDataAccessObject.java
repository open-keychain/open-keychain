/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeer;


public class AutocryptPeerDataAccessObject {
    private final SimpleContentResolverInterface mQueryInterface;
    private final String packageName;


    public AutocryptPeerDataAccessObject(Context context, String packageName) {
        this.packageName = packageName;

        final ContentResolver contentResolver = context.getContentResolver();
        mQueryInterface = new SimpleContentResolverInterface() {
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
        mQueryInterface = queryInterface;
        this.packageName = packageName;
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        Cursor cursor = mQueryInterface.query(
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
        Cursor cursor = mQueryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
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
        Cursor cursor = mQueryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
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

    public void updateToResetState(String autocryptId, Date effectiveDate) {
        updateAutocryptState(autocryptId, effectiveDate, null, ApiAutocryptPeer.RESET);
    }

    public void updateToSelectedState(String autocryptId, long masterKeyId) {
        updateAutocryptState(autocryptId, new Date(), masterKeyId, ApiAutocryptPeer.SELECTED);
    }

    public void updateToGossipState(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateAutocryptState(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.GOSSIP);
    }

    public void updateToMutualState(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateAutocryptState(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.MUTUAL);
    }

    public void updateToAvailableState(String autocryptId, Date effectiveDate, long masterKeyId) {
        updateAutocryptState(autocryptId, effectiveDate, masterKeyId, ApiAutocryptPeer.AVAILABLE);
    }

    private void updateAutocryptState(String autocryptId, Date date, Long masterKeyId, int status) {
        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.MASTER_KEY_ID, masterKeyId);
        cv.put(ApiAutocryptPeer.LAST_SEEN, date.getTime());
        if (masterKeyId != null) {
            cv.put(ApiAutocryptPeer.LAST_SEEN_KEY, masterKeyId);
        }
        cv.put(ApiAutocryptPeer.STATE, status);
        mQueryInterface.update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }

    public void delete(String autocryptId) {
        mQueryInterface.delete(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), null, null);
    }
}
