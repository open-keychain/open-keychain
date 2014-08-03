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
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;

import java.io.File;
import java.text.DecimalFormat;

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
     * @param fragment
     * @param last        default selected Uri, not supported by all file managers
     * @param mimeType    can be text/plain for example
     * @param requestCode requestCode used to identify the result coming back from file manager to
     *                    onActivityResult() in your activity
     */
    public static void openFile(Fragment fragment, Uri last, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(last);
        intent.setType(mimeType);

        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(fragment.getActivity(), R.string.no_filemanager_installed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void saveFile(final FileDialogCallback callback, final FragmentManager fragmentManager,
                                final String title, final String message, final File defaultFile,
                                final String checkMsg) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    callback.onFileSelected(
                            new File(message.getData().getString(FileDialogFragment.MESSAGE_DATA_FILE)),
                            message.getData().getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED));
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            @Override
            public void run() {
                FileDialogFragment fileDialog = FileDialogFragment.newInstance(messenger, title, message,
                        defaultFile, checkMsg);

                fileDialog.show(fragmentManager, "fileDialog");
            }
        });
    }

    public static void saveFile(Fragment fragment, String title, String message, File defaultFile, int requestCode) {
        saveFile(fragment, title, message, defaultFile, requestCode, null);
    }

    public static void saveFile(final Fragment fragment, String title, String message, File defaultFile,
                                final int requestCode, String checkMsg) {
        saveFile(new FileDialogCallback() {
            @Override
            public void onFileSelected(File file, boolean checked) {
                Intent intent = new Intent();
                intent.setData(Uri.fromFile(file));
                fragment.onActivityResult(requestCode, Activity.RESULT_OK, intent);
            }
        }, fragment.getActivity().getSupportFragmentManager(), title, message, defaultFile, checkMsg);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void openDocument(Fragment fragment, String mimeType, int requestCode) {
        openDocument(fragment, mimeType, false, requestCode);
    }

    /**
     * Opens the storage browser on Android 4.4 or later for opening a file
     * @param fragment
     * @param mimeType can be text/plain for example
     * @param multiple allow file chooser to return multiple files
     * @param requestCode used to identify the result coming back from storage browser onActivityResult() in your
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void openDocument(Fragment fragment, String mimeType, boolean multiple, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);

        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Opens the storage browser on Android 4.4 or later for saving a file
     * @param fragment
     * @param mimeType can be text/plain for example
     * @param suggestedName a filename desirable for the file to be saved
     * @param requestCode used to identify the result coming back from storage browser onActivityResult() in your
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void saveDocument(Fragment fragment, String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true); // Note: This is not documented, but works
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static String getFilename(Context context, Uri uri) {
        String filename = null;
        try {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

            if (cursor != null) {
                if (cursor.moveToNext()) {
                    filename = cursor.getString(0);
                }
                cursor.close();
            }
        } catch (Exception ignored) {
            // This happens in rare cases (eg: document deleted since selection) and should not cause a failure
        }
        if (filename == null) {
            String[] split = uri.toString().split("/");
            filename = split[split.length - 1];
        }
        return filename;
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = -1;
        try {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);

            if (cursor != null) {
                if (cursor.moveToNext()) {
                    size = cursor.getLong(0);
                }
                cursor.close();
            }
        } catch (Exception ignored) {
            // This happens in rare cases (eg: document deleted since selection) and should not cause a failure
        }
        return size;
    }

    /**
     * Retrieve thumbnail of file, document api feature and thus KitKat only
     */
    public static Bitmap getThumbnail(Context context, Uri uri, Point size) {
        if (Constants.KITKAT) {
            return DocumentsContract.getDocumentThumbnail(context.getContentResolver(), uri, size, null);
        } else {
            return null;
        }
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static interface FileDialogCallback {
        public void onFileSelected(File file, boolean checked);
    }
}
