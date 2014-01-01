/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Gonçalo Ferreira
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.util;

import java.util.LinkedHashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Originally from https://github.com/monxalo/android-section-adapter
 * 
 * getCustomGroup() has been modified
 */
public abstract class SectionCursorAdapter extends CursorAdapter {

    private static final String TAG = "SectionCursorAdapter";
    private static final boolean LOGV = false;

    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COUNT = 2;

    private final int mHeaderRes;
    private final int mGroupColumn;
    private final LayoutInflater mLayoutInflater;

    private LinkedHashMap<Integer, String> sectionsIndexer;

    public static class ViewHolder {
        public TextView textView;
    }

    public SectionCursorAdapter(Context context, Cursor c, int headerLayout, int groupColumn) {
        super(context, c, 0);

        sectionsIndexer = new LinkedHashMap<Integer, String>();

        mHeaderRes = headerLayout;
        mGroupColumn = groupColumn;
        mLayoutInflater = LayoutInflater.from(context);

        if (c != null) {
            calculateSectionHeaders();
            c.registerDataSetObserver(mDataSetObserver);
        }
    }

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        public void onChanged() {
            calculateSectionHeaders();
        };

        public void onInvalidated() {
            sectionsIndexer.clear();
        };
    };

    /**
     * <p>
     * This method serve as an intercepter before the sections are calculated so you can transform
     * some computer data into human readable, e.g. format a unix timestamp, or a status.
     * </p>
     * 
     * <p>
     * By default this method returns the original data for the group column.
     * </p>
     * 
     * @param groupData
     * @return
     */
    protected String getCustomGroup(String groupData) {
        return groupData.substring(0, 1);
    }

    private void calculateSectionHeaders() {
        int i = 0;

        String previous = "";
        int count = 0;

        final Cursor c = getCursor();

        sectionsIndexer.clear();

        if (c == null) {
            return;
        }

        c.moveToPosition(-1);

        while (c.moveToNext()) {
            final String group = getCustomGroup(c.getString(mGroupColumn));

            if (!previous.equals(group)) {
                sectionsIndexer.put(i + count, group);
                previous = group;

                if (LOGV)
                    Log.v(TAG, "Group " + group + "at position: " + (i + count));

                count++;
            }

            i++;
        }
    }

    public String getGroupCustomFormat(Object obj) {
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_NORMAL) {
            Cursor c = (Cursor) getItem(position);

            if (c == null) {
                if (LOGV)
                    Log.d(TAG, "getItem(" + position + ") = null");
                return mLayoutInflater.inflate(mHeaderRes, parent, false);
            }

            final int mapCursorPos = getSectionForPosition(position);
            c.moveToPosition(mapCursorPos);

            return super.getView(mapCursorPos, convertView, parent);
        } else {
            ViewHolder holder = null;

            if (convertView == null) {
                if (LOGV)
                    Log.v(TAG, "Creating new view for section");

                holder = new ViewHolder();
                convertView = mLayoutInflater.inflate(mHeaderRes, parent, false);
                holder.textView = (TextView) convertView;

                convertView.setTag(holder);
            } else {
                if (LOGV)
                    Log.v(TAG, "Reusing view for section");

                holder = (ViewHolder) convertView.getTag();
            }

            TextView sectionText = holder.textView;

            final String group = sectionsIndexer.get(position);
            final String customFormat = getGroupCustomFormat(group);

            sectionText.setText(customFormat == null ? group : customFormat);

            return sectionText;
        }
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getCount() {
        return super.getCount() + sectionsIndexer.size();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == TYPE_NORMAL;
    }

    public int getPositionForSection(int section) {
        if (sectionsIndexer.containsKey(section)) {
            return section + 1;
        }
        return section;
    }

    public int getSectionForPosition(int position) {
        int offset = 0;
        for (Integer key : sectionsIndexer.keySet()) {
            if (position > key) {
                offset++;
            } else {
                break;
            }
        }

        return position - offset;
    }

    @Override
    public Object getItem(int position) {
        if (getItemViewType(position) == TYPE_NORMAL) {
            return super.getItem(getSectionForPosition(position));
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_NORMAL) {
            return super.getItemId(getSectionForPosition(position));
        }
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getPositionForSection(position)) {
            return TYPE_NORMAL;
        }
        return TYPE_HEADER;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        final Cursor old = swapCursor(cursor);

        if (old != null) {
            old.close();
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (getCursor() != null) {
            getCursor().unregisterDataSetObserver(mDataSetObserver);
        }

        final Cursor oldCursor = super.swapCursor(newCursor);

        calculateSectionHeaders();

        if (newCursor != null) {
            newCursor.registerDataSetObserver(mDataSetObserver);
        }

        return oldCursor;
    }
}