package org.sufficientlysecure.keychain.ui.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;

/**
 * Created by Andrea on 19/06/2016.
 */
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
    public static boolean checkAndRequestReadPermission(Activity activity, Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_READ_EXTERNAL_STORAGE);

        return false;
    }

    public static boolean checkReadPermissionResult(Activity activity,
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
            Toast.makeText(activity, R.string.error_denied_storage_permission, Toast.LENGTH_LONG).show();

            return false;
        }
    }

}
