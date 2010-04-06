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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.ui.widget.SectionView;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class EditKeyActivity extends Activity
        implements OnClickListener, ProgressDialogUpdater, Runnable {
    static final int OPTION_MENU_NEW_PASS_PHRASE = 1;

    static final int DIALOG_NEW_PASS_PHRASE = 1;
    static final int DIALOG_PASS_PHRASES_DO_NOT_MATCH = 2;
    static final int DIALOG_NO_PASS_PHRASE = 3;
    static final int DIALOG_SAVING = 4;

    static final int MESSAGE_PROGRESS_UPDATE = 1;
    static final int MESSAGE_DONE = 2;

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIds;
    private SectionView mKeys;

    private Button mSaveButton;
    private Button mDiscardButton;

    private ProgressDialog mProgressDialog = null;
    private Thread mRunningThread = null;

    private String mNewPassPhrase = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
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
                        removeDialog(DIALOG_SAVING);
                        mProgressDialog = null;

                        String error = data.getString("error");
                        if (error != null) {
                            Toast.makeText(EditKeyActivity.this,
                                           "Error: " + data.getString("error"),
                                           Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditKeyActivity.this, R.string.key_saved,
                                           Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_key);

        Vector<String> userIds = new Vector<String>();
        Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();

        Intent intent = getIntent();
        long keyId = 0;
        if (intent.getExtras() != null) {
            keyId = intent.getExtras().getLong("keyId");
        }
        if (keyId != 0) {
            PGPSecretKey masterKey = null;
            mKeyRing = Apg.getSecretKeyRing(keyId);
            if (mKeyRing != null) {
                masterKey = Apg.getMasterKey(mKeyRing);
                for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(mKeyRing.getSecretKeys())) {
                    keys.add(key);
                }
            }
            if (masterKey != null) {
                for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                    userIds.add(userId);
                }
            }
        }

        if (Apg.getPassPhrase() == null) {
            Apg.setPassPhrase("");
        }

        mSaveButton = (Button) findViewById(R.id.btn_save);
        mDiscardButton = (Button) findViewById(R.id.btn_discard);

        mSaveButton.setOnClickListener(this);
        mDiscardButton.setOnClickListener(this);

        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout container = (LinearLayout) findViewById(R.id.container);
        mUserIds = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mUserIds.setType(SectionView.TYPE_USER_ID);
        mUserIds.setUserIds(userIds);
        container.addView(mUserIds);
        mKeys = (SectionView) inflater.inflate(R.layout.edit_key_section, container, false);
        mKeys.setType(SectionView.TYPE_KEY);
        mKeys.setKeys(keys);
        container.addView(mKeys);

        Toast.makeText(this, "Warning: Key editing is still kind of beta.", Toast.LENGTH_LONG).show();
    }

    public boolean havePassPhrase() {
        return (Apg.getPassPhrase() != null && !Apg.getPassPhrase().equals("")) ||
               (mNewPassPhrase != null && mNewPassPhrase.equals(""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPTION_MENU_NEW_PASS_PHRASE, 0,
                 (havePassPhrase() ? "Change Pass Phrase" : "Set Pass Phrase"))
                .setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTION_MENU_NEW_PASS_PHRASE: {
                showDialog(DIALOG_NEW_PASS_PHRASE);
                return true;
            }

            default: {
                break;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SAVING: {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("saving...");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;
            }

            case DIALOG_NEW_PASS_PHRASE: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                if (havePassPhrase()) {
                    alert.setTitle("Change Pass Phrase");
                } else {
                    alert.setTitle("Set Pass Phrase");
                }
                alert.setMessage("Enter the pass phrase twice.");

                final EditText input1 = new EditText(this);
                final EditText input2 = new EditText(this);
                input1.setText("");
                input2.setText("");
                input1.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input2.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input1.setTransformationMethod(new PasswordTransformationMethod());
                input2.setTransformationMethod(new PasswordTransformationMethod());

                // 5dip padding
                int padding = (int) (10 * getResources().getDisplayMetrics().densityDpi / 160);
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(padding, 0, padding, 0);
                layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                        LayoutParams.WRAP_CONTENT));
                input1.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                        LayoutParams.WRAP_CONTENT));
                input2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                        LayoutParams.WRAP_CONTENT));
                layout.addView(input1);
                layout.addView(input2);

                alert.setView(layout);

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(DIALOG_NEW_PASS_PHRASE);

                                                String passPhrase1 = "" + input1.getText();
                                                String passPhrase2 = "" + input2.getText();
                                                if (!passPhrase1.equals(passPhrase2)) {
                                                    showDialog(DIALOG_PASS_PHRASES_DO_NOT_MATCH);
                                                    return;
                                                }

                                                if (passPhrase1.equals("")) {
                                                    showDialog(DIALOG_NO_PASS_PHRASE);
                                                    return;
                                                }

                                                mNewPassPhrase = passPhrase1;
                                            }
                                        });

                alert.setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(DIALOG_NEW_PASS_PHRASE);
                                            }
                                        });

                return alert.create();
            }

            case DIALOG_PASS_PHRASES_DO_NOT_MATCH: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle("Error");
                alert.setMessage("The pass phrases didn't match.");

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(DIALOG_PASS_PHRASES_DO_NOT_MATCH);
                                            }
                                        });
                alert.setCancelable(false);

                return alert.create();
            }

            case DIALOG_NO_PASS_PHRASE: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle("Error");
                alert.setMessage("Empty pass phrases are not supported.");

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(DIALOG_NO_PASS_PHRASE);
                                            }
                                        });
                alert.setCancelable(false);

                return alert.create();
            }

            default: {
                break;
            }
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onClick(View v) {
        if (v == mSaveButton) {
            // TODO: some warning
            saveClicked();
        } else if (v == mDiscardButton) {
            finish();
        }
    }

    private void saveClicked() {
        if ((Apg.getPassPhrase() == null || Apg.getPassPhrase().equals("")) &&
            (mNewPassPhrase == null || mNewPassPhrase.equals(""))) {
            Toast.makeText(this, R.string.set_a_pass_phrase, Toast.LENGTH_SHORT).show();
            return;
        }
        showDialog(DIALOG_SAVING);
        mRunningThread = new Thread(this);
        mRunningThread.start();
    }

    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        try {
            String oldPassPhrase = Apg.getPassPhrase();
            String newPassPhrase = mNewPassPhrase;
            if (newPassPhrase == null) {
                newPassPhrase = oldPassPhrase;
            }
            Apg.buildSecretKey(this, mUserIds, mKeys, oldPassPhrase, newPassPhrase, this);
        } catch (NoSuchProviderException e) {
            error = e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            error = e.getMessage();
        } catch (PGPException e) {
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
        mHandler.sendMessage(msg);
    }

    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", MESSAGE_PROGRESS_UPDATE);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", MESSAGE_PROGRESS_UPDATE);
        data.putString("message", message);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
}