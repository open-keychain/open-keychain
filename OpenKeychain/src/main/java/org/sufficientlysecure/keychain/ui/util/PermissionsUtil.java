/*
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;

public class PermissionsUtil {

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 1;

    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     * <p/>
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise.
     * <p/>
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    @SuppressLint("NewApi") // Api level is checked in checkReadPermission
    public static boolean checkAndRequestReadPermission(Activity activity, Uri uri) {
        boolean result = checkReadPermission(activity, uri);
        if (!result) {
            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_READ_EXTERNAL_STORAGE);
        }
        return result;
    }

    public static boolean checkAndRequestReadPermission(Fragment fragment, Uri uri) {
        boolean result = checkReadPermission(fragment.getContext(), uri);
        if (!result) {
            fragment.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_READ_EXTERNAL_STORAGE);
        }
        return result;
    }

    private static boolean checkReadPermission(Context context, Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to:
        // https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }

    public static boolean checkReadPermissionResult(Context context,
                                                    int requestCode,
                                                    int[] grantResults) {

        if (requestCode != PERMISSION_READ_EXTERNAL_STORAGE) {
            return false;
        }

        boolean permissionWasGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionWasGranted) {
            return true;
        } else {
            Toast.makeText(context, R.string.error_denied_storage_permission, Toast.LENGTH_LONG)
                    .show();

            return false;
        }
    }

}
