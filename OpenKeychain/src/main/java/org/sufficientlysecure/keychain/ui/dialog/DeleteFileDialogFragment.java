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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;
import java.util.ArrayList;

public class DeleteFileDialogFragment extends DialogFragment {
    private static final String ARG_DELETE_URIS = "delete_uris";

    private OnDeletedListener onDeletedListener;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteFileDialogFragment newInstance(Uri... deleteUris) {
        DeleteFileDialogFragment frag = new DeleteFileDialogFragment();
        Bundle args = new Bundle();

        args.putParcelableArray(ARG_DELETE_URIS, deleteUris);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final Uri[] deleteUris = (Uri[]) getArguments().getParcelableArray(ARG_DELETE_URIS);

        final StringBuilder deleteFileNames = new StringBuilder();
        for (Uri deleteUri : deleteUris) {
            deleteFileNames.append('\n')
                    .append(FileHelper.getFilename(getActivity(), deleteUri));
        }

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        alert.setMessage(this.getString(R.string.file_delete_confirmation, deleteFileNames.toString()));

        alert.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                ArrayList<Uri> failedUris = new ArrayList<>();

                for (Uri deleteUri : deleteUris) {
                    String scheme = deleteUri.getScheme();

                    if(scheme.equals(ContentResolver.SCHEME_FILE)) {
                        if(new File(deleteUri.getPath()).delete()) {
                            continue;
                        }
                    }
                    else if(scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        // We can not securely delete Uris, so just use usual delete on them
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            if (DocumentsContract.deleteDocument(getActivity().getContentResolver(), deleteUri)) {
                                continue;
                            }
                        }

                        if (getActivity().getContentResolver().delete(deleteUri, null, null) > 0) {
                            continue;
                        }

                        // some Uri's a ContentResolver fails to delete is handled by the java.io.File's delete
                        // via the path of the Uri
                        if(new File(deleteUri.getPath()).delete()) {
                            continue;
                        }
                    }

                    failedUris.add(deleteUri);

                    // Note: We can't delete every file...
                    // If possible we should find out if deletion is possible before even showing the option to do so.
                }

                if (failedUris.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.file_delete_successful, Toast.LENGTH_SHORT).show();
                } else {
                    for (Uri deleteUri : deleteUris) {
                        Toast.makeText(getActivity(), getActivity().getString(R.string.error_file_delete_failed,
                                FileHelper.getFilename(getActivity(), deleteUri)), Toast.LENGTH_SHORT).show();
                    }
                }

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

    public interface OnDeletedListener {

        public void onDeleted();

    }
}
