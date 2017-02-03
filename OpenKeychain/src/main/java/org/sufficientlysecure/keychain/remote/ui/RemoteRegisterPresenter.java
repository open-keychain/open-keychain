package org.sufficientlysecure.keychain.remote.ui;


import android.content.Context;
import android.content.Intent;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.util.Log;


class RemoteRegisterPresenter {
    private final ApiDataAccessObject apiDao;

    private RemoteRegisterView view;
    private Intent resultData;
    AppSettings appSettings;

    RemoteRegisterPresenter(Context context) {
        apiDao = new ApiDataAccessObject(context);
    }

    public void setView(RemoteRegisterView view) {
        this.view = view;
    }

    void setupFromIntent(Intent resultData, String packageName, byte[] packageSignature) {
        this.appSettings = new AppSettings(packageName, packageSignature);
        this.resultData = resultData;

        Log.d(Constants.TAG, "ACTION_REGISTER packageName: " + packageName);
    }

    void onClickAllow() {
        apiDao.insertApiApp(appSettings);
        view.finishWithResult(resultData);
    }

    void onClickCancel() {
        view.finishAsCancelled();
    }

    interface RemoteRegisterView {
        void finishWithResult(Intent resultData);
        void finishAsCancelled();
    }
}

