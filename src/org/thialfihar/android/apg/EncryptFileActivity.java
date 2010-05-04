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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.bouncycastle2.openpgp.PGPEncryptedData;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.openintents.intents.FileManager;
import org.thialfihar.android.apg.Apg.GeneralException;
import org.thialfihar.android.apg.utils.Choice;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class EncryptFileActivity extends BaseActivity {
    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;
    private Spinner mAlgorithm = null;
    private EditText mPassPhrase = null;
    private EditText mPassPhraseAgain = null;
    private CheckBox mAsciiArmour = null;
    private RadioGroup mEncryptionMode = null;
    private ViewGroup mAsymmetricLayout = null;
    private ViewGroup mSymmetricLayout = null;
    private Button mEncryptButton = null;
    private Button mSelectKeysButton = null;

    private long mEncryptionKeyIds[] = null;
    private String mInputFilename = null;
    private String mOutputFilename = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt_file);

        mAsciiArmour = (CheckBox) findViewById(R.id.ascii_armour);
        mAsciiArmour.setChecked(getDefaultAsciiArmour());
        mAsciiArmour.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                guessOutputFilename();
            }
        });

        // asymmetric tab
        mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
        mSelectKeysButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPublicKeys();
            }
        });

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        mDeleteAfter = (CheckBox) findViewById(R.id.delete_after_encryption);

        mEncryptButton = (Button) findViewById(R.id.btn_encrypt);
        mSign = (CheckBox) findViewById(R.id.sign);
        mMainUserId = (TextView) findViewById(R.id.main_user_id);
        mMainUserIdRest = (TextView) findViewById(R.id.main_user_id_rest);

        mSign.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    selectSecretKey();
                } else {
                    setSecretKeyId(0);
                    Apg.setPassPhrase(null);
                    updateView();
                }
            }
        });

        // symmetric tab

        mAlgorithm = (Spinner) findViewById(R.id.algorithm);
        Choice choices[] = {
                new Choice(PGPEncryptedData.AES_128, "AES 128"),
                new Choice(PGPEncryptedData.AES_192, "AES 192"),
                new Choice(PGPEncryptedData.AES_256, "AES 256"),
                new Choice(PGPEncryptedData.BLOWFISH, "Blowfish"),
                new Choice(PGPEncryptedData.TWOFISH, "Twofish"),
                new Choice(PGPEncryptedData.CAST5, "CAST5"),
                new Choice(PGPEncryptedData.DES, "DES"),
                new Choice(PGPEncryptedData.TRIPLE_DES, "Triple DES"),
                new Choice(PGPEncryptedData.IDEA, "IDEA"),
        };
        ArrayAdapter<Choice> adapter =
                new ArrayAdapter<Choice>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlgorithm.setAdapter(adapter);
        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == getDefaultEncryptionAlgorithm()) {
                mAlgorithm.setSelection(i);
                break;
            }
        }

        mEncryptionMode = (RadioGroup) findViewById(R.id.encryption_mode);
        mAsymmetricLayout = (ViewGroup) findViewById(R.id.layout_asymmetric);
        mSymmetricLayout = (ViewGroup) findViewById(R.id.layout_symmetric);
        mPassPhrase = (EditText) findViewById(R.id.pass_phrase);
        mPassPhraseAgain = (EditText) findViewById(R.id.pass_phrase_again);

        mEncryptionMode.check(R.id.use_asymmetric);
        mEncryptionMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.use_symmetric) {
                    mAsymmetricLayout.setVisibility(ViewGroup.GONE);
                    mSymmetricLayout.setVisibility(ViewGroup.VISIBLE);
                    mEncryptionKeyIds = null;
                    setSecretKeyId(0);
                } else {
                    mAsymmetricLayout.setVisibility(ViewGroup.VISIBLE);
                    mSymmetricLayout.setVisibility(ViewGroup.GONE);
                    mPassPhrase.setText("");
                    mPassPhraseAgain.setText("");
                }
                updateView();
            }
        });
        if (mEncryptionMode.getCheckedRadioButtonId() == R.id.use_symmetric) {
            mAsymmetricLayout.setVisibility(ViewGroup.GONE);
            mSymmetricLayout.setVisibility(ViewGroup.VISIBLE);
        } else {
            mAsymmetricLayout.setVisibility(ViewGroup.VISIBLE);
            mSymmetricLayout.setVisibility(ViewGroup.GONE);
        }

        mEncryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked();
            }
        });

        updateView();
    }

    private void updateView() {
        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(R.string.no_keys_selected);
        } else if (mEncryptionKeyIds.length == 1) {
            mSelectKeysButton.setText(R.string.one_key_selected);
        } else {
            mSelectKeysButton.setText("" + mEncryptionKeyIds.length + " " +
                                      getResources().getString(R.string.n_keys_selected));
        }

        if (getSecretKeyId() == 0) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRing(getSecretKeyId());
            if (keyRing != null) {
                PGPSecretKey key = Apg.getMasterKey(keyRing);
                if (key != null) {
                    String userId = Apg.getMainUserIdSafe(this, key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mMainUserId.setText(uid);
            mMainUserIdRest.setText(uidExtra);
            mSign.setChecked(true);
        }
    }

    private void openFile() {
        String filename = mFilename.getText().toString();

        Intent intent = new Intent(FileManager.ACTION_PICK_FILE);

        intent.setData(Uri.parse("file://" + filename));

        intent.putExtra(FileManager.EXTRA_TITLE, "Select file to encrypt...");
        intent.putExtra(FileManager.EXTRA_BUTTON_TEXT, "Open");

        try {
            startActivityForResult(intent, Id.request.filename);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyListActivity.class);
        intent.putExtra("selection", mEncryptionKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String ending = (mAsciiArmour.isChecked() ? ".asc" : ".gpg");
        mOutputFilename = Constants.path.app_dir + "/" + file.getName() + ending;
    }

    private void encryptClicked() {
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            guessOutputFilename();
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

        // symmetric encryption
        if (mEncryptionMode.getCheckedRadioButtonId() == R.id.use_symmetric) {
            boolean gotPassPhrase = false;
            String passPhrase = mPassPhrase.getText().toString();
            String passPhraseAgain = mPassPhraseAgain.getText().toString();
            if (!passPhrase.equals(passPhraseAgain)) {
                Toast.makeText(this, "Pass phrases don't match.",
                               Toast.LENGTH_SHORT).show();
                return;
            }

            gotPassPhrase = (passPhrase.length() != 0);
            if (!gotPassPhrase) {
                Toast.makeText(this, "Enter a pass phrase.",
                               Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            boolean encryptIt = mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0;
            // for now require at least one form of encryption
            if (!encryptIt) {
                Toast.makeText(this, "Select at least one encryption key.",
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSecretKeyId() != 0 && Apg.getPassPhrase() == null) {
                showDialog(Id.dialog.pass_phrase);
                return;
            }
        }

        askForOutputFilename();
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        askForOutputFilename();
    }

    private void askForOutputFilename() {
        showDialog(Id.dialog.output_filename);
    }

    private void encryptStart() {
        showDialog(Id.dialog.encrypting);
        startThread();
    }

    @Override
    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        try {
            if (mInputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()) ||
                mOutputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    throw new GeneralException("external storage not ready");
                }
            }

            InputStream in = new FileInputStream(mInputFilename);
            OutputStream out = new FileOutputStream(mOutputFilename);

            String passPhrase = null;
            if (mEncryptionMode.getCheckedRadioButtonId() == R.id.use_symmetric) {
                passPhrase = mPassPhrase.getText().toString();
                if (passPhrase.length() == 0) {
                    passPhrase = null;
                }
            }

            File file = new File(mInputFilename);
            long fileSize = file.length();

            Apg.encrypt(in, out, fileSize, mAsciiArmour.isChecked(),
                        mEncryptionKeyIds, getSecretKeyId(),
                        Apg.getPassPhrase(), this,
                        ((Choice) mAlgorithm.getSelectedItem()).getId(),
                        getDefaultHashAlgorithm(),
                        passPhrase);

            out.close();
        } catch (FileNotFoundException e) {
            error = "file not found: " + e.getMessage();
        }
        catch (IOException e) {
            error = e.getMessage();
        } catch (PGPException e) {
            error = e.getMessage();
        } catch (NoSuchProviderException e) {
            error = e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            error = e.getMessage();
        } catch (SignatureException e) {
            error = e.getMessage();
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
                return FileDialog.build(this, "Encrypt to file",
                                        "Please specify which file to encrypt to.\n" +
                                        "WARNING! File will be overwritten if it exists.",
                                        mOutputFilename,
                                        new FileDialog.OnClickListener() {

                                            @Override
                                            public void onOkClick(String filename) {
                                                removeDialog(Id.dialog.output_filename);
                                                mOutputFilename = filename;
                                                encryptStart();
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

            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                updateView();
                break;
            }

            case Id.request.public_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    mEncryptionKeyIds = bundle.getLongArray("selection");
                }
                updateView();
                break;
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
        removeDialog(Id.dialog.encrypting);

        String error = data.getString("error");
        if (error != null) {
            Toast.makeText(EncryptFileActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(EncryptFileActivity.this,
                           "Successfully encrypted.",
                           Toast.LENGTH_SHORT).show();
            if (mDeleteAfter.isChecked()) {
                setDeleteFile(mInputFilename);
                showDialog(Id.dialog.delete_file);
            }
        }
    }
}
