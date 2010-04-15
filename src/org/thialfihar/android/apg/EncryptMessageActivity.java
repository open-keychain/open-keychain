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
import java.io.IOException;
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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EncryptMessageActivity extends Activity
                                    implements Runnable, ProgressDialogUpdater,
                                               AskForSecretKeyPassPhrase.PassPhraseCallbackInterface {
    static final int GET_PUCLIC_KEYS = 1;
    static final int GET_SECRET_KEY = 2;

    static final int DIALOG_ENCRYPTING = 1;

    static final int MESSAGE_PROGRESS_UPDATE = 1;
    static final int MESSAGE_DONE = 2;

    private String mSubject = null;
    private String mSendTo = null;

    private long mEncryptionKeyIds[] = null;
    private long mSignatureKeyId = 0;

    private ProgressDialog mProgressDialog = null;
    private Thread mRunningThread = null;

    private EditText mMessage = null;
    private Button mSelectKeysButton = null;
    private Button mSendButton = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;

    private Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message mSg) {
            Bundle data = mSg.getData();
            if (data != null) {
                int type = data.getInt("type");
                switch (type) {
                    case MESSAGE_PROGRESS_UPDATE: {
                        String message = data.getString("message");
                        if (mProgressDialog != null) {
                            if (message != null) {
                                mProgressDialog.setMessage(message);
                            }
                            mProgressDialog.setMax(data.getInt("max"));
                            mProgressDialog.setProgress(data.getInt("progress"));
                        }
                        break;
                    }

                    case MESSAGE_DONE: {
                        removeDialog(DIALOG_ENCRYPTING);
                        mProgressDialog = null;

                        String error = data.getString("error");
                        if (error != null) {
                            Toast.makeText(EncryptMessageActivity.this,
                                           "Error: " + data.getString("error"),
                                           Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            String message = data.getString("message");
                            String signature = data.getString("signature");
                            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                            emailIntent.setType("text/plain; charset=utf-8");
                            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
                            if (signature != null) {
                                String fullText = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                                                  "Hash: SHA256\n" + "\n" +
                                                  message + "\n" + signature;
                                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, fullText);
                            }
                            if (mSubject != null) {
                                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                                     mSubject);
                            }
                            if (mSendTo != null) {
                                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                                     new String[] { mSendTo });
                            }
                            EncryptMessageActivity.this.
                                    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                        }
                        break;
                    }

                    default: {
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", MESSAGE_PROGRESS_UPDATE);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mhandler.sendMessage(msg);
    }

    @Override
    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", MESSAGE_PROGRESS_UPDATE);
        data.putString("message", message);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mhandler.sendMessage(msg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt_message);

        Apg.initialize(this);

        mMessage = (EditText) findViewById(R.id.message);
        mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
        mSendButton = (Button) findViewById(R.id.btn_send);
        mSign = (CheckBox) findViewById(R.id.sign);
        mMainUserId = (TextView) findViewById(R.id.main_user_id);
        mMainUserIdRest = (TextView) findViewById(R.id.main_user_id_rest);

        Intent intent = getIntent();
        if (intent.getAction() != null &&
            intent.getAction().equals(Apg.Intent.ENCRYPT)) {
            String data = intent.getExtras().getString("data");
            mSendTo = intent.getExtras().getString("sendTo");
            mSubject = intent.getExtras().getString("subject");
            long signatureKeyId = intent.getExtras().getLong("signatureKeyId");
            long encryptionKeyIds[] = intent.getExtras().getLongArray("encryptionKeyIds");
            if (signatureKeyId != 0) {
                PGPSecretKeyRing keyRing = Apg.findSecretKeyRing(signatureKeyId);
                PGPSecretKey masterKey = null;
                if (keyRing != null) {
                    masterKey = Apg.getMasterKey(keyRing);
                    if (masterKey != null) {
                        Vector<PGPSecretKey> signKeys = Apg.getUsableSigningKeys(keyRing);
                        if (signKeys.size() > 0) {
                            mSignatureKeyId = masterKey.getKeyID();
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
            if (data != null) {
                mMessage.setText(data);
            }
        }

        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendClicked();
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
                    mSignatureKeyId = 0;
                    Apg.setPassPhrase(null);
                    updateView();
                }
            }
        });

        updateView();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ENCRYPTING: {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("initializing...");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;
            }

            case AskForSecretKeyPassPhrase.DIALOG_PASS_PHRASE: {
                return AskForSecretKeyPassPhrase.createDialog(this, mSignatureKeyId, this);
            }
        }

        return super.onCreateDialog(id);
    }

    private void sendClicked() {
        if (mSignatureKeyId != 0 && Apg.getPassPhrase() == null) {
            showDialog(AskForSecretKeyPassPhrase.DIALOG_PASS_PHRASE);
        } else {
            encryptStart();
        }
    }

    public void passPhraseCallback(String passPhrase) {
        Apg.setPassPhrase(passPhrase);
        encryptStart();
    }

    private void encryptStart() {
        showDialog(DIALOG_ENCRYPTING);
        mRunningThread = new Thread(this);
        mRunningThread.start();
    }

    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();
        String message = mMessage.getText().toString();
        // fix the message a bit, trailing spaces and newlines break stuff,
        // because GMail sends as HTML and such things fuck up the signature,
        // TODO: things like "<" and ">" also fuck up the signature
        message = message.replaceAll(" +\n", "\n");
        message = message.replaceAll("\n\n+", "\n\n");
        message = message.replaceFirst("^\n+", "");
        message = message.replaceFirst("\n+$", "");
        ByteArrayInputStream in =
                new ByteArrayInputStream(Strings.toUTF8ByteArray(message));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            if (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) {
                Apg.encrypt(in, out, true, mEncryptionKeyIds, mSignatureKeyId,
                            Apg.getPassPhrase(), this);
                data.putString("message", new String(out.toByteArray()));
            } else {
                Apg.sign(in, out, mSignatureKeyId, Apg.getPassPhrase(), this);
                data.putString("message", message);
                data.putString("signature", new String(out.toByteArray()));
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

        data.putInt("type", MESSAGE_DONE);

        if (error != null) {
            data.putString("error", error);
        }

        msg.setData(data);
        mhandler.sendMessage(msg);
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

        if (mSignatureKeyId == 0) {
            mSign.setText(R.string.sign);
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRing(mSignatureKeyId);
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
            mSign.setText(R.string.sign_as);
            mSign.setChecked(true);
        }
    }

    private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyListActivity.class);
        intent.putExtra("selection", mEncryptionKeyIds);
        startActivityForResult(intent, GET_PUCLIC_KEYS);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
        startActivityForResult(intent, GET_SECRET_KEY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_PUCLIC_KEYS: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    mEncryptionKeyIds = bundle.getLongArray("selection");
                    updateView();
                }
                break;
            }

            case GET_SECRET_KEY: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    long newId = bundle.getLong("selectedKeyId");
                    if (mSignatureKeyId != newId) {
                        Apg.setPassPhrase(null);
                    }
                    mSignatureKeyId = newId;
                } else {
                    mSignatureKeyId = 0;
                    Apg.setPassPhrase(null);
                }
                updateView();
                break;
            }

            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}