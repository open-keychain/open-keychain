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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DeleteFileDialogFragment extends DialogFragment {
    private static final String ARG_DELETE_URIS = "delete_uris";

    private OnDeletedListener onDeletedListener;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteFileDialogFragment newInstance(ArrayList<Uri> deleteUris) {
        DeleteFileDialogFragment frag = new DeleteFileDialogFragment();
        Bundle args = new Bundle();

        args.putParcelableArrayList(ARG_DELETE_URIS, deleteUris);

        frag.setArguments(args);

        return frag;
    }

    public static DeleteFileDialogFragment newInstance(Uri deleteUri) {
        DeleteFileDialogFragment frag = new DeleteFileDialogFragment();
        Bundle args = new Bundle();

        ArrayList<Uri> list = new ArrayList<>();
        list.add(deleteUri);
        args.putParcelableArrayList(ARG_DELETE_URIS, list);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final ArrayList<Uri> deleteUris = getArguments().getParcelableArrayList(ARG_DELETE_URIS);

        final StringBuilder deleteFileNames = new StringBuilder();
        //Retrieving file names after deletion gives unexpected results
        final HashMap<Uri, String> deleteFileNameMap = new HashMap<>();
        for (Uri deleteUri : deleteUris) {
            String deleteFileName = FileHelper.getFilename(getActivity(), deleteUri);
            deleteFileNames.append('\n').append(deleteFileName);
            deleteFileNameMap.put(deleteUri, deleteFileName);
        }

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        alert.setTitle(getString(R.string.file_delete_confirmation_title));
        alert.setMessage(getString(R.string.file_delete_confirmation, deleteFileNames.toString()));

        alert.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                ArrayList<String> failedFileNameList = new ArrayList<>();

                for (Uri deleteUri : deleteUris) {
                    // Use DocumentsContract on Android >= 4.4
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            if (DocumentsContract.deleteDocument(getActivity().getContentResolver(), deleteUri)) {
                                continue;
                            }
                        } catch (Exception e) {
                            Log.d(Constants.TAG, "Catched Exception, can happen when delete is not supported!", e);
                        }
                    }

                    try {
                        if (getActivity().getContentResolver().delete(deleteUri, null, null) > 0) {
                            continue;
                        }
                    } catch (Exception e) {
                        Log.d(Constants.TAG, "Catched Exception, can happen when delete is not supported!", e);
                    }

                    // some Uri's a ContentResolver fails to delete is handled by the java.io.File's delete
                    // via the path of the Uri
                    if (new File(deleteUri.getPath()).delete()) {
                        continue;
                    }

                    // Note: We can't delete every file...
                    failedFileNameList.add(deleteFileNameMap.get(deleteUri));
                }

                StringBuilder failedFileNames = new StringBuilder();
                if (!failedFileNameList.isEmpty()) {
                    for (String failedFileName : failedFileNameList) {
                        failedFileNames.append('\n').append(failedFileName);
                    }
                    failedFileNames.append('\n').append(getActivity().getString(R.string.error_file_delete_failed));
                }

                // NOTE: Use Toasts, not Snackbars. When sharing to another application snackbars
                // would not show up!
                Toast.makeText(getActivity(), getActivity().getString(R.string.file_delete_successful,
                                deleteUris.size() - failedFileNameList.size(), deleteUris.size(), failedFileNames.toString()),
                        Toast.LENGTH_LONG).show();

                if (onDeletedListener != null) {
                    onDeletedListener.onDeleted();
                }
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        alert.setCancelable(true);

        return alert.show();
    }

    public void setOnDeletedListener(OnDeletedListener onDeletedListener) {
        this.onDeletedListener = onDeletedListener;
    }

    /**
     * Callback for performing tasks after the deletion of files
     */
    public interface OnDeletedListener {

        public void onDeleted();

    }

}
