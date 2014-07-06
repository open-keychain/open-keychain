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
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Choice;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EncryptFileFragment extends Fragment {
    public static final String ARG_URIS = "uris";

    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private EncryptActivityInterface mEncryptInterface;

    // view
    private Spinner mFileCompression = null;
    private View mShareFile;
    private View mEncryptFile;
    private SelectedFilesAdapter mAdapter = new SelectedFilesAdapter();

    // model
    private ArrayList<Uri> mInputUri = new ArrayList<Uri>();
    private ArrayList<Uri> mOutputUri = new ArrayList<Uri>();

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

        mEncryptFile = view.findViewById(R.id.action_encrypt_file);
        mEncryptFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(false);
            }
        });
        mShareFile = view.findViewById(R.id.action_encrypt_share);
        mShareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(true);
            }
        });

        //mFilename = (TextView) view.findViewById(R.id.filename);
        //view.findViewById(R.id.btn_browse).setOnClickListener(new View.OnClickListener() {
        //    public void onClick(View v) {
        //        if (Constants.KITKAT) {
        //            FileHelper.openDocument(EncryptFileFragment.this, "*/*", REQUEST_CODE_INPUT);
        //        } else {
        //            FileHelper.openFile(EncryptFileFragment.this,
        //                    mInputUri.isEmpty() ? null : mInputUri.get(mInputUri.size() - 1), "*/*", REQUEST_CODE_INPUT);
        //        }
        //    }
        //});

        View addFile = inflater.inflate(R.layout.file_list_entry_add, null);
        addFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constants.KITKAT) {
                    FileHelper.openDocument(EncryptFileFragment.this, "*/*", REQUEST_CODE_INPUT);
                } else {
                    FileHelper.openFile(EncryptFileFragment.this,
                            mInputUri.isEmpty() ? null : mInputUri.get(mInputUri.size() - 1), "*/*", REQUEST_CODE_INPUT);
                }
            }
        });
        ListView listView = (ListView) view.findViewById(R.id.selected_files_list);
        listView.addFooterView(addFile);
        listView.setAdapter(mAdapter);

        mFileCompression = (Spinner) view.findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[]{
                new Choice(Constants.choice.compression.none, getString(R.string.choice_none) + " ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.zip, "ZIP ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.zlib, "ZLIB ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.bzip2, "BZIP2 ("
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

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addInputUris(getArguments().<Uri>getParcelableArrayList(ARG_URIS));
    }

    private void addInputUris(List<Uri> uris) {
        if (uris != null) {
            for (Uri uri : uris) {
                addInputUri(uri);
            }
        }
    }

    private void addInputUri(Uri inputUri) {
        if (inputUri == null) {
            return;
        }

        mInputUri.add(inputUri);
        mAdapter.notifyDataSetChanged();
    }

    private void delInputUri(int position) {
        mInputUri.remove(position);
        mAdapter.notifyDataSetChanged();
    }

    private void showOutputFileDialog() {
        if (mInputUri.size() > 1 || mInputUri.isEmpty()) {
            throw new IllegalStateException();
        }
        Uri inputUri = mInputUri.get(0);
        if (!Constants.KITKAT) {
            File file = new File(inputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            String targetName = FileHelper.getFilename(getActivity(), inputUri) +
                    (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg");
            File targetFile = new File(parentDir, targetName);
            FileHelper.saveFile(this, getString(R.string.title_encrypt_to_file),
                    getString(R.string.specify_file_to_encrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "*/*", FileHelper.getFilename(getActivity(), inputUri) +
                    (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg"), REQUEST_CODE_OUTPUT);
        }
    }

    private void encryptClicked(boolean share) {
        if (mInputUri.isEmpty()) {
            AppMsg.makeText(getActivity(), R.string.no_file_selected, AppMsg.STYLE_ALERT).show();
            return;
        } else if (mInputUri.size() > 1 && !share) {
            AppMsg.makeText(getActivity(), "TODO", AppMsg.STYLE_ALERT).show(); // TODO
            return;
        }

        if (mEncryptInterface.isModeSymmetric()) {
            // symmetric encryption

            boolean gotPassphrase = (mEncryptInterface.getPassphrase() != null
                    && mEncryptInterface.getPassphrase().length() != 0);
            if (!gotPassphrase) {
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
                PassphraseDialogFragment.show(getActivity(), mEncryptInterface.getSignatureKey(),
                        new Handler() {
                            @Override
                            public void handleMessage(Message message) {
                                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                    showOutputFileDialog();
                                }
                            }
                        });

                return;
            }
        }

        if (share) {
            mOutputUri.clear();
            for (Uri uri : mInputUri) {
                String targetName = FileHelper.getFilename(getActivity(), uri) +
                        (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg");
                mOutputUri.add(TemporaryStorageProvider.createFile(getActivity(), targetName));
            }
            encryptStart(true);
        } else if (mInputUri.size() == 1) {
            showOutputFileDialog();
        }
    }

    private void encryptStart(final boolean share) {
        if (mInputUri == null || mOutputUri == null || mInputUri.size() != mOutputUri.size()) {
            throw new IllegalStateException("Something went terribly wrong if this happens!");
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_ENCRYPT_SIGN);

        // fill values for this action
        Bundle data = new Bundle();

        Log.d(Constants.TAG, "mInputUri=" + mInputUri + ", mOutputUri=" + mOutputUri);

        data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_URIS);
        data.putParcelableArrayList(KeychainIntentService.ENCRYPT_INPUT_URIS, mInputUri);

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_URIS);
        data.putParcelableArrayList(KeychainIntentService.ENCRYPT_OUTPUT_URIS, mOutputUri);

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

        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, mEncryptInterface.isUseArmor());

        int compressionId = ((Choice) mFileCompression.getSelectedItem()).getId();
        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID, compressionId);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    AppMsg.makeText(getActivity(), R.string.encrypt_sign_successful,
                            AppMsg.STYLE_INFO).show();

                    if (mEncryptInterface.isDeleteAfterEncrypt()) {
                        // Create and show dialog to delete original file
                        /*DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment.newInstance(mInputUri);
                        deleteFileDialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
                        setInputUri(null);*/
                    }

                    if (share) {
                        // Share encrypted file
                        Intent sendFileIntent;
                        if (mOutputUri.size() == 1) {
                            sendFileIntent = new Intent(Intent.ACTION_SEND);
                            sendFileIntent.setType("*/*");
                            sendFileIntent.putExtra(Intent.EXTRA_STREAM, mOutputUri.get(0));
                        } else {
                            sendFileIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            sendFileIntent.setType("*/*");
                            sendFileIntent.putExtra(Intent.EXTRA_STREAM, mOutputUri);
                        }
                        if (!mEncryptInterface.isModeSymmetric() && mEncryptInterface.getEncryptionUsers() != null) {
                            Set<String> users = new HashSet<String>();
                            for (String user : mEncryptInterface.getEncryptionUsers()) {
                                String[] userId = KeyRing.splitUserId(user);
                                if (userId[1] != null) {
                                    users.add(userId[1]);
                                }
                            }
                            sendFileIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));
                        }
                        startActivity(Intent.createChooser(sendFileIntent,
                                getString(R.string.title_share_file)));
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
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addInputUri(data.getData());
                }
                return;
            }
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mOutputUri.add(data.getData());
                    encryptStart(false);
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    private class SelectedFilesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mInputUri.size();
        }

        @Override
        public Object getItem(int position) {
            return mInputUri.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.file_list_entry, null);
            } else {
                view = convertView;
            }
            ((TextView) view.findViewById(R.id.filename)).setText(FileHelper.getFilename(getActivity(), mInputUri.get(position)));
            long size = FileHelper.getFileSize(getActivity(), mInputUri.get(position));
            if (size == -1) {
                ((TextView) view.findViewById(R.id.filesize)).setText("");
            } else {
                ((TextView) view.findViewById(R.id.filesize)).setText(FileHelper.readableFileSize(size));
            }
            view.findViewById(R.id.action_remove_file_from_list).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delInputUri(position);
                }
            });
            int px = OtherHelper.dpToPx(getActivity(), 48);
            Bitmap bitmap = FileHelper.getThumbnail(getActivity(), mInputUri.get(position), new Point(px, px));
            if (bitmap != null) {
                ((ImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(bitmap);
            } else {
                ((ImageView) view.findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_doc_generic_am);
            }
            return view;
        }
    }
}
