package org.sufficientlysecure.keychain.remote.ui;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.util.Log;


class RemoteRegisterPresenter {
    private final ApiDataAccessObject apiDao;
    private final PackageManager packageManager;
    private final Context context;


    private RemoteRegisterView view;
    private Intent resultData;
    private AppSettings appSettings;


    RemoteRegisterPresenter(Context context) {
        this.context = context;

        apiDao = new ApiDataAccessObject(context);
        packageManager = context.getPackageManager();
    }

    public void setView(RemoteRegisterView view) {
        this.view = view;
    }

    void setupFromIntentData(Intent resultData, String packageName, byte[] packageSignature) {
        this.appSettings = new AppSettings(packageName, packageSignature);
        this.resultData = resultData;

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Unable to find info of calling app!");
            view.finishAsCancelled();
            return;
        }
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
        view.setTitleText(context.getString(R.string.api_register_text, appName));
    }

    void onClickAllow() {
        apiDao.insertApiApp(appSettings);
        view.finishWithResult(resultData);
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    void onCancel() {
        view.finishAsCancelled();
    }

    interface RemoteRegisterView {
        void finishWithResult(Intent resultData);
        void finishAsCancelled();

        void setTitleText(String text);
        void setTitleClientIcon(Drawable drawable);
    }
}

