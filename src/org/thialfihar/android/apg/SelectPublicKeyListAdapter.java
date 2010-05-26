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

import org.thialfihar.android.apg.provider.Database;
import org.thialfihar.android.apg.provider.KeyRings;
import org.thialfihar.android.apg.provider.Keys;
import org.thialfihar.android.apg.provider.UserIds;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class SelectPublicKeyListAdapter extends BaseAdapter {
    protected LayoutInflater mInflater;
    protected ListView mParent;
    protected SQLiteDatabase mDatabase;
    protected Cursor mCursor;

    public SelectPublicKeyListAdapter(ListView parent) {
        mParent = parent;
        mDatabase = new Database(parent.getContext()).getReadableDatabase();
        mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCursor = mDatabase.query(
              KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " +
                                    "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                                    Keys.TABLE_NAME + "." + Keys.KEY_RING_ID + " AND " +
                                    Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" +
                                    ") " +
                                    " INNER JOIN " + UserIds.TABLE_NAME + " ON " +
                                    "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " +
                                    UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " +
                                    UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ",
              new String[] {
                  KeyRings.TABLE_NAME + "." + KeyRings._ID,           // 0
                  KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 1
                  UserIds.TABLE_NAME + "." + UserIds.USER_ID,         // 2
                  "(SELECT COUNT(tmp." + Keys._ID + ") FROM " + Keys.TABLE_NAME + " AS tmp WHERE " +
                      "tmp." + Keys.KEY_RING_ID + " = " +
                      KeyRings.TABLE_NAME + "." + KeyRings._ID + " AND " +
                      "tmp." + Keys.CAN_ENCRYPT + " = '1')",          // 3
              },
              KeyRings.TABLE_NAME + "." + KeyRings.TYPE + " = ?",
              new String[] { "" + Id.database.type_public },
              null, null, UserIds.TABLE_NAME + "." + UserIds.USER_ID + " ASC");
    }

    @Override
    protected void finalize() throws Throwable {
        mCursor.close();
        mDatabase.close();
        super.finalize();
    }

    @Override
    public boolean isEnabled(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(3) > 0; // CAN_ENCRYPT
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(1); // MASTER_KEY_ID
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mCursor.moveToPosition(position);

        View view = mInflater.inflate(R.layout.select_public_key_item, null);
        boolean enabled = isEnabled(position);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknownUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.noKey);
        /*TextView creation = (TextView) view.findViewById(R.id.creation);
        creation.setText(R.string.noDate);
        TextView expiry = (TextView) view.findViewById(R.id.expiry);
        expiry.setText(R.string.noExpiry);*/
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText(R.string.unknownStatus);

        String userId = mCursor.getString(2); // USER_ID
        if (userId != null) {
            String chunks[] = userId.split(" <", 2);
            userId = chunks[0];
            if (chunks.length > 1) {
                mainUserIdRest.setText("<" + chunks[1]);
            }
            mainUserId.setText(userId);
        }

        long masterKeyId = mCursor.getLong(1); // MASTER_KEY_ID
        keyId.setText("" + Long.toHexString(masterKeyId & 0xffffffffL));

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        // TODO: must get this functionality in again
        /*PGPPublicKey timespanKey = key;
        if (usableKeys.size() > 0) {
            timespanKey = usableKeys.get(0);
            status.setText(R.string.canEncrypt);
        } else if (encryptKeys.size() > 0) {
            timespanKey = encryptKeys.get(0);
            Date now = new Date();
            if (now.compareTo(Apg.getCreationDate(timespanKey)) > 0) {
                status.setText(R.string.notValid);
            } else {
                status.setText(R.string.expired);
            }
        } else {
            status.setText(R.string.noKey);
        }*/
        if (enabled) {
            status.setText(R.string.canEncrypt);
        } else {
            status.setText(R.string.noKey);
        }

        /*
        creation.setText(DateFormat.getDateInstance().format(Apg.getCreationDate(timespanKey)));
        Date expiryDate = Apg.getExpiryDate(timespanKey);
        if (expiryDate != null) {
            expiry.setText(DateFormat.getDateInstance().format(expiryDate));
        }*/

        status.setText(status.getText() + " ");

        CheckBox selected = (CheckBox) view.findViewById(R.id.selected);

        selected.setChecked(mParent.isItemChecked(position));

        view.setEnabled(enabled);
        mainUserId.setEnabled(enabled);
        mainUserIdRest.setEnabled(enabled);
        keyId.setEnabled(enabled);
        //creation.setEnabled(enabled);
        //expiry.setEnabled(enabled);
        selected.setEnabled(enabled);
        status.setEnabled(enabled);

        return view;
    }
}
