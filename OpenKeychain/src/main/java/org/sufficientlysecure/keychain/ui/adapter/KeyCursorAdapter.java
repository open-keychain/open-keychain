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
