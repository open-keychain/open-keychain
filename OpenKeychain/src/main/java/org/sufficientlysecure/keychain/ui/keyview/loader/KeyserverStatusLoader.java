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

package org.sufficientlysecure.keychain.ui.keyview.loader;


import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;
import org.sufficientlysecure.keychain.ui.keyview.loader.KeyserverStatusLoader.KeyserverStatus;


public class KeyserverStatusLoader extends AsyncTaskLoader<KeyserverStatus> {
    public static final String[] PROJECTION = new String[] {
            UpdatedKeys.LAST_UPDATED,
            UpdatedKeys.SEEN_ON_KEYSERVERS
    };
    private static final int INDEX_LAST_UPDATED = 0;
    private static final int INDEX_SEEN_ON_KEYSERVERS = 1;


    private final ContentResolver contentResolver;
    private final long masterKeyId;

    private KeyserverStatus cachedResult;

    private ForceLoadContentObserver keyserverStatusObserver;

    public KeyserverStatusLoader(Context context, ContentResolver contentResolver, long masterKeyId) {
        super(context);

        this.contentResolver = contentResolver;
        this.masterKeyId = masterKeyId;

        this.keyserverStatusObserver = new ForceLoadContentObserver();
    }

    @Override
    public KeyserverStatus loadInBackground() {
        Cursor cursor = contentResolver.query(UpdatedKeys.CONTENT_URI, PROJECTION,
                UpdatedKeys.MASTER_KEY_ID + " = ?", new String[] { Long.toString(masterKeyId) }, null);
        if (cursor == null) {
            Log.e(Constants.TAG, "Error loading key items!");
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                if (cursor.isNull(INDEX_SEEN_ON_KEYSERVERS) || cursor.isNull(INDEX_LAST_UPDATED)) {
                    return new KeyserverStatus(masterKeyId);
                }

                boolean isPublished = cursor.getInt(INDEX_SEEN_ON_KEYSERVERS) != 0;
                Date lastUpdated = new Date(cursor.getLong(INDEX_LAST_UPDATED) * 1000);

                return new KeyserverStatus(masterKeyId, isPublished, lastUpdated);
            }

            return new KeyserverStatus(masterKeyId);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void deliverResult(KeyserverStatus keySubkeyStatus) {
        cachedResult = keySubkeyStatus;

        if (isStarted()) {
            super.deliverResult(keySubkeyStatus);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cachedResult != null) {
            deliverResult(cachedResult);
        }

        if (takeContentChanged() || cachedResult == null) {
            forceLoad();
        }

        getContext().getContentResolver().registerContentObserver(
                KeyRings.buildGenericKeyRingUri(masterKeyId), true, keyserverStatusObserver);
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();

        getContext().getContentResolver().unregisterContentObserver(keyserverStatusObserver);
    }

    public static class KeyserverStatus {
        private final long masterKeyId;
        private final boolean isPublished;
        private final Date lastUpdated;

        KeyserverStatus(long masterKeyId, boolean isPublished, Date lastUpdated) {
            this.masterKeyId = masterKeyId;
            this.isPublished = isPublished;
            this.lastUpdated = lastUpdated;
        }

        KeyserverStatus(long masterKeyId) {
            this.masterKeyId = masterKeyId;
            this.isPublished = false;
            this.lastUpdated = null;
        }

        long getMasterKeyId() {
            return masterKeyId;
        }

        public boolean hasBeenUpdated() {
            return lastUpdated != null;
        }

        public boolean isPublished() {
            if (lastUpdated == null) {
                throw new IllegalStateException("Cannot get publication state if key has never been updated!");
            }
            return isPublished;
        }

        public Date getLastUpdated() {
            return lastUpdated;
        }
    }
}
