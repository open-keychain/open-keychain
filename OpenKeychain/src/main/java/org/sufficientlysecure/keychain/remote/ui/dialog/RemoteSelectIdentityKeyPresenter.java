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
import java.util.List;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeySelector;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import timber.log.Timber;


class RemoteSelectIdentityKeyPresenter {
    private final PackageManager packageManager;
    private final Context context;
    private final RemoteSelectIdViewModel viewModel;


    private RemoteSelectIdentityKeyView view;
    private List<KeyInfo> keyInfoData;

    private UserId userId;
    private long selectedMasterKeyId;
    private byte[] generatedKeyData;
    private ApiDataAccessObject apiDao;
    private AppSettings appSettings;


    RemoteSelectIdentityKeyPresenter(Context context, RemoteSelectIdViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.viewModel = viewModel;
        this.apiDao = new ApiDataAccessObject(context);

        packageManager = context.getPackageManager();

        viewModel.getKeyGenerationLiveData(context).observe(lifecycleOwner, this::onChangeKeyGeneration);
        viewModel.getKeyInfo(context).observe(lifecycleOwner, this::onChangeKeyInfoData);
    }

    public void setView(RemoteSelectIdentityKeyView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, byte[] packageSignature, String rawUserId, boolean clientHasAutocryptSetupMsg) {
        try {
            setPackageInfo(packageName, packageSignature);
        } catch (NameNotFoundException e) {
            Timber.e(e, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        this.userId = OpenPgpUtils.splitUserId(rawUserId);
        view.setAddressText(userId.email);
        view.setShowAutocryptHint(clientHasAutocryptSetupMsg);

        loadKeyInfo();
    }

    private void loadKeyInfo() {
        Uri listedKeyRingUri = viewModel.isListAllKeys() ?
                KeyRings.buildUnifiedKeyRingsUri() : KeyRings.buildUnifiedKeyRingsFindByUserIdUri(userId.email);
        viewModel.getKeyInfo(context).setKeySelector(KeySelector.createOnlySecret(listedKeyRingUri, null));
    }

    private void setPackageInfo(String packageName, byte[] packageSignature) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appLabel = packageManager.getApplicationLabel(applicationInfo);

        appSettings = new AppSettings(packageName, packageSignature);

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
        selectedMasterKeyId = keyInfoData.get(position).getMasterKeyId();
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
        apiDao.insertApiApp(appSettings);
        apiDao.addAllowedKeyIdForApp(appSettings.getPackageName(), selectedMasterKeyId);
        view.finishAndReturn(selectedMasterKeyId);
    }

    void onImportOpSuccess(ImportKeyResult result) {
        long importedMasterKeyId = result.getImportedMasterKeyIds()[0];
        apiDao.insertApiApp(appSettings);
        apiDao.addAllowedKeyIdForApp(appSettings.getPackageName(), selectedMasterKeyId);
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
        loadKeyInfo();
        view.showLayoutSelectKeyList();
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

        void setKeyListData(List<KeyInfo> data);

        void highlightKey(int position);

        void launchImportOperation(ImportKeyringParcel importKeyringParcel);

        void showImportInternalError();

        void displayOverflowMenu();

        void showOpenKeychainIntent();
    }
}
