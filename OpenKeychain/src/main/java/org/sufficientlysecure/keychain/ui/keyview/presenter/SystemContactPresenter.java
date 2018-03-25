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


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;

import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao.SystemContactInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.ViewKeyLiveData.SystemContactInfoLiveData;


public class SystemContactPresenter implements Observer<SystemContactInfo> {
    private final Context context;
    private final SystemContactMvpView view;

    private final long masterKeyId;
    private final boolean isSecret;

    private long contactId;


    public SystemContactPresenter(Context context, SystemContactMvpView view, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        view.setSystemContactClickListener(SystemContactPresenter.this::onSystemContactClick);
    }

    public LiveData<SystemContactInfo> getLiveDataInstance() {
        return new SystemContactInfoLiveData(context, masterKeyId, isSecret);
    }

    @Override
    public void onChanged(@Nullable SystemContactInfo systemContactInfo) {
        if (systemContactInfo == null) {
            view.hideLinkedSystemContact();
            return;
        }

        this.contactId = systemContactInfo.contactId;
        view.showLinkedSystemContact(systemContactInfo.contactName, systemContactInfo.contactPicture);
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
