package org.sufficientlysecure.keychain.ui.util;


import java.util.HashMap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;


public class PackageIconGetter {
    private final PackageManager packageManager;
    private final HashMap<String, Drawable> appIconCache = new HashMap<>();


    public static PackageIconGetter getInstance(Context context) {
        PackageManager pm = context.getPackageManager();

        return new PackageIconGetter(pm);
    }

    private PackageIconGetter(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    public Drawable getDrawableForPackageName(String packageName) {
        if (appIconCache.containsKey(packageName)) {
            return appIconCache.get(packageName);
        }

        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);

            Drawable appIcon = packageManager.getApplicationIcon(ai);
            appIconCache.put(packageName, appIcon);

            return appIcon;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

}
