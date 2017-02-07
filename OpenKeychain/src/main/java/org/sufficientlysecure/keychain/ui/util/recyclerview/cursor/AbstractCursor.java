package org.sufficientlysecure.keychain.ui.util.recyclerview.cursor;

import android.database.Cursor;
import android.database.CursorWrapper;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Created by daquexian on 17-2-7.
 */

public abstract class AbstractCursor extends CursorWrapper {
    public static final String[] PROJECTION = {"_id"};

    public static <T extends AbstractCursor> T wrap(Cursor cursor, Class<T> type) {
        if (cursor != null) {
            try {
                Constructor<T> constructor = type.getConstructor(Cursor.class);
                return constructor.newInstance(cursor);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Could not create instance of cursor wrapper!", e);
            }
        }

        return null;
    }

    private HashMap<String, Integer> mColumnIndices;

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    protected AbstractCursor(Cursor cursor) {
        super(cursor);
        mColumnIndices = new HashMap<>(cursor.getColumnCount() * 4 / 3, 0.75f);
    }

    @Override
    public void close() {
        mColumnIndices.clear();
        super.close();
    }

    public final int getEntryId() {
        int index = getColumnIndexOrThrow("_id");
        return getInt(index);
    }

    @Override
    public final int getColumnIndexOrThrow(String colName) {
        Integer colIndex = mColumnIndices.get(colName);
        if (colIndex == null) {
            colIndex = super.getColumnIndexOrThrow(colName);
            mColumnIndices.put(colName, colIndex);
        } else if (colIndex < 0) {
            throw new IllegalArgumentException("Could not get column index for name: \"" + colName + "\"");
        }

        return colIndex;
    }

    @Override
    public final int getColumnIndex(String colName) {
        Integer colIndex = mColumnIndices.get(colName);
        if (colIndex == null) {
            colIndex = super.getColumnIndex(colName);
            mColumnIndices.put(colName, colIndex);
        }

        return colIndex;
    }
}
