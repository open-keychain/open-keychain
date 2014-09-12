package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListAwareSwipeRefreshLayout extends SwipeRefreshLayout {

    /**
     * A StickyListHeadersListView whose parent view is this SwipeRefreshLayout
     */
    private StickyListHeadersListView mStickyListHeadersListView;

    public ListAwareSwipeRefreshLayout(Context context) {
        super(context);
    }

    public ListAwareSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setStickyListHeadersListView(StickyListHeadersListView stickyListHeadersListView) {
        mStickyListHeadersListView = stickyListHeadersListView;
    }

    @Override
    public boolean canChildScrollUp() {
        if (mStickyListHeadersListView != null) {
            // In order to scroll a StickyListHeadersListView up:
            // Firstly, the wrapped ListView must have at least one item
            return (mStickyListHeadersListView.getListChildCount() > 0) &&
                    // And then, the first visible item must not be the first item
                    ((mStickyListHeadersListView.getFirstVisiblePosition() > 0) ||
                            // If the first visible item is the first item,
                            // (we've reached the first item)
                            // make sure that its top must not cross over the padding top of the wrapped ListView
                            (mStickyListHeadersListView.getListChildAt(0).getTop() < 0));

            // If the wrapped ListView is empty or,
            // the first item is located below the padding top of the wrapped ListView,
            // we can allow performing refreshing now
        } else {
            // Fall back to default implementation
            return super.canChildScrollUp();
        }
    }
}