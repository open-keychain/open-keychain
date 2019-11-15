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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.model.ApiApp;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectIdKeyActivity.RemoteSelectIdViewModel;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import timber.log.Timber;


class RemoteSelectIdentityKeyPresenter {
    private final PackageManager packageManager;
    private LifecycleOwner lifecycleOwner;
    private final Context context;

    private RemoteSelectIdentityKeyView view;
    private RemoteSelectIdViewModel viewModel;
    private List<UnifiedKeyInfo> keyInfoData;

    private UserId userId;
    private Long selectedMasterKeyId;
    private byte[] generatedKeyData;
    private ApiAppDao apiAppDao;
    private ApiApp apiApp;


    RemoteSelectIdentityKeyPresenter(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.apiAppDao = ApiAppDao.getInstance(context);

        packageManager = context.getPackageManager();
    }

    public void setView(RemoteSelectIdentityKeyView view) {
        this.view = view;
    }

    void setupFromViewModel(RemoteSelectIdViewModel viewModel) {
        this.viewModel = viewModel;

        try {
            setPackageInfo(viewModel.packageName, viewModel.packageSignature);
        } catch (NameNotFoundException e) {
            Timber.e(e, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        this.userId = OpenPgpUtils.splitUserId(viewModel.rawUserId);
        view.setAddressText(userId.email);
        view.setShowAutocryptHint(viewModel.clientHasAutocryptSetupMsg);

        viewModel.getKeyGenerationLiveData(context).observe(lifecycleOwner, this::onChangeKeyGeneration);
        viewModel.getSecretUnifiedKeyInfo(context).observe(lifecycleOwner, this::onChangeKeyInfoData);
    }

    private void setPackageInfo(String packageName, byte[] packageSignature) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appLabel = packageManager.getApplicationLabel(applicationInfo);

        apiApp = ApiApp.create(packageName, packageSignature);

        view.setTitleClientIconAndName(appIcon, appLabel);
    }

    private void onChangeKeyInfoData(List<UnifiedKeyInfo> data) {
        if (data == null) {
            return;
        }
        keyInfoData = data;
        goToSelectLayout();
    }

    private void goToSelectLayout() {
        List<UnifiedKeyInfo> filteredKeyInfoData = getFilteredKeyInfo(userId.email);

        if (filteredKeyInfoData == null) {
            view.showLayoutEmpty();
        } else if (filteredKeyInfoData.isEmpty()) {
            view.showLayoutSelectNoKeys();
        } else {
            view.setKeyListData(filteredKeyInfoData);
            view.showLayoutSelectKeyList();
        }
    }

    private List<UnifiedKeyInfo> getFilteredKeyInfo(String filterString) {
        if (viewModel.isListAllKeys() || TextUtils.isEmpty(filterString)) {
            return keyInfoData;
        }
        filterString = filterString.toLowerCase().trim();
        if (viewModel.filteredKeyInfo == null) {
            viewModel.filteredKeyInfo = new ArrayList<>();
            for (UnifiedKeyInfo unifiedKeyInfo : keyInfoData) {
                if (unifiedKeyInfo.uidSearchString().contains(filterString)) {
                    viewModel.filteredKeyInfo.add(unifiedKeyInfo);
                }
            }
        }
        return viewModel.filteredKeyInfo;
    }

    private void onChangeKeyGeneration(PgpEditKeyResult pgpEditKeyResult) {
        if (pgpEditKeyResult == null) {
            return;
        }

        try {
            UncachedKeyRing generatedRing = pgpEditKeyResult.getRing();
            this.generatedKeyData = generatedRing.getEncoded();
        } catch (IOException e) {
            throw new AssertionError("Newly generated key ring must be encodable!");
        }

        viewModel.getKeyGenerationLiveData(context).setSaveKeyringParcel(null);
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
        selectedMasterKeyId = getFilteredKeyInfo(userId.email.toLowerCase()).get(position).master_key_id();
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
        if (generatedKeyData == null) {
            return;
        }

        ImportKeyringParcel importKeyringParcel = ImportKeyringParcel.createFromBytes(generatedKeyData);
        generatedKeyData = null;

        view.launchImportOperation(importKeyringParcel);
        view.showLayoutGenerateSave();
    }

    void onHighlightFinished() {
        apiAppDao.insertApiApp(apiApp);
        apiAppDao.addAllowedKeyIdForApp(apiApp.package_name(), Objects.requireNonNull(selectedMasterKeyId));
        view.finishAndReturn(selectedMasterKeyId);
    }

    void onImportOpSuccess(ImportKeyResult result) {
        long importedMasterKeyId = result.getImportedMasterKeyIds()[0];
        apiAppDao.insertApiApp(apiApp);
        apiAppDao.addAllowedKeyIdForApp(apiApp.package_name(), importedMasterKeyId);
        view.finishAndReturn(importedMasterKeyId);
    }

    void onImportOpError() {
        view.showImportInternalError();
    }

    public void onClickOverflowMenu() {
        view.displayOverflowMenu();
    }

    public void onClickMenuListAllKeys() {
        viewModel.setListAllKeys(!viewModel.isListAllKeys());
        goToSelectLayout();
    }

    public void onClickGoToOpenKeychain() {
        view.showOpenKeychainIntent();
    }

    interface RemoteSelectIdentityKeyView {
        void finishAndReturn(long masterKeyId);
        void finishAsCancelled();

        void setAddressText(String text);
        void setTitleClientIconAndName(Drawable drawable, CharSequence name);
        void setShowAutocryptHint(boolean showAutocryptHint);

        void showLayoutEmpty();
        void showLayoutSelectNoKeys();
        void showLayoutSelectKeyList();
        void showLayoutImportExplanation();
        void showLayoutGenerateProgress();
        void showLayoutGenerateOk();
        void showLayoutGenerateSave();

        void setKeyListData(List<UnifiedKeyInfo> data);

        void highlightKey(int position);

        void launchImportOperation(ImportKeyringParcel importKeyringParcel);

        void showImportInternalError();

        void displayOverflowMenu();

        void showOpenKeychainIntent();
    }
}
