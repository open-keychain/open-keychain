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
import java.util.Collections;
import java.util.Vector;

import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.openintents.intents.FileManager;
import org.thialfihar.android.apg.Apg.GeneralException;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
    private final String TAB_ASYMMETRIC = "TAB_ASYMMETRIC";
    private final String TAB_SYMMETRIC = "TAB_SYMMETRIC";

    private TabHost mTabHost = null;
    private EditText mFilename = null;
    private ImageButton mBrowse = null;
    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;
    private ListView mPublicKeyList = null;
    private Button mEncryptButton = null;

    private long mEncryptionKeyIds[] = null;
    private String mInputFilename = null;
    private String mOutputFilename = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encrypt_file);

        mTabHost = (TabHost) findViewById(R.id.tab_host);
        mTabHost.setup();

        TabSpec ts1 = mTabHost.newTabSpec(TAB_ASYMMETRIC);
        ts1.setIndicator(getString(R.string.tab_asymmetric),
                         getResources().getDrawable(R.drawable.key));
        ts1.setContent(R.id.tab_asymmetric);
        mTabHost.addTab(ts1);

        TabSpec ts2 = mTabHost.newTabSpec(TAB_SYMMETRIC);
        ts2.setIndicator(getString(R.string.tab_symmetric),
                         getResources().getDrawable(R.drawable.encrypted));
        ts2.setContent(R.id.tab_symmetric);
        mTabHost.addTab(ts2);

        mTabHost.setCurrentTab(0);

        Vector<PGPPublicKeyRing> keyRings =
                (Vector<PGPPublicKeyRing>) Apg.getPublicKeyRings().clone();
        Collections.sort(keyRings, new Apg.PublicKeySorter());
        mPublicKeyList = (ListView) findViewById(R.id.public_key_list);
        mPublicKeyList.setAdapter(new SelectPublicKeyListAdapter(mPublicKeyList, keyRings));

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
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            mInputFilename = mFilename.getText().toString();
            File file = new File(mInputFilename);
            mOutputFilename = Constants.path.app_dir + "/" + file.getName() + ".gpg";
        }

        if (mInputFilename.equals("")) {
            Toast.makeText(this, "Select a file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mTabHost.getCurrentTabTag().equals(TAB_ASYMMETRIC)) {
            Vector<Long> vector = new Vector<Long>();
            for (int i = 0; i < mPublicKeyList.getCount(); ++i) {
                if (mPublicKeyList.isItemChecked(i)) {
                    vector.add(mPublicKeyList.getItemIdAtPosition(i));
                }
            }
            if (vector.size() > 0) {
                mEncryptionKeyIds = new long[vector.size()];
                for (int i = 0; i < vector.size(); ++i) {
                    mEncryptionKeyIds[i] = vector.get(i);
                }
            } else {
                mEncryptionKeyIds = null;
            }

            boolean encryptIt = mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0;
            if (getSecretKeyId() == 0 && !encryptIt) {
                Toast.makeText(this, "Select a signature key or encryption keys.",
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSecretKeyId() != 0 && Apg.getPassPhrase() == null) {
                showDialog(Id.dialog.pass_phrase);
                return;
            }
        } else {

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

            if (mTabHost.getCurrentTabTag().equals(TAB_ASYMMETRIC)) {
                boolean encryptIt = mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0;

                if (encryptIt) {
                    Apg.encrypt(in, out, true, mEncryptionKeyIds, getSecretKeyId(),
                                Apg.getPassPhrase(), this);
                } else {
                    Apg.signText(in, out, getSecretKeyId(),
                                 Apg.getPassPhrase(), HashAlgorithmTags.SHA256, this);
                }
            } else {

            }

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
            // delete the file if an error occurred
            File file = new File(mOutputFilename);
            file.delete();
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
