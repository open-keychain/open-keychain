/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.helper;

import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.content.Context;
import android.os.Bundle;

public class OtherHelper {

    /**
     * Gets input stream of raw resource
     * 
     * @param context
     *            current context
     * @param resourceID
     *            of html file to read
     * @return input stream of resource
     */
    public static InputStream getInputStreamFromResource(Context context, int resourceID) {
        InputStream raw = context.getResources().openRawResource(resourceID);
        return raw;
    }

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
     * Set actionbar without home button if called from another app
     * 
     * @param activity
     */
    public static void setActionBarBackButton(SherlockFragmentActivity activity) {
        // set actionbar without home button if called from another app
        final ActionBar actionBar = activity.getSupportActionBar();
        Log.d(Constants.TAG, "calling package (only set when using startActivityForResult)="
                + activity.getCallingPackage());
        if (activity.getCallingPackage() != null
                && activity.getCallingPackage().equals(Constants.PACKAGE_NAME)) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
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
