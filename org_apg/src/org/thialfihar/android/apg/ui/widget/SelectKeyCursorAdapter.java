/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui.widget;

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.provider.ApgContract.KeyRings;
import org.thialfihar.android.apg.provider.ApgContract.UserIds;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class SelectKeyCursorAdapter extends CursorAdapter {

    protected int mKeyType;

    private LayoutInflater mInflater;
    private ListView mListView;

    public final static String PROJECTION_ROW_AVAILABLE = "available";
    public final static String PROJECTION_ROW_VALID = "valid";

    @SuppressWarnings("deprecation")
    public SelectKeyCursorAdapter(Context context, ListView listView, Cursor c, int keyType) {
        super(context, c);

        mInflater = LayoutInflater.from(context);
        mListView = listView;
        mKeyType = keyType;
    }

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(mCursor.getColumnIndex(UserIds.USER_ID));
    }

    public long getMasterKeyId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(mCursor.getColumnIndex(KeyRings.MASTER_KEY_ID));
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean valid = cursor.getInt(cursor
                .getColumnIndex(PROJECTION_ROW_VALID)) > 0;

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknownUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.noKey);
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText(R.string.unknownStatus);

        String userId = cursor.getString(cursor.getColumnIndex(UserIds.USER_ID));
        if (userId != null) {
            String[] userIdSplit = OtherHelper.splitUserId(userId);

            if (userIdSplit[1] != null) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
            mainUserId.setText(userIdSplit[0]);
        }

        long masterKeyId = cursor.getLong(cursor.getColumnIndex(KeyRings.MASTER_KEY_ID));
        keyId.setText(PGPHelper.getSmallFingerPrint(masterKeyId));

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (valid) {
            if (mKeyType == Id.type.public_key) {
                status.setText(R.string.canEncrypt);
            } else {
                status.setText(R.string.canSign);
            }
        } else {
            if (cursor.getInt(cursor
                    .getColumnIndex(PROJECTION_ROW_AVAILABLE)) > 0) {
                // has some CAN_ENCRYPT keys, but col(ROW_VALID) = 0, so must be revoked or
                // expired
                status.setText(R.string.expired);
            } else {
                status.setText(R.string.noKey);
            }
        }

        CheckBox selected = (CheckBox) view.findViewById(R.id.selected);
        if (mKeyType == Id.type.public_key) {
            selected.setVisibility(View.VISIBLE);

            if (!valid) {
                mListView.setItemChecked(cursor.getPosition(), false);
            }

            selected.setChecked(mListView.isItemChecked(cursor.getPosition()));
            selected.setEnabled(valid);
        } else {
            selected.setVisibility(View.GONE);
        }

        status.setText(status.getText() + " ");

        view.setEnabled(valid);
        mainUserId.setEnabled(valid);
        mainUserIdRest.setEnabled(valid);
        keyId.setEnabled(valid);
        status.setEnabled(valid);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.select_key_item, null);
    }

}