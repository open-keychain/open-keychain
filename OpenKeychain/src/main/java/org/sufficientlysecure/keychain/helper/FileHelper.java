/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class FileHelper {

    /**
     * Checks if external storage is mounted if file is located on external storage
     *
     * @param file
     * @return true if storage is mounted
     */
    public static boolean isStorageMounted(String file) {
        if (file.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Opens the preferred installed file manager on Android and shows a toast if no manager is
     * installed.
     *
     * @param activity
     * @param filename    default selected file, not supported by all file managers
     * @param mimeType    can be text/plain for example
     * @param requestCode requestCode used to identify the result coming back from file manager to
     *                    onActivityResult() in your activity
     */
    public static void openFile(Activity activity, String filename, String mimeType, int requestCode) {
        Intent intent = buildFileIntent(filename, mimeType);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(activity, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }
    }

    public static void openFile(Fragment fragment, String filename, String mimeType, int requestCode) {
        Intent intent = buildFileIntent(filename, mimeType);

        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(fragment.getActivity(), R.string.no_filemanager_installed,
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Opens the storage browser on Android 4.4 or later for opening a file
     * @param fragment
     * @param last default selected file
     * @param mimeType can be text/plain for example
     * @param requestCode used to identify the result coming back from storage browser onActivityResult() in your
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void openDocument(Fragment fragment, Uri last, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setData(last);
        intent.setType(mimeType);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Opens the storage browser on Android 4.4 or later for saving a file
     * @param fragment
     * @param last default selected file
     * @param mimeType can be text/plain for example
     * @param requestCode used to identify the result coming back from storage browser onActivityResult() in your
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void saveDocument(Fragment fragment, Uri last, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setData(last);
        intent.setType(mimeType);
        fragment.startActivityForResult(intent, requestCode);
    }

    private static Intent buildFileIntent(String filename, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType(mimeType);

        return intent;
    }

    /**
     * Get a file path from a Uri.
     * <p/>
     * from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/
     * afilechooser/utils/FileUtils.java
     *
     * @param context
     * @param uri
     * @return
     * @author paulburke
     */
    public static String getPath(Context context, Uri uri) {
        Log.d(Constants.TAG + " File -",
                "Authority: " + uri.getAuthority() + ", Fragment: " + uri.getFragment()
                        + ", Port: " + uri.getPort() + ", Query: " + uri.getQuery() + ", Scheme: "
                        + uri.getScheme() + ", Host: " + uri.getHost() + ", Segments: "
                        + uri.getPathSegments().toString());

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow("_data");
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                // Eat it
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}
