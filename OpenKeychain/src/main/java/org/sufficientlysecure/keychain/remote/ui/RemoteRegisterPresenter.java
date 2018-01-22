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

package org.sufficientlysecure.keychain.remote.ui;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.remote.AppSettings;
import timber.log.Timber;


class RemoteRegisterPresenter {
    private final ApiDataAccessObject apiDao;
    private final PackageManager packageManager;
    private final Context context;


    private RemoteRegisterView view;
    private Intent resultData;
    private AppSettings appSettings;


    RemoteRegisterPresenter(Context context) {
        this.context = context;

        apiDao = new ApiDataAccessObject(context);
        packageManager = context.getPackageManager();
    }

    public void setView(RemoteRegisterView view) {
        this.view = view;
    }

    void setupFromIntentData(Intent resultData, String packageName, byte[] packageSignature) {
        this.appSettings = new AppSettings(packageName, packageSignature);
        this.resultData = resultData;

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Timber.e("Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
        view.setTitleText(context.getString(R.string.api_register_text, appName));
    }

    void onClickAllow() {
        apiDao.insertApiApp(appSettings);
        view.finishWithResult(resultData);
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    void onCancel() {
        view.finishAsCancelled();
    }

    interface RemoteRegisterView {
        void finishWithResult(Intent resultData);
        void finishAsCancelled();

        void setTitleText(String text);
        void setTitleClientIcon(Drawable drawable);
    }
}

