/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * https://gist.github.com/yrom/3b4bcbc2370ca2290434
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;
    private int spanCount;
    private int lastItemInFirstLane = -1;

    public SpacesItemDecoration(int space) {
        this(space, 1);
    }

    /**
     * @param space
     * @param spanCount spans count of one lane
     */
    public SpacesItemDecoration(int space, int spanCount) {
        this.space = space;
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        final int position = params.getViewPosition();
        final int spanSize;
        final int index;
        if (params instanceof GridLayoutManager.LayoutParams) {
            GridLayoutManager.LayoutParams gridParams = (GridLayoutManager.LayoutParams) params;
            spanSize = gridParams.getSpanSize();
            index = gridParams.getSpanIndex();
        } else {
            spanSize = 1;
            index = position % spanCount;
        }
        // invalid value
        if (spanSize < 1 || index < 0) return;

        if (spanSize == spanCount) { // full span
            outRect.left = space;
            outRect.right = space;
        } else {
            if (index == 0) { // left one
                outRect.left = space;
            }
            // spanCount >= 1
            if (index == spanCount - 1) { // right one
                outRect.right = space;
            }
            if (outRect.left == 0) {
                outRect.left = space / 2;
            }
            if (outRect.right == 0) {
                outRect.right = space / 2;
            }
        }
        // set top to all in first lane
        if (position < spanCount && spanSize <= spanCount) {
            if (lastItemInFirstLane < 0) { // lay out at first time
                lastItemInFirstLane = position + spanSize == spanCount ? position : lastItemInFirstLane;
                outRect.top = space;
            } else if (position <= lastItemInFirstLane) { // scroll to first lane again
                outRect.top = space;
            }
        }
        outRect.bottom = space;

    }
} 