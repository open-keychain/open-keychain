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
import android.view.MotionEvent;

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

    /** Called on a touch event, this method exempts a small area in the upper right from pull to
     * refresh handling.
     *
     * If the touch event happens somewhere in the upper right corner of the screen, we return false
     * to indicate that the event was not handled. This ensures events in that area are always
     * handed through to the list scrollbar handle. For all other cases, we pass the message through
     * to the pull to refresh handler.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // The device may be null. This actually happens
        if (event.getDevice() != null) {
            // MotionEvent.AXIS_X is api level 12, for some reason, so we use a constant 0 here
            float ratioX = event.getX() / event.getDevice().getMotionRange(0).getMax();
            float ratioY = event.getY() / event.getDevice().getMotionRange(1).getMax();
            // if this is the upper right corner, don't handle as pull to refresh event
            if (ratioX > 0.85f && ratioY < 0.15f) {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }

}