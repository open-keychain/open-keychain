/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class OtherHelper {

    /**
     * Return the number if days between two dates
     * 
     * @param first
     * @param second
     * @return number of days
     */
    public static long getNumDaysBetween(GregorianCalendar first, GregorianCalendar second) {
        GregorianCalendar tmp = new GregorianCalendar();
        tmp.setTime(first.getTime());
        long numDays = (second.getTimeInMillis() - first.getTimeInMillis()) / 1000 / 86400;
        tmp.add(Calendar.DAY_OF_MONTH, (int) numDays);
        while (tmp.before(second)) {
            tmp.add(Calendar.DAY_OF_MONTH, 1);
            ++numDays;
        }
        return numDays;
    }

    /**
     * Logs bundle content to debug for inspecting the content
     * 
     * @param bundle
     * @param bundleName
     */
    public static void logDebugBundle(Bundle bundle, String bundleName) {
        if (Constants.DEBUG) {
            if (bundle != null) {
                Set<String> ks = bundle.keySet();
                Iterator<String> iterator = ks.iterator();

                Log.d(Constants.TAG, "Bundle " + bundleName + ":");
                Log.d(Constants.TAG, "------------------------------");
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = bundle.get(key);

                    if (value != null) {
                        Log.d(Constants.TAG, key + " : " + value.toString());
                    } else {
                        Log.d(Constants.TAG, key + " : null");
                    }
                }
                Log.d(Constants.TAG, "------------------------------");
            } else {
                Log.d(Constants.TAG, "Bundle " + bundleName + ": null");
            }
        }
    }

    /**
     * Check if the calling package has the needed permission to invoke an intent with specific
     * restricted actions.
     * 
     * If pkgName is null, this will also deny the use of the given action
     * 
     * @param activity
     * @param pkgName
     * @param permName
     * @param action
     * @param restrictedActions
     */
    public static void checkPackagePermissionForActions(Activity activity, String pkgName,
            String permName, String action, String[] restrictedActions) {
        if (action != null) {
            PackageManager pkgManager = activity.getPackageManager();

            for (int i = 0; i < restrictedActions.length; i++) {
                if (restrictedActions[i].equals(action)) {

                    // TODO: currently always cancels! THis is the old API
                    // end activity
                    activity.setResult(Activity.RESULT_CANCELED, null);
                    activity.finish();

                    // if (pkgName != null
                    // && (pkgManager.checkPermission(permName, pkgName) ==
                    // PackageManager.PERMISSION_GRANTED || pkgName
                    // .equals(Constants.PACKAGE_NAME))) {
                    // Log.d(Constants.TAG, pkgName + " has permission " + permName + ". Action "
                    // + action + " was granted!");
                    // } else {
                    // String error = pkgName + " does NOT have permission " + permName
                    // + ". Action " + action + " was NOT granted!";
                    // Log.e(Constants.TAG, error);
                    // Toast.makeText(activity, activity.getString(R.string.errorMessage, error),
                    // Toast.LENGTH_LONG).show();
                    //
                    // // end activity
                    // activity.setResult(Activity.RESULT_CANCELED, null);
                    // activity.finish();
                    // }
                }
            }

        }
    }

    /**
     * Splits userId string into naming part and email part
     * 
     * @param userId
     * @return array with naming (0) and email (1)
     */
    public static String[] splitUserId(String userId) {
        String[] output = new String[2];

        String chunks[] = userId.split(" <", 2);
        userId = chunks[0];
        if (chunks.length > 1) {
            output[1] = "<" + chunks[1];
        }
        output[0] = userId;

        return output;
    }

}
