/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;


import android.database.Cursor;
import android.database.CursorWrapper;

public abstract class FilterCursorWrapper extends CursorWrapper {
    private int[] mIndex;
    private int mCount = 0;
    private int mPos = 0;

    public abstract boolean isVisible(Cursor cursor);

    public FilterCursorWrapper(Cursor cursor) {
        super(cursor);
        mCount = super.getCount();
        mIndex = new int[mCount];
        for (int i = 0; i < mCount; i++) {
            super.moveToPosition(i);
            if (isVisible(cursor)) {
                mIndex[mPos++] = i;
            }
        }
        mCount = mPos;
        mPos = 0;
        super.moveToFirst();
    }

    @Override
    public boolean move(int offset) {
        return this.moveToPosition(mPos + offset);
    }

    @Override
    public boolean moveToNext() {
        return this.moveToPosition(mPos + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return this.moveToPosition(mPos - 1);
    }

    @Override
    public boolean moveToFirst() {
        return this.moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return this.moveToPosition(mCount - 1);
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position >= mCount || position < 0) {
            return false;
        }
        return super.moveToPosition(mIndex[position]);
    }

    @Override
    public int getCount() {
        return mCount;
    }

    public int getHiddenCount() {
        return super.getCount() - mCount;
    }

    @Override
    public int getPosition() {
        return mPos;
    }

}