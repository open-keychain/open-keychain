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

package org.sufficientlysecure.keychain.ui.transfer.loader;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader.SecretKeyItem;


public class SecretKeyLoader extends AsyncTaskLoader<List<SecretKeyItem>> {
    public static final String[] PROJECTION = new String[] {
            KeyRings.MASTER_KEY_ID,
            KeyRings.CREATION,
            KeyRings.NAME,
            KeyRings.EMAIL,
            KeyRings.HAS_ANY_SECRET
    };
    private static final int INDEX_KEY_ID = 0;
    private static final int INDEX_CREATION = 1;
    private static final int INDEX_NAME = 2;
    private static final int INDEX_EMAIL = 3;


    private final ContentResolver contentResolver;

    private List<SecretKeyItem> cachedResult;


    public SecretKeyLoader(Context context, ContentResolver contentResolver) {
        super(context);

        this.contentResolver = contentResolver;
    }

    @Override
    public List<SecretKeyItem> loadInBackground() {
        String where = KeyRings.HAS_ANY_SECRET + " = 1";
        Cursor cursor = contentResolver.query(KeyRings.buildUnifiedKeyRingsUri(), PROJECTION, where, null, null);
        if (cursor == null) {
            Log.e(Constants.TAG, "Error loading key items!");
            return null;
        }

        try {
            ArrayList<SecretKeyItem> secretKeyItems = new ArrayList<>();
            while (cursor.moveToNext()) {
                SecretKeyItem secretKeyItem = new SecretKeyItem(cursor);
                secretKeyItems.add(secretKeyItem);
            }

            return Collections.unmodifiableList(secretKeyItems);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void deliverResult(List<SecretKeyItem> keySubkeyStatus) {
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

    public static class SecretKeyItem {
        final int position;
        public final long masterKeyId;
        public final long creationMillis;
        public final String name;
        public final String email;

        SecretKeyItem(Cursor cursor) {
            position = cursor.getPosition();

            masterKeyId = cursor.getLong(INDEX_KEY_ID);
            creationMillis = cursor.getLong(INDEX_CREATION) * 1000;

            name = cursor.getString(INDEX_NAME);
            email = cursor.getString(INDEX_EMAIL);
        }
    }
}
