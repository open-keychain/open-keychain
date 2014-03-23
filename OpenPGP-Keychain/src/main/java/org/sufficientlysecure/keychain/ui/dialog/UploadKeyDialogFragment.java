package org.sufficientlysecure.keychain.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.EditKeyActivity;

public class UploadKeyDialogFragment extends DialogFragment {

    public static UploadKeyDialogFragment newInstance(Uri uri){
        Bundle bundle = new Bundle();
        bundle.putParcelable(EditKeyActivity.ARG_DATA_URI, uri);
        UploadKeyDialogFragment fragment = new UploadKeyDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Bundle data = getArguments();
        final Uri dataUri = data.getParcelable(EditKeyActivity.ARG_DATA_URI);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(getString(R.string.section_upload_key))
                .setMessage(getString(R.string.upload_key_dialog_message))
                .setPositiveButton(getString(R.string.choice_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((EditKeyActivity) getActivity()).uploadToKeyserver(dataUri);
                    }

                })
                .setNegativeButton(getString(R.string.choice_no), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((EditKeyActivity) getActivity()).finish();
                    }
                });
        return dialog.create();
    }
}