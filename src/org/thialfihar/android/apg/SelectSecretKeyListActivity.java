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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SelectSecretKeyListActivity extends BaseActivity {
    protected ListView mList;
    protected SelectSecretKeyListAdapter mListAdapter;
    protected View mFilterLayout;
    protected Button mClearFilterButton;
    protected TextView mFilterInfo;

    protected long mSelectedKeyId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setContentView(R.layout.select_secret_key);

        mList = (ListView) findViewById(R.id.list);

        mList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent data = new Intent();
                data.putExtra(Apg.EXTRA_KEY_ID, id);
                data.putExtra(Apg.EXTRA_USER_ID, (String)mList.getItemAtPosition(position));
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mFilterLayout = findViewById(R.id.layout_filter);
        mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
        mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);

        mClearFilterButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                handleIntent(new Intent());
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchString = intent.getStringExtra(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
        }

        if (searchString == null) {
            mFilterLayout.setVisibility(View.GONE);
        } else {
            mFilterLayout.setVisibility(View.VISIBLE);
            mFilterInfo.setText(getString(R.string.filterInfo, searchString));
        }

        if (mListAdapter != null) {
            mListAdapter.cleanup();
        }

        mListAdapter = new SelectSecretKeyListAdapter(this, mList, searchString);
        mList.setAdapter(mListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.search, 0, R.string.menu_search)
                .setIcon(android.R.drawable.ic_menu_search);
        return true;
    }
}
