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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.remote.ui.dialog.KeyLoader.KeyInfo;
import org.sufficientlysecure.keychain.util.Log;


class RemoteDeduplicatePresenter implements LoaderCallbacks<List<KeyInfo>> {
    private final PackageManager packageManager;
    private final Context context;
    private final int loaderId;


    private AutocryptPeerDataAccessObject autocryptPeerDao;
    private String duplicateAddress;

    private RemoteDeduplicateView view;
    private Integer selectedItem;
    private List<KeyInfo> keyInfoData;


    RemoteDeduplicatePresenter(Context context, int loaderId) {
        this.context = context;

        packageManager = context.getPackageManager();

        this.loaderId = loaderId;
    }

    public void setView(RemoteDeduplicateView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, String duplicateAddress) {
        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        autocryptPeerDao = new AutocryptPeerDataAccessObject(context, packageName);

        this.duplicateAddress = duplicateAddress;
        view.setAddressText(duplicateAddress);
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
        return new KeyLoader(context, context.getContentResolver(), duplicateAddress);
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
            Log.e(Constants.TAG, "got click on select with no data…?");
            return;
        }
        if (selectedItem == null) {
            Log.e(Constants.TAG, "got click on select with no selection…?");
            return;
        }

        long masterKeyId = keyInfoData.get(selectedItem).getMasterKeyId();
        autocryptPeerDao.updateToSelectedState(duplicateAddress, masterKeyId);

        view.finish();
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

    interface RemoteDeduplicateView {
        void finish();
        void finishAsCancelled();

        void setAddressText(String text);
        void setTitleClientIcon(Drawable drawable);

        void setKeyListData(List<KeyInfo> data);
        void setActiveItem(Integer position);
        void setEnableSelectButton(boolean enabled);
    }
}
