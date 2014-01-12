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

import java.util.HashMap;
import java.util.Set;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.util.Log;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Implements StickyListHeadersAdapter from library
 */
public class KeyListPublicAdapter extends CursorAdapter implements StickyListHeadersAdapter {
    private LayoutInflater mInflater;
    private int mSectionColumnIndex;

    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    public KeyListPublicAdapter(Context context, Cursor c, int flags, int sectionColumnIndex) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
        mSectionColumnIndex = sectionColumnIndex;
    }

    /**
     * Bind cursor data to the item list view
     * 
     * NOTE: CursorAdapter already implements the ViewHolder pattern in its getView() method. Thus
     * no ViewHolder is required here.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int userIdIndex = cursor.getColumnIndex(UserIds.USER_ID);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");

        String userId = cursor.getString(userIdIndex);
        if (userId != null) {
            String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

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

    /**
     * Creates a new header view and binds the section headers to it. It uses the ViewHolder
     * pattern. Most functionality is similar to getView() from Android's CursorAdapter.
     * 
     * NOTE: The variables mDataValid and mCursor are available due to the super class
     * CursorAdapter.
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.key_list_public_header, parent, false);
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

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        // set header text as first char in user id
        String headerText = "" + mCursor.getString(mSectionColumnIndex).subSequence(0, 1).charAt(0);
        holder.text.setText(headerText);
        return convertView;
    }

    /**
     * Header IDs should be static, position=1 should always return the same Id that is.
     */
    @Override
    public long getHeaderId(int position) {
        if (!mDataValid) {
            // no data available at this point
            Log.d(Constants.TAG, "getHeaderView: No data available at this point!");
            return -1;
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        // return the first character of the name as ID because this is what
        // headers private HashMap<Integer, Boolean> mSelection = new HashMap<Integer,
        // Boolean>();are based upon
        return mCursor.getString(mSectionColumnIndex).subSequence(0, 1).charAt(0);
    }

    class HeaderViewHolder {
        TextView text;
    }

    /** -------------------------- MULTI-SELECTION METHODS -------------- */
    public void setNewSelection(int position, boolean value) {
        mSelection.put(position, value);
        notifyDataSetChanged();
    }

    public boolean isPositionChecked(int position) {
        Boolean result = mSelection.get(position);
        return result == null ? false : result;
    }

    public Set<Integer> getCurrentCheckedPosition() {
        return mSelection.keySet();
    }

    public void removeSelection(int position) {
        mSelection.remove(position);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        mSelection.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // let the adapter handle setting up the row views
        View v = super.getView(position, convertView, parent);

        /**
         * Change color for multi-selection
         */
        // default color
        v.setBackgroundColor(Color.TRANSPARENT);
        if (mSelection.get(position) != null) {
            // this is a selected position, change color!
            v.setBackgroundColor(parent.getResources().getColor(R.color.emphasis));
        }
        return v;
    }

}
