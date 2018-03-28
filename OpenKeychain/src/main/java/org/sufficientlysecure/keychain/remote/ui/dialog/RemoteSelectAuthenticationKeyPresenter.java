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

package org.sufficientlysecure.keychain.remote.ui.dialog;


import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeySelector;
import timber.log.Timber;


class RemoteSelectAuthenticationKeyPresenter implements LoaderCallbacks<List<KeyInfo>> {
    private final PackageManager packageManager;
    private final Context context;
    private final int loaderId;


    private RemoteSelectAuthenticationKeyView view;
    private Integer selectedItem;
    private List<KeyInfo> keyInfoData;


    RemoteSelectAuthenticationKeyPresenter(Context context, int loaderId) {
        this.context = context;

        packageManager = context.getPackageManager();

        this.loaderId = loaderId;
    }

    public void setView(RemoteSelectAuthenticationKeyView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName) {
        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Timber.e("Unable to find info of calling app!");
            view.finishAsCancelled();
        }
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);

        view.setTitleClientIcon(appIcon);
    }

    void startLoaders(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<List<KeyInfo>> onCreateLoader(int id, Bundle args) {
        String selection = KeyRings.HAS_AUTHENTICATE_SECRET + " != 0";
        KeySelector keySelector = KeySelector.create(
                KeyRings.buildUnifiedKeyRingsUri(), selection);
        return new KeyInfoLoader(context, context.getContentResolver(), keySelector);
    }

    @Override
    public void onLoadFinished(Loader<List<KeyInfo>> loader, List<KeyInfo> data) {
        this.keyInfoData = data;
        view.setKeyListData(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if (view != null) {
            view.setKeyListData(null);
        }
    }

    void onClickSelect() {
        if (keyInfoData == null) {
            Timber.e("got click on select with no data…?");
            return;
        }
        if (selectedItem == null) {
            Timber.e("got click on select with no selection…?");
            return;
        }

        long masterKeyId = keyInfoData.get(selectedItem).getMasterKeyId();
        view.finish(masterKeyId);
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    public void onCancel() {
        view.finishAsCancelled();
    }

    void onKeyItemClick(int position) {
        if (selectedItem != null && position == selectedItem) {
            selectedItem = null;
        } else {
            selectedItem = position;
        }
        view.setActiveItem(selectedItem);
        view.setEnableSelectButton(selectedItem != null);
    }

    interface RemoteSelectAuthenticationKeyView {
        void finish(long masterKeyId);
        void finishAsCancelled();

        void setTitleClientIcon(Drawable drawable);

        void setKeyListData(List<KeyInfo> data);
        void setActiveItem(Integer position);
        void setEnableSelectButton(boolean enabled);
    }
}
