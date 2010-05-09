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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.bouncycastle2.util.Strings;
import org.openintents.intents.FileManager;
import org.thialfihar.android.apg.Apg.GeneralException;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class EncryptActivity extends BaseActivity {
    private String mSubject = null;
    private String mSendTo = null;

    private long mEncryptionKeyIds[] = null;

    private EditText mMessage = null;
    private Button mSelectKeysButton = null;
    private Button mEncryptButton = null;
    private Button mEncryptToClipboardButton = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;

    private ViewFlipper mSource = null;
    private TextView mSourceLabel = null;
    private ImageView mSourcePrevious = null;
    private ImageView mSourceNext = null;

    private ViewFlipper mMode = null;
    private TextView mModeLabel = null;
    private ImageView mModePrevious = null;
    private ImageView mModeNext = null;

    private int mEncryptTarget;

    private EditText mPassPhrase = null;
    private EditText mPassPhraseAgain = null;
    private CheckBox mAsciiArmour = null;

    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt);

        mSource = (ViewFlipper) findViewById(R.id.source);
        mSourceLabel = (TextView) findViewById(R.id.source_label);
        mSourcePrevious = (ImageView) findViewById(R.id.source_previous);
        mSourceNext = (ImageView) findViewById(R.id.source_next);

        mSourcePrevious.setClickable(true);
        mSourcePrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_right_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_right_out));
                mSource.showPrevious();
                updateSource();
            }
        });

        mSourceNext.setClickable(true);
        OnClickListener nextSourceClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_left_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_left_out));
                mSource.showNext();
                updateSource();
            }
        };
        mSourceNext.setOnClickListener(nextSourceClickListener);

        mSourceLabel.setClickable(true);
        mSourceLabel.setOnClickListener(nextSourceClickListener);

        mMode = (ViewFlipper) findViewById(R.id.mode);
        mModeLabel = (TextView) findViewById(R.id.mode_label);
        mModePrevious = (ImageView) findViewById(R.id.mode_previous);
        mModeNext = (ImageView) findViewById(R.id.mode_next);

        mModePrevious.setClickable(true);
        mModePrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_right_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_right_out));
                mMode.showPrevious();
                updateMode();
            }
        });

        OnClickListener nextModeClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                    R.anim.push_left_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                                                                     R.anim.push_left_out));
                mMode.showNext();
                updateMode();
            }
        };
        mModeNext.setOnClickListener(nextModeClickListener);

        mModeLabel.setClickable(true);
        mModeLabel.setOnClickListener(nextModeClickListener);

        mMessage = (EditText) findViewById(R.id.message);
        mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
        mEncryptButton = (Button) findViewById(R.id.btn_encrypt);
        mEncryptToClipboardButton = (Button) findViewById(R.id.btn_encrypt_to_clipboard);
        mSign = (CheckBox) findViewById(R.id.sign);
        mMainUserId = (TextView) findViewById(R.id.main_user_id);
        mMainUserIdRest = (TextView) findViewById(R.id.main_user_id_rest);

        mPassPhrase = (EditText) findViewById(R.id.pass_phrase);
        mPassPhraseAgain = (EditText) findViewById(R.id.pass_phrase_again);

        // measure the height of the source_file view and set the message view's min height to that,
        // so it fills mSource fully... bit of a hack.
        View tmp = findViewById(R.id.source_file);
        tmp.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = tmp.getMeasuredHeight();
        mMessage.setMinimumHeight(height);

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        mDeleteAfter = (CheckBox) findViewById(R.id.delete_after_encryption);

        mAsciiArmour = (CheckBox) findViewById(R.id.ascii_armour);
        mAsciiArmour.setChecked(getDefaultAsciiArmour());
        mAsciiArmour.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                guessOutputFilename();
            }
        });

        mEncryptToClipboardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptToClipboardClicked();
            }
        });

        mEncryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked();
            }
        });

        mSelectKeysButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPublicKeys();
            }
        });

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

        Intent intent = getIntent();
        if (intent.getAction() != null &&
            (intent.getAction().equals(Apg.Intent.ENCRYPT) ||
             intent.getAction().equals(Apg.Intent.ENCRYPT_FILE))) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }
            String data = extras.getString("data");
            mSendTo = extras.getString("sendTo");
            mSubject = extras.getString("subject");
            long signatureKeyId = extras.getLong("signatureKeyId");
            long encryptionKeyIds[] = extras.getLongArray("encryptionKeyIds");
            if (signatureKeyId != 0) {
                PGPSecretKeyRing keyRing = Apg.findSecretKeyRing(signatureKeyId);
                PGPSecretKey masterKey = null;
                if (keyRing != null) {
                    masterKey = Apg.getMasterKey(keyRing);
                    if (masterKey != null) {
                        Vector<PGPSecretKey> signKeys = Apg.getUsableSigningKeys(keyRing);
                        if (signKeys.size() > 0) {
                            setSecretKeyId(masterKey.getKeyID());
                        }
                    }
                }
            }

            if (encryptionKeyIds != null) {
                Vector<Long> goodIds = new Vector<Long>();
                for (int i = 0; i < encryptionKeyIds.length; ++i) {
                    PGPPublicKeyRing keyRing = Apg.findPublicKeyRing(encryptionKeyIds[i]);
                    PGPPublicKey masterKey = null;
                    if (keyRing == null) {
                        continue;
                    }
                    masterKey = Apg.getMasterKey(keyRing);
                    if (masterKey == null) {
                        continue;
                    }
                    Vector<PGPPublicKey> encryptKeys = Apg.getUsableEncryptKeys(keyRing);
                    if (encryptKeys.size() == 0) {
                        continue;
                    }
                    goodIds.add(masterKey.getKeyID());
                }
                if (goodIds.size() > 0) {
                    mEncryptionKeyIds = new long[goodIds.size()];
                    for (int i = 0; i < goodIds.size(); ++i) {
                        mEncryptionKeyIds[i] = goodIds.get(i);
                    }
                }
            }

            if (intent.getAction().equals(Apg.Intent.ENCRYPT)) {
                if (data != null) {
                    mMessage.setText(data);
                }
                mSource.setInAnimation(null);
                mSource.setOutAnimation(null);
                while (mSource.getCurrentView().getId() != R.id.source_message) {
                    mSource.showNext();
                }
            } else if (intent.getAction().equals(Apg.Intent.ENCRYPT_FILE)) {
                mSource.setInAnimation(null);
                mSource.setOutAnimation(null);
                while (mSource.getCurrentView().getId() != R.id.source_file) {
                    mSource.showNext();
                }
            }
        }

        updateView();
        updateSource();
        updateMode();
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

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String ending = (mAsciiArmour.isChecked() ? ".asc" : ".gpg");
        mOutputFilename = Constants.path.app_dir + "/" + file.getName() + ending;
    }

    private void updateSource() {
        switch (mSource.getCurrentView().getId()) {
            case R.id.source_file: {
                mSourceLabel.setText(R.string.label_file);
                mEncryptButton.setText(R.string.btn_encrypt);
                mEncryptToClipboardButton.setEnabled(false);
                break;
            }

            case R.id.source_message: {
                mSourceLabel.setText(R.string.label_message);
                mEncryptButton.setText(R.string.btn_send);
                mEncryptToClipboardButton.setEnabled(true);
                break;
            }

            default: {
                break;
            }
        }
    }

    private void updateMode() {
        switch (mMode.getCurrentView().getId()) {
            case R.id.mode_asymmetric: {
                mModeLabel.setText(R.string.label_asymmetric);
                break;
            }

            case R.id.mode_symmetric: {
                mModeLabel.setText(R.string.label_symmetric);
                break;
            }

            default: {
                break;
            }
        }
    }

    private void encryptToClipboardClicked() {
        mEncryptTarget = Id.target.clipboard;
        initiateEncryption();
    }

    private void encryptClicked() {
        if (mSource.getCurrentView().getId() == R.id.source_file) {
            mEncryptTarget = Id.target.file;
        } else {
            mEncryptTarget = Id.target.email;
        }
        initiateEncryption();
    }

    private void initiateEncryption() {
        if (mEncryptTarget == Id.target.file) {
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
        }

        // symmetric encryption
        if (mMode.getCurrentView().getId() == R.id.mode_symmetric) {
            boolean gotPassPhrase = false;
            String passPhrase = mPassPhrase.getText().toString();
            String passPhraseAgain = mPassPhraseAgain.getText().toString();
            if (!passPhrase.equals(passPhraseAgain)) {
                Toast.makeText(this, "Pass phrases don't match.", Toast.LENGTH_SHORT).show();
                return;
            }

            gotPassPhrase = (passPhrase.length() != 0);
            if (!gotPassPhrase) {
                Toast.makeText(this, "Enter a pass phrase.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            boolean encryptIt = mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0;
            // for now require at least one form of encryption for files
            if (!encryptIt && mEncryptTarget == Id.target.file) {
                Toast.makeText(this, "Select at least one encryption key.",
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (!encryptIt && getSecretKeyId() == 0) {
                Toast.makeText(this, "Select at least one encryption key or a signature key.",
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSecretKeyId() != 0 && Apg.getPassPhrase() == null) {
                showDialog(Id.dialog.pass_phrase);
                return;
            }
        }

        if (mEncryptTarget == Id.target.file) {
            askForOutputFilename();
        } else {
            encryptStart();
        }
    }

    private void askForOutputFilename() {
        showDialog(Id.dialog.output_filename);
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        if (mEncryptTarget == Id.target.file) {
            askForOutputFilename();
        } else {
            encryptStart();
        }
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
            InputStream in;
            OutputStream out;
            long size;
            boolean useAsciiArmour = true;
            long encryptionKeyIds[] = null;
            long signatureKeyId = 0;
            boolean signOnly = false;

            String passPhrase = null;
            if (mMode.getCurrentView().getId() == R.id.mode_symmetric) {
                passPhrase = mPassPhrase.getText().toString();
                if (passPhrase.length() == 0) {
                    passPhrase = null;
                }
            } else {
                encryptionKeyIds = mEncryptionKeyIds;
                signatureKeyId = getSecretKeyId();
                signOnly = mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0;
            }

            if (mEncryptTarget == Id.target.file) {
                if (mInputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()) ||
                    mOutputFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        throw new GeneralException("external storage not ready");
                    }
                }

                in = new FileInputStream(mInputFilename);
                out = new FileOutputStream(mOutputFilename);

                File file = new File(mInputFilename);
                size = file.length();
                useAsciiArmour = mAsciiArmour.isChecked();
            } else {
                String message = mMessage.getText().toString();

                if (signOnly) {
                    // fix the message a bit, trailing spaces and newlines break stuff,
                    // because GMail sends as HTML and such things fuck up the signature,
                    // TODO: things like "<" and ">" also fuck up the signature
                    message = message.replaceAll(" +\n", "\n");
                    message = message.replaceAll("\n\n+", "\n\n");
                    message = message.replaceFirst("^\n+", "");
                    // make sure there'll be exactly one newline at the end
                    message = message.replaceFirst("\n*$", "\n");
                }

                byte[] byteData = Strings.toUTF8ByteArray(message);
                in = new ByteArrayInputStream(byteData);
                out = new ByteArrayOutputStream();

                size = byteData.length;
                useAsciiArmour = true;
            }

            if (signOnly) {
                Apg.signText(in, out, getSecretKeyId(),
                             Apg.getPassPhrase(), getDefaultHashAlgorithm(), this);
            } else {
                Apg.encrypt(in, out, size, useAsciiArmour,
                            encryptionKeyIds, signatureKeyId,
                            Apg.getPassPhrase(), this,
                            getDefaultEncryptionAlgorithm(), getDefaultHashAlgorithm(),
                            passPhrase);
            }

            out.close();
            if (mEncryptTarget != Id.target.file) {
                data.putString("message", new String(((ByteArrayOutputStream)out).toByteArray()));
            }
        } catch (IOException e) {
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

    private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyListActivity.class);
        intent.putExtra("selection", mEncryptionKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
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

        removeDialog(Id.dialog.encrypting);

        Bundle data = msg.getData();
        String error = data.getString("error");
        if (error != null) {
            Toast.makeText(EncryptActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
            return;
        } else {
            String message = data.getString("message");
            switch (mEncryptTarget) {
                case Id.target.clipboard: {
                    ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clip.setText(message);
                    Toast.makeText(this, "Successfully encrypted to clipboard.",
                                   Toast.LENGTH_SHORT).show();
                    break;
                }

                case Id.target.email: {
                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("text/plain; charset=utf-8");
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
                    if (mSubject != null) {
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                             mSubject);
                    }
                    if (mSendTo != null) {
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                             new String[] { mSendTo });
                    }
                    EncryptActivity.this.
                            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                }

                case Id.target.file: {
                    Toast.makeText(this, "Successfully encrypted.", Toast.LENGTH_SHORT).show();
                    if (mDeleteAfter.isChecked()) {
                        setDeleteFile(mInputFilename);
                        showDialog(Id.dialog.delete_file);
                    }
                    break;
                }

                default: {
                    // shouldn't happen
                    break;
                }
            }
        }
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
}