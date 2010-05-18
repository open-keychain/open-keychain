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

import java.util.Collections;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class SelectPublicKeyListActivity extends BaseActivity {
    protected Intent mIntent;
    protected ListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_public_key);

        // fill things
        mIntent = getIntent();
        long selectedKeyIds[] = null;
        if (mIntent.getExtras() != null) {
            selectedKeyIds = mIntent.getExtras().getLongArray("selection");
        }

        mList = (ListView) findViewById(R.id.list);
        // needed in Android 1.5, where the XML attribute gets ignored
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        Vector<PGPPublicKeyRing> keyRings =
                (Vector<PGPPublicKeyRing>) Apg.getPublicKeyRings().clone();
        Collections.sort(keyRings, new Apg.PublicKeySorter());
        mList.setAdapter(new SelectPublicKeyListAdapter(mList, keyRings));

        if (selectedKeyIds != null) {
            for (int i = 0; i < keyRings.size(); ++i) {
                PGPPublicKeyRing keyRing = keyRings.get(i);
                PGPPublicKey key = Apg.getMasterKey(keyRing);
                if (key == null) {
                    continue;
                }
                for (int j = 0; j < selectedKeyIds.length; ++j) {
                    if (key.getKeyID() == selectedKeyIds[j]) {
                        mList.setItemChecked(i, true);
                        break;
                    }
                }
            }
        }

        Button okButton = (Button) findViewById(R.id.btn_ok);

        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                okClicked();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.btn_cancel);

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelClicked();
            }
        });
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void okClicked() {
        Intent data = new Intent();
        Vector<Long> vector = new Vector<Long>();
        for (int i = 0; i < mList.getCount(); ++i) {
            if (mList.isItemChecked(i)) {
                vector.add(mList.getItemIdAtPosition(i));
            }
        }
        long selectedKeyIds[] = new long[vector.size()];
        for (int i = 0; i < vector.size(); ++i) {
            selectedKeyIds[i] = vector.get(i);
        }
        data.putExtra("selection", selectedKeyIds);
        setResult(RESULT_OK, data);
        finish();
    }
}