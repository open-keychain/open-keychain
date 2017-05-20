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

package org.sufficientlysecure.keychain.ui.widget;


import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;


public class SystemContactPresenter implements LoaderCallbacks<Cursor> {
    private static final String[] RAW_CONTACT_PROJECTION = {
            ContactsContract.RawContacts.CONTACT_ID
    };
    private static final int INDEX_CONTACT_ID = 0;


    private final Context context;
    private final SystemContactMvpView view;
    private final int loaderId;

    private final long masterKeyId;
    private final boolean isSecret;

    private long contactId;


    public SystemContactPresenter(Context context, SystemContactMvpView view, int loaderId, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        view.setSystemContactClickListener(new SystemContactClickListener() {
            @Override
            public void onSystemContactClick() {
                SystemContactPresenter.this.onSystemContactClick();
            }
        });
    }

    public void startLoader(LoaderManager loaderManager) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_DENIED) {
            Log.w(Constants.TAG, "loading linked system contact not possible READ_CONTACTS permission denied!");
            view.hideLinkedSystemContact();
            return;
        }

        Bundle linkedContactData = new Bundle();

        // initialises loader for contact query so we can listen to any updates
        loaderManager.restartLoader(loaderId, linkedContactData, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = isSecret ? ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI :
                ContactsContract.RawContacts.CONTENT_URI;

        return new CursorLoader(context, baseUri, RAW_CONTACT_PROJECTION,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.SOURCE_ID + "=? AND " +
                        ContactsContract.RawContacts.DELETED + "=?",
                new String[] {
                        Constants.ACCOUNT_TYPE,
                        Long.toString(masterKeyId),
                        "0" // "0" for "not deleted"
                },
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        long contactId = data.getLong(INDEX_CONTACT_ID);
        loadLinkedSystemContact(contactId);
    }

    private void loadLinkedSystemContact(final long contactId) {
        this.contactId = contactId;

        if (contactId == -1) {
            return;
        }

        ContactHelper contactHelper = new ContactHelper(context);

        String contactName = null;
        if (isSecret) { //all secret keys are linked to "me" profile in contacts
            List<String> mainProfileNames = contactHelper.getMainProfileContactName();
            if (mainProfileNames != null && mainProfileNames.size() > 0) {
                contactName = mainProfileNames.get(0);
            }
        } else {
            contactName = contactHelper.getContactName(contactId);
        }

        if (contactName != null) { //contact name exists for given master key
            Bitmap picture;
            if (isSecret) {
                picture = contactHelper.loadMainProfilePhoto(false);
            } else {
                picture = contactHelper.loadPhotoByContactId(contactId, false);
            }

            view.showLinkedSystemContact(contactName, picture);
        } else {
            view.hideLinkedSystemContact();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void onSystemContactClick() {
        launchAndroidContactActivity(contactId, context);
    }

    interface SystemContactMvpView {
        void setSystemContactClickListener(SystemContactClickListener systemContactClickListener);

        void showLinkedSystemContact(String contactName, Bitmap picture);
        void hideLinkedSystemContact();
    }

    interface SystemContactClickListener {
        void onSystemContactClick();
    }

    private static void launchAndroidContactActivity(long contactId, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
        intent.setData(uri);
        context.startActivity(intent);
    }
}
