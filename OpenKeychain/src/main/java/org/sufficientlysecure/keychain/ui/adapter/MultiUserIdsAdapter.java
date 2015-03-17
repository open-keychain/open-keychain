/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;

import java.util.ArrayList;

public class MultiUserIdsAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    private final ArrayList<Boolean> mCheckStates;

    public MultiUserIdsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        mCheckStates = new ArrayList<>();
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        mCheckStates.clear();
        if (newCursor != null) {
            int count = newCursor.getCount();
            mCheckStates.ensureCapacity(count);
            // initialize to true (use case knowledge: we usually want to sign all uids)
            for (int i = 0; i < count; i++) {
                mCheckStates.add(true);
            }
        }

        return super.swapCursor(newCursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.certify_item, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vHeaderId = (TextView) view.findViewById(R.id.user_id_header);
        TextView vName = (TextView) view.findViewById(R.id.user_id_item_name);
        TextView vAddresses = (TextView) view.findViewById(R.id.user_id_item_addresses);

        byte[] data = cursor.getBlob(1);
        int isHeader = cursor.getInt(2);
        Parcel p = Parcel.obtain();
        p.unmarshall(data, 0, data.length);
        p.setDataPosition(0);
        ArrayList<String> uids = p.createStringArrayList();
        p.recycle();

        { // first one
            String userId = uids.get(0);
            KeyRing.UserId splitUserId = KeyRing.splitUserId(userId);
            if (splitUserId.name != null) {
                vName.setText(splitUserId.name);
            } else {
                vName.setText(R.string.user_id_no_name);
            }

            if (isHeader == 1) {
                vHeaderId.setVisibility(View.VISIBLE);
                String message;
                if (splitUserId.name != null) {
                    message = mContext.getString(R.string.section_uids_to_certify) +
                            splitUserId.name;
                } else {
                    message = mContext.getString(R.string.section_uids_to_certify) +
                           context.getString(R.string.user_id_no_name);
                }
                vHeaderId.setText(message);
            } else {
                vHeaderId.setVisibility(View.GONE);
            }
        }

        StringBuilder lines = new StringBuilder();
        for (String uid : uids) {
            KeyRing.UserId splitUserId = KeyRing.splitUserId(uid);
            if (splitUserId.email == null) {
                continue;
            }
            lines.append(splitUserId.email);
            if (splitUserId.comment != null) {
                lines.append(" (").append(splitUserId.comment).append(")");
            }
            lines.append('\n');
        }

        // If we have any data here, show it
        if (lines.length() > 0) {
            // delete last newline
            lines.setLength(lines.length() - 1);
            vAddresses.setVisibility(View.VISIBLE);
            vAddresses.setText(lines);
        } else {
            vAddresses.setVisibility(View.GONE);
        }

        final CheckBox vCheckBox = (CheckBox) view.findViewById(R.id.user_id_item_check_box);
        final int position = cursor.getPosition();
        vCheckBox.setOnCheckedChangeListener(null);
        vCheckBox.setChecked(mCheckStates.get(position));
        vCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mCheckStates.set(position, b);
            }
        });
        vCheckBox.setClickable(false);

        View vUidBody = view.findViewById(R.id.user_id_body);
        vUidBody.setClickable(true);
        vUidBody.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                vCheckBox.toggle();
            }
        });

    }

    public ArrayList<CertifyAction> getSelectedCertifyActions() {
        LongSparseArray<CertifyAction> actions = new LongSparseArray<>();
        for (int i = 0; i < mCheckStates.size(); i++) {
            if (mCheckStates.get(i)) {
                mCursor.moveToPosition(i);

                long keyId = mCursor.getLong(0);
                byte[] data = mCursor.getBlob(1);

                Parcel p = Parcel.obtain();
                p.unmarshall(data, 0, data.length);
                p.setDataPosition(0);
                ArrayList<String> uids = p.createStringArrayList();
                p.recycle();

                CertifyAction action = actions.get(keyId);
                if (actions.get(keyId) == null) {
                    actions.put(keyId, new CertifyAction(keyId, uids));
                } else {
                    action.mUserIds.addAll(uids);
                }
            }
        }

        ArrayList<CertifyAction> result = new ArrayList<>(actions.size());
        for (int i = 0; i < actions.size(); i++) {
            result.add(actions.valueAt(i));
        }
        return result;
    }

}
