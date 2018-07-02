package org.sufficientlysecure.keychain.livedata;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData.ListedApp;
import org.sufficientlysecure.keychain.model.ApiApp;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;


public class ApiAppsLiveData extends AsyncTaskLiveData<List<ListedApp>> {
    private final ApiAppDao apiAppDao;
    private final PackageManager packageManager;

    public ApiAppsLiveData(Context context) {
        super(context, DatabaseNotifyManager.getNotifyUriAllApps());

        packageManager = getContext().getPackageManager();
        apiAppDao = ApiAppDao.getInstance(context);
    }

    @Override
    protected List<ListedApp> asyncLoadData() {
        ArrayList<ListedApp> result = new ArrayList<>();

        loadRegisteredApps(result);
        addPlaceholderApps(result);

        Collections.sort(result, (o1, o2) -> o1.readableName.compareTo(o2.readableName));
        return result;
    }

    private void loadRegisteredApps(ArrayList<ListedApp> result) {
        List<ApiApp> registeredApiApps = apiAppDao.getAllApiApps();

        for (ApiApp apiApp : registeredApiApps) {
            ListedApp listedApp;
            try {
                ApplicationInfo ai = packageManager.getApplicationInfo(apiApp.package_name(), 0);
                CharSequence applicationLabel = packageManager.getApplicationLabel(ai);
                Drawable applicationIcon = packageManager.getApplicationIcon(ai);

                listedApp = new ListedApp(apiApp.package_name(), true, true, applicationLabel, applicationIcon, null);
            } catch (PackageManager.NameNotFoundException e) {
                listedApp = new ListedApp(apiApp.package_name(), false, true, apiApp.package_name(), null, null);
            }
            result.add(listedApp);
        }
    }

    private void addPlaceholderApps(ArrayList<ListedApp> result) {
        for (ListedApp placeholderApp : PLACERHOLDER_APPS) {
            if (!containsByPackageName(result, placeholderApp.packageName)) {
                try {
                    packageManager.getApplicationInfo(placeholderApp.packageName, 0);
                    result.add(placeholderApp.withIsInstalled());
                } catch (PackageManager.NameNotFoundException e) {
                    result.add(placeholderApp);
                }
            }
        }
    }

    private boolean containsByPackageName(ArrayList<ListedApp> result, String packageName) {
        for (ListedApp app : result) {
            if (packageName.equals(app.packageName)) {
                return true;
            }
        }
        return false;
    }


    public static class ListedApp {
        public final String packageName;
        public final boolean isInstalled;
        public final boolean isRegistered;
        public final String readableName;
        public final Drawable applicationIcon;
        public final Integer applicationIconRes;

        ListedApp(String packageName, boolean isInstalled, boolean isRegistered, CharSequence readableName,
                Drawable applicationIcon, Integer applicationIconRes) {
            this.packageName = packageName;
            this.isInstalled = isInstalled;
            this.isRegistered = isRegistered;
            this.readableName = readableName.toString();
            this.applicationIcon = applicationIcon;
            this.applicationIconRes = applicationIconRes;
        }

        public ListedApp withIsInstalled() {
            return new ListedApp(packageName, true, isRegistered, readableName, applicationIcon, applicationIconRes);
        }
    }

    private static final ListedApp[] PLACERHOLDER_APPS = {
            new ListedApp("com.fsck.k9", false, false, "K-9 Mail", null, R.drawable.apps_k9),
            new ListedApp("com.zeapo.pwdstore", false, false, "Password Store", null, R.drawable.apps_password_store),
            new ListedApp("eu.siacs.conversations", false, false, "Conversations (Instant Messaging)", null,
                    R.drawable.apps_conversations)
    };
}
