package org.sufficientlysecure.keychain.ui.dialog;
/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.R;

public class BadImportKeyDialogFragment extends DialogFragment {
    private static final String ARG_BAD_IMPORT = "bad_import";


    /**
     *  Creates a new instance of this Bad Import Key DialogFragment
     * @param bad
     * @return
     */

    public static BadImportKeyDialogFragment newInstance(int bad) {
        BadImportKeyDialogFragment frag = new BadImportKeyDialogFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_BAD_IMPORT, bad);
        frag.setArguments(args);


        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final FragmentActivity activity = getActivity();

        final int badImport = getArguments().getInt(ARG_BAD_IMPORT);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setIcon(R.drawable.ic_dialog_alert_holo_light);
        alert.setTitle(R.string.warning);

        alert.setMessage(activity.getResources()
                .getQuantityString(R.plurals.bad_keys_encountered, badImport, badImport));

        alert.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        alert.setCancelable(true);


        return alert.create();


    }
}
