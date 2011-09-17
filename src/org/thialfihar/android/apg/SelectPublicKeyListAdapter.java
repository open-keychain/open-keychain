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

import java.util.Date;

import org.thialfihar.android.apg.provider.KeyRings;
import org.thialfihar.android.apg.provider.Keys;
import org.thialfihar.android.apg.provider.UserIds;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
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
    protected String mSearchString;
    protected Activity mActivity;

    public SelectPublicKeyListAdapter(Activity activity, ListView parent,
                                      String searchString, long selectedKeyIds[]) {
        mSearchString = searchString;

        mActivity = activity;
        mParent = parent;
        mDatabase =  Apg.getDatabase().db();
        mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        long now = new Date().getTime() / 1000;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " +
                                    "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                                    Keys.TABLE_NAME + "." + Keys.KEY_RING_ID + " AND " +
                                    Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" +
                                    ") " +
                                    " INNER JOIN " + UserIds.TABLE_NAME + " ON " +
                                    "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " +
                                    UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " +
                                    UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ");

        String inIdList = null;

        if (selectedKeyIds != null && selectedKeyIds.length > 0) {
            inIdList = KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < selectedKeyIds.length; ++i) {
                if (i != 0) {
                    inIdList += ", ";
                }
                inIdList += DatabaseUtils.sqlEscapeString("" + selectedKeyIds[i]);
            }
            inIdList += ")";
        }

        if (searchString != null && searchString.trim().length() > 0) {
            String[] chunks = searchString.trim().split(" +");
            qb.appendWhere("(EXISTS (SELECT tmp." + UserIds._ID + " FROM " +
                                    UserIds.TABLE_NAME + " AS tmp WHERE " +
                                    "tmp." + UserIds.KEY_ID + " = " +
                                    Keys.TABLE_NAME + "." + Keys._ID);
            for (int i = 0; i < chunks.length; ++i) {
                qb.appendWhere(" AND tmp." + UserIds.USER_ID + " LIKE ");
                qb.appendWhereEscapeString("%" + chunks[i] + "%");
            }
            qb.appendWhere("))");

            if (inIdList != null) {
                qb.appendWhere(" OR (" + inIdList + ")");
            }
        }

        String orderBy = UserIds.TABLE_NAME + "." + UserIds.USER_ID + " ASC";
        if (inIdList != null) {
            orderBy = inIdList + " DESC, " + orderBy;
        }

        mCursor = qb.query(mDatabase,
              new String[] {
                  KeyRings.TABLE_NAME + "." + KeyRings._ID,           // 0
                  KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 1
                  UserIds.TABLE_NAME + "." + UserIds.USER_ID,         // 2
                  "(SELECT COUNT(tmp." + Keys._ID + ") FROM " + Keys.TABLE_NAME + " AS tmp WHERE " +
                      "tmp." + Keys.KEY_RING_ID + " = " +
                      KeyRings.TABLE_NAME + "." + KeyRings._ID + " AND " +
                      "tmp." + Keys.IS_REVOKED + " = '0' AND " +
                      "tmp." + Keys.CAN_ENCRYPT + " = '1')",          // 3
                  "(SELECT COUNT(tmp." + Keys._ID + ") FROM " + Keys.TABLE_NAME + " AS tmp WHERE " +
                      "tmp." + Keys.KEY_RING_ID + " = " +
                      KeyRings.TABLE_NAME + "." + KeyRings._ID + " AND " +
                      "tmp." + Keys.IS_REVOKED + " = '0' AND " +
                      "tmp." + Keys.CAN_ENCRYPT + " = '1' AND " +
                      "tmp." + Keys.CREATION + " <= '" + now + "' AND " +
                      "(tmp." + Keys.EXPIRY + " IS NULL OR " +
                       "tmp." + Keys.EXPIRY + " >= '" + now + "'))",  // 4
              },
              KeyRings.TABLE_NAME + "." + KeyRings.TYPE + " = ?",
              new String[] { "" + Id.database.type_public },
              null, null, orderBy);

        activity.startManagingCursor(mCursor);
    }

    public void cleanup() {
        if (mCursor != null) {
            mActivity.stopManagingCursor(mCursor);
            mCursor.close();
        }
    }

    @Override
    public boolean isEnabled(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(4) > 0; // valid CAN_ENCRYPT
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public Object getItem(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(2); // USER_ID
    }

    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(1); // MASTER_KEY_ID
    }

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
        keyId.setText(Apg.getSmallFingerPrint(masterKeyId));

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (enabled) {
            status.setText(R.string.canEncrypt);
        } else {
            if (mCursor.getInt(3) > 0) {
                // has some CAN_ENCRYPT keys, but col(4) = 0, so must be revoked or expired
                status.setText(R.string.expired);
            } else {
                status.setText(R.string.noKey);
            }
        }

        status.setText(status.getText() + " ");

        CheckBox selected = (CheckBox) view.findViewById(R.id.selected);

        if (!enabled) {
            mParent.setItemChecked(position, false);
        }

        selected.setChecked(mParent.isItemChecked(position));

        view.setEnabled(enabled);
        mainUserId.setEnabled(enabled);
        mainUserIdRest.setEnabled(enabled);
        keyId.setEnabled(enabled);
        selected.setEnabled(enabled);
        status.setEnabled(enabled);

        return view;
    }
}
