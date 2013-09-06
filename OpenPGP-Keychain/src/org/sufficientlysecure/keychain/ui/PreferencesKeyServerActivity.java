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

import java.util.Vector;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.Editor;
import org.sufficientlysecure.keychain.ui.widget.Editor.EditorListener;
import org.sufficientlysecure.keychain.ui.widget.KeyServerEditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

public class PreferencesKeyServerActivity extends SherlockActivity implements OnClickListener,
        EditorListener {

    public static final String EXTRA_KEY_SERVERS = "keyServers";

    private LayoutInflater mInflater;
    private ViewGroup mEditors;
    private View mAdd;
    private TextView mTitle;
    private TextView mSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done"/"Cancel" custom action bar view
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_cancel, null);

        ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text))
                .setText(R.string.btn_okay);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ok
                        okClicked();
                    }
                });
        ((TextView) customActionBarView.findViewById(R.id.actionbar_cancel_text))
                .setText(R.string.btn_doNotSave);
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(R.layout.key_server_preference);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);

        mTitle.setText(R.string.label_keyServers);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(this);

        Intent intent = getIntent();
        String servers[] = intent.getStringArrayExtra(EXTRA_KEY_SERVERS);
        if (servers != null) {
            for (int i = 0; i < servers.length; ++i) {
                KeyServerEditor view = (KeyServerEditor) mInflater.inflate(
                        R.layout.key_server_editor, mEditors, false);
                view.setEditorListener(this);
                view.setValue(servers[i]);
                mEditors.addView(view);
            }
        }
    }

    public void onDeleted(Editor editor) {
        // nothing to do
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
