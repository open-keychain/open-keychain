/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.R;

public class AdvancedAppSettingsDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE_NAME = "package_name";
    private static final String ARG_SIGNATURE = "signature";

    /**
     * Creates new instance of this fragment
     */
    public static AdvancedAppSettingsDialogFragment newInstance(String packageName, String digest) {
        AdvancedAppSettingsDialogFragment frag = new AdvancedAppSettingsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, packageName);
        args.putString(ARG_SIGNATURE, digest);

        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        alert.setTitle(R.string.api_settings_advanced);
        alert.setCancelable(true);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        String packageName = getArguments().getString(ARG_PACKAGE_NAME);
        String signature = getArguments().getString(ARG_SIGNATURE);

        alert.setMessage(getString(R.string.api_settings_package_name) + ": " + packageName + "\n\n"
                + getString(R.string.api_settings_package_signature) + ": " + signature);

        return alert.show();
    }
}
