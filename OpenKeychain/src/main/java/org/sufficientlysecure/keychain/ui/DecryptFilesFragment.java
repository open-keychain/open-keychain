/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;

public class DecryptFilesFragment extends DecryptFragment {
    public static final String ARG_URI = "uri";
    public static final String ARG_OPEN_DIRECTLY = "open_directly";

    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    // view
    private TextView mFilename;
    private CheckBox mDeleteAfter;
    private View mDecryptButton;

    // model
    private Uri mInputUri = null;
    private Uri mOutputUri = null;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptFilesFragment newInstance(Uri uri, boolean openDirectly) {
        DecryptFilesFragment frag = new DecryptFilesFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        args.putBoolean(ARG_OPEN_DIRECTLY, openDirectly);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_files_fragment, container, false);

        mFilename = (TextView) view.findViewById(R.id.decrypt_files_filename);
        mDeleteAfter = (CheckBox) view.findViewById(R.id.decrypt_files_delete_after_decryption);
        mDecryptButton = view.findViewById(R.id.decrypt_files_action_decrypt);
        view.findViewById(R.id.decrypt_files_browse).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    FileHelper.openDocument(DecryptFilesFragment.this, "*/*", REQUEST_CODE_INPUT);
                } else {
                    FileHelper.openFile(DecryptFilesFragment.this, mInputUri, "*/*",
                            REQUEST_CODE_INPUT);
                }
            }
        });
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAction();
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_URI, mInputUri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle state = savedInstanceState != null ? savedInstanceState : getArguments();
        setInputUri(state.<Uri>getParcelable(ARG_URI));

        // should only come from args
        if (state.getBoolean(ARG_OPEN_DIRECTLY, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                FileHelper.openDocument(DecryptFilesFragment.this, "*/*", REQUEST_CODE_INPUT);
            } else {
                FileHelper.openFile(DecryptFilesFragment.this, mInputUri, "*/*", REQUEST_CODE_INPUT);
            }
        }
    }

    private void setInputUri(Uri inputUri) {
        if (inputUri == null) {
            mInputUri = null;
            mFilename.setText("");
            return;
        }

        mInputUri = inputUri;
        mFilename.setText(FileHelper.getFilename(getActivity(), mInputUri));
    }

    private void decryptAction() {
        if (mInputUri == null) {
            Notify.create(getActivity(), R.string.no_file_selected, Notify.Style.ERROR).show();
            return;
        }

        cryptoOperation();
    }

    private String removeEncryptedAppend(String name) {
        if (name.endsWith(Constants.FILE_EXTENSION_ASC)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_MAIN)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_ALTERNATE)) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void askForOutputFilename(String originalFilename) {
        if (TextUtils.isEmpty(originalFilename)) {
            originalFilename = removeEncryptedAppend(FileHelper.getFilename(getActivity(), mInputUri));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File file = new File(mInputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            File targetFile = new File(parentDir, originalFilename);
            FileHelper.saveFile(this, getString(R.string.title_decrypt_to_file),
                    getString(R.string.specify_file_to_decrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "*/*", originalFilename, REQUEST_CODE_OUTPUT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    setInputUri(data.getData());
                }
                return;
            }

            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mOutputUri = data.getData();
                    cryptoOperation();
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onVerifyLoaded(boolean hideErrorOverlay) {

    }

    @Override
    protected PgpDecryptVerifyInputParcel createOperationInput() {
        return new PgpDecryptVerifyInputParcel(mInputUri, mOutputUri).setAllowSymmetricDecryption(true);
    }

    @Override
    protected void onCryptoOperationSuccess(DecryptVerifyResult result) {

        // display signature result in activity
        loadVerifyResult(result);

        // TODO delete after decrypt not implemented!

    }

}
