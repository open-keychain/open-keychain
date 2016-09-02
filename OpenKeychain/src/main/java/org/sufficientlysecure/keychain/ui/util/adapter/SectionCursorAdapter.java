package org.sufficientlysecure.keychain.ui.util.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import org.sufficientlysecure.keychain.util.Log;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;
/**
 * @param <T> section type.
 * @param <VH> the view holder extending {@code BaseViewHolder<Cursor>} that is bound to the cursor data.
 * @param <SH> the view holder extending {@code BaseViewHolder<<T>>} that is bound to the section data.
 */
public abstract class SectionCursorAdapter<T, VH extends RecyclerView.ViewHolder, SH extends RecyclerView.ViewHolder>
        extends CursorAdapter implements SectionIndexer {

    public static final String TAG = "SectionCursorAdapter";

    private static final int VIEW_TYPE_ITEM = 0x0;
    private static final int VIEW_TYPE_SECTION = 0x1;

    private SparseArrayCompat<T> mSectionMap = new SparseArrayCompat<>();
    private ArrayList<Integer> mSectionIndexList = new ArrayList<>();
    private Comparator<T> mSectionComparator;
    private Object[] mFastScrollItems;

    public SectionCursorAdapter(Context context, Cursor cursor, int flags) {
        this(context, cursor, flags, new Comparator<T>() {
            @Override
            public boolean equal(T obj1, T obj2) {
                return (obj1 == null) ?
                        obj2 == null : obj1.equals(obj2);
            }
        });
    }

    public SectionCursorAdapter(Context context, Cursor cursor, int flags, Comparator<T> comparator) {
        super(context, cursor, flags);
        setSectionComparator(comparator);
    }

    @Override
    public void onContentChanged() {
        if (hasValidData()) {
            buildSections();
        } else {
            mSectionMap.clear();
            mSectionIndexList.clear();
            mFastScrollItems = null;
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
                mSectionIndexList.clear();
                mFastScrollItems = null;

                appendSections(getCursor());
            } catch (IllegalStateException e) {
                Log.e(TAG, "Couldn't build sections. Perhaps you're moving the cursor" +
                        "in #getSectionFromCursor(Cursor)?", e);
                swapCursor(null);

                mSectionMap.clear();
                mSectionIndexList.clear();
                mFastScrollItems = null;
            }
        }
    }

    protected void appendSections(Cursor cursor) throws IllegalStateException {
        int cursorPosition = 0;
        while(hasValidData() && cursor.moveToNext()) {
            T section = getSectionFromCursor(cursor);
            if (cursor.getPosition() != cursorPosition)
                throw new IllegalStateException("Do not move the cursor's position in getSectionFromCursor.");
            if (!hasSection(section))
                mSectionMap.append(cursorPosition + mSectionMap.size(), section);
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
    protected abstract T getSectionFromCursor(Cursor cursor) throws IllegalStateException;
    protected String getTitleFromSection(T section) {
        return section != null ? section.toString() : "";
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + mSectionMap.size();
    }

    @Override
    public final long getItemId(int listPosition) {
        if (isSection(listPosition))
            return listPosition;
        else {
            int cursorPosition = getCursorPositionWithoutSections(listPosition);
            return super.getItemId(cursorPosition);
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

    /**
     * Get the section object for the index within the array of sections.
     * @param sectionPosition The section index.
     * @return The specified section object for this position.
     */
    public T getSection(int sectionPosition) {
        if (mSectionIndexList.contains(sectionPosition)) {
            return mSectionMap.get(mSectionIndexList.get(sectionPosition));
        }

        return null;
    }

    /**
     * Returns all indices at which the first item of a section is placed.
     * @return The first index of each section.
     */
    public List<Integer> getSectionListPositions() {
        return mSectionIndexList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionIndexList.isEmpty()) {
            for (int i = 0; i < mSectionMap.size(); i++) {
                mSectionIndexList.add(mSectionMap.keyAt(i));
            }
        }

        return sectionIndex < mSectionIndexList.size() ?
                mSectionIndexList.get(sectionIndex) : getItemCount();
    }

    /**
     * Given the position of a section, returns its index in the array of sections.
     * @param sectionPosition The first position of the corresponding section in the array of items.
     * @return The section index in the array of sections.
     */
    public int getSectionIndexForPosition(int sectionPosition) {
        if (mSectionIndexList.isEmpty()) {
            for (int i = 0; i < mSectionMap.size(); i++) {
                mSectionIndexList.add(mSectionMap.keyAt(i));
            }
        }

        return mSectionIndexList.indexOf(sectionPosition);
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

    /**
     * {@inheritDoc}
     */
    @Override
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

    @Override
    public Object[] getSections() {
        if(mFastScrollItems == null) {
            mFastScrollItems = getSectionLabels();
        }

        return mFastScrollItems;
    }

    private Object[] getSectionLabels() {
        if(mSectionMap == null)
            return new Object[0];

        String[] ret = new String[mSectionMap.size()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = getTitleFromSection(mSectionMap.valueAt(i));
        }

        return ret;
    }

    private boolean isListPositionBeforeFirstSection(int listPosition, int sectionIndex) {
        boolean hasSections = mSectionMap != null && mSectionMap.size() > 0;
        return sectionIndex == 0 && hasSections && listPosition < mSectionMap.keyAt(0);
    }

    @Override
    public final int getItemViewType(int listPosition) {
        return isSection(listPosition) ? VIEW_TYPE_SECTION : VIEW_TYPE_ITEM;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LayoutManager.LayoutParams layoutParams = LayoutManager.LayoutParams
                .from(holder.itemView.getLayoutParams());

        // assign first position of section to each item
        layoutParams.setFirstPosition(getFirstSectionPosition(position));

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_ITEM :
                moveCursorOrThrow(getCursorPositionWithoutSections(position));
                onBindItemViewHolder((VH) holder, getCursor());

                layoutParams.isHeader = false;
                break;

            case VIEW_TYPE_SECTION:
                T section = mSectionMap.get(position);
                int sectionIndex = getSectionIndexForPosition(position);
                onBindSectionViewHolder((SH) holder, sectionIndex, section);

                layoutParams.isHeader = true;
                break;
        }

        holder.itemView.setLayoutParams(layoutParams);
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SECTION:
                return onCreateSectionViewHolder(parent);

            case VIEW_TYPE_ITEM:
                return onCreateItemViewHolder(parent, viewType);

            default:
                return null;
        }
    }

    protected abstract SH onCreateSectionViewHolder(ViewGroup parent);
    protected abstract VH onCreateItemViewHolder(ViewGroup parent, int viewType);

    protected abstract void onBindSectionViewHolder(SH holder, int sectionIndex, T section);
    protected abstract void onBindItemViewHolder(VH holder, Cursor cursor);

    public interface Comparator<T> {
        boolean equal(T obj1, T obj2);
    }
}

