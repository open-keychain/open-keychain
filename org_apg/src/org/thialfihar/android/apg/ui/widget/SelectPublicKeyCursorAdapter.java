/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPHelper;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class SelectPublicKeyCursorAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
//    private int mActivityIndex;
//    private int mTimeIndex;
//    private int mActionIndex;
//    private int mAmountIndex;

    public SelectPublicKeyCursorAdapter(Context context, Cursor c) {
        super(context, c);
//
//        mActivityIndex = c.getColumnIndex(Notes.ACTIVITY);
//        mTimeIndex = c.getColumnIndex(Notes.TIME);
//        mActionIndex = c.getColumnIndex(Notes.ACTION);
//        mAmountIndex = c.getColumnIndex(Notes.AMOUNT);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TextView activity = (TextView) view.findViewById(android.R.id.text1);
        // TextView time = (TextView) view.findViewById(android.R.id.text2);
        // TextView actionAndAmount = (TextView) view.findViewById(R.id.text3);
        //
        // activity.setText(cursor.getString(mActivityIndex));
        //
        // long lTime = cursor.getLong(mTimeIndex);
        // Calendar cal = Calendar.getInstance();
        // cal.setTimeInMillis(lTime);
        // time.setText(cal.get(Calendar.HOUR_OF_DAY) + “:” + String.format(“%02d”,
        // cal.get(Calendar.MINUTE)));
        //
        // String amount = cursor.getString(mAmountIndex);
        // if ( amount.length() > 0){
        // actionAndAmount.setText(cursor.getString(mActionIndex) + ” (” + amount + “)”);
        // } else {
        // actionAndAmount.setText(cursor.getString(mActionIndex));
        // }
        
//        boolean enabled = isEnabled(position);

        
        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknownUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.noKey);
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText(R.string.unknownStatus);

        String userId = cursor.getString(2); // USER_ID
        if (userId != null) {
            String[] userIdSplit = OtherHelper.splitUserId(userId);

            if (userIdSplit[1] != null) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
            mainUserId.setText(userIdSplit[0]);
        }

        long masterKeyId = cursor.getLong(1); // MASTER_KEY_ID
        keyId.setText(PGPHelper.getSmallFingerPrint(masterKeyId));

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

//        if (enabled) {
//            status.setText(R.string.canEncrypt);
//        } else {
            if (cursor.getInt(3) > 0) {
                // has some CAN_ENCRYPT keys, but col(4) = 0, so must be revoked or expired
                status.setText(R.string.expired);
            } else {
                status.setText(R.string.noKey);
            }
//        }

        status.setText(status.getText() + " ");

        CheckBox selected = (CheckBox) view.findViewById(R.id.selected);

//        if (!enabled) {
//            mParent.setItemChecked(position, false);
//        }

//        selected.setChecked(mParent.isItemChecked(position));

//        view.setEnabled(enabled);
//        mainUserId.setEnabled(enabled);
//        mainUserIdRest.setEnabled(enabled);
//        keyId.setEnabled(enabled);
//        selected.setEnabled(enabled);
//        status.setEnabled(enabled);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.select_public_key, null);
    }

}