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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.security.SignatureException;
import java.util.regex.Matcher;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.util.Strings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DecryptMessageActivity extends BaseActivity {
    private long mSignatureKeyId = 0;

    private String mReplyTo = null;
    private String mSubject = null;
    private boolean mSignedOnly = false;

    private EditText mMessage = null;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mUserId = null;
    private TextView mUserIdRest = null;
    private Button mDecryptButton = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.decrypt_message);

        mMessage = (EditText) findViewById(R.id.message);
        mDecryptButton = (Button) findViewById(R.id.btn_decrypt);
        mSignatureLayout = (LinearLayout) findViewById(R.id.layout_signature);
        mSignatureStatusImage = (ImageView) findViewById(R.id.ic_signature_status);
        mUserId = (TextView) findViewById(R.id.main_user_id);
        mUserIdRest = (TextView) findViewById(R.id.main_user_id_rest);

        Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            try {
                InputStream attachment = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte bytes[] = new byte[1 << 16];
                int length;
                while ((length = attachment.read(bytes)) > 0) {
                    byteOut.write(bytes, 0, length);
                }
                byteOut.close();
                String data = Strings.fromUTF8ByteArray(byteOut.toByteArray());
                mMessage.setText(data);
            } catch (FileNotFoundException e) {
                // ignore, then
            } catch (IOException e) {
                // ignore, then
            }
        } else if (intent.getAction() != null && intent.getAction().equals(Apg.Intent.DECRYPT)) {
            String data = intent.getExtras().getString("data");
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
            mReplyTo = intent.getExtras().getString("replyTo");
            mSubject = intent.getExtras().getString("subject");
        } else {
            ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String data = "";
            Matcher matcher = Apg.PGP_MESSAGE.matcher(clip.getText());
            if (matcher.matches()) {
                data = matcher.group(1);
                mMessage.setText(data);
                Toast.makeText(this, R.string.using_clipboard_content, Toast.LENGTH_SHORT).show();
            }
        }

        mSignatureLayout.setVisibility(View.INVISIBLE);

        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptClicked();
            }
        });

        if (mMessage.getText().length() > 0) {
            mDecryptButton.performClick();
        }
    }

    private void decryptClicked() {
        String error = null;
        String messageData = mMessage.getText().toString();
        Matcher matcher = Apg.PGP_SIGNED_MESSAGE.matcher(messageData);
        if (matcher.matches()) {
            mSignedOnly = true;
            decryptStart();
            return;
        }

        // else treat it as an encrypted message
        mSignedOnly = false;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(messageData.getBytes());
            setSecretKeyId(Apg.getDecryptionKeyId(in));
            showDialog(Id.dialog.pass_phrase);
        } catch (IOException e) {
            error = e.getLocalizedMessage();
        } catch (Apg.GeneralException e) {
            error = e.getLocalizedMessage();
        } catch (Apg.NoAsymmetricEncryptionException e) {
            error = "no asymmetric encryption found";
        }
        if (error != null) {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    private void replyClicked() {
        Intent intent = new Intent(this, EncryptMessageActivity.class);
        intent.setAction(Apg.Intent.ENCRYPT);
        String data = mMessage.getText().toString();
        data = data.replaceAll("(?m)^", "> ");
        data = "\n\n" + data;
        intent.putExtra("data", data);
        intent.putExtra("subject", "Re: " + mSubject);
        intent.putExtra("sendTo", mReplyTo);
        intent.putExtra("eyId", mSignatureKeyId);
        intent.putExtra("signatureKeyId", getSecretKeyId());
        intent.putExtra("encryptionKeyIds", new long[] { mSignatureKeyId });
        startActivity(intent);
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        decryptStart();
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

        String messageData = mMessage.getText().toString();

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(messageData.getBytes());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (mSignedOnly) {
                data = Apg.verifyText(in, out, this);
            } else {
                data = Apg.decrypt(in, out, Apg.getPassPhrase(), this, false);
            }

            out.close();
            data.putString("decryptedMessage", Strings.fromUTF8ByteArray(out.toByteArray()));
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
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        Bundle data = msg.getData();
        removeDialog(Id.dialog.decrypting);
        mSignatureKeyId = 0;
        String error = data.getString("error");
        String decryptedMessage = data.getString("decryptedMessage");
        if (error != null) {
            Toast.makeText(DecryptMessageActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
        }
        mSignatureLayout.setVisibility(View.INVISIBLE);
        if (decryptedMessage != null) {
            mMessage.setText(decryptedMessage);
            mDecryptButton.setText(R.string.btn_reply);
            mDecryptButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    replyClicked();
                }
            });

            if (data.getBoolean("signature")) {
                String userId = data.getString("signatureUserId");
                mSignatureKeyId = data.getLong("signatureKeyId");
                mUserIdRest.setText("id: " + Long.toHexString(mSignatureKeyId & 0xffffffffL));
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
}
