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

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.*;

public class KeyValueSpinnerAdapter extends ArrayAdapter<String> {
    private final HashMap<Integer, String> mData;
    private final int[] mKeys;
    private final String[] mValues;

    static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(
            Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e1.getValue().compareTo(e2.getValue());
                    }
                });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public KeyValueSpinnerAdapter(Context context, HashMap<Integer, String> objects) {
        // To make the drop down a simple text box
        super(context, android.R.layout.simple_spinner_item);
        mData = objects;

        // To make the drop down view a radio button list
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SortedSet<Map.Entry<Integer, String>> sorted = entriesSortedByValues(objects);

        // Assign hash keys with a position so that we can present and retrieve them
        int i = 0;
        mKeys = new int[mData.size()];
        mValues = new String[mData.size()];
        for (Map.Entry<Integer, String> entry : sorted) {
            mKeys[i] = entry.getKey();
            mValues[i] = entry.getValue();
            i++;
        }
    }

    public int getCount() {
        return mData.size();
    }

    /**
     * Returns the value
     */
    @Override
    public String getItem(int position) {
        // return the value based on the position. This is displayed in the list.
        return mValues[position];
    }

    /**
     * Returns item key
     */
    public long getItemId(int position) {
        // Return an id to represent the item.

        return mKeys[position];
    }

    /**
     * Find position from key
     */
    public int getPosition(long itemId) {
        for (int i = 0; i < mKeys.length; i++) {
            if ((int) itemId == mKeys[i]) {
                return i;
            }
        }
        return -1;
    }
}