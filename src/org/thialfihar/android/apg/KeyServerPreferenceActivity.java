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

import java.util.Vector;

import org.thialfihar.android.apg.ui.widget.Editor;
import org.thialfihar.android.apg.ui.widget.Editor.EditorListener;
import org.thialfihar.android.apg.ui.widget.KeyServerEditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class KeyServerPreferenceActivity extends BaseActivity
        implements OnClickListener, EditorListener {
    private LayoutInflater mInflater;
    private ViewGroup mEditors;
    private View mAdd;
    private TextView mTitle;
    private TextView mSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_server_preference);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);

        mTitle.setText(R.string.label_keyServers);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(this);

        Intent intent = getIntent();
        String servers[] = intent.getStringArrayExtra(Apg.EXTRA_KEY_SERVERS);
        if (servers != null) {
            for (int i = 0; i < servers.length; ++i) {
                KeyServerEditor view = (KeyServerEditor) mInflater.inflate(R.layout.key_server_editor, mEditors, false);
                view.setEditorListener(this);
                view.setValue(servers[i]);
                mEditors.addView(view);
            }
        }

        Button okButton = (Button) findViewById(R.id.btn_ok);
        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                okClicked();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cancelClicked();
            }
        });
    }

    public void onDeleted(Editor editor) {
        // nothing to do
    }

    public void onClick(View v) {
        KeyServerEditor view = (KeyServerEditor) mInflater.inflate(R.layout.key_server_editor, mEditors, false);
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
        data.putExtra(Apg.EXTRA_KEY_SERVERS, servers.toArray(dummy));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // override this, so no option menu is added (as would be in BaseActivity), since
        // we're still in preferences
        return true;
    }
}
