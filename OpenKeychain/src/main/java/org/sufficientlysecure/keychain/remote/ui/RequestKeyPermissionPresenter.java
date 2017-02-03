package org.sufficientlysecure.keychain.remote.ui;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import com.android.annotations.VisibleForTesting;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper.WrongPackageCertificateException;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.util.Log;


class RequestKeyPermissionPresenter {
    private final Context context;
    private final RequestKeyPermissionMvpView view;
    private final PackageManager packageManager;
    private final ApiDataAccessObject apiDataAccessObject;
    private final ApiPermissionHelper apiPermissionHelper;


    private String packageName;
    private long masterKeyId;


    static RequestKeyPermissionPresenter createRequestKeyPermissionPresenter(Context context,
            RequestKeyPermissionMvpView view) {
        PackageManager packageManager = context.getPackageManager();
        ApiDataAccessObject apiDataAccessObject = new ApiDataAccessObject(context);
        ApiPermissionHelper apiPermissionHelper = new ApiPermissionHelper(context, apiDataAccessObject);

        return new RequestKeyPermissionPresenter(
                context, view, apiDataAccessObject, apiPermissionHelper, packageManager);
    }

    @VisibleForTesting
    RequestKeyPermissionPresenter(Context context, RequestKeyPermissionMvpView view,
            ApiDataAccessObject apiDataAccessObject, ApiPermissionHelper apiPermissionHelper,
            PackageManager packageManager) {
        this.context = context;
        this.view = view;
        this.apiDataAccessObject = apiDataAccessObject;
        this.apiPermissionHelper = apiPermissionHelper;
        this.packageManager = packageManager;
    }

    void setupFromIntentData(String packageName, long[] requestedMasterKeyIds) {
        checkPackageAllowed(packageName);

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }

        this.packageName = packageName;
        this.masterKeyId = requestedMasterKeyIds[0];
//        long masterKeyId = 4817915339785265755L;
        try {
            CachedPublicKeyRing cachedPublicKeyRing = new ProviderHelper(context).getCachedPublicKeyRing(masterKeyId);

            UserId userId = cachedPublicKeyRing.getSplitPrimaryUserIdWithFallback();
            view.displayKeyInfo(userId);

            if (cachedPublicKeyRing.hasAnySecret()) {
                view.switchToLayoutRequestKeyChoice();
            } else {
                view.switchToLayoutNoSecret();
            }
        } catch (PgpKeyNotFoundException e) {
            view.switchToLayoutUnknownKey();

        }
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        Drawable appIcon;
        CharSequence appName;

        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        appIcon = packageManager.getApplicationIcon(applicationInfo);
        appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
        view.setTitleText(context.getString(R.string.request_permission_title, appName));
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
        apiDataAccessObject.addAllowedKeyIdForApp(packageName, masterKeyId);
        view.finish();
    }

    void onClickDeny() {
        view.finishAsCancelled();
    }

    void onClickDisplayKey() {
        Intent intent = new Intent(context, ViewKeyActivity.class);
        intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
        view.startActivity(intent);
    }

    void onClickCancelDialog() {
        view.finishAsCancelled();
    }

    interface RequestKeyPermissionMvpView {
        void switchToLayoutRequestKeyChoice();
        void switchToLayoutUnknownKey();
        void switchToLayoutNoSecret();

        void setTitleText(String text);
        void setTitleClientIcon(Drawable drawable);

        void displayKeyInfo(UserId userId);

        void finish();
        void finishAsCancelled();
        void startActivity(Intent intent);
    }
}
