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
import android.support.v7.widget.RecyclerView;

import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;

public abstract class KeyCursorAdapter<C extends CursorAdapter.KeyCursor, VH extends RecyclerView.ViewHolder>
        extends CursorAdapter<C, VH> {

    private String mQuery;

    public KeyCursorAdapter(Context context, C cursor){
        super(context, cursor, 0);
        setHasStableIds(true);
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    @Override
    public int getItemViewType(int position) {
        moveCursorOrThrow(position);
        return getItemViewType(getCursor());
    }

    @Override
    public long getIdFromCursor(C keyCursor) {
        return keyCursor.getKeyId();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        moveCursorOrThrow(position);
        onBindViewHolder(holder, getCursor(), mQuery);
    }

    public int getItemViewType(C keyCursor) { return 0; }
    public abstract void onBindViewHolder(VH holder, C cursor, String query);
}
