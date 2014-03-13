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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;

public class ViewKeyUserIdsAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private int mIndexUserId;

    public ViewKeyUserIdsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);

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
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String userIdStr = cursor.getString(mIndexUserId);

        TextView userId = (TextView) view.findViewById(R.id.userId);
        userId.setText(userIdStr);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.view_key_userids_item, null);
    }

}
