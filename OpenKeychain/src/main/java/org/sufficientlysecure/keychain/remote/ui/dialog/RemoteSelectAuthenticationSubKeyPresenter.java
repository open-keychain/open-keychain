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


import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectAuthenticationSubKeyActivity.SelectAuthSubKeyViewModel;
import timber.log.Timber;

import java.util.List;


class RemoteSelectAuthenticationSubKeyPresenter {
    private final PackageManager packageManager;
    private final LifecycleOwner lifecycleOwner;
    private final Context context;


    private RemoteSelectAuthenticationSubKeyView view;
    private Integer selectedItem;
    private List<SubKey> keyInfoData;


    RemoteSelectAuthenticationSubKeyPresenter(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;

        packageManager = context.getPackageManager();
    }

    public void setView(RemoteSelectAuthenticationSubKeyView view) {
        this.view = view;
    }

    void setupFromViewModel(SelectAuthSubKeyViewModel viewModel) {
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

    private void onLoadKeyInfos(List<SubKey> data) {
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

        long subKeyId = keyInfoData.get(selectedItem).key_id();
        view.finish(subKeyId);
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

    interface RemoteSelectAuthenticationSubKeyView {
        void finish(long authSubKeyId);
        void finishAsCancelled();

        void setTitleClientIcon(Drawable drawable);

        void setKeyListData(List<SubKey> data);
        void setActiveItem(Integer position);
        void setEnableSelectButton(boolean enabled);
    }
}
