package org.sufficientlysecure.keychain.ui.util.adapter;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;

import org.sufficientlysecure.keychain.util.Log;

public abstract class CursorAdapter extends RecyclerView.Adapter {

    public static final String TAG = "CursorAdapter";

    private Cursor mCursor;
    private Context mContext;
    private boolean mDataValid;

    private ChangeObserver mChangeObserver;
    private DataSetObserver mDataSetObserver;

    /**
     * If set the adapter will register a content observer on the cursor and will call
     * {@link #onContentChanged()} when a notification comes in.  Be careful when
     * using this flag: you will need to unset the current Cursor from the adapter
     * to avoid leaks due to its registered observers.  This flag is not needed
     * when using a CursorAdapter with a
     * {@link android.content.CursorLoader}.
     */
    public static final int FLAG_REGISTER_CONTENT_OBSERVER = 0x02;

    /**
     * Constructor that allows control over auto-requery.  It is recommended
     * you not use this, but instead {@link #CursorAdapter(Context, Cursor, int)}.
     * When using this constructor, {@link #FLAG_REGISTER_CONTENT_OBSERVER}
     * will always be set.
     *
     * @param c The cursor from which to get the data.
     * @param context The context
     */
    public CursorAdapter(Context context, Cursor c) {
        init(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
    }

    /**
     * Recommended constructor.
     *
     * @param c The cursor from which to get the data.
     * @param context The context
     * @param flags Flags used to determine the behavior of the adapter
     * @see #FLAG_REGISTER_CONTENT_OBSERVER
     */
    public CursorAdapter(Context context, Cursor c, int flags) {
        init(context, c, flags);
    }

    private void init(Context context, Cursor c, int flags) {
        boolean cursorPresent = c != null;
        mCursor = c;
        mDataValid = cursorPresent;
        mContext = context;
        if ((flags & FLAG_REGISTER_CONTENT_OBSERVER) == FLAG_REGISTER_CONTENT_OBSERVER) {
            mChangeObserver = new ChangeObserver();
            mDataSetObserver = new MyDataSetObserver();
        } else {
            mChangeObserver = null;
            mDataSetObserver = null;
        }

        if (cursorPresent) {
            if (mChangeObserver != null) c.registerContentObserver(mChangeObserver);
            if (mDataSetObserver != null) c.registerDataSetObserver(mDataSetObserver);
        }

        setHasStableIds(true);
    }

    /**
     * Returns the cursor.
     * @return the cursor.
     */
    public Cursor getCursor() {
        return mCursor;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * @see android.support.v7.widget.RecyclerView.Adapter#getItemCount()
     */
    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    public boolean hasValidData() {
        mDataValid = hasOpenCursor();
        return mDataValid;
    }

    private boolean hasOpenCursor() {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.isClosed()) {
            swapCursor(null);
            return false;
        }

        return cursor != null;
    }

    /**
     * @see android.support.v7.widget.RecyclerView.Adapter#getItemId(int)
     *
     * @param position Adapter position to query
     * @return the id of the item
     */
    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null) {
            if (moveCursor(position)) {
                return getIdFromCursor(mCursor);
            } else {
                return RecyclerView.NO_ID;
            }
        } else {
            return RecyclerView.NO_ID;
        }
    }

    /**
     * Return the id of the item represented by the row the cursor
     * is currently moved to.
     * @param cursor The cursor moved to the correct position.
     * @return The id of the dataset
     */
    public long getIdFromCursor(Cursor cursor) {
        if(cursor != null) {
            return cursor.getPosition();
        } else {
            return RecyclerView.NO_ID;
        }
    }

    public void moveCursorOrThrow(int position)
            throws IndexOutOfBoundsException, IllegalStateException {

        if(position >= getItemCount() || position < -1) {
            throw new IndexOutOfBoundsException("Position: " + position
                    + " is invalid for this data set!");
        }

        if(!mDataValid) {
            throw new IllegalStateException("Attempt to move cursor over invalid data set!");
        }

        if(!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Couldn't move cursor from position: "
                    + mCursor.getPosition() + " to position: " + position + "!");
        }
    }

    public boolean moveCursor(int position) {
        if(position >= getItemCount() || position < -1) {
            Log.w(TAG, "Position: %d is invalid for this data set!");
            return false;
        }

        if(!mDataValid) {
            Log.d(TAG, "Attempt to move cursor over invalid data set!");
        }

        return mCursor.moveToPosition(position);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }

        Cursor oldCursor = mCursor;
        if (oldCursor != null) {
            if (mChangeObserver != null) oldCursor.unregisterContentObserver(mChangeObserver);
            if (mDataSetObserver != null) oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }

        mCursor = newCursor;
        if (newCursor != null) {
            if (mChangeObserver != null) newCursor.registerContentObserver(mChangeObserver);
            if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);
            mDataValid = true;
            // notify the observers about the new cursor
            onContentChanged();
        } else {
            mDataValid = false;
            // notify the observers about the lack of a data set
            onContentChanged();
        }

        return oldCursor;
    }

    /**
     * <p>Converts the cursor into a CharSequence. Subclasses should override this
     * method to convert their results. The default implementation returns an
     * empty String for null values or the default String representation of
     * the value.</p>
     *
     * @param cursor the cursor to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    public CharSequence convertToString(Cursor cursor) {
        return cursor == null ? "" : cursor.toString();
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     *
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        notifyDataSetChanged();
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataValid = true;
            onContentChanged();
        }

        @Override
        public void onInvalidated() {
            mDataValid = false;
            onContentChanged();
        }
    }
}