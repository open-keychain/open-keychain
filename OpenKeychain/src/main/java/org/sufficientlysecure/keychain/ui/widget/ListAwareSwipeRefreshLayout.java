/*
 * Copyright (C) 2014 Daniel Albert
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
import android.support.v4.widget.NoScrollableSwipeRefreshLayout;
import android.util.AttributeSet;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListAwareSwipeRefreshLayout extends NoScrollableSwipeRefreshLayout {

    private StickyListHeadersListView mStickyListHeadersListView = null;
    private boolean mIsLocked = false;

    /**
     * Constructors
     */
    public ListAwareSwipeRefreshLayout(Context context) {
        super(context);
    }

    public ListAwareSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Getters / Setters
     */
    public void setStickyListHeadersListView(StickyListHeadersListView stickyListHeadersListView) {
        mStickyListHeadersListView = stickyListHeadersListView;
    }

    public StickyListHeadersListView getStickyListHeadersListView() {
        return mStickyListHeadersListView;
    }

    public void setIsLocked(boolean locked) {
        mIsLocked = locked;
    }

    public boolean getIsLocked() {
        return mIsLocked;
    }

    @Override
    public boolean canChildScrollUp() {
        if (mStickyListHeadersListView == null) {
            return super.canChildScrollUp();
        }

        return (mIsLocked || (
                mStickyListHeadersListView.getWrappedList().getChildCount() > 0
                        && (mStickyListHeadersListView.getWrappedList().getChildAt(0).getTop() < 0
                        || mStickyListHeadersListView.getFirstVisiblePosition() > 0)
        )
        );
    }
}