/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;

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

    public SelectKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView,
            int keyType) {
        super(context, c, flags);

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
        boolean valid = cursor.getInt(cursor.getColumnIndex(PROJECTION_ROW_VALID)) > 0;

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.no_key);
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText(R.string.unknown_status);

        String userId = cursor.getString(cursor.getColumnIndex(UserIds.USER_ID));
        if (userId != null) {
            String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

            if (userIdSplit[1] != null) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
            mainUserId.setText(userIdSplit[0]);
        }

        long masterKeyId = cursor.getLong(cursor.getColumnIndex(KeyRings.MASTER_KEY_ID));
        keyId.setText(PgpKeyHelper.convertKeyIdToHex(masterKeyId));

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (valid) {
            if (mKeyType == Id.type.public_key) {
                status.setText(R.string.can_encrypt);
            } else {
                status.setText(R.string.can_sign);
            }
        } else {
            if (cursor.getInt(cursor.getColumnIndex(PROJECTION_ROW_AVAILABLE)) > 0) {
                // has some CAN_ENCRYPT keys, but col(ROW_VALID) = 0, so must be revoked or
                // expired
                status.setText(R.string.expired);
            } else {
                status.setText(R.string.no_key);
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
