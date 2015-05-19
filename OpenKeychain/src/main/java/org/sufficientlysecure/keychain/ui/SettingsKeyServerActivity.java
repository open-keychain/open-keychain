/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.dialog.AddKeyserverDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.Editor;
import org.sufficientlysecure.keychain.ui.widget.Editor.EditorListener;
import org.sufficientlysecure.keychain.ui.widget.KeyServerEditor;

import java.util.Vector;

public class SettingsKeyServerActivity extends BaseActivity implements OnClickListener,
        EditorListener {

    public static final String EXTRA_KEY_SERVERS = "key_servers";

    private LayoutInflater mInflater;
    private ViewGroup mEditors;
    private View mAdd;
    private View mRotate;
    private TextView mTitle;
    private TextView mSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        setFullScreenDialogDoneClose(R.string.btn_save,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        okClicked();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelClicked();
                    }
                });

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);
        mSummary.setText(getText(R.string.label_first_keyserver_is_used));

        mTitle.setText(R.string.label_keyservers);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(this);

        mRotate = findViewById(R.id.rotate);
        mRotate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Vector<String> servers = serverList();
                String first = servers.get(0);
                if (first != null) {
                    servers.remove(0);
                    servers.add(first);
                    String[] dummy = {};
                    makeServerList(servers.toArray(dummy));
                }
            }
        });

        Intent intent = getIntent();
        String servers[] = intent.getStringArrayExtra(EXTRA_KEY_SERVERS);
        makeServerList(servers);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.key_server_preference);
    }

    private void makeServerList(String[] servers) {
        if (servers != null) {
            mEditors.removeAllViews();
            for (String serv : servers) {
                KeyServerEditor view = (KeyServerEditor) mInflater.inflate(
                        R.layout.key_server_editor, mEditors, false);
                view.setEditorListener(this);
                view.setValue(serv);
                mEditors.addView(view);
            }
        }
    }

    public void onDeleted(Editor editor, boolean wasNewItem) {
        // nothing to do
    }

    @Override
    public void onEdited() {

    }

    // button to add keyserver clicked
    public void onClick(View v) {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData();
                switch (message.what) {
                    case AddKeyserverDialogFragment.MESSAGE_OKAY: {
                        boolean verified = data.getBoolean(AddKeyserverDialogFragment.MESSAGE_VERIFIED);
                        if (verified) {
                            Notify.create(SettingsKeyServerActivity.this,
                                    R.string.add_keyserver_verified, Notify.Style.OK).show();
                        } else {
                            Notify.create(SettingsKeyServerActivity.this,
                                    R.string.add_keyserver_without_verification,
                                    Notify.Style.WARN).show();
                        }
                        String keyserver = data.getString(AddKeyserverDialogFragment.MESSAGE_KEYSERVER);
                        addKeyserver(keyserver);
                        break;
                    }
                    case AddKeyserverDialogFragment.MESSAGE_VERIFICATION_FAILED: {
                        AddKeyserverDialogFragment.FailureReason failureReason =
                                (AddKeyserverDialogFragment.FailureReason) data.getSerializable(
                                        AddKeyserverDialogFragment.MESSAGE_FAILURE_REASON);
                        switch (failureReason) {
                            case CONNECTION_FAILED: {
                                Notify.create(SettingsKeyServerActivity.this,
                                        R.string.add_keyserver_connection_failed,
                                        Notify.Style.ERROR).show();
                                break;
                            }
                            case INVALID_URL: {
                                Notify.create(SettingsKeyServerActivity.this,
                                        R.string.add_keyserver_invalid_url,
                                        Notify.Style.ERROR).show();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        AddKeyserverDialogFragment dialogFragment = AddKeyserverDialogFragment
                .newInstance(messenger);
        dialogFragment.show(getSupportFragmentManager(), "addKeyserverDialog");
    }

    public void addKeyserver(String keyserverUrl) {
        KeyServerEditor view = (KeyServerEditor) mInflater.inflate(R.layout.key_server_editor,
                mEditors, false);
        view.setEditorListener(this);
        view.setValue(keyserverUrl);
        mEditors.addView(view);
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private Vector<String> serverList() {
        Vector<String> servers = new Vector<>();
        for (int i = 0; i < mEditors.getChildCount(); ++i) {
            KeyServerEditor editor = (KeyServerEditor) mEditors.getChildAt(i);
            String tmp = editor.getValue();
            if (tmp.length() > 0) {
                servers.add(tmp);
            }
        }
        return servers;
    }

    private void okClicked() {
        Intent data = new Intent();
        Vector<String> servers = new Vector<>();
        for (int i = 0; i < mEditors.getChildCount(); ++i) {
            KeyServerEditor editor = (KeyServerEditor) mEditors.getChildAt(i);
            String tmp = editor.getValue();
            if (tmp.length() > 0) {
                servers.add(tmp);
            }
        }
        String[] dummy = new String[0];
        data.putExtra(EXTRA_KEY_SERVERS, servers.toArray(dummy));
        setResult(RESULT_OK, data);
        finish();
    }
}
