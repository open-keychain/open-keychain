/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;


public class SelectKeyCursorAdapter extends HighlightQueryCursorAdapter {

    protected int mKeyType;

    private LayoutInflater mInflater;
    private ListView mListView;

    private int mIndexUserId;
    private int mIndexMasterKeyId;
    private int mIndexProjectionValid;
    private int mIndexProjectionAvailable;

    public static final String PROJECTION_ROW_AVAILABLE = "available";
    public static final String PROJECTION_ROW_VALID = "valid";

    public SelectKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView,
                                  int keyType) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
        mListView = listView;
        mKeyType = keyType;
        initIndex(c);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        initIndex(newCursor);

        return super.swapCursor(newCursor);
    }

    /**
     * Get column indexes for performance reasons just once in constructor and swapCursor. For a
     * performance comparison see http://stackoverflow.com/a/17999582
     *
     * @param cursor
     */
    private void initIndex(Cursor cursor) {
        if (cursor != null) {
            mIndexUserId = cursor.getColumnIndexOrThrow(UserIds.USER_ID);
            mIndexMasterKeyId = cursor.getColumnIndexOrThrow(KeyRings.MASTER_KEY_ID);
            mIndexProjectionValid = cursor.getColumnIndexOrThrow(PROJECTION_ROW_VALID);
            mIndexProjectionAvailable = cursor.getColumnIndexOrThrow(PROJECTION_ROW_AVAILABLE);
        }
    }

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(mIndexUserId);
    }

    public long getMasterKeyId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(mIndexMasterKeyId);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean valid = cursor.getInt(mIndexProjectionValid) > 0;

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        TextView status = (TextView) view.findViewById(R.id.status);

        String userId = cursor.getString(mIndexUserId);
        String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

        if (userIdSplit[0] != null) {
            mainUserId.setText(highlightSearchQuery(userIdSplit[0]));
        } else {
            mainUserId.setText(R.string.user_id_no_name);
        }
        if (userIdSplit[1] != null) {
            mainUserIdRest.setText(highlightSearchQuery(userIdSplit[1]));
        } else {
            mainUserIdRest.setText("");
        }

        // TODO: needed to key id to no?
        keyId.setText(R.string.no_key);
        long masterKeyId = cursor.getLong(mIndexMasterKeyId);
        keyId.setText(PgpKeyHelper.convertKeyIdToHexShort(masterKeyId));

        // TODO: needed to set unknown_status?
        status.setText(R.string.unknown_status);
        if (valid) {
            if (mKeyType == Id.type.public_key) {
                status.setText(R.string.can_encrypt);
            } else {
                status.setText(R.string.can_sign);
            }
        } else {
            if (cursor.getInt(mIndexProjectionAvailable) > 0) {
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
