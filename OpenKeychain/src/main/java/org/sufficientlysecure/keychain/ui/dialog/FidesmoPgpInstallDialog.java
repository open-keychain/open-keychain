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
