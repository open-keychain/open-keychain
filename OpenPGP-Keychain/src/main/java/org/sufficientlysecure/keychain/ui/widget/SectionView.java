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

package org.sufficientlysecure.keychain.ui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.spongycastle.openpgp.PGPKeyFlags;
import org.spongycastle.openpgp.PGPSecretKey;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.Editor.EditorListener;
import org.sufficientlysecure.keychain.util.Choice;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class SectionView extends LinearLayout implements OnClickListener, EditorListener, Editor {
    private LayoutInflater mInflater;
    private BootstrapButton mPlusButton;
    private ViewGroup mEditors;
    private TextView mTitle;
    private int mType = 0;
    private EditorListener mEditorListener = null;

    private Choice mNewKeyAlgorithmChoice;
    private int mNewKeySize;
    private boolean canEdit = true;
    private boolean oldItemDeleted = false;
    private ArrayList<String> mDeletedIDs = new ArrayList<String>();
    private ArrayList<PGPSecretKey> mDeletedKeys = new ArrayList<PGPSecretKey>();

    private ActionBarActivity mActivity;

    private ProgressDialogFragment mGeneratingDialog;

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }

    public SectionView(Context context) {
        super(context);
        mActivity = (ActionBarActivity) context;
    }

    public SectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (ActionBarActivity) context;
    }

    public ViewGroup getEditors() {
        return mEditors;
    }

    public void setType(int type) {
        mType = type;
        switch (type) {
        case Id.type.user_id: {
            mTitle.setText(R.string.section_user_ids);
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

    public void setCanEdit(boolean bCanEdit) {
        canEdit = bCanEdit;
        if (!canEdit) {
            mPlusButton.setVisibility(View.INVISIBLE);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mPlusButton = (BootstrapButton) findViewById(R.id.plusbutton);
        mPlusButton.setOnClickListener(this);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mTitle = (TextView) findViewById(R.id.title);

        updateEditorsVisible();
        super.onFinishInflate();
    }

    /** {@inheritDoc} */
    public void onDeleted(Editor editor, boolean wasNewItem) {
        oldItemDeleted |= !wasNewItem;
        if (oldItemDeleted) {
            if (mType == Id.type.user_id)
                mDeletedIDs.add(((UserIdEditor)editor).getOriginalID());
            else if (mType == Id.type.key)
                mDeletedKeys.add(((KeyEditor)editor).getValue());

        }
        this.updateEditorsVisible();
        if (mEditorListener != null) {
            mEditorListener.onEdited();
        }
    }

    @Override
    public void onEdited() {
        if (mEditorListener != null) {
            mEditorListener.onEdited();
        }
    }

    protected void updateEditorsVisible() {
        final boolean hasChildren = mEditors.getChildCount() > 0;
        mEditors.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
    }

    public boolean needsSaving()
    {
        //check each view for needs saving, take account of deleted items
        boolean ret = oldItemDeleted;
        for (int i = 0; i < mEditors.getChildCount(); ++i) {
            Editor editor = (Editor) mEditors.getChildAt(i);
            ret |= editor.needsSaving();
        }
        return ret;
    }

    public ArrayList<String> getOriginalIDs()
    {
        ArrayList<String> orig = new ArrayList<String>();
        if (mType == Id.type.user_id) {
            for (int i = 0; i < mEditors.getChildCount(); ++i) {
                UserIdEditor editor = (UserIdEditor) mEditors.getChildAt(i);
                if (editor.isMainUserId())
                    orig.add(0, editor.getOriginalID());
                else
                    orig.add(editor.getOriginalID());
            }
            return orig;
        } else {
            return null;
        }
    }

    public ArrayList<String> getDeletedIDs()
    {
        return mDeletedIDs;
    }

    public ArrayList<PGPSecretKey> getDeletedKeys()
    {
        return mDeletedKeys;
    }

    public List<Boolean> getNeedsSavingArray()
    {
        ArrayList<Boolean> mList = new ArrayList<Boolean>();
        for (int i = 0; i < mEditors.getChildCount(); ++i) {
            Editor editor = (Editor) mEditors.getChildAt(i);
            mList.add(editor.needsSaving());
        }
        return mList;
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        if (canEdit) {
            switch (mType) {
            case Id.type.user_id: {
                UserIdEditor view = (UserIdEditor) mInflater.inflate(
                        R.layout.edit_key_user_id_item, mEditors, false);
                view.setEditorListener(this);
                view.setValue("", mEditors.getChildCount() == 0, true);
                mEditors.addView(view);
                if (mEditorListener != null) {
                    mEditorListener.onEdited();
                }
                break;
            }

            case Id.type.key: {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

                View view = mInflater.inflate(R.layout.create_key_dialog, null);
                dialog.setView(view);
                dialog.setTitle(R.string.title_create_key);

                boolean wouldBeMasterKey = (mEditors.getChildCount() == 0);

                final Spinner algorithm = (Spinner) view.findViewById(R.id.create_key_algorithm);
                Vector<Choice> choices = new Vector<Choice>();
                choices.add(new Choice(Id.choice.algorithm.dsa, getResources().getString(
                        R.string.dsa)));
                if (!wouldBeMasterKey) {
                    choices.add(new Choice(Id.choice.algorithm.elgamal, getResources().getString(
                            R.string.elgamal)));
                }

                choices.add(new Choice(Id.choice.algorithm.rsa, getResources().getString(
                        R.string.rsa)));

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

                final Spinner keySize = (Spinner) view.findViewById(R.id.create_key_size);
                ArrayAdapter<CharSequence> keySizeAdapter = ArrayAdapter.createFromResource(
                        getContext(), R.array.key_size_spinner_values,
                        android.R.layout.simple_spinner_item);
                keySizeAdapter
                        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                keySize.setAdapter(keySizeAdapter);
                keySize.setSelection(3); // Default to 4096 for the key length
                dialog.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int id) {
                                di.dismiss();
                                try {
                                    int nKeyIndex = keySize.getSelectedItemPosition();
                                    switch (nKeyIndex) {
                                    case 0:
                                        mNewKeySize = 512;
                                        break;
                                    case 1:
                                        mNewKeySize = 1024;
                                        break;
                                    case 2:
                                        mNewKeySize = 2048;
                                        break;
                                    case 3:
                                        mNewKeySize = 4096;
                                        break;
                                    }
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
            view.setValue(userId, mEditors.getChildCount() == 0, false);
            view.setCanEdit(canEdit);
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    public void setKeys(Vector<PGPSecretKey> list, Vector<Integer> usages, boolean newKeys) {
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
            view.setValue(list.get(i), isMasterKey, usages.get(i), newKeys);
            view.setCanEdit(canEdit);
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    private void createKey() {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(mActivity, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_GENERATE_KEY);

        // fill values for this action
        Bundle data = new Bundle();
        Boolean isMasterKey;

        String passPhrase;
        if (mEditors.getChildCount() > 0) {
            PGPSecretKey masterKey = ((KeyEditor) mEditors.getChildAt(0)).getValue();
            passPhrase = PassphraseCacheService
                    .getCachedPassphrase(mActivity, masterKey.getKeyID());
            isMasterKey = false;
        } else {
            passPhrase = "";
            isMasterKey = true;
        }
        data.putBoolean(KeychainIntentService.GENERATE_KEY_MASTER_KEY, isMasterKey);
        data.putString(KeychainIntentService.GENERATE_KEY_SYMMETRIC_PASSPHRASE, passPhrase);
        data.putInt(KeychainIntentService.GENERATE_KEY_ALGORITHM, mNewKeyAlgorithmChoice.getId());
        data.putInt(KeychainIntentService.GENERATE_KEY_KEY_SIZE, mNewKeySize);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // show progress dialog
        mGeneratingDialog = ProgressDialogFragment.newInstance(R.string.progress_generating,
                ProgressDialog.STYLE_SPINNER);

        // Message is received after generating is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(mActivity,
                mGeneratingDialog) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get new key from data bundle returned from service
                    Bundle data = message.getData();
                    PGPSecretKey newKey = (PGPSecretKey) PgpConversionHelper
                            .BytesToPGPSecretKey(data
                                    .getByteArray(KeychainIntentService.RESULT_NEW_KEY));
                    addGeneratedKeyToView(newKey);
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        mGeneratingDialog.show(mActivity.getSupportFragmentManager(), "dialog");

        // start service with intent
        mActivity.startService(intent);
    }

    private void addGeneratedKeyToView(PGPSecretKey newKey) {
        // add view with new key
        KeyEditor view = (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item,
                mEditors, false);
        view.setEditorListener(SectionView.this);
        int usage = 0;
        if (mEditors.getChildCount() == 0)
            usage = PGPKeyFlags.CAN_CERTIFY;
        view.setValue(newKey, newKey.isMasterKey(), usage, true);
        mEditors.addView(view);
        SectionView.this.updateEditorsVisible();
        if (mEditorListener != null) {
            mEditorListener.onEdited();
        }
    }
}
