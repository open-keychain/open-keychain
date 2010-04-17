package org.thialfihar.android.apg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class FileDialog {

    public static interface OnClickListener {
        public void onCancelClick();
        public void onOkClick(String filename);
    }

    public static AlertDialog build(Context context, String title, String message,
                                    String defaultFile, OnClickListener onClickListener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(title);
        alert.setMessage(message);

        final EditText input = new EditText(context);
        input.setText(defaultFile);
        alert.setView(input);

        final OnClickListener clickListener = onClickListener;

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        clickListener.onOkClick(input.getText().toString());
                                    }
                                });

        alert.setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        clickListener.onCancelClick();
                                    }
                                });
        return alert.create();
    }
}
