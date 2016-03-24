package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;

import java.io.Serializable;

public class EncryptFilesOptionsDialog extends DialogFragment {

    private static final String ARG_FILE_OPTIONS = "file_options";


    public interface FileOptionsDialogListener {
        boolean onFileOptionsUpdated(FileOptions options);
    }

    public static class FileOptions implements Serializable {
        public String realFilename;
        public String curFilename;

        public FileOptions(String realFilename, String curFilename) {
            this.realFilename = realFilename;
            this.curFilename = curFilename;
        }
    }

    private FileOptionsDialogListener listener;

    public static EncryptFilesOptionsDialog newInstance(FileOptions options) {
        EncryptFilesOptionsDialog fragment = new EncryptFilesOptionsDialog();

        Bundle args = new Bundle();
        args.putSerializable(ARG_FILE_OPTIONS, options);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.encrypt_files_options_dialog, null);

        Bundle args = getArguments();
        final FileOptions options = (FileOptions) args.getSerializable(ARG_FILE_OPTIONS);

        final EditText curFilename = (EditText) view.findViewById(R.id.filename_out);

        builder.setTitle(options.realFilename);
        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        options.curFilename = curFilename.getText().toString();

                        InputMethodManager imm = (InputMethodManager)
                                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        if (listener.onFileOptionsUpdated(options))
                            EncryptFilesOptionsDialog.this.getDialog().cancel();
                    }
                })
                .setNegativeButton(R.string.btn_do_not_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EncryptFilesOptionsDialog.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    public void setListener(FileOptionsDialogListener listener) {
        this.listener = listener;
    }

}
