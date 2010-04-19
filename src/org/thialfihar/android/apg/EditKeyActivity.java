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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
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

public class EditKeyActivity extends BaseActivity implements OnClickListener {

    private PGPSecretKeyRing mKeyRing = null;

    private SectionView mUserIds;
    private SectionView mKeys;

    private Button mSaveButton;
    private Button mDiscardButton;

    private String mNewPassPhrase = null;

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
        Log.e("oink", "password is " + Apg.getPassPhrase());
        return (Apg.getPassPhrase() != null && !Apg.getPassPhrase().equals("")) ||
               (mNewPassPhrase != null && mNewPassPhrase.equals(""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.new_pass_phrase, 0,
                 (havePassPhrase() ? "Change Pass Phrase" : "Set Pass Phrase"))
                .setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.new_pass_phrase: {
                showDialog(Id.dialog.new_pass_phrase);
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
            case Id.dialog.new_pass_phrase: {
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
                                                removeDialog(Id.dialog.new_pass_phrase);

                                                String passPhrase1 = "" + input1.getText();
                                                String passPhrase2 = "" + input2.getText();
                                                if (!passPhrase1.equals(passPhrase2)) {
                                                    showDialog(Id.dialog.pass_phrases_do_not_match);
                                                    return;
                                                }

                                                if (passPhrase1.equals("")) {
                                                    showDialog(Id.dialog.no_pass_phrase);
                                                    return;
                                                }

                                                mNewPassPhrase = passPhrase1;
                                            }
                                        });

                alert.setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(Id.dialog.new_pass_phrase);
                                            }
                                        });

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
        if (!havePassPhrase()) {
            Toast.makeText(this, R.string.set_a_pass_phrase, Toast.LENGTH_SHORT).show();
            return;
        }
        showDialog(Id.dialog.saving);
        startThread();
    }

    @Override
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
        removeDialog(Id.dialog.saving);

        String error = data.getString("error");
        if (error != null) {
            Toast.makeText(EditKeyActivity.this,
                           "Error: " + data.getString("error"),
                           Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(EditKeyActivity.this, R.string.key_saved, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
}