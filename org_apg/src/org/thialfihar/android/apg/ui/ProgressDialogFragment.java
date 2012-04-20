package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {

    public static final int ID_ENCRYPTING = 1;
    public static final int ID_DECRYPTING = 2;
    public static final int ID_SAVING = 3;
    public static final int ID_IMPORTING = 4;
    public static final int ID_EXPORTING = 5;
    public static final int ID_DELETING = 6;
    public static final int ID_QUERYING = 7;
    public static final int ID_SIGNING = 8;

    public static ProgressDialogFragment newInstance(int id) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        return frag;
    }

    public static void test() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);

        int id = getArguments().getInt("id");

        switch (id) {
        case ID_ENCRYPTING:
            dialog.setMessage(this.getString(R.string.progress_initializing));

        case ID_DECRYPTING:
            dialog.setMessage(this.getString(R.string.progress_initializing));

        case ID_SAVING:
            dialog.setMessage(this.getString(R.string.progress_saving));

        case ID_IMPORTING:
            dialog.setMessage(this.getString(R.string.progress_importing));

        case ID_EXPORTING:
            dialog.setMessage(this.getString(R.string.progress_exporting));

        case ID_DELETING:
            dialog.setMessage(this.getString(R.string.progress_initializing));

        case ID_QUERYING:
            dialog.setMessage(this.getString(R.string.progress_querying));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);

        case ID_SIGNING:
            dialog.setMessage(this.getString(R.string.progress_signing));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);

        default:
            break;

        }

        // Disable the back button
        // OnKeyListener keyListener = new OnKeyListener() {
        //
        // @Override
        // public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        //
        // if (keyCode == KeyEvent.KEYCODE_BACK) {
        // return true;
        // }
        // return false;
        // }
        //
        // };
        // dialog.setOnKeyListener(keyListener);

        return dialog;
    }
}