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
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Choice;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;

public class EncryptFileFragment extends Fragment {
    public static final String ARG_FILENAME = "filename";
    public static final String ARG_ASCII_ARMOR = "ascii_armor";

    private static final int RESULT_CODE_FILE = 0x00007003;

    private EncryptActivityInterface mEncryptInterface;

    // view
    private CheckBox mAsciiArmor = null;
    private Spinner mFileCompression = null;
    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private CheckBox mShareAfter = null;
    private BootstrapButton mBrowse = null;
    private BootstrapButton mEncryptFile;

    private FileDialogFragment mFileDialog;

    // model
    private String mInputFilename = null;
    private String mOutputFilename = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_file_fragment, container, false);

        mEncryptFile = (BootstrapButton) view.findViewById(R.id.action_encrypt_file);
        mEncryptFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked();
            }
        });

        mFilename = (EditText) view.findViewById(R.id.filename);
        mBrowse = (BootstrapButton) view.findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileHelper.openFile(EncryptFileFragment.this, mFilename.getText().toString(), "*/*",
                        Id.request.filename);
            }
        });

        mFileCompression = (Spinner) view.findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[] {
                new Choice(Id.choice.compression.none, getString(R.string.choice_none) + " ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.zip, "ZIP ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.zlib, "ZLIB ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.bzip2, "BZIP2 ("
                        + getString(R.string.compression_very_slow) + ")"),
        };
        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getActivity(),
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileCompression.setAdapter(adapter);

        int defaultFileCompression = Preferences.getPreferences(getActivity()).getDefaultFileCompression();
        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == defaultFileCompression) {
                mFileCompression.setSelection(i);
                break;
            }
        }

        mDeleteAfter = (CheckBox) view.findViewById(R.id.deleteAfterEncryption);
        mShareAfter = (CheckBox) view.findViewById(R.id.shareAfterEncryption);

        mAsciiArmor = (CheckBox) view.findViewById(R.id.asciiArmour);
        mAsciiArmor.setChecked(Preferences.getPreferences(getActivity()).getDefaultAsciiArmour());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String filename = getArguments().getString(ARG_FILENAME);
        if (filename != null) {
            mFilename.setText(filename);
        }
        boolean asciiArmor = getArguments().getBoolean(ARG_ASCII_ARMOR);
        if (asciiArmor) {
            mAsciiArmor.setChecked(asciiArmor);
        }
    }

    /**
     * Guess output filename based on input path
     *
     * @param path
     * @return Suggestion for output filename
     */
    private String guessOutputFilename(String path) {
        // output in the same directory but with additional ending
        File file = new File(path);
        String ending = (mAsciiArmor.isChecked() ? ".asc" : ".gpg");
        String outputFilename = file.getParent() + File.separator + file.getName() + ending;

        return outputFilename;
    }

    private void showOutputFileDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mOutputFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
                    encryptStart();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger,
                getString(R.string.title_encrypt_to_file),
                getString(R.string.specify_file_to_encrypt_to), mOutputFilename, null);

        mFileDialog.show(getActivity().getSupportFragmentManager(), "fileDialog");
    }

    private void encryptClicked() {
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            mInputFilename = mFilename.getText().toString();
        }

        mOutputFilename = guessOutputFilename(mInputFilename);

        if (mInputFilename.equals("")) {
            AppMsg.makeText(getActivity(), R.string.no_file_selected, AppMsg.STYLE_ALERT).show();
            return;
        }

        if (!mInputFilename.startsWith("content")) {
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

        if (mEncryptInterface.isModeSymmetric()) {
            // symmetric encryption

            boolean gotPassPhrase = (mEncryptInterface.getPassphrase() != null
                    && mEncryptInterface.getPassphrase().length() != 0);
            if (!gotPassPhrase) {
                AppMsg.makeText(getActivity(), R.string.passphrase_must_not_be_empty, AppMsg.STYLE_ALERT)
                        .show();
                return;
            }

            if (!mEncryptInterface.getPassphrase().equals(mEncryptInterface.getPassphraseAgain())) {
                AppMsg.makeText(getActivity(), R.string.passphrases_do_not_match, AppMsg.STYLE_ALERT).show();
                return;
            }
        } else {
            // asymmetric encryption

            boolean gotEncryptionKeys = (mEncryptInterface.getEncryptionKeys() != null
                    && mEncryptInterface.getEncryptionKeys().length > 0);

            if (!gotEncryptionKeys) {
                AppMsg.makeText(getActivity(), R.string.select_encryption_key, AppMsg.STYLE_ALERT).show();
                return;
            }

            if (!gotEncryptionKeys && mEncryptInterface.getSignatureKey() == 0) {
                AppMsg.makeText(getActivity(), R.string.select_encryption_or_signature_key,
                        AppMsg.STYLE_ALERT).show();
                return;
            }

            if (mEncryptInterface.getSignatureKey() != 0 &&
                PassphraseCacheService.getCachedPassphrase(getActivity(),
                    mEncryptInterface.getSignatureKey()) == null) {
                showPassphraseDialog();

                return;
            }
        }

        showOutputFileDialog();
    }

    private void encryptStart() {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_ENCRYPT_SIGN);

        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_URI);

        if (mEncryptInterface.isModeSymmetric()) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passphrase = mEncryptInterface.getPassphrase();
            if (passphrase.length() == 0) {
                passphrase = null;
            }
            data.putString(KeychainIntentService.ENCRYPT_SYMMETRIC_PASSPHRASE, passphrase);
        } else {
            data.putLong(KeychainIntentService.ENCRYPT_SIGNATURE_KEY_ID,
                mEncryptInterface.getSignatureKey());
            data.putLongArray(KeychainIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS,
                mEncryptInterface.getEncryptionKeys());
        }

        Log.d(Constants.TAG, "mInputFilename=" + mInputFilename + ", mOutputFilename="
                + mOutputFilename);

        data.putString(KeychainIntentService.ENCRYPT_INPUT_FILE, mInputFilename);
        data.putString(KeychainIntentService.ENCRYPT_OUTPUT_FILE, mOutputFilename);

        boolean useAsciiArmor = mAsciiArmor.isChecked();
        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, useAsciiArmor);

        int compressionId = ((Choice) mFileCompression.getSelectedItem()).getId();
        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID, compressionId);
//        data.putBoolean(KeychainIntentService.ENCRYPT_GENERATE_SIGNATURE, mGenerateSignature);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    AppMsg.makeText(getActivity(), R.string.encryption_successful,
                            AppMsg.STYLE_INFO).show();

                    if (mDeleteAfter.isChecked()) {
                        // Create and show dialog to delete original file
                        DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment
                                .newInstance(mInputFilename);
                        deleteFileDialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
                    }

                    if (mShareAfter.isChecked()) {
                        // Share encrypted file
                        Intent sendFileIntent = new Intent(Intent.ACTION_SEND);
                        sendFileIntent.setType("*/*");
                        sendFileIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(mOutputFilename));
                        startActivity(Intent.createChooser(sendFileIntent,
                                getString(R.string.title_send_file)));
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_CODE_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    try {
                        String path = FileHelper.getPath(getActivity(), data.getData());
                        Log.d(Constants.TAG, "path=" + path);

                        mFilename.setText(path);
                    } catch (NullPointerException e) {
                        Log.e(Constants.TAG, "Nullpointer while retrieving path!");
                    }
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption
     */
    private void showPassphraseDialog() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    showOutputFileDialog();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    getActivity(), messenger, mEncryptInterface.getSignatureKey());

            passphraseDialog.show(getActivity().getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }
}
