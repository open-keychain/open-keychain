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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class KeyListSecretAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private int mIndexUserId;

    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    public KeyListSecretAdapter(Context context, Cursor c, int flags) {
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
        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");

        String userId = cursor.getString(mIndexUserId);
        if (userId != null) {
            String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

            if (userIdSplit[0] != null && userIdSplit[0].length() > 0) {
                mainUserId.setText(userIdSplit[0]);
            }

            if (userIdSplit[1] != null && userIdSplit[1].length() > 0) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.key_list_item, null);
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
