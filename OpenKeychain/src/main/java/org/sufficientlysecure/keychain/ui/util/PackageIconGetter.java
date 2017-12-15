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
