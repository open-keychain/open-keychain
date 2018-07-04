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

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectAuthenticationKeyActivity.SelectAuthKeyViewModel;
import timber.log.Timber;


class RemoteSelectAuthenticationKeyPresenter {
    private final PackageManager packageManager;
    private final LifecycleOwner lifecycleOwner;
    private final Context context;


    private RemoteSelectAuthenticationKeyView view;
    private Integer selectedItem;
    private List<UnifiedKeyInfo> keyInfoData;


    RemoteSelectAuthenticationKeyPresenter(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;

        packageManager = context.getPackageManager();
    }

    public void setView(RemoteSelectAuthenticationKeyView view) {
        this.view = view;
    }

    void setupFromViewModel(SelectAuthKeyViewModel viewModel) {
        try {
            setPackageInfo(viewModel.getPackageName());
        } catch (NameNotFoundException e) {
            Timber.e("Unable to find info of calling app!");
            view.finishAsCancelled();
        }

        viewModel.getKeyInfoLiveData(context).observe(lifecycleOwner, this::onLoadKeyInfos);
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);

        view.setTitleClientIcon(appIcon);
    }

    private void onLoadKeyInfos(List<UnifiedKeyInfo> data) {
        this.keyInfoData = data;
        view.setKeyListData(data);
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

        long masterKeyId = keyInfoData.get(selectedItem).master_key_id();
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

        void setKeyListData(List<UnifiedKeyInfo> data);
        void setActiveItem(Integer position);
        void setEnableSelectButton(boolean enabled);
    }
}
