/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import android.view.View;

import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;

public abstract class UserAttributesAdapter extends CursorAdapter {
    public static final String[] USER_PACKETS_PROJECTION = new String[]{
            UserPackets._ID,
            UserPackets.TYPE,
            UserPackets.USER_ID,
            UserPackets.ATTRIBUTE_DATA,
            UserPackets.RANK,
            UserPackets.VERIFIED,
            UserPackets.IS_PRIMARY,
            UserPackets.IS_REVOKED,
            UserPackets.NAME,
            UserPackets.EMAIL,
            UserPackets.COMMENT,
    };
    public static final int INDEX_ID = 0;
    public static final int INDEX_TYPE = 1;
    public static final int INDEX_USER_ID = 2;
    public static final int INDEX_ATTRIBUTE_DATA = 3;
    public static final int INDEX_RANK = 4;
    public static final int INDEX_VERIFIED = 5;
    public static final int INDEX_IS_PRIMARY = 6;
    public static final int INDEX_IS_REVOKED = 7;
    public static final int INDEX_NAME = 8;
    public static final int INDEX_EMAIL = 9;
    public static final int INDEX_COMMENT = 10;

    public UserAttributesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public abstract void bindView(View view, Context context, Cursor cursor);

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(INDEX_USER_ID);
    }

    public boolean getIsRevoked(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(INDEX_IS_REVOKED) > 0;
    }

    public int getIsVerified(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(INDEX_VERIFIED);
    }

}
