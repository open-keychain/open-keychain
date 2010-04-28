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

import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.bouncycastle2.util.Strings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EncryptMessageActivity extends BaseActivity {
    private String mSubject = null;
    private String mSendTo = null;

    private long mEncryptionKeyIds[] = null;

    private EditText mMessage = null;
    private Button mSelectKeysButton = null;
    private Button mSendButton = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt_message);

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
                    setSecretKeyId(0);
                    Apg.setPassPhrase(null);
                    updateView();
                }
            }
        });

        updateView();
    }

    private void sendClicked() {
        if (getSecretKeyId() != 0 && Apg.getPassPhrase() == null) {
            showDialog(Id.dialog.pass_phrase);
        } else {
            encryptStart();
        }
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        encryptStart();
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
            boolean encryptIt = mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0;
            if (getSecretKeyId() == 0 && !encryptIt) {
                throw new Apg.GeneralException("no signature key or encryption key selected");
            }

            String message = mMessage.getText().toString();
            if (!encryptIt) {
                // fix the message a bit, trailing spaces and newlines break stuff,
                // because GMail sends as HTML and such things fuck up the signature,
                // TODO: things like "<" and ">" also fuck up the signature
                message = message.replaceAll(" +\n", "\n");
                message = message.replaceAll("\n\n+", "\n\n");
                message = message.replaceFirst("^\n+", "");
                // make sure there'll be exactly one newline at the end
                message = message.replaceFirst("\n*$", "\n");
            }

            ByteArrayInputStream in =
                new ByteArrayInputStream(Strings.toUTF8ByteArray(message));
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (encryptIt) {
                Apg.encrypt(in, out, true, mEncryptionKeyIds, getSecretKeyId(),
                            Apg.getPassPhrase(), this,
                            getDefaultEncryptionAlgorithm(), getDefaultHashAlgorithm(),
                            null);
            } else {
                Apg.signText(in, out, getSecretKeyId(),
                             Apg.getPassPhrase(), getDefaultHashAlgorithm(), this);
            }
            data.putString("message", new String(out.toByteArray()));
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
            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    super.onActivityResult(requestCode, resultCode, data);
                    updateView();
                }
                break;
            }

            case Id.request.public_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    mEncryptionKeyIds = bundle.getLongArray("selection");
                    updateView();
                }
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
            Toast.makeText(EncryptMessageActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
            return;
        } else {
            String message = data.getString("message");
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
            EncryptMessageActivity.this.
                    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        }
    }
}