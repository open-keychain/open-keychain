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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.security.SignatureException;
import java.util.regex.Matcher;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.thialfihar.android.apg.provider.DataProvider;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class DecryptActivity extends BaseActivity {
    private long mSignatureKeyId = 0;

    private Intent mIntent;

    private boolean mReturnResult = false;
    private String mReplyTo = null;
    private String mSubject = null;
    private boolean mSignedOnly = false;
    private boolean mAssumeSymmetricEncryption = false;

    private EditText mMessage = null;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mUserId = null;
    private TextView mUserIdRest = null;

    private ViewFlipper mSource = null;
    private TextView mSourceLabel = null;
    private ImageView mSourcePrevious = null;
    private ImageView mSourceNext = null;

    private Button mDecryptButton = null;
    private Button mReplyButton = null;

    private int mDecryptTarget;

    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    private Uri mContentUri = null;
    private byte[] mData = null;
    private boolean mReturnBinary = false;

    private DataSource mDataSource = null;
    private DataDestination mDataDestination = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.decrypt);

        mSource = (ViewFlipper) findViewById(R.id.source);
        mSourceLabel = (TextView) findViewById(R.id.sourceLabel);
        mSourcePrevious = (ImageView) findViewById(R.id.sourcePrevious);
        mSourceNext = (ImageView) findViewById(R.id.sourceNext);

        mSourcePrevious.setClickable(true);
        mSourcePrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(DecryptActivity.this,
                                                                    R.anim.push_right_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(DecryptActivity.this,
                                                                     R.anim.push_right_out));
                mSource.showPrevious();
                updateSource();
            }
        });

        mSourceNext.setClickable(true);
        OnClickListener nextSourceClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(DecryptActivity.this,
                                                                    R.anim.push_left_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(DecryptActivity.this,
                                                                     R.anim.push_left_out));
                mSource.showNext();
                updateSource();
            }
        };
        mSourceNext.setOnClickListener(nextSourceClickListener);

        mSourceLabel.setClickable(true);
        mSourceLabel.setOnClickListener(nextSourceClickListener);

        mMessage = (EditText) findViewById(R.id.message);
        mDecryptButton = (Button) findViewById(R.id.btn_decrypt);
        mReplyButton = (Button) findViewById(R.id.btn_reply);
        mSignatureLayout = (LinearLayout) findViewById(R.id.signature);
        mSignatureStatusImage = (ImageView) findViewById(R.id.ic_signature_status);
        mUserId = (TextView) findViewById(R.id.mainUserId);
        mUserIdRest = (TextView) findViewById(R.id.mainUserIdRest);

        // measure the height of the source_file view and set the message view's min height to that,
        // so it fills mSource fully... bit of a hack.
        View tmp = findViewById(R.id.sourceFile);
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

        mDeleteAfter = (CheckBox) findViewById(R.id.deleteAfterDecryption);

        // default: message source
        mSource.setInAnimation(null);
        mSource.setOutAnimation(null);
        while (mSource.getCurrentView().getId() != R.id.sourceMessage) {
            mSource.showNext();
        }

        mIntent = getIntent();
        if (Intent.ACTION_VIEW.equals(mIntent.getAction())) {
            Uri uri = mIntent.getData();
            try {
                InputStream attachment = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte bytes[] = new byte[1 << 16];
                int length;
                while ((length = attachment.read(bytes)) > 0) {
                    byteOut.write(bytes, 0, length);
                }
                byteOut.close();
                String data = new String(byteOut.toByteArray());
                mMessage.setText(data);
            } catch (FileNotFoundException e) {
                // ignore, then
            } catch (IOException e) {
                // ignore, then
            }
        } else if (Apg.Intent.DECRYPT.equals(mIntent.getAction())) {
            Bundle extras = mIntent.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }

            mData = extras.getByteArray(Apg.EXTRA_DATA);
            String textData = null;
            if (mData == null) {
                textData = extras.getString(Apg.EXTRA_TEXT);
            }
            if (textData != null) {
                Matcher matcher = Apg.PGP_MESSAGE.matcher(textData);
                if (matcher.matches()) {
                    textData = matcher.group(1);
                    // replace non breakable spaces
                    textData = textData.replaceAll("\\xa0", " ");
                    mMessage.setText(textData);
                } else {
                    matcher = Apg.PGP_SIGNED_MESSAGE.matcher(textData);
                    if (matcher.matches()) {
                        textData = matcher.group(1);
                        // replace non breakable spaces
                        textData = textData.replaceAll("\\xa0", " ");
                        mMessage.setText(textData);
                        mDecryptButton.setText(R.string.btn_verify);
                    }
                }
            }
            mReplyTo = extras.getString(Apg.EXTRA_REPLY_TO);
            mSubject = extras.getString(Apg.EXTRA_SUBJECT);
        } else if (Apg.Intent.DECRYPT_FILE.equals(mIntent.getAction())) {
            mInputFilename = mIntent.getDataString();
            if ("file".equals(mIntent.getScheme())) {
                mInputFilename = Uri.decode(mInputFilename.substring(7));
            }
            mFilename.setText(mInputFilename);
            guessOutputFilename();
            mSource.setInAnimation(null);
            mSource.setOutAnimation(null);
            while (mSource.getCurrentView().getId() != R.id.sourceFile) {
                mSource.showNext();
            }
        } else if (Apg.Intent.DECRYPT_AND_RETURN.equals(mIntent.getAction())) {
            mContentUri = mIntent.getData();
            Bundle extras = mIntent.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }

            mReturnBinary = extras.getBoolean(Apg.EXTRA_BINARY, false);

            if (mContentUri == null) {
                mData = extras.getByteArray(Apg.EXTRA_DATA);
                String data = extras.getString(Apg.EXTRA_TEXT);
                if (data != null) {
                    Matcher matcher = Apg.PGP_MESSAGE.matcher(data);
                    if (matcher.matches()) {
                        data = matcher.group(1);
                        // replace non breakable spaces
                        data = data.replaceAll("\\xa0", " ");
                        mMessage.setText(data);
                    } else {
                        matcher = Apg.PGP_SIGNED_MESSAGE.matcher(data);
                        if (matcher.matches()) {
                            data = matcher.group(1);
                            // replace non breakable spaces
                            data = data.replaceAll("\\xa0", " ");
                            mMessage.setText(data);
                            mDecryptButton.setText(R.string.btn_verify);
                        }
                    }
                }
            }
            mReturnResult = true;
        }

        if (mSource.getCurrentView().getId() == R.id.sourceMessage &&
            mMessage.getText().length() == 0) {
            ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String data = "";
            Matcher matcher = Apg.PGP_MESSAGE.matcher(clip.getText());
            if (!matcher.matches()) {
                matcher = Apg.PGP_SIGNED_MESSAGE.matcher(clip.getText());
            }
            if (matcher.matches()) {
                data = matcher.group(1);
                mMessage.setText(data);
                Toast.makeText(this, R.string.usingClipboardContent, Toast.LENGTH_SHORT).show();
            }
        }

        mSignatureLayout.setVisibility(View.GONE);
        mSignatureLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSignatureKeyId == 0) {
                    return;
                }
                PGPPublicKeyRing key = Apg.getPublicKeyRing(mSignatureKeyId);
                if (key != null) {
                    Intent intent = new Intent(DecryptActivity.this, KeyServerQueryActivity.class);
                    intent.setAction(Apg.Intent.LOOK_UP_KEY_ID);
                    intent.putExtra(Apg.EXTRA_KEY_ID, mSignatureKeyId);
                    startActivity(intent);
                }
            }
        });

        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptClicked();
            }
        });

        mReplyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                replyClicked();
            }
        });
        mReplyButton.setVisibility(View.INVISIBLE);

        if (mReturnResult) {
            mSourcePrevious.setClickable(false);
            mSourcePrevious.setEnabled(false);
            mSourcePrevious.setVisibility(View.INVISIBLE);

            mSourceNext.setClickable(false);
            mSourceNext.setEnabled(false);
            mSourceNext.setVisibility(View.INVISIBLE);

            mSourceLabel.setClickable(false);
            mSourceLabel.setEnabled(false);
        }

        updateSource();

        if (mSource.getCurrentView().getId() == R.id.sourceMessage &&
            mMessage.getText().length() > 0) {
             mDecryptButton.performClick();
        }
    }

    private void openFile() {
        String filename = mFilename.getText().toString();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType("*/*");

        try {
            startActivityForResult(intent, Id.request.filename);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.noFilemanagerInstalled, Toast.LENGTH_SHORT).show();
        }
    }

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String filename = file.getName();
        if (filename.endsWith(".asc") || filename.endsWith(".gpg") || filename.endsWith(".pgp")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        mOutputFilename = Constants.path.app_dir + "/" + filename;
    }

    private void updateSource() {
        switch (mSource.getCurrentView().getId()) {
            case R.id.sourceFile: {
                mSourceLabel.setText(R.string.label_file);
                mDecryptButton.setText(R.string.btn_decrypt);
                break;
            }

            case R.id.sourceMessage: {
                mSourceLabel.setText(R.string.label_message);
                mDecryptButton.setText(R.string.btn_decrypt);
                break;
            }

            default: {
                break;
            }
        }
    }

    private void decryptClicked() {
        if (mSource.getCurrentView().getId() == R.id.sourceFile) {
            mDecryptTarget = Id.target.file;
        } else {
            mDecryptTarget = Id.target.message;
        }
        initiateDecryption();
    }

    private void initiateDecryption() {
        if (mDecryptTarget == Id.target.file) {
            String currentFilename = mFilename.getText().toString();
            if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
                guessOutputFilename();
            }

            if (mInputFilename.equals("")) {
                Toast.makeText(this, R.string.noFileSelected, Toast.LENGTH_SHORT).show();
                return;
            }

            if (mInputFilename.startsWith("file")) {
                File file = new File(mInputFilename);
                if (!file.exists() || !file.isFile()) {
                    Toast.makeText(this, getString(R.string.errorMessage,
                                                   getString(R.string.error_fileNotFound)),
                                   Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (mDecryptTarget == Id.target.message) {
            String messageData = mMessage.getText().toString();
            Matcher matcher = Apg.PGP_SIGNED_MESSAGE.matcher(messageData);
            if (matcher.matches()) {
                mSignedOnly = true;
                decryptStart();
                return;
            }
        }

        // else treat it as an decrypted message/file
        mSignedOnly = false;
        String error = null;
        fillDataSource();
        try {
            InputData in = mDataSource.getInputData(this, false);
            try {
                setSecretKeyId(Apg.getDecryptionKeyId(this, in));
                if (getSecretKeyId() == Id.key.none) {
                    throw new Apg.GeneralException(getString(R.string.error_noSecretKeyFound));
                }
                mAssumeSymmetricEncryption = false;
            } catch (Apg.NoAsymmetricEncryptionException e) {
                setSecretKeyId(Id.key.symmetric);
                in = mDataSource.getInputData(this, false);
                if (!Apg.hasSymmetricEncryption(this, in)) {
                    throw new Apg.GeneralException(getString(R.string.error_noKnownEncryptionFound));
                }
                mAssumeSymmetricEncryption = true;
            }

            if (getSecretKeyId() == Id.key.symmetric ||
                Apg.getCachedPassPhrase(getSecretKeyId()) == null) {
                showDialog(Id.dialog.pass_phrase);
            } else {
                if (mDecryptTarget == Id.target.file) {
                    askForOutputFilename();
                } else {
                    decryptStart();
                }
            }
        } catch (FileNotFoundException e) {
            error = getString(R.string.error_fileNotFound);
        } catch (IOException e) {
            error = "" + e;
        } catch (Apg.GeneralException e) {
            error = "" + e;
        }
        if (error != null) {
            Toast.makeText(this, getString(R.string.errorMessage, error),
                           Toast.LENGTH_SHORT).show();
        }
    }

    private void replyClicked() {
        Intent intent = new Intent(this, EncryptActivity.class);
        intent.setAction(Apg.Intent.ENCRYPT);
        String data = mMessage.getText().toString();
        data = data.replaceAll("(?m)^", "> ");
        data = "\n\n" + data;
        intent.putExtra(Apg.EXTRA_TEXT, data);
        intent.putExtra(Apg.EXTRA_SUBJECT, "Re: " + mSubject);
        intent.putExtra(Apg.EXTRA_SEND_TO, mReplyTo);
        intent.putExtra(Apg.EXTRA_SIGNATURE_KEY_ID, getSecretKeyId());
        intent.putExtra(Apg.EXTRA_ENCRYPTION_KEY_IDS, new long[] { mSignatureKeyId });
        startActivity(intent);
    }

    private void askForOutputFilename() {
        showDialog(Id.dialog.output_filename);
    }

    @Override
    public void passPhraseCallback(long keyId, String passPhrase) {
        super.passPhraseCallback(keyId, passPhrase);
        if (mDecryptTarget == Id.target.file) {
            askForOutputFilename();
        } else {
            decryptStart();
        }
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
        fillDataSource();
        fillDataDestination();
        try {
            InputData in = mDataSource.getInputData(this, true);
            OutputStream out = mDataDestination.getOutputStream(this);

            if (mSignedOnly) {
                data = Apg.verifyText(this, in, out, this, getRunningThread(), getHandler());
            } else {
                data = Apg.decrypt(this, in, out, Apg.getCachedPassPhrase(getSecretKeyId()),
                                   this, mAssumeSymmetricEncryption);
            }

            out.close();

            if (mDataDestination.getStreamFilename() != null) {
                data.putString(Apg.EXTRA_RESULT_URI, "content://" + DataProvider.AUTHORITY +
                               "/data/" + mDataDestination.getStreamFilename());
            } else if (mDecryptTarget == Id.target.message) {
                if (mReturnBinary) {
                    data.putByteArray(Apg.EXTRA_DECRYPTED_DATA,
                                      ((ByteArrayOutputStream) out).toByteArray());
                } else {
                    data.putString(Apg.EXTRA_DECRYPTED_MESSAGE,
                                   new String(((ByteArrayOutputStream) out).toByteArray()));
                }
            }
        } catch (PGPException e) {
            error = "" + e;
        } catch (IOException e) {
            error = "" + e;
        } catch (SignatureException e) {
            error = "" + e;
        } catch (Apg.GeneralException e) {
            error = "" + e;
        }

        data.putInt(Constants.extras.status, Id.message.done);

        if (error != null) {
            data.putString(Apg.EXTRA_ERROR, error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    public void handlerCallback(Message msg) {
        Bundle data = msg.getData();
        if (data == null) {
            return;
        }

        if (data.getInt(Constants.extras.status) == Id.message.unknown_signature_key) {

        }

        super.handlerCallback(msg);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        Bundle data = msg.getData();
        removeDialog(Id.dialog.decrypting);
        mSignatureKeyId = 0;
        mSignatureLayout.setVisibility(View.GONE);
        mReplyButton.setVisibility(View.INVISIBLE);

        String error = data.getString(Apg.EXTRA_ERROR);
        if (error != null) {
            Toast.makeText(this, getString(R.string.errorMessage, error), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.decryptionSuccessful, Toast.LENGTH_SHORT).show();
        if (mReturnResult) {
            Intent intent = new Intent();
            intent.putExtras(data);
            setResult(RESULT_OK, intent);
            finish();
            return;
        }

        switch (mDecryptTarget) {
            case Id.target.message: {
                String decryptedMessage = data.getString(Apg.EXTRA_DECRYPTED_MESSAGE);
                mMessage.setText(decryptedMessage);
                mMessage.setHorizontallyScrolling(false);
                mReplyButton.setVisibility(View.VISIBLE);
                break;
            }

            case Id.target.file: {
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

        if (data.getBoolean(Apg.EXTRA_SIGNATURE)) {
            String userId = data.getString(Apg.EXTRA_SIGNATURE_USER_ID);
            mSignatureKeyId = data.getLong(Apg.EXTRA_SIGNATURE_KEY_ID);
            mUserIdRest.setText("id: " + Apg.getFingerPrint(mSignatureKeyId));
            if (userId == null) {
                userId = getResources().getString(R.string.unknownUserId);
            }
            String chunks[] = userId.split(" <", 2);
            userId = chunks[0];
            if (chunks.length > 1) {
                mUserIdRest.setText("<" + chunks[1]);
            }
            mUserId.setText(userId);

            if (data.getBoolean(Apg.EXTRA_SIGNATURE_SUCCESS)) {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
            } else if (data.getBoolean(Apg.EXTRA_SIGNATURE_UNKNOWN)) {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                Toast.makeText(this, R.string.unknownSignatureKeyTouchToLookUp, Toast.LENGTH_LONG).show();
            } else {
                mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            mSignatureLayout.setVisibility(View.VISIBLE);
        }
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
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.output_filename: {
                return FileDialog.build(this, getString(R.string.title_decryptToFile),
                                        getString(R.string.specifyFileToDecryptTo),
                                        mOutputFilename,
                                        new FileDialog.OnClickListener() {
                                            @Override
                                            public void onOkClick(String filename, boolean checked) {
                                                removeDialog(Id.dialog.output_filename);
                                                mOutputFilename = filename;
                                                decryptStart();
                                            }

                                            @Override
                                            public void onCancelClick() {
                                                removeDialog(Id.dialog.output_filename);
                                            }
                                        },
                                        getString(R.string.filemanager_titleSave),
                                        getString(R.string.filemanager_btnSave),
                                        null,
                                        Id.request.output_filename);
            }

            default: {
                break;
            }
        }

        return super.onCreateDialog(id);
    }

    protected void fillDataSource() {
        mDataSource = new DataSource();
        if (mContentUri != null) {
            mDataSource.setUri(mContentUri);
        } else if (mDecryptTarget == Id.target.file) {
            mDataSource.setUri(mInputFilename);
        } else {
            if (mData != null) {
                mDataSource.setData(mData);
            } else {
                mDataSource.setText(mMessage.getText().toString());
            }
        }
    }

    protected void fillDataDestination() {
        mDataDestination = new DataDestination();
        if (mContentUri != null) {
            mDataDestination.setMode(Id.mode.stream);
        } else if (mDecryptTarget == Id.target.file) {
            mDataDestination.setFilename(mOutputFilename);
            mDataDestination.setMode(Id.mode.file);
        } else {
            mDataDestination.setMode(Id.mode.byte_array);
        }
    }
}
