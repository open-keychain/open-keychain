package org.sufficientlysecure.keychain.remote.ui;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper;
import org.sufficientlysecure.keychain.remote.ApiPermissionHelper.WrongPackageCertificateException;
import org.sufficientlysecure.keychain.util.Log;


class RequestKeyPermissionPresenter {
    private final Context context;
    private final PackageManager packageManager;
    private final ApiDataAccessObject apiDataAccessObject;
    private final ApiPermissionHelper apiPermissionHelper;

    private RequestKeyPermissionMvpView view;

    private String packageName;
    private long masterKeyId;
    private ProviderHelper providerHelper;


    static RequestKeyPermissionPresenter createRequestKeyPermissionPresenter(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApiDataAccessObject apiDataAccessObject = new ApiDataAccessObject(context);
        ApiPermissionHelper apiPermissionHelper = new ApiPermissionHelper(context, apiDataAccessObject);
        ProviderHelper providerHelper = new ProviderHelper(context);

        return new RequestKeyPermissionPresenter(context, apiDataAccessObject, apiPermissionHelper, packageManager,
                providerHelper);
    }

    private RequestKeyPermissionPresenter(Context context, ApiDataAccessObject apiDataAccessObject,
            ApiPermissionHelper apiPermissionHelper, PackageManager packageManager, ProviderHelper providerHelper) {
        this.context = context;
        this.apiDataAccessObject = apiDataAccessObject;
        this.apiPermissionHelper = apiPermissionHelper;
        this.packageManager = packageManager;
        this.providerHelper = providerHelper;
    }

    void setView(RequestKeyPermissionMvpView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, long[] masterKeyIds) {
        checkPackageAllowed(packageName);

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Unable to find info of calling app!");
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
        CachedPublicKeyRing secretKeyRingOrPublicFallback = findSecretKeyRingOrPublicFallback(subKeyIds);

        if (secretKeyRingOrPublicFallback == null) {
            throw new PgpKeyNotFoundException("No key found among requested!");
        }

        this.masterKeyId = secretKeyRingOrPublicFallback.getMasterKeyId();

        UserId userId = secretKeyRingOrPublicFallback.getSplitPrimaryUserIdWithFallback();
        view.displayKeyInfo(userId);

        if (secretKeyRingOrPublicFallback.hasAnySecret()) {
            view.switchToLayoutRequestKeyChoice();
        } else {
            view.switchToLayoutNoSecret();
        }
    }

    @Nullable
    private CachedPublicKeyRing findSecretKeyRingOrPublicFallback(long[] subKeyIds) {
        CachedPublicKeyRing publicFallbackRing = null;
        for (long candidateSubKeyId : subKeyIds) {
            try {
                CachedPublicKeyRing cachedPublicKeyRing = providerHelper.getCachedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(candidateSubKeyId)
                );

                SecretKeyType secretKeyType = cachedPublicKeyRing.getSecretKeyType(candidateSubKeyId);
                if (secretKeyType.isUsable()) {
                    return cachedPublicKeyRing;
                }
                if (publicFallbackRing == null) {
                    publicFallbackRing = cachedPublicKeyRing;
                }
            } catch (PgpKeyNotFoundException | NotFoundException e) {
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
        apiDataAccessObject.addAllowedKeyIdForApp(packageName, masterKeyId);
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

        void displayKeyInfo(UserId userId);

        void finish();
        void finishAsCancelled();
    }
}
