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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Implements StickyListHeadersAdapter from library
 */
public class KeyListPublicAdapter extends HighlightQueryCursorAdapter implements StickyListHeadersAdapter {
    private LayoutInflater mInflater;
    private int mSectionColumnIndex;
    private int mIndexUserId;
    private int mIndexIsRevoked;

    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    public KeyListPublicAdapter(Context context, Cursor c, int flags, int sectionColumnIndex) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        mSectionColumnIndex = sectionColumnIndex;
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
            mIndexUserId = cursor.getColumnIndexOrThrow(KeychainContract.UserIds.USER_ID);
            mIndexIsRevoked = cursor.getColumnIndexOrThrow(KeychainContract.Keys.IS_REVOKED);
        }
    }

    /**
     * Bind cursor data to the item list view
     * <p/>
     * NOTE: CursorAdapter already implements the ViewHolder pattern in its getView() method. Thus
     * no ViewHolder is required here.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        TextView revoked = (TextView) view.findViewById(R.id.revoked);

        String userId = cursor.getString(mIndexUserId);
        String[] userIdSplit = PgpKeyHelper.splitUserId(userId);
        if (userIdSplit[0] != null) {
            mainUserId.setText(highlightSearchQuery(userIdSplit[0]));
        } else {
            mainUserId.setText(R.string.user_id_no_name);
        }
        if (userIdSplit[1] != null) {
            mainUserIdRest.setText(highlightSearchQuery(userIdSplit[1]));
            mainUserIdRest.setVisibility(View.VISIBLE);
        } else {
            mainUserIdRest.setVisibility(View.GONE);
        }

        boolean isRevoked = cursor.getInt(mIndexIsRevoked) > 0;
        if (isRevoked) {
            revoked.setVisibility(View.VISIBLE);
        } else {
            revoked.setVisibility(View.GONE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.key_list_public_item, null);
    }

    /**
     * Creates a new header view and binds the section headers to it. It uses the ViewHolder
     * pattern. Most functionality is similar to getView() from Android's CursorAdapter.
     * <p/>
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
        String userId = mCursor.getString(mSectionColumnIndex);
        String headerText = convertView.getResources().getString(R.string.user_id_no_name);
        if (userId != null && userId.length() > 0) {
            headerText = "" + mCursor.getString(mSectionColumnIndex).subSequence(0, 1).charAt(0);
        }
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
        // headers are based upon
        String userId = mCursor.getString(mSectionColumnIndex);
        if (userId != null && userId.length() > 0) {
            return userId.subSequence(0, 1).charAt(0);
        } else {
            return Long.MAX_VALUE;
        }
    }

    class HeaderViewHolder {
        TextView text;
    }

    /**
     * -------------------------- MULTI-SELECTION METHODS --------------
     */
    public void setNewSelection(int position, boolean value) {
        mSelection.put(position, value);
        notifyDataSetChanged();
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
        if (mSelection.get(position) != null && mSelection.get(position).booleanValue()) {
            // color for selected items
            v.setBackgroundColor(parent.getResources().getColor(R.color.emphasis));
        } else {
            // default color
            v.setBackgroundColor(Color.TRANSPARENT);
        }
        return v;
    }

}
