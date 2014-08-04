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
import org.sufficientlysecure.keychain.helper.FileHelper;

public class DeleteFileDialogFragment extends DialogFragment {
    private static final String ARG_DELETE_URI = "delete_uri";

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteFileDialogFragment newInstance(Uri deleteUri) {
        DeleteFileDialogFragment frag = new DeleteFileDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_DELETE_URI, deleteUri);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final Uri deleteUri = getArguments().getParcelable(ARG_DELETE_URI);
        final String deleteFilename = FileHelper.getFilename(getActivity(), deleteUri);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);


        alert.setIcon(R.drawable.ic_dialog_alert_holo_light);
        alert.setTitle(R.string.warning);
        alert.setMessage(this.getString(R.string.file_delete_confirmation, deleteFilename));

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                // We can not securely delete Uris, so just use usual delete on them
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (DocumentsContract.deleteDocument(getActivity().getContentResolver(), deleteUri)) {
                        Toast.makeText(getActivity(), R.string.file_delete_successful, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (getActivity().getContentResolver().delete(deleteUri, null, null) > 0) {
                    Toast.makeText(getActivity(), R.string.file_delete_successful, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(getActivity(), getActivity().getString(R.string.error_file_delete_failed, deleteFilename), Toast.LENGTH_SHORT).show();

                // Note: We can't delete every file...
                // If possible we should find out if deletion is possible before even showing the option to do so.
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
}
