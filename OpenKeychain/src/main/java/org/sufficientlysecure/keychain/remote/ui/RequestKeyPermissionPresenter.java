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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper.WrongPackageCertificateException;
import timber.log.Timber;


class RequestKeyPermissionPresenter {
    private final Context context;
    private final PackageManager packageManager;
    private final ApiAppDao apiAppDao;
    private final ApiPermissionHelper apiPermissionHelper;

    private RequestKeyPermissionMvpView view;

    private String packageName;
    private long masterKeyId;
    private KeyRepository keyRepository;


    static RequestKeyPermissionPresenter createRequestKeyPermissionPresenter(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApiAppDao apiAppDao = ApiAppDao.getInstance(context);
        ApiPermissionHelper apiPermissionHelper = new ApiPermissionHelper(context, apiAppDao);
        KeyRepository keyRepository =
                KeyRepository.create(context);

        return new RequestKeyPermissionPresenter(context, apiAppDao, apiPermissionHelper, packageManager,
                keyRepository);
    }

    private RequestKeyPermissionPresenter(Context context, ApiAppDao apiAppDao,
            ApiPermissionHelper apiPermissionHelper, PackageManager packageManager, KeyRepository keyRepository) {
        this.context = context;
        this.apiAppDao = apiAppDao;
        this.apiPermissionHelper = apiPermissionHelper;
        this.packageManager = packageManager;
        this.keyRepository = keyRepository;
    }

    void setView(RequestKeyPermissionMvpView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, long[] masterKeyIds) {
        checkPackageAllowed(packageName);

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Timber.e("Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        try {
            setRequestedMasterKeyId(masterKeyIds);
        } catch (PgpKeyNotFoundException e) {
            view.finishAsCancelled();
        }
    }

    private void setRequestedMasterKeyId(long[] subKeyIds) throws PgpKeyNotFoundException {
        UnifiedKeyInfo secretKeyRingOrPublicFallback = findSecretKeyRingOrPublicFallback(subKeyIds);

        if (secretKeyRingOrPublicFallback == null) {
            throw new PgpKeyNotFoundException("No key found among requested!");
        }

        masterKeyId = secretKeyRingOrPublicFallback.master_key_id();
        view.displayKeyInfo(secretKeyRingOrPublicFallback.name());

        if (secretKeyRingOrPublicFallback.has_any_secret()) {
            view.switchToLayoutRequestKeyChoice();
        } else {
            view.switchToLayoutNoSecret();
        }
    }

    @Nullable
    private UnifiedKeyInfo findSecretKeyRingOrPublicFallback(long[] subKeyIds) {
        UnifiedKeyInfo publicFallbackRing = null;
        for (long candidateSubKeyId : subKeyIds) {
            try {
                Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(candidateSubKeyId);
                if (masterKeyId == null) {
                    continue;
                }
                UnifiedKeyInfo unifiedKeyInfo = keyRepository.getUnifiedKeyInfo(masterKeyId);

                SecretKeyType secretKeyType = keyRepository.getSecretKeyType(candidateSubKeyId);
                if (secretKeyType.isUsable()) {
                    return unifiedKeyInfo;
                }
                if (publicFallbackRing == null) {
                    publicFallbackRing = unifiedKeyInfo;
                }
            } catch (NotFoundException e) {
                // no matter
            }
        }
        return publicFallbackRing;
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        this.packageName = packageName;

        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
        view.setTitleText(context.getString(R.string.request_permission_msg, appName));
    }

    private void checkPackageAllowed(String packageName) {
        boolean packageAllowed;
        try {
            packageAllowed = apiPermissionHelper.isPackageAllowed(packageName);
        } catch (WrongPackageCertificateException e) {
            packageAllowed = false;
        }
        if (!packageAllowed) {
            throw new IllegalStateException("Pending intent launched by unknown app!");
        }
    }

    void onClickAllow() {
        apiAppDao.addAllowedKeyIdForApp(packageName, masterKeyId);
        view.finish();
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    void onCancel() {
        view.finishAsCancelled();
    }

    interface RequestKeyPermissionMvpView {
        void switchToLayoutRequestKeyChoice();
        void switchToLayoutNoSecret();

        void setTitleText(String text);
        void setTitleClientIcon(Drawable drawable);

        void displayKeyInfo(String userIdName);

        void finish();
        void finishAsCancelled();
    }
}
