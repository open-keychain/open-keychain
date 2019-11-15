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
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.sufficientlysecure.keychain.R;

public class FidesmoInstallDialog extends DialogFragment {

    // URLs for Google Play app and to install apps via browser
    private final static String PLAY_STORE_URI = "market://details?id=";
    private final static String PLAY_STORE_VIA_BROWSER_URI = "http://play.google.com/store/apps/details?id=";

    // Fidesmo constants
    private static final String FIDESMO_APP_PACKAGE = "com.fidesmo.sec.android";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        CustomAlertDialogBuilder mCustomAlertDialogBuilder = new CustomAlertDialogBuilder(getActivity());
        mCustomAlertDialogBuilder.setTitle(getString(R.string.prompt_fidesmo_app_install_title));
        mCustomAlertDialogBuilder.setMessage(getString(R.string.prompt_fidesmo_app_install_message));
        mCustomAlertDialogBuilder.setPositiveButton(
                getString(R.string.prompt_fidesmo_app_install_button_positive),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                startPlayStoreFidesmoAppActivity();
            }
        });
        mCustomAlertDialogBuilder.setNegativeButton(
                getString(R.string.prompt_fidesmo_app_install_button_negative),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return mCustomAlertDialogBuilder.show();
    }

    private void startPlayStoreFidesmoAppActivity() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI +
                    FIDESMO_APP_PACKAGE)));
        } catch (android.content.ActivityNotFoundException exception) {
            // if the Google Play app is not installed, call the browser
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_VIA_BROWSER_URI +
                    FIDESMO_APP_PACKAGE)));
        }
    }
}
