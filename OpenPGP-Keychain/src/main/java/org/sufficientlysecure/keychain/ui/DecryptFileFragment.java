/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;

import java.io.File;

public class DecryptFileFragment extends Fragment {

    private EditText mFilename;
    private CheckBox mDeleteAfter;
    private BootstrapButton mBrowse;
    private BootstrapButton mDecryptButton;


    private String mInputFilename = null;
    private String mOutputFilename = null;

    private FileDialogFragment mFileDialog;

    private static final int RESULT_CODE_FILE = 0x00007003;


    /**
     * Creates new instance of this fragment
     */
    public static DecryptFileFragment newInstance() {
        DecryptFileFragment frag = new DecryptFileFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_file_fragment, container, false);

        mFilename = (EditText) view.findViewById(R.id.filename);
        mBrowse = (BootstrapButton) view.findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileHelper.openFile(getActivity(), mFilename.getText().toString(), "*/*",
                        RESULT_CODE_FILE);
            }
        });
        mDeleteAfter = (CheckBox) view.findViewById(R.id.deleteAfterDecryption);
        mDecryptButton = (BootstrapButton) view.findViewById(R.id.action_decrypt);
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAction();
            }
        });

        return view;
    }

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String filename = file.getName();
        if (filename.endsWith(".asc") || filename.endsWith(".gpg") || filename.endsWith(".pgp")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        mOutputFilename = Constants.Path.APP_DIR + "/" + filename;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void decryptAction() {
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            guessOutputFilename();
        }

        if (mInputFilename.equals("")) {
            AppMsg.makeText(getActivity(), R.string.no_file_selected, AppMsg.STYLE_ALERT).show();
            return;
        }

        if (mInputFilename.startsWith("file")) {
            File file = new File(mInputFilename);
            if (!file.exists() || !file.isFile()) {
                AppMsg.makeText(
                        getActivity(),
                        getString(R.string.error_message,
                                getString(R.string.error_file_not_found)), AppMsg.STYLE_ALERT)
                        .show();
                return;
            }
        }

        askForOutputFilename();
    }

    private void askForOutputFilename() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mOutputFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
//                    decryptStart();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger,
                getString(R.string.title_decrypt_to_file),
                getString(R.string.specify_file_to_decrypt_to), mOutputFilename, null);

        mFileDialog.show(getActivity().getSupportFragmentManager(), "fileDialog");
    }

}
