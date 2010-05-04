/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.security.SignatureException;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.openpgp.PGPException;
import org.openintents.intents.FileManager;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DecryptFileActivity extends BaseActivity {
    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;
    private Button mDecryptButton = null;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mUserId = null;
    private TextView mUserIdRest = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    private boolean mAssumeSymmetricEncryption = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.decrypt_file);

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        mDeleteAfter = (CheckBox) findViewById(R.id.delete_after_decryption);

        mDecryptButton = (Button) findViewById(R.id.btn_decrypt);
        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptClicked();
            }
        });

        mSignatureLayout = (LinearLayout) findViewById(R.id.layout_signature);
        mSignatureStatusImage = (ImageView) findViewById(R.id.ic_signature_status);
        mUserId = (TextView) findViewById(R.id.main_user_id);
        mUserIdRest = (TextView) findViewById(R.id.main_user_id_rest);

        mSignatureLayout.setVisibility(View.INVISIBLE);
    }

    private void openFile() {
        String filename = mFilename.getText().toString();

        Intent intent = new Intent(FileManager.ACTION_PICK_FILE);

        intent.setData(Uri.parse("file://" + filename));

        intent.putExtra(FileManager.EXTRA_TITLE, "Select file to decrypt...");
        intent.putExtra(FileManager.EXTRA_BUTTON_TEXT, "Open");

        try {
            startActivityForResult(intent, Id.request.filename);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptClicked() {
        String error = null;
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            mInputFilename = mFilename.getText().toString();
            File file = new File(mInputFilename);
            String filename = file.getName();
            if (filename.endsWith(".asc") || filename.endsWith(".gpg")) {
                filename = filename.substring(0, filename.length() - 4);
            }
            mOutputFilename = Constants.path.app_dir + "/" + filename;
        }

        if (mInputFilename.equals("")) {
            Toast.makeText(this, "Select a file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(mInputFilename);
        if (!file.exists() || !file.isFile()) {
            Toast.makeText(this, "Error: file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InputStream in = new FileInputStream(mInputFilename);
            try {
                setSecretKeyId(Apg.getDecryptionKeyId(in));
                if (getSecretKeyId() == 0) {
                    throw new Apg.GeneralException("no suitable keys found");
                }
                mAssumeSymmetricEncryption = false;
            } catch (Apg.NoAsymmetricEncryptionException e) {
                setSecretKeyId(0);
                // reopen the file to check whether there's symmetric encryption data in there
                in = new FileInputStream(mInputFilename);
                if (!Apg.hasSymmetricEncryption(in)) {
                    throw new Apg.GeneralException("no known kind of encryption found");
                }
                mAssumeSymmetricEncryption = true;
           }

           showDialog(Id.dialog.pass_phrase);
        } catch (FileNotFoundException e) {
            error = "file not found: " + e.getLocalizedMessage();
        } catch (IOException e) {
            error = e.getLocalizedMessage();
        } catch (Apg.GeneralException e) {
            error = e.getLocalizedMessage();
        }
        if (error != null) {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        askForOutputFilename();
    }

    private void askForOutputFilename() {
        showDialog(Id.dialog.output_filename);
    }

    private void decryptStart() {
        showDialog(Id.dialog.decrypting);
        startThread();
    }

    @Override
    public void run() {
        String error = null;
        Security.addProvider(new BouncyCastleProvider());

        Bundle data = new Bundle();
        Message msg = new Message();

        try {
            InputStream in = new FileInputStream(mInputFilename);
            OutputStream out = new FileOutputStream(mOutputFilename);

            data = Apg.decrypt(in, out, Apg.getPassPhrase(), this, mAssumeSymmetricEncryption);

            out.close();
        } catch (PGPException e) {
            error = e.getMessage();
        } catch (IOException e) {
            error = e.getMessage();
        } catch (SignatureException e) {
            error = e.getMessage();
            e.printStackTrace();
        } catch (Apg.GeneralException e) {
            error = e.getMessage();
        }

        data.putInt("type", Id.message.done);

        if (error != null) {
            data.putString("error", error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.output_filename: {
                return FileDialog.build(this, "Decrypt to file",
                                        "Please specify which file to decrypt to.\n" +
                                        "WARNING! File will be overwritten if it exists.",
                                        mOutputFilename,
                                        new FileDialog.OnClickListener() {

                                            @Override
                                            public void onOkClick(String filename) {
                                                removeDialog(Id.dialog.output_filename);
                                                mOutputFilename = filename;
                                                decryptStart();
                                            }

                                            @Override
                                            public void onCancelClick() {
                                                removeDialog(Id.dialog.output_filename);
                                            }
                                        },
                                        getString(R.string.filemanager_title_save),
                                        getString(R.string.filemanager_btn_save),
                                        Id.request.output_filename);
            }

            default: {
                break;
            }
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.filename: {
                if (resultCode == RESULT_OK && data != null) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        // Get rid of URI prefix:
                        if (filename.startsWith("file://")) {
                            filename = filename.substring(7);
                        }
                        // replace %20 and so on
                        filename = Uri.decode(filename);

                        mFilename.setText(filename);
                    }
                }
                return;
            }

            case Id.request.output_filename: {
                if (resultCode == RESULT_OK && data != null) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        // Get rid of URI prefix:
                        if (filename.startsWith("file://")) {
                            filename = filename.substring(7);
                        }
                        // replace %20 and so on
                        filename = Uri.decode(filename);

                        FileDialog.setFilename(filename);
                    }
                }
                return;
            }

            default: {
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);
        Bundle data = msg.getData();
        removeDialog(Id.dialog.decrypting);

        String error = data.getString("error");
        if (error != null) {
            Toast.makeText(DecryptFileActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DecryptFileActivity.this,
                           "Successfully decrypted.",
                           Toast.LENGTH_SHORT).show();
            if (mDeleteAfter.isChecked()) {
                setDeleteFile(mInputFilename);
                showDialog(Id.dialog.delete_file);
            }
        }

        mSignatureLayout.setVisibility(View.INVISIBLE);
        if (data.getBoolean("signature")) {
            String userId = data.getString("signatureUserId");
            long signatureKeyId = data.getLong("signatureKeyId");
            mUserIdRest.setText("id: " + Long.toHexString(signatureKeyId & 0xffffffffL));
            if (userId == null) {
                userId = getResources().getString(R.string.unknown_user_id);
            }
            String chunks[] = userId.split(" <", 2);
            userId = chunks[0];
            if (chunks.length > 1) {
                mUserIdRest.setText("<" + chunks[1]);
            }
            mUserId.setText(userId);

            if (data.getBoolean("signatureSuccess")) {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
            } else if (data.getBoolean("signatureUnknown")) {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            } else {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            mSignatureLayout.setVisibility(View.VISIBLE);
        }
    }
}
