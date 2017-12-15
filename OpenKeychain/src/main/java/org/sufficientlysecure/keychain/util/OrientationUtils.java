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
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Static methods related to device orientation.
 */
public class OrientationUtils {

    /**
     * Locks the device window in landscape mode.
     */
    public static void lockOrientationLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * Locks the device window in portrait mode.
     */
    public static void lockOrientationPortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Locks the device window in actual screen mode.
     */
    public static void lockOrientation(Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
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

    /**
     * Unlocks the device window in user defined screen mode.
     */
    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

}