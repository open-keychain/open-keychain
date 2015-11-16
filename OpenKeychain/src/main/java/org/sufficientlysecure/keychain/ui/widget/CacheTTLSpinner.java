/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;


public class CacheTTLSpinner extends AppCompatSpinner {

    public CacheTTLSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CacheTTLSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        MatrixCursor  cursor = new MatrixCursor(new String[] { "_id", "TTL", "description" }, 5);
        cursor.addRow(new Object[] { 0, 60*5, getContext().getString(R.string.cache_ttl_five_minutes) });
        cursor.addRow(new Object[] { 1, 60*60, getContext().getString(R.string.cache_ttl_one_hour) });
        cursor.addRow(new Object[] { 2, 60*60*3, getContext().getString(R.string.cache_ttl_three_hours) });
        cursor.addRow(new Object[] { 3, 60*60*24, getContext().getString(R.string.cache_ttl_one_day) });
        cursor.addRow(new Object[] { 4, 60*60*24*3, getContext().getString(R.string.cache_ttl_three_days) });

        setAdapter(new SimpleCursorAdapter(getContext(), R.layout.simple_item, cursor,
                new String[] { "description" },
                new int[] { R.id.simple_item_text },
                0));
    }

    public long getSelectedTimeToLive() {
        int selectedItemPosition = getSelectedItemPosition();
        Object item = getAdapter().getItem(selectedItemPosition);
        return ((Cursor) item).getLong(1);
    }

}
