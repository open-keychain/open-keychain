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

import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeySelector;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import timber.log.Timber;


class RemoteSelectIdentityKeyPresenter {
    private final PackageManager packageManager;
    private final Context context;
    private final RemoteSelectIdViewModel viewModel;


    private UserId userId;

    private RemoteSelectIdentityKeyView view;
    private List<KeyInfo> keyInfoData;
    private long masterKeyId;


    RemoteSelectIdentityKeyPresenter(Context context, RemoteSelectIdViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.viewModel = viewModel;

        packageManager = context.getPackageManager();

        viewModel.getKeyGenerationLiveData(context).observe(lifecycleOwner, this::onChangeKeyGeneration);
        viewModel.getKeyInfo(context).observe(lifecycleOwner, this::onChangeKeyInfoData);
    }

    public void setView(RemoteSelectIdentityKeyView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, String rawUserId) {
        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Timber.e(e, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        this.userId = OpenPgpUtils.splitUserId(rawUserId);
        view.setAddressText(userId.email);

        viewModel.getKeyInfo(context).setKeySelector(KeySelector.createOnlySecret(
                KeyRings.buildUnifiedKeyRingsFindByUserIdUri(userId.email), null));
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appLabel = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIconAndName(appIcon, appLabel);
    }

    private void onChangeKeyInfoData(List<KeyInfo> data) {
        keyInfoData = data;
        goToSelectLayout();
    }

    private void goToSelectLayout() {
        if (keyInfoData == null) {
            view.showLayoutEmpty();
        } else if (keyInfoData.isEmpty()) {
            view.showLayoutSelectNoKeys();
        } else {
            view.setKeyListData(keyInfoData);
            view.showLayoutSelectKeyList();
        }
    }

    private void onChangeKeyGeneration(PgpEditKeyResult pgpEditKeyResult) {
        viewModel.getKeyGenerationLiveData(context).setSaveKeyringParcel(null);
        if (pgpEditKeyResult == null) {
            return;
        }

        view.showLayoutGenerateOk();
    }

    void onDialogCancel() {
        view.finishAsCancelled();
    }

    void onClickKeyListOther() {
        view.showLayoutImportExplanation();
    }

    void onClickKeyListCancel() {
        view.finishAndReturn(Constants.key.none);
    }

    void onClickNoKeysGenerate() {
        view.showLayoutGenerateProgress();

        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
        Constants.addDefaultSubkeys(builder);
        builder.addUserId(userId.email);

        viewModel.getKeyGenerationLiveData(context).setSaveKeyringParcel(builder.build());
    }

    void onClickNoKeysExisting() {
        view.showLayoutImportExplanation();
    }

    void onClickNoKeysCancel() {
        view.finishAndReturn(Constants.key.none);
    }

    void onKeyItemClick(int position) {
        viewModel.selectedMasterKeyId = keyInfoData.get(position).getMasterKeyId();
        view.highlightKey(position);
    }

    void onClickExplanationBack() {
        goToSelectLayout();
    }

    void onClickExplanationGotIt() {
        view.finishAsCancelled();
    }

    void onClickGenerateOkBack() {
        view.showLayoutSelectNoKeys();
    }

    void onClickGenerateOkFinish() {
        // saveKey
        // view.finishAndReturn
    }

    void onHighlightFinished() {
        view.finishAndReturn(viewModel.selectedMasterKeyId);
    }

    interface RemoteSelectIdentityKeyView {
        void finishAndReturn(long masterKeyId);
        void finishAsCancelled();

        void setAddressText(String text);
        void setTitleClientIconAndName(Drawable drawable, CharSequence name);

        void showLayoutEmpty();
        void showLayoutSelectNoKeys();
        void showLayoutSelectKeyList();
        void showLayoutImportExplanation();
        void showLayoutGenerateProgress();
        void showLayoutGenerateOk();

        void setKeyListData(List<KeyInfo> data);

        void highlightKey(int position);
    }
}
