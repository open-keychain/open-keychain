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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Collections;
import java.util.Vector;

import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.openintents.intents.FileManager;

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
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class EncryptFileActivity extends BaseActivity {

    private EditText mFilename = null;
    private ImageButton mBrowse = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;
    private ListView mList;
    private Button mEncryptButton = null;

    private long mEncryptionKeyIds[] = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt_file);

        TabHost tabHost = (TabHost) findViewById(R.id.tab_host);
        tabHost.setup();

        TabSpec ts1 = tabHost.newTabSpec("TAB_ASYMMETRIC");
        ts1.setIndicator(getString(R.string.tab_asymmetric),
                         getResources().getDrawable(R.drawable.key));
        ts1.setContent(R.id.tab_asymmetric);
        tabHost.addTab(ts1);

        TabSpec ts2 = tabHost.newTabSpec("TAB_SYMMETRIC");
        ts2.setIndicator(getString(R.string.tab_symmetric),
                         getResources().getDrawable(R.drawable.encrypted));
        ts2.setContent(R.id.tab_symmetric);
        tabHost.addTab(ts2);

        tabHost.setCurrentTab(0);

        Vector<PGPPublicKeyRing> keyRings =
                (Vector<PGPPublicKeyRing>) Apg.getPublicKeyRings().clone();
        Collections.sort(keyRings, new Apg.PublicKeySorter());
        mList = (ListView) findViewById(R.id.public_key_list);
        mList.setAdapter(new SelectPublicKeyListAdapter(mList, keyRings));

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

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

        mEncryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked();
            }
        });

        updateView();
    }

    private void updateView() {
        if (getSecretKeyId() == 0) {
            mSign.setText(R.string.sign);
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
            mSign.setText(R.string.sign_as);
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

    private void encryptClicked() {
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
            InputStream in = new FileInputStream(mFilename.getText().toString());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) {
                Apg.encrypt(in, out, true, mEncryptionKeyIds, getSecretKeyId(),
                            Apg.getPassPhrase(), this);
                data.putString("message", new String(out.toByteArray()));
            } else {
                Apg.signText(in, out, getSecretKeyId(),
                             Apg.getPassPhrase(), HashAlgorithmTags.SHA256, this);
                data.putString("message", new String(out.toByteArray()));
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

            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    long newId = bundle.getLong("selectedKeyId");
                    if (getSecretKeyId() != newId) {
                        Apg.setPassPhrase(null);
                    }
                    setSecretKeyId(newId);
                } else {
                    setSecretKeyId(0);
                    Apg.setPassPhrase(null);
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
        }
    }
}
