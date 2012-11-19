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

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.service.PassphraseCacheService;
import org.thialfihar.android.apg.ui.dialog.ProgressDialogFragment;
import org.thialfihar.android.apg.ui.widget.Editor.EditorListener;
import org.thialfihar.android.apg.util.Choice;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
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

import java.util.Iterator;
import java.util.Vector;

public class SectionView extends LinearLayout implements OnClickListener, EditorListener {
    private LayoutInflater mInflater;
    private View mAdd;
    private ViewGroup mEditors;
    private TextView mTitle;
    private int mType = 0;

    private Choice mNewKeyAlgorithmChoice;
    private int mNewKeySize;

    private SherlockFragmentActivity mActivity;

    private ProgressDialogFragment mGeneratingDialog;

    public SectionView(Context context) {
        super(context);
        mActivity = (SherlockFragmentActivity) context;
    }

    public SectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (SherlockFragmentActivity) context;
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
            UserIdEditor view = (UserIdEditor) mInflater.inflate(R.layout.edit_key_user_id_item,
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

            boolean wouldBeMasterKey = (mEditors.getChildCount() == 0);

            final Spinner algorithm = (Spinner) view.findViewById(R.id.create_key_algorithm);
            Vector<Choice> choices = new Vector<Choice>();
            choices.add(new Choice(Id.choice.algorithm.dsa, getResources().getString(R.string.dsa)));
            if (!wouldBeMasterKey) {
                choices.add(new Choice(Id.choice.algorithm.elgamal, getResources().getString(
                        R.string.elgamal)));
            }

            choices.add(new Choice(Id.choice.algorithm.rsa, getResources().getString(R.string.rsa)));

            ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getContext(),
                    android.R.layout.simple_spinner_item, choices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            algorithm.setAdapter(adapter);
            // make RSA the default
            for (int i = 0; i < choices.size(); ++i) {
                if (choices.get(i).getId() == Id.choice.algorithm.rsa) {
                    algorithm.setSelection(i);
                    break;
                }
            }

            final EditText keySize = (EditText) view.findViewById(R.id.create_key_size);

            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
            UserIdEditor view = (UserIdEditor) mInflater.inflate(R.layout.edit_key_user_id_item,
                    mEditors, false);
            view.setEditorListener(this);
            view.setValue(userId);
            if (mEditors.getChildCount() == 0) {
                view.setIsMainUserId(true);
            }
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    public void setKeys(Vector<PGPSecretKey> list, Vector<Integer> usages) {
        if (mType != Id.type.key) {
            return;
        }

        mEditors.removeAllViews();

        // go through all keys and set view based on them
        for (int i = 0; i < list.size(); i++) {
            KeyEditor view = (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item, mEditors,
                    false);
            view.setEditorListener(this);
            boolean isMasterKey = (mEditors.getChildCount() == 0);
            view.setValue(list.get(i), isMasterKey, usages.get(i));
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    private void createKey() {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(mActivity, ApgIntentService.class);

        intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_GENERATE_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        String passPhrase;
        if (mEditors.getChildCount() > 0) {
            PGPSecretKey masterKey = ((KeyEditor) mEditors.getChildAt(0)).getValue();
            passPhrase = PassphraseCacheService
                    .getCachedPassphrase(mActivity, masterKey.getKeyID());

            data.putByteArray(ApgIntentService.MASTER_KEY,
                    PGPConversionHelper.PGPSecretKeyToBytes(masterKey));
        } else {
            passPhrase = "";
        }
        data.putString(ApgIntentService.SYMMETRIC_PASSPHRASE, passPhrase);
        data.putInt(ApgIntentService.ALGORITHM, mNewKeyAlgorithmChoice.getId());
        data.putInt(ApgIntentService.KEY_SIZE, mNewKeySize);

        intent.putExtra(ApgIntentService.EXTRA_DATA, data);

        // show progress dialog
        mGeneratingDialog = ProgressDialogFragment.newInstance(R.string.progress_generating,
                ProgressDialog.STYLE_SPINNER);

        // Message is received after generating is done in ApgService
        ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(mActivity, mGeneratingDialog) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                    // get new key from data bundle returned from service
                    Bundle data = message.getData();
                    PGPSecretKeyRing newKeyRing = (PGPSecretKeyRing) PGPConversionHelper
                            .BytesToPGPKeyRing(data.getByteArray(ApgIntentService.RESULT_NEW_KEY));

                    boolean isMasterKey = (mEditors.getChildCount() == 0);

                    // take only the key from this ring
                    PGPSecretKey newKey = null;
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSecretKey> it = newKeyRing.getSecretKeys();

                    if (isMasterKey) {
                        newKey = it.next();
                    } else {
                        // first one is the master key
                        it.next();
                        newKey = it.next();
                    }

                    // add view with new key
                    KeyEditor view = (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item,
                            mEditors, false);
                    view.setEditorListener(SectionView.this);
                    view.setValue(newKey, isMasterKey, -1);
                    mEditors.addView(view);
                    SectionView.this.updateEditorsVisible();
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

        mGeneratingDialog.show(mActivity.getSupportFragmentManager(), "dialog");

        // start service with intent
        mActivity.startService(intent);
    }
}
