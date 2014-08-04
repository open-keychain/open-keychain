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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.util.Highlighter;

import java.util.Date;


/**
 * Yes this class is abstract!
 */
abstract public class SelectKeyCursorAdapter extends CursorAdapter {

    private String mQuery;
    private LayoutInflater mInflater;

    protected int mIndexUserId, mIndexMasterKeyId, mIndexRevoked, mIndexExpiry;

    public SelectKeyCursorAdapter(Context context, Cursor c, int flags, ListView listView) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        initIndex(c);
    }

    public void setSearchQuery(String query) {
        mQuery = query;
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
    protected void initIndex(Cursor cursor) {
        if (cursor != null) {
            mIndexUserId = cursor.getColumnIndexOrThrow(KeyRings.USER_ID);
            mIndexMasterKeyId = cursor.getColumnIndexOrThrow(KeyRings.MASTER_KEY_ID);
            mIndexExpiry = cursor.getColumnIndexOrThrow(KeyRings.EXPIRY);
            mIndexRevoked = cursor.getColumnIndexOrThrow(KeyRings.IS_REVOKED);
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

    public static class ViewHolderItem {
        public View view;
        public TextView mainUserId, mainUserIdRest, keyId, status;
        public CheckBox selected;

        public void setEnabled(boolean enabled) {
            view.setEnabled(enabled);
            selected.setEnabled(enabled);
            mainUserId.setEnabled(enabled);
            mainUserIdRest.setEnabled(enabled);
            keyId.setEnabled(enabled);
            status.setEnabled(enabled);

            // Sorta special: We set an item as clickable to disable it in the ListView. This works
            // because the list item will handle the clicks itself (which is a nop)
            view.setClickable(!enabled);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Highlighter highlighter = new Highlighter(context, mQuery);
        ViewHolderItem h = (ViewHolderItem) view.getTag();

        String userId = cursor.getString(mIndexUserId);
        String[] userIdSplit = KeyRing.splitUserId(userId);

        if (userIdSplit[0] != null) {
            h.mainUserId.setText(highlighter.highlight(userIdSplit[0]));
        } else {
            h.mainUserId.setText(R.string.user_id_no_name);
        }
        if (userIdSplit[1] != null) {
            h.mainUserIdRest.setVisibility(View.VISIBLE);
            h.mainUserIdRest.setText(highlighter.highlight(userIdSplit[1]));
        } else {
            h.mainUserIdRest.setVisibility(View.GONE);
        }

        long masterKeyId = cursor.getLong(mIndexMasterKeyId);
        h.keyId.setText(PgpKeyHelper.convertKeyIdToHex(masterKeyId));

        boolean enabled = true;
        if (cursor.getInt(mIndexRevoked) != 0) {
            h.status.setText(R.string.revoked);
            enabled = false;
        } else if (!cursor.isNull(mIndexExpiry)
                && new Date(cursor.getLong(mIndexExpiry) * 1000).before(new Date())) {
            h.status.setText(R.string.expired);
            enabled = false;
        } else {
            h.status.setText("");
        }

        h.status.setTag(enabled);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.select_key_item, null);
        ViewHolderItem holder = new ViewHolderItem();
        holder.view = view;
        holder.mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        holder.mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        holder.keyId = (TextView) view.findViewById(R.id.subkey_item_key_id);
        holder.status = (TextView) view.findViewById(R.id.status);
        holder.selected = (CheckBox) view.findViewById(R.id.selected);
        view.setTag(holder);
        return view;
    }
}
