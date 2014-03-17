package org.sufficientlysecure.keychain.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.EditKeyActivity;

/**
 * Created by sreeram on 17/3/14.
 */
public class CloseActivityDialog extends DialogFragment {
    private ActionBarActivity activity;

    public static CloseActivityDialog newInstance() {
        CloseActivityDialog fragment = new CloseActivityDialog();
        return fragment;
    }

    public CloseActivityDialog(){}

    public CloseActivityDialog(ActionBarActivity mActivity){
        activity = mActivity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Window")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }

                })
                .setNegativeButton("No", null);
        if(getActivity() instanceof EditKeyActivity) {
            dialog.setMessage(getString(R.string.closing_edit_key_activity));
        }
        else{
            dialog.setMessage(getString(R.string.closing_activity));
        }
        return dialog.create();
    }
}
