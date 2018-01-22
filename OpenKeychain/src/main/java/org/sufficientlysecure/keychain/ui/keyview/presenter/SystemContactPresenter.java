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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactInfoLoader;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactInfoLoader.SystemContactInfo;
import timber.log.Timber;


public class SystemContactPresenter implements LoaderCallbacks<SystemContactInfo> {
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
            Timber.w("loading linked system contact not possible READ_CONTACTS permission denied!");
            view.hideLinkedSystemContact();
            return;
        }

        Bundle linkedContactData = new Bundle();

        // initialises loader for contact query so we can listen to any updates
        loaderManager.restartLoader(loaderId, linkedContactData, this);
    }

    @Override
    public Loader<SystemContactInfo> onCreateLoader(int id, Bundle args) {
        return new SystemContactInfoLoader(context, context.getContentResolver(), masterKeyId, isSecret);
    }

    @Override
    public void onLoadFinished(Loader<SystemContactInfo> loader, SystemContactInfo data) {
        if (data == null) {
            view.hideLinkedSystemContact();
            return;
        }

        this.contactId = data.contactId;
        view.showLinkedSystemContact(data.contactName, data.contactPicture);
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void onSystemContactClick() {
        launchAndroidContactActivity(contactId, context);
    }

    public interface SystemContactMvpView {
        void setSystemContactClickListener(SystemContactClickListener systemContactClickListener);

        void showLinkedSystemContact(String contactName, Bitmap picture);
        void hideLinkedSystemContact();
    }

    public interface SystemContactClickListener {
        void onSystemContactClick();
    }

    private static void launchAndroidContactActivity(long contactId, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
        intent.setData(uri);
        context.startActivity(intent);
    }
}
