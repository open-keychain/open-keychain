/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.util.Log;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * - implements StickyListHeadersAdapter from library - uses view holder pattern for performance
 * 
 */
public class KeyListPublicAdapter extends CursorAdapter implements StickyListHeadersAdapter {
    private LayoutInflater mInflater;

    int mSectionColumnIndex;

    public KeyListPublicAdapter(Context context, Cursor c, int flags, int sectionColumnIndex) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
        mSectionColumnIndex = sectionColumnIndex;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TODO: view holder pattern?
        int userIdIndex = cursor.getColumnIndex(UserIds.USER_ID);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");

        String userId = cursor.getString(userIdIndex);
        if (userId != null) {
            String[] userIdSplit = OtherHelper.splitUserId(userId);

            if (userIdSplit[1] != null) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
            mainUserId.setText(userIdSplit[0]);
        }

        if (mainUserId.getText().length() == 0) {
            mainUserId.setText(R.string.unknown_user_id);
        }

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        } else {
            mainUserIdRest.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.key_list_item, null);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {

        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.stickylist_header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.stickylist_header_text);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        if (!mDataValid) {
            // no data available at this point
            Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
            return convertView;
        }

        // similar to getView in CursorAdapter
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        // set header text as first char in name
        String headerText = "" + mCursor.getString(mSectionColumnIndex).subSequence(0, 1).charAt(0);
        holder.text.setText(headerText);
        return convertView;
    }

    /**
     * Remember that these have to be static, position=1 should always return the same Id that is.
     */
    @Override
    public long getHeaderId(int position) {
        if (!mDataValid) {
            // no data available at this point
            Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
            return -1;
        }

        // similar to getView in CursorAdapter
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        // return the first character of the name as ID because this is what
        // headers are based upon
        return mCursor.getString(mSectionColumnIndex).subSequence(0, 1).charAt(0);
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView mainUserId;
        TextView mainUserIdRest;
    }

}
