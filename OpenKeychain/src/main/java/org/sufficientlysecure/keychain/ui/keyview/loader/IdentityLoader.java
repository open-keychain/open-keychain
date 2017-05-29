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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.IdentityInfo;


public class IdentityLoader extends AsyncTaskLoader<List<IdentityInfo>> {
    private static final String[] USER_PACKETS_PROJECTION = new String[]{
            UserPackets._ID,
            UserPackets.TYPE,
            UserPackets.USER_ID,
            UserPackets.ATTRIBUTE_DATA,
            UserPackets.RANK,
            UserPackets.VERIFIED,
            UserPackets.IS_PRIMARY,
            UserPackets.IS_REVOKED,
            UserPackets.NAME,
            UserPackets.EMAIL,
            UserPackets.COMMENT,
    };
    private static final int INDEX_ID = 0;
    private static final int INDEX_TYPE = 1;
    private static final int INDEX_USER_ID = 2;
    private static final int INDEX_ATTRIBUTE_DATA = 3;
    private static final int INDEX_RANK = 4;
    private static final int INDEX_VERIFIED = 5;
    private static final int INDEX_IS_PRIMARY = 6;
    private static final int INDEX_IS_REVOKED = 7;
    private static final int INDEX_NAME = 8;
    private static final int INDEX_EMAIL = 9;
    private static final int INDEX_COMMENT = 10;

    private static final String USER_IDS_WHERE = UserPackets.IS_REVOKED + " = 0";

    private final ContentResolver contentResolver;
    private final long masterKeyId;

    private List<IdentityInfo> cachedResult;


    public IdentityLoader(Context context, ContentResolver contentResolver, long masterKeyId) {
        super(context);

        this.contentResolver = contentResolver;
        this.masterKeyId = masterKeyId;
    }

    @Override
    public List<IdentityInfo> loadInBackground() {
        Cursor cursor = contentResolver.query(UserPackets.buildUserIdsUri(masterKeyId),
                USER_PACKETS_PROJECTION, USER_IDS_WHERE, null, null);
        if (cursor == null) {
            Log.e(Constants.TAG, "Error loading key items!");
            return null;
        }

        try {
            ArrayList<IdentityInfo> identities = new ArrayList<>();
            while (cursor.moveToNext()) {
                IdentityInfo identityInfo = new IdentityInfo(masterKeyId, cursor);
                identities.add(identityInfo);
            }

            return Collections.unmodifiableList(identities);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void deliverResult(List<IdentityInfo> keySubkeyStatus) {
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
    }

    public static class IdentityInfo {
        final int position;

        public final int verified;
        public final byte[] data;
        public final String name;
        public final String email;
        public final String comment;

        public boolean isPrimary;

        IdentityInfo(long masterKeyId, Cursor cursor) {
            position = cursor.getPosition();

            verified = cursor.getInt(INDEX_VERIFIED);
            if (cursor.isNull(INDEX_NAME)) {
                data = cursor.getBlob(INDEX_ATTRIBUTE_DATA);

                name = null;
                email = null;
                comment = null;
            } else {
                data = null;

                name = cursor.getString(INDEX_NAME);
                email = cursor.getString(INDEX_EMAIL);
                comment = cursor.getString(INDEX_COMMENT);
            }

            isPrimary = cursor.getInt(INDEX_IS_PRIMARY) != 0;
        }
    }
}
