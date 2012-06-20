package org.thialfihar.android.apg.helper;

import org.thialfihar.android.apg.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

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
     * @param filename
     *            default selected file, not supported by all file managers
     * @param type
     *            can be text/plain for example
     * @param requestCode
     *            requestCode used to identify the result coming back from file manager to
     *            onActivityResult() in your activity
     */
    public static void openFile(Activity activity, String filename, String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType(type);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(activity, R.string.noFilemanagerInstalled, Toast.LENGTH_SHORT).show();
        }
    }
}
