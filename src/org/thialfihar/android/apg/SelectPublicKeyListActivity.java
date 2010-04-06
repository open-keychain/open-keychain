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

import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class SelectPublicKeyListActivity extends Activity {
    protected Vector<PGPPublicKeyRing> mKeyRings;
    protected LayoutInflater mInflater;
    protected Intent mIntent;
    protected ListView mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // fill things
        mIntent = getIntent();
        long selectedKeyIds[] = null;
        if (mIntent.getExtras() != null) {
            selectedKeyIds = mIntent.getExtras().getLongArray("selection");
        }

        Apg.initialize(this);
        mKeyRings = (Vector<PGPPublicKeyRing>) Apg.getPublicKeyRings().clone();
        Collections.sort(mKeyRings, new Apg.PublicKeySorter());

        setContentView(R.layout.select_public_key);

        mList = (ListView) findViewById(R.id.list);
        mList.setAdapter(new PublicKeyListAdapter(this));
        if (selectedKeyIds != null) {
            for (int i = 0; i < mKeyRings.size(); ++i) {
                PGPPublicKeyRing keyRing = mKeyRings.get(i);
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

    private class PublicKeyListAdapter extends BaseAdapter {
        public PublicKeyListAdapter(Context context) {
        }

        @Override
        public boolean isEnabled(int position) {
            PGPPublicKeyRing keyRing = mKeyRings.get(position);

            if (Apg.getMasterKey(keyRing) == null) {
                return false;
            }

            Vector<PGPPublicKey> encryptKeys = Apg.getUsableEncryptKeys(keyRing);
            if (encryptKeys.size() == 0) {
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
            PGPPublicKeyRing keyRing = mKeyRings.get(position);
            PGPPublicKey key = Apg.getMasterKey(keyRing);
            if (key != null) {
                return key.getKeyID();
            }

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.select_public_key_item, null);
            boolean enabled = isEnabled(position);

            PGPPublicKeyRing keyRing = mKeyRings.get(position);
            PGPPublicKey key = null;
            for (PGPPublicKey tKey : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
                if (tKey.isMasterKey()) {
                    key = tKey;
                    break;
                }
            }

            Vector<PGPPublicKey> encryptKeys = Apg.getEncryptKeys(keyRing);
            Vector<PGPPublicKey> usableKeys = Apg.getUsableEncryptKeys(keyRing);

            TextView mainUserId = (TextView) view.findViewById(R.id.main_user_id);
            mainUserId.setText(R.string.unknown_user_id);
            TextView mainUserIdRest = (TextView) view.findViewById(R.id.main_user_id_rest);
            mainUserIdRest.setText("");
            TextView keyId = (TextView) view.findViewById(R.id.key_id);
            keyId.setText("<no key>");
            TextView creation = (TextView) view.findViewById(R.id.creation);
            creation.setText("-");
            TextView expiry = (TextView) view.findViewById(R.id.expiry);
            expiry.setText("no expire");
            TextView status = (TextView) view.findViewById(R.id.status);
            status.setText("???");

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

            PGPPublicKey timespanKey = key;
            if (usableKeys.size() > 0) {
                timespanKey = usableKeys.get(0);
                status.setText("can encrypt");
            } else if (encryptKeys.size() > 0) {
                timespanKey = encryptKeys.get(0);
                Date now = new Date();
                if (now.compareTo(Apg.getCreationDate(timespanKey)) > 0) {
                    status.setText("not valid");
                } else {
                    status.setText("expired");
                }
            } else {
                status.setText("no key");
            }

            creation.setText(DateFormat.getDateInstance().format(Apg.getCreationDate(timespanKey)));
            Date expiryDate = Apg.getExpiryDate(timespanKey);
            if (expiryDate != null) {
                expiry.setText(DateFormat.getDateInstance().format(expiryDate));
            }

            status.setText(status.getText() + " ");

            CheckBox selected = (CheckBox) view.findViewById(R.id.selected);
            selected.setChecked(mList.isItemChecked(position));

            view.setEnabled(enabled);
            mainUserId.setEnabled(enabled);
            mainUserIdRest.setEnabled(enabled);
            keyId.setEnabled(enabled);
            creation.setEnabled(enabled);
            expiry.setEnabled(enabled);
            selected.setEnabled(enabled);
            status.setEnabled(enabled);

            return view;
        }
    }
}