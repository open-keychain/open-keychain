/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.io.File;

/**
 * This is a file chooser dialog no longer used with KitKat
 */
public class FileDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_DEFAULT_FILE = "default_file";
    private static final String ARG_CHECKBOX_TEXT = "checkbox_text";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_DATA_FILE = "file";
    public static final String MESSAGE_DATA_CHECKED = "checked";

    private Messenger mMessenger;

    private EditText mFilename;
    private ImageButton mBrowse;
    private CheckBox mCheckBox;
    private TextView mMessageTextView;

    private File mFile;

    private static final int REQUEST_CODE = 0x00007004;

    /**
     * Creates new instance of this file dialog fragment
     */
    public static FileDialogFragment newInstance(Messenger messenger, String title, String message,
                                                 File defaultFile, String checkboxText) {
        FileDialogFragment frag = new FileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);

        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_DEFAULT_FILE, defaultFile.getAbsolutePath());
        args.putString(ARG_CHECKBOX_TEXT, checkboxText);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        String title = getArguments().getString(ARG_TITLE);
        String message = getArguments().getString(ARG_MESSAGE);
        mFile = new File(getArguments().getString(ARG_DEFAULT_FILE));
        if (!mFile.isAbsolute()) {
            // We use OK dir by default
            mFile = new File(Constants.Path.APP_DIR.getAbsolutePath(), mFile.getName());
        }
        String checkboxText = getArguments().getString(ARG_CHECKBOX_TEXT);

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);
        alert.setTitle(title);

        View view = inflater.inflate(R.layout.file_dialog, null);

        mMessageTextView = (TextView) view.findViewById(R.id.message);
        mMessageTextView.setText(message);

        mFilename = (EditText) view.findViewById(R.id.input);
        mFilename.setText(mFile.getName());
        mBrowse = (ImageButton) view.findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // only .asc or .gpg files
                // setting it to text/plain prevents Cynaogenmod's file manager from selecting asc
                // or gpg types!
                FileHelper.openFile(FileDialogFragment.this, Uri.fromFile(mFile), "*/*", REQUEST_CODE);
            }
        });

        mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        if (checkboxText == null) {
            mCheckBox.setEnabled(false);
            mCheckBox.setVisibility(View.GONE);
        } else {
            mCheckBox.setEnabled(true);
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setText(checkboxText);
        }

        alert.setView(view);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                String currentFilename = mFilename.getText().toString();
                if (currentFilename == null || currentFilename.isEmpty()) {
                    // No file is like pressing cancel, UI: maybe disable positive button in this case?
                    return;
                }

                if (mFile == null || currentFilename.startsWith("/")) {
                    mFile = new File(currentFilename);
                } else if (!mFile.getName().equals(currentFilename)) {
                    // We update our File object if user changed name!
                    mFile = new File(mFile.getParentFile(), currentFilename);
                }

                boolean checked = mCheckBox.isEnabled() && mCheckBox.isChecked();

                // return resulting data back to activity
                Bundle data = new Bundle();
                data.putString(MESSAGE_DATA_FILE, mFile.getAbsolutePath());
                data.putBoolean(MESSAGE_DATA_CHECKED, checked);

                sendMessageToHandler(MESSAGE_OKAY, data);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        return alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode & 0xFFFF) {
            case REQUEST_CODE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    File file = new File(data.getData().getPath());
                    if (file.getParentFile().exists()) {
                        mFile = file;
                        mFilename.setText(mFile.getName());
                    } else {
                        Notify.showNotify(getActivity(), R.string.no_file_selected, Notify.Style.ERROR);
                    }
                }

                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }

    /**
     * Send message back to handler which is initialized in a activity
     *
     * @param what Message integer you want to send
     */
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}
