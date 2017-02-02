package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-2.
 */

public class KeyHeaderItem extends AbstractHeaderItem<KeyHeaderItem.ViewHolder> {
    int mId;
    String mTitle;

    public KeyHeaderItem(Cursor cursor) {
        super();
        boolean isSecret = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ANY_SECRET)) != 0;
        String name = cursor.getString(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.NAME));
        setTitle(isSecret ? "My keys" : name.substring(0, 1).toUpperCase());
    }

    public KeyHeaderItem(String title) {
        super();
        mTitle = title;
    }

    public int getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof KeyHeaderItem) {
            return getTitle().equals(((KeyHeaderItem) o).getTitle());
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.key_list_header_private;
    }

    @Override
    public ViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List payloads) {
        holder.textView.setText(mTitle);
    }

    static final class ViewHolder extends FlexibleViewHolder {
        TextView textView;
        ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);
            textView = (TextView) view.findViewById(android.R.id.text1);
        }
    }
}
