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


import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactInfoLoader.SystemContactInfo;
import org.sufficientlysecure.keychain.util.ContactHelper;


public class SystemContactInfoLoader extends AsyncTaskLoader<SystemContactInfo> {
    private static final String[] PROJECTION = {
            ContactsContract.RawContacts.CONTACT_ID
    };
    private static final int INDEX_CONTACT_ID = 0;
    private static final String CONTACT_NOT_DELETED = "0";


    private final ContentResolver contentResolver;
    private final long masterKeyId;
    private final boolean isSecret;

    private SystemContactInfo cachedResult;


    public SystemContactInfoLoader(Context context, ContentResolver contentResolver, long masterKeyId, boolean isSecret) {
        super(context);

        this.contentResolver = contentResolver;
        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;
    }

    @Override
    public SystemContactInfo loadInBackground() {
        Uri baseUri = isSecret ? ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI :
                ContactsContract.RawContacts.CONTENT_URI;
        Cursor cursor = contentResolver.query(baseUri, PROJECTION,
                ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " +
                        ContactsContract.RawContacts.SOURCE_ID + " = ? AND " +
                        ContactsContract.RawContacts.DELETED + " = ?",
                new String[] {
                        Constants.ACCOUNT_TYPE,
                        Long.toString(masterKeyId),
                        CONTACT_NOT_DELETED
                }, null);

        if (cursor == null) {
            Log.e(Constants.TAG, "Error loading key items!");
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            long contactId = cursor.getLong(INDEX_CONTACT_ID);
            if (contactId == -1) {
                return null;
            }

            ContactHelper contactHelper = new ContactHelper(getContext());

            String contactName = null;
            if (isSecret) { //all secret keys are linked to "me" profile in contacts
                List<String> mainProfileNames = contactHelper.getMainProfileContactName();
                if (mainProfileNames != null && mainProfileNames.size() > 0) {
                    contactName = mainProfileNames.get(0);
                }
            } else {
                contactName = contactHelper.getContactName(contactId);
            }

            if (contactName == null) {
                return null;
            }

            Bitmap contactPicture;
            if (isSecret) {
                contactPicture = contactHelper.loadMainProfilePhoto(false);
            } else {
                contactPicture = contactHelper.loadPhotoByContactId(contactId, false);
            }

            return new SystemContactInfo(masterKeyId, contactId, contactName, contactPicture);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void deliverResult(SystemContactInfo systemContactInfo) {
        cachedResult = systemContactInfo;

        if (isStarted()) {
            super.deliverResult(systemContactInfo);
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

    public static class SystemContactInfo {
        final long masterKeyId;
        public final long contactId;
        public final String contactName;
        public final Bitmap contactPicture;

        SystemContactInfo(long masterKeyId, long contactId, String contactName, Bitmap contactPicture) {
            this.masterKeyId = masterKeyId;
            this.contactId = contactId;
            this.contactName = contactName;
            this.contactPicture = contactPicture;
        }
    }
}
