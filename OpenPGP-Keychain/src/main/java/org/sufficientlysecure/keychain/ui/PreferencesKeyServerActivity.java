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

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.ui.widget.Editor;
import org.sufficientlysecure.keychain.ui.widget.Editor.EditorListener;
import org.sufficientlysecure.keychain.ui.widget.KeyServerEditor;

import java.util.Vector;

public class PreferencesKeyServerActivity extends ActionBarActivity implements OnClickListener,
        EditorListener {

    public static final String EXTRA_KEY_SERVERS = "key_servers";

    private LayoutInflater mInflater;
    private ViewGroup mEditors;
    private View mAdd;
    private TextView mTitle;
    private TextView mSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        ActionBarHelper.setTwoButtonView(getSupportActionBar(), R.string.btn_okay, R.drawable.ic_action_done,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ok
                        okClicked();
                    }
                }, R.string.btn_do_not_save, R.drawable.ic_action_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                }
        );

        setContentView(R.layout.key_server_preference);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);

        mTitle.setText(R.string.label_key_servers);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(this);

        Intent intent = getIntent();
        String servers[] = intent.getStringArrayExtra(EXTRA_KEY_SERVERS);
        if (servers != null) {
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

    public void onClick(View v) {
        KeyServerEditor view = (KeyServerEditor) mInflater.inflate(R.layout.key_server_editor,
                mEditors, false);
        view.setEditorListener(this);
        mEditors.addView(view);
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void okClicked() {
        Intent data = new Intent();
        Vector<String> servers = new Vector<String>();
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
