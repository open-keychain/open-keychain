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

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectSecretKeyListActivity extends BaseActivity {
    protected Vector<PGPSecretKeyRing> mKeyRings;
    protected LayoutInflater mInflater;
    protected Intent mIntent;
    protected ListView mList;

    protected long mSelectedKeyId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // fill things
        mIntent = getIntent();

        mKeyRings = (Vector<PGPSecretKeyRing>) Apg.getSecretKeyRings().clone();
        Collections.sort(mKeyRings, new Apg.SecretKeySorter());

        setContentView(R.layout.select_secret_key);

        mList = (ListView) findViewById(R.id.list);
        mList.setAdapter(new SecretKeyListAdapter(this));

        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent data = new Intent();
                data.putExtra("selectedKeyId", id);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    private class SecretKeyListAdapter extends BaseAdapter {

        public SecretKeyListAdapter(Context context) {
        }

        @Override
        public boolean isEnabled(int position) {
            PGPSecretKeyRing keyRing = mKeyRings.get(position);

            if (Apg.getMasterKey(keyRing) == null) {
                return false;
            }

            Vector<PGPSecretKey> usableKeys = Apg.getUsableSigningKeys(keyRing);
            if (usableKeys.size() == 0) {
                return false;
            }

            return true;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return mKeyRings.size();
        }

        @Override
        public Object getItem(int position) {
            return mKeyRings.get(position);
        }

        @Override
        public long getItemId(int position) {
            PGPSecretKeyRing keyRing = mKeyRings.get(position);
            PGPSecretKey key = Apg.getMasterKey(keyRing);
            if (key != null) {
                return key.getKeyID();
            }

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.select_secret_key_item, null);
            boolean enabled = isEnabled(position);

            PGPSecretKeyRing keyRing = mKeyRings.get(position);
            PGPSecretKey key = null;
            for (PGPSecretKey tKey : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
                if (tKey.isMasterKey()) {
                    key = tKey;
                    break;
                }
            }

            TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
            mainUserId.setText(R.string.unknownUserId);
            TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
            mainUserIdRest.setText("");
            TextView keyId = (TextView) view.findViewById(R.id.keyId);
            keyId.setText(R.string.noKey);
            TextView creation = (TextView) view.findViewById(R.id.creation);
            creation.setText(R.string.noDate);
            TextView expiry = (TextView) view.findViewById(R.id.expiry);
            expiry.setText(R.string.noExpiry);
            TextView status = (TextView) view.findViewById(R.id.status);
            status.setText(R.string.unknownStatus);

            if (key != null) {
                String userId = Apg.getMainUserId(key);
                if (userId != null) {
                    String chunks[] = userId.split(" <", 2);
                    userId = chunks[0];
                    if (chunks.length > 1) {
                        mainUserIdRest.setText("<" + chunks[1]);
                    }
                    mainUserId.setText(userId);
                }

                keyId.setText("" + Long.toHexString(key.getKeyID() & 0xffffffffL));
            }

            if (mainUserIdRest.getText().length() == 0) {
                mainUserIdRest.setVisibility(View.GONE);
            }

            Vector<PGPSecretKey> signingKeys = Apg.getSigningKeys(keyRing);
            Vector<PGPSecretKey> usableKeys = Apg.getUsableSigningKeys(keyRing);

            PGPSecretKey timespanKey = key;
            if (usableKeys.size() > 0) {
                timespanKey = usableKeys.get(0);
                status.setText(R.string.canSign);
            } else if (signingKeys.size() > 0) {
                timespanKey = signingKeys.get(0);
                Date now = new Date();
                if (now.compareTo(Apg.getCreationDate(timespanKey)) > 0) {
                    status.setText(R.string.notValid);
                } else {
                    status.setText(R.string.expired);
                }
            } else {
                status.setText(R.string.noKey);
            }

            creation.setText(DateFormat.getDateInstance().format(Apg.getCreationDate(timespanKey)));
            Date expiryDate = Apg.getExpiryDate(timespanKey);
            if (expiryDate != null) {
                expiry.setText(DateFormat.getDateInstance().format(expiryDate));
            }

            status.setText(status.getText() + " ");

            view.setEnabled(enabled);
            mainUserId.setEnabled(enabled);
            mainUserIdRest.setEnabled(enabled);
            keyId.setEnabled(enabled);
            creation.setEnabled(enabled);
            expiry.setEnabled(enabled);
            status.setEnabled(enabled);

            return view;
        }
    }
}