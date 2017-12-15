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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class FidesmoPgpInstallDialog extends DialogFragment {

    // Fidesmo constants
    private static final String FIDESMO_SERVICE_DELIVERY_CARD_ACTION = "com.fidesmo.sec.DELIVER_SERVICE";
    private static final String FIDESMO_SERVICE_URI = "https://api.fidesmo.com/service/";
    private static final String FIDESMO_PGP_APPLICATION_ID = "0cdc651e";
    private static final String FIDESMO_PGP_SERVICE_ID = "OKC-install";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        CustomAlertDialogBuilder mCustomAlertDialogBuilder = new CustomAlertDialogBuilder(getActivity());
        mCustomAlertDialogBuilder.setTitle(getString(R.string.prompt_fidesmo_pgp_install_title));
        mCustomAlertDialogBuilder.setMessage(getString(R.string.prompt_fidesmo_pgp_install_message));
        mCustomAlertDialogBuilder.setPositiveButton(
                getString(R.string.prompt_fidesmo_pgp_install_button_positive),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                startFidesmoPgpAppletActivity();
            }
        });
        mCustomAlertDialogBuilder.setNegativeButton(
                getString(R.string.prompt_fidesmo_pgp_install_button_negative),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return mCustomAlertDialogBuilder.show();
    }

    private void startFidesmoPgpAppletActivity() {
        try {
            // Call the Fidesmo app with the PGP applet as parameter to
            // send the user straight to it
            final String mPgpInstallServiceUrl = FIDESMO_SERVICE_URI + FIDESMO_PGP_APPLICATION_ID
                    + "/" + FIDESMO_PGP_SERVICE_ID;
            Intent mPgpServiceIntent = new Intent(FIDESMO_SERVICE_DELIVERY_CARD_ACTION,
                    Uri.parse(mPgpInstallServiceUrl));
            startActivity(mPgpServiceIntent);
        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "Error when parsing URI");
        }
    }
}
