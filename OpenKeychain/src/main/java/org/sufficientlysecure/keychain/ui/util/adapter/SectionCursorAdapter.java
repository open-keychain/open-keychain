/*
 * Copyright (C) 2016 Tobias Erthal
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.util.adapter;

import android.content.Context;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.tonicartos.superslim.LayoutManager;

import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter.SimpleCursor;
import org.sufficientlysecure.keychain.util.Log;


/**
 * @param <T> section type.
 * @param <VH> the view holder extending {@code BaseViewHolder<Cursor>} that is bound to the cursor data.
 * @param <SH> the view holder extending {@code BaseViewHolder<<T>>} that is bound to the section data.
 */
public abstract class SectionCursorAdapter<C extends SimpleCursor, T, VH extends SectionCursorAdapter.ViewHolder,
        SH extends SectionCursorAdapter.ViewHolder> extends CursorAdapter<C, RecyclerView.ViewHolder> {

    public static final String TAG = "SectionCursorAdapter";

    private static final short VIEW_TYPE_ITEM = 0x1;
    private static final short VIEW_TYPE_SECTION = 0x2;

    private SparseArrayCompat<T> mSectionMap = new SparseArrayCompat<>();
    private Comparator<T> mSectionComparator;

    public SectionCursorAdapter(Context context, C cursor, int flags) {
        this(context, cursor, flags, new Comparator<T>() {
            @Override
            public boolean equal(T obj1, T obj2) {
                return (obj1 == null) ?
                        obj2 == null : obj1.equals(obj2);
            }
        });
    }

    public SectionCursorAdapter(Context context, C cursor, int flags, Comparator<T> comparator) {
        super(context, cursor, flags);
        setSectionComparator(comparator);
    }

    @Override
    public void onContentChanged() {
        if (hasValidData()) {
            buildSections();
        } else {
            mSectionMap.clear();
        }

        super.onContentChanged();
    }

    /**
     * Assign a comparator which will be used to check whether
     * a section is contained in the list of sections. The default implementation
     * will check for null pointers and compare sections using the {@link #equals(Object)} method.
     * @param comparator The comparator to compare section objects.
     */
    public void setSectionComparator(Comparator<T> comparator) {
        this.mSectionComparator = comparator;
        buildSections();
    }

    /**
     * If the adapter's cursor is not null then this method will call buildSections(Cursor cursor).
     */
    private void buildSections() {
        if (hasValidData()) {
            moveCursor(-1);
            try {
                mSectionMap.clear();
                appendSections(getCursor());
            } catch (IllegalStateException e) {
                Log.e(TAG, "Couldn't build sections. Perhaps you're moving the cursor" +
                        "in #getSectionFromCursor(Cursor)?", e);
                swapCursor(null);

                mSectionMap.clear();
            }
        }
    }

    private void appendSections(C cursor) throws IllegalStateException {
        int cursorPosition = 0;
        while(hasValidData() && cursor.moveToNext()) {
            T section = getSectionFromCursor(cursor);
            if (cursor.getPosition() != cursorPosition) {
                throw new IllegalStateException("Do not move the cursor's position in getSectionFromCursor.");
            }
            if (!hasSection(section)) {
                mSectionMap.append(cursorPosition + mSectionMap.size(), section);
            }
            cursorPosition++;
        }
    }

    public boolean hasSection(T section) {
        for(int i = 0; i < mSectionMap.size(); i++) {
            T obj = mSectionMap.valueAt(i);
            if(mSectionComparator.equal(obj, section))
                return true;
        }

        return false;
    }

    /**
     * The object which is return will determine what section this cursor position will be in.
     * @return the section from the cursor at its current position.
     * This object will be passed to newSectionView and bindSectionView.
     */
    protected abstract T getSectionFromCursor(C cursor) throws IllegalStateException;

    /**
     * Return the id of the item represented by the row the cursor
     * is currently moved to.
     * @param section The section item to get the id from
     * @return The id of the dataset
     */
    public long getIdFromSection(T section) {
        return section != null ? section.hashCode() : 0L;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + mSectionMap.size();
    }

    @Override
    public final long getItemId(int listPosition) {
        int index = mSectionMap.indexOfKey(listPosition);
        if (index < 0) {
            int cursorPosition = getCursorPositionWithoutSections(listPosition);
            return super.getItemId(cursorPosition);
        } else {
            T section = mSectionMap.valueAt(index);
            return getIdFromSection(section);
        }
    }

    /**
     * @param listPosition  the position of the current item in the list with mSectionMap included
     * @return Whether or not the listPosition points to a section.
     */
    public boolean isSection(int listPosition) {
        return mSectionMap.indexOfKey(listPosition) >= 0;
    }

    /**
     * This will map a position in the list adapter (which includes mSectionMap) to a position in
     * the cursor (which does not contain mSectionMap).
     *
     * @param listPosition the position of the current item in the list with mSectionMap included
     * @return the correct position to use with the cursor
     */
    public int getCursorPositionWithoutSections(int listPosition) {
        if (mSectionMap.size() == 0) {
            return listPosition;
        } else if (!isSection(listPosition)) {
            int sectionIndex = getSectionForPosition(listPosition);
            if (isListPositionBeforeFirstSection(listPosition, sectionIndex)) {
                return listPosition;
            } else {
                return listPosition - (sectionIndex + 1);
            }
        } else {
            return -1;
        }
    }

    public int getListPosition(int cursorPosition) {
        for(int i = 0; i < mSectionMap.size(); i++) {
            int sectionIndex = mSectionMap.keyAt(i);
            if (sectionIndex > cursorPosition) {
                return cursorPosition;
            }

            cursorPosition +=1;
        }

        return cursorPosition;
    }

    /**
     * Given the list position of an item in the adapter, returns the
     * adapter position of the first item of the section the given item belongs to.
     * @param listPosition The absolute list position.
     * @return The position of the first item of the section.
     */
    public int getFirstSectionPosition(int listPosition) {
        int start = 0;
        for(int i = 0; i <= listPosition; i++) {
            if(isSection(i)) {
                start = i;
            }
        }

        return start;
    }


    public int getSectionForPosition(int listPosition) {
        boolean isSection = false;
        int numPrecedingSections = 0;
        for (int i = 0; i < mSectionMap.size(); i++) {
            int sectionPosition = mSectionMap.keyAt(i);

            if (listPosition > sectionPosition) {
                numPrecedingSections++;
            } else if (listPosition == sectionPosition) {
                isSection = true;
            } else {
                break;
            }
        }

        return isSection ? numPrecedingSections : Math.max(numPrecedingSections - 1, 0);
    }

    private boolean isListPositionBeforeFirstSection(int listPosition, int sectionIndex) {
        boolean hasSections = mSectionMap != null && mSectionMap.size() > 0;
        return sectionIndex == 0 && hasSections && listPosition < mSectionMap.keyAt(0);
    }

    @Override
    public final int getItemViewType(int listPosition) {
        int sectionIndex = mSectionMap.indexOfKey(listPosition);
        if(sectionIndex < 0) {
            int cursorPosition = getCursorPositionWithoutSections(listPosition);
            return (getSectionItemViewType(cursorPosition) << 16) | VIEW_TYPE_ITEM;
        } else {
            return (getSectionHeaderViewType(sectionIndex) << 16) | VIEW_TYPE_SECTION;
        }
    }

    protected short getSectionHeaderViewType(int sectionIndex) {
        return 0;
    }

    protected short getSectionItemViewType(int position) {
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LayoutManager.LayoutParams layoutParams = LayoutManager.LayoutParams
                .from(holder.itemView.getLayoutParams());

        // assign first position of section to each item
        layoutParams.setFirstPosition(getFirstSectionPosition(position));

        int viewType = holder.getItemViewType() & 0xFF;
        switch (viewType) {
            case VIEW_TYPE_ITEM :
                moveCursorOrThrow(getCursorPositionWithoutSections(position));
                onBindItemViewHolder((VH) holder, getCursor());

                layoutParams.isHeader = false;
                break;

            case VIEW_TYPE_SECTION:
                T section = mSectionMap.get(position);
                onBindSectionViewHolder((SH) holder, section);

                layoutParams.isHeader = true;
                break;
        }

        holder.itemView.setLayoutParams(layoutParams);
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType & 0xFF) {
            case VIEW_TYPE_SECTION:
                return onCreateSectionViewHolder(parent, viewType >> 16);

            case VIEW_TYPE_ITEM:
                return onCreateItemViewHolder(parent, viewType >> 16);

            default:
                return null;
        }
    }

    protected abstract SH onCreateSectionViewHolder(ViewGroup parent, int viewType);
    protected abstract VH onCreateItemViewHolder(ViewGroup parent, int viewType);

    protected abstract void onBindSectionViewHolder(SH holder, T section);
    protected abstract void onBindItemViewHolder(VH holder, C cursor);

    public interface Comparator<T> {
        boolean equal(T obj1, T obj2);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        /**
         * Returns the view type assigned in
         * {@link SectionCursorAdapter#getSectionHeaderViewType(int)} or
         * {@link SectionCursorAdapter#getSectionItemViewType(int)}
         *
         * Note that a call to {@link #getItemViewType()} will return a value that contains
         * internal stuff necessary to distinguish sections from items.
         * @return The view type you set.
         */
        public short getItemViewTypeWithoutSections(){
            return (short) (getItemViewType() >> 16);
        }
    }
}

