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

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;

import org.sufficientlysecure.keychain.R;


public class CacheTTLSpinner extends AppCompatSpinner {
    public static final int[] TTL_TIMES = {
            0,
            60 * 60,
            60 * 60 * 24,
            Integer.MAX_VALUE
    };
    public static final int[] TTL_STRINGS = {
            R.string.cache_ttl_lock_screen,
            R.string.cache_ttl_one_hour,
            R.string.cache_ttl_one_day,
            R.string.cache_ttl_forever
    };

    public CacheTTLSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CacheTTLSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        MatrixCursor  cursor = new MatrixCursor(new String[] { "_id", "TTL", "description" }, TTL_TIMES.length);
        for (int i = 0; i < TTL_TIMES.length; i++) {
            cursor.addRow(new Object[] { i, TTL_TIMES[i], getContext().getString(TTL_STRINGS[i]) });
        }

        setAdapter(new SimpleCursorAdapter(getContext(), R.layout.simple_item, cursor,
                new String[] { "description" }, new int[] { R.id.simple_item_text }, 0));
    }

    public int getSelectedTimeToLive() {
        int selectedItemPosition = getSelectedItemPosition();
        Object item = getAdapter().getItem(selectedItemPosition);
        return ((Cursor) item).getInt(1);
    }

    public void setSelectedTimeToLive(int ttlSeconds) {
        for (int i = 0; i < TTL_TIMES.length; i++) {
            boolean isSelectedOrLast = ttlSeconds <= TTL_TIMES[i] || (i == TTL_TIMES.length - 1);
            if (isSelectedOrLast) {
                setSelection(i);
                break;
            }
        }
    }
}
