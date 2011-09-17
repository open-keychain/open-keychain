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

package org.thialfihar.android.apg.ui.widget;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.thialfihar.android.apg.Apg;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.ui.widget.Editor.EditorListener;
import org.thialfihar.android.apg.utils.Choice;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SectionView extends LinearLayout implements OnClickListener, EditorListener, Runnable {
    private LayoutInflater mInflater;
    private View mAdd;
    private ViewGroup mEditors;
    private TextView mTitle;
    private int mType = 0;

    private Choice mNewKeyAlgorithmChoice;
    private int mNewKeySize;

    volatile private PGPSecretKey mNewKey;
    private ProgressDialog mProgressDialog;
    private Thread mRunningThread = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            if (data != null) {
                boolean closeProgressDialog = data.getBoolean("closeProgressDialog");
                if (closeProgressDialog) {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                }

                String error = data.getString(Apg.EXTRA_ERROR);
                if (error != null) {
                    Toast.makeText(getContext(),
                                   getContext().getString(R.string.errorMessage, error),
                                   Toast.LENGTH_SHORT).show();
                }

                boolean gotNewKey = data.getBoolean("gotNewKey");
                if (gotNewKey) {
                    KeyEditor view =
                        (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item,
                                                      mEditors, false);
                    view.setEditorListener(SectionView.this);
                    boolean isMasterKey = (mEditors.getChildCount() == 0);
                    view.setValue(mNewKey, isMasterKey);
                    mEditors.addView(view);
                    SectionView.this.updateEditorsVisible();
                }
            }
        }
    };

    public SectionView(Context context) {
        super(context);
    }

    public SectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewGroup getEditors() {
        return mEditors;
    }

    public void setType(int type) {
        mType = type;
        switch (type) {
            case Id.type.user_id: {
                mTitle.setText(R.string.section_userIds);
                break;
            }

            case Id.type.key: {
                mTitle.setText(R.string.section_keys);
                break;
            }

            default: {
                break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mAdd = findViewById(R.id.header);
        mAdd.setOnClickListener(this);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mTitle = (TextView) findViewById(R.id.title);

        updateEditorsVisible();
        super.onFinishInflate();
    }

    /** {@inheritDoc} */
    public void onDeleted(Editor editor) {
        this.updateEditorsVisible();
    }

    protected void updateEditorsVisible() {
        final boolean hasChildren = mEditors.getChildCount() > 0;
        mEditors.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (mType) {
            case Id.type.user_id: {
                UserIdEditor view =
                        (UserIdEditor) mInflater.inflate(R.layout.edit_key_user_id_item,
                                                         mEditors, false);
                view.setEditorListener(this);
                if (mEditors.getChildCount() == 0) {
                    view.setIsMainUserId(true);
                }
                mEditors.addView(view);
                break;
            }

            case Id.type.key: {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

                View view = mInflater.inflate(R.layout.create_key, null);
                dialog.setView(view);
                dialog.setTitle(R.string.title_createKey);
                dialog.setMessage(R.string.keyCreationElGamalInfo);

                boolean wouldBeMasterKey = (mEditors.getChildCount() == 0);

                final Spinner algorithm = (Spinner) view.findViewById(R.id.algorithm);
                Vector<Choice> choices = new Vector<Choice>();
                choices.add(new Choice(Id.choice.algorithm.dsa,
                                       getResources().getString(R.string.dsa)));
                if (!wouldBeMasterKey) {
                    choices.add(new Choice(Id.choice.algorithm.elgamal,
                                           getResources().getString(R.string.elgamal)));
                }

                choices.add(new Choice(Id.choice.algorithm.rsa,
                                       getResources().getString(R.string.rsa)));

                ArrayAdapter<Choice> adapter =
                        new ArrayAdapter<Choice>(getContext(),
                                                 android.R.layout.simple_spinner_item,
                                                 choices);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                algorithm.setAdapter(adapter);
                // make RSA the default
                for (int i = 0; i < choices.size(); ++i) {
                    if (choices.get(i).getId() == Id.choice.algorithm.rsa) {
                        algorithm.setSelection(i);
                        break;
                    }
                }

                final EditText keySize = (EditText) view.findViewById(R.id.size);

                dialog.setPositiveButton(android.R.string.ok,
                                         new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                        try {
                            mNewKeySize = Integer.parseInt("" + keySize.getText());
                        } catch (NumberFormatException e) {
                            mNewKeySize = 0;
                        }

                        mNewKeyAlgorithmChoice = (Choice) algorithm.getSelectedItem();
                        createKey();
                    }
                });

                dialog.setCancelable(true);
                dialog.setNegativeButton(android.R.string.cancel,
                                         new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                    }
                });

                dialog.create().show();
                break;
            }

            default: {
                break;
            }
        }
        this.updateEditorsVisible();
    }

    public void setUserIds(Vector<String> list) {
        if (mType != Id.type.user_id) {
            return;
        }

        mEditors.removeAllViews();
        for (String userId : list) {
            UserIdEditor view =
                (UserIdEditor) mInflater.inflate(R.layout.edit_key_user_id_item, mEditors, false);
            view.setEditorListener(this);
            view.setValue(userId);
            if (mEditors.getChildCount() == 0) {
                view.setIsMainUserId(true);
            }
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    public void setKeys(Vector<PGPSecretKey> list) {
        if (mType != Id.type.key) {
            return;
        }

        mEditors.removeAllViews();
        for (PGPSecretKey key : list) {
            KeyEditor view =
                (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item, mEditors, false);
            view.setEditorListener(this);
            boolean isMasterKey = (mEditors.getChildCount() == 0);
            view.setValue(key, isMasterKey);
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    private void createKey() {
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setMessage(getContext().getString(R.string.progress_generating));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
        mRunningThread = new Thread(this);
        mRunningThread.start();
    }

    public void run() {
        String error = null;
        try {
            PGPSecretKey masterKey = null;
            String passPhrase;
            if (mEditors.getChildCount() > 0) {
                masterKey = ((KeyEditor) mEditors.getChildAt(0)).getValue();
                passPhrase = Apg.getCachedPassPhrase(masterKey.getKeyID());
            } else {
                passPhrase = "";
            }
            mNewKey = Apg.createKey(getContext(),
                                    mNewKeyAlgorithmChoice.getId(),
                                    mNewKeySize, passPhrase,
                                    masterKey);
        } catch (NoSuchProviderException e) {
            error = "" + e;
        } catch (NoSuchAlgorithmException e) {
            error = "" + e;
        } catch (PGPException e) {
            error = "" + e;
        } catch (InvalidParameterException e) {
            error = "" + e;
        } catch (InvalidAlgorithmParameterException e) {
            error = "" + e;
        } catch (Apg.GeneralException e) {
            error = "" + e;
        }

        Message message = new Message();
        Bundle data = new Bundle();
        data.putBoolean("closeProgressDialog", true);
        if (error != null) {
            data.putString(Apg.EXTRA_ERROR, error);
        } else {
            data.putBoolean("gotNewKey", true);
        }
        message.setData(data);
        mHandler.sendMessage(message);
    }
}
