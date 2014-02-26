package org.sufficientlysecure.keychain.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.R;


/**
 * Created by Haven on 26/2/14.
 */
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
