package org.sufficientlysecure.keychain.ui.adapter;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;


public class KeySelectableAdapter extends KeyAdapter implements OnItemClickListener {

    HashSet<Long> mSelectedItems = new HashSet<>();

    public KeySelectableAdapter(Context context, Cursor c, int flags, Set<Long> initialChecked) {
        super(context, c, flags);
        if (initialChecked != null) {
            mSelectedItems.addAll(initialChecked);
        }
    }

    public static class KeySelectableItemViewHolder extends KeyItemViewHolder {

        public CheckBox mCheckbox;

        public KeySelectableItemViewHolder(View view) {
            super(view);
            mCheckbox = (CheckBox) view.findViewById(R.id.selected);
        }

        public void setCheckedState(boolean checked) {
            mCheckbox.setChecked(checked);
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.key_list_selectable_item, parent, false);
        KeySelectableItemViewHolder holder = new KeySelectableItemViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        KeySelectableItemViewHolder h = (KeySelectableItemViewHolder) view.getTag();
        h.setCheckedState(mSelectedItems.contains(h.mDisplayedItem.mKeyId));

    }

    public void setCheckedStates(Set<Long> checked) {
        mSelectedItems.clear();
        mSelectedItems.addAll(checked);
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedMasterKeyIds() {
        return Collections.unmodifiableSet(mSelectedItems);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Constants.TAG, "clicked id: " + id);
        long masterKeyId = getMasterKeyId(position);
        if (mSelectedItems.contains(masterKeyId)) {
            mSelectedItems.remove(masterKeyId);
        } else {
            mSelectedItems.add(masterKeyId);
        }
        notifyDataSetChanged();
    }

}
