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

package org.sufficientlysecure.keychain.util;


import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class OrientationUtils {
    public static void lockCurrentOrientation(Activity activity) {
        // locking orientation for non-fullscreen activities is restricted in *exactly* Android 8.0.
        // see https://github.com/aosp-mirror/platform_frameworks_base/commit/39791594560b2326625b663ed6796882900c220f#diff-960c6fdd4a4b336d98b785268b2a78ff
        // But apparently, they changed their mind in 8.1 again
        // and https://github.com/aosp-mirror/platform_frameworks_base/commit/d4ecffae67fa0dea03c581ca26f76b87a14be763#diff-960c6fdd4a4b336d98b785268b2a78ff
        if (VERSION.SDK_INT == VERSION_CODES.O) {
            return;
        }
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
                break;
            }
        }
        activity.setRequestedOrientation(orientation);
    }
}