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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectSecretKeyListActivity extends BaseActivity {
    protected ListView mList;
    protected SelectSecretKeyListAdapter mListAdapter;

    protected long mSelectedKeyId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_secret_key);

        mList = (ListView) findViewById(R.id.list);
        mListAdapter = new SelectSecretKeyListAdapter(this, mList);
        mList.setAdapter(mListAdapter);

        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent data = new Intent();
                data.putExtra(Apg.EXTRA_KEY_ID, id);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
}
