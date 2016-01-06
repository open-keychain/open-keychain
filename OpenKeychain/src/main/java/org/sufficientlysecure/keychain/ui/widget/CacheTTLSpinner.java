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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Preferences.CacheTTLPrefs;


public class CacheTTLSpinner extends AppCompatSpinner {

    public CacheTTLSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public CacheTTLSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {

        CacheTTLPrefs prefs = Preferences.getPreferences(context).getPassphraseCacheTtl();
        MatrixCursor  cursor = new MatrixCursor(new String[] { "_id", "TTL", "description" }, 5);
        int i = 0;
        for (int ttl : CacheTTLPrefs.CACHE_TTLS) {
            if ( ! prefs.ttlTimes.contains(ttl)) {
                continue;
            }
            cursor.addRow(new Object[] { i++, ttl, getContext().getString(CacheTTLPrefs.CACHE_TTL_NAMES.get(ttl)) });
        }

        setAdapter(new SimpleCursorAdapter(getContext(), R.layout.simple_item, cursor,
                new String[] { "description" }, new int[] { R.id.simple_item_text }, 0));
    }

    public int getSelectedTimeToLive() {
        int selectedItemPosition = getSelectedItemPosition();
        Object item = getAdapter().getItem(selectedItemPosition);
        return ((Cursor) item).getInt(1);
    }

}
