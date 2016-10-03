package org.sufficientlysecure.keychain.ui.util.recyclerview.fastscroll;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.tonicartos.superslim.LayoutManager;

public class FastScrollRecyclerView extends RecyclerView implements RecyclerView.OnItemTouchListener {
    private FastScroller mFastScroller;

    private int mLastX;
    private int mLastY;

    public FastScrollRecyclerView(Context context) {
        this(context, null);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        mFastScroller = new FastScroller(this, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mFastScroller.draw(canvas);
    }

    @Override
    public void onScrolled(int x, int y) {
        super.onScrolled(x, y);
        mFastScroller.updateThumb(
                computeVerticalScrollOffset(),
                computeVerticalScrollExtent());
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mFastScroller.updateContainer(top, right, bottom);
        mFastScroller.updateThumb(
                computeVerticalScrollOffset(),
                computeVerticalScrollExtent());
    }

    @Override
    public void onScrollStateChanged(int state) {
        switch (state) {
            case SCROLL_STATE_IDLE:
                mFastScroller.hideBar();
                break;
            case SCROLL_STATE_DRAGGING:
                mFastScroller.showBar();
                break;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if(mFastScroller.onInterceptTouchEvent(e)) {
            onTouchEvent(rv, e);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        mFastScroller.handleTouchEvent(event.getAction(), x, y, mLastX, mLastY);
        mLastX = x;
        mLastY = y;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private int getItemCount() {
        return getAdapter() != null ?
                getAdapter().getItemCount() : 0;
    }

    public void scrollToFraction(float fraction) {
        int count = getItemCount();
        if (count > 0) {
            stopScroll();
            scrollToPosition((int) ((count - 1) * fraction));
        }
    }

    public void scrollByFraction(float fraction) {
        int count = getItemCount();
        if (count > 0) {
            stopScroll();

            int pixelsToScroll = (int) (computeVerticalScrollRange() * fraction);
            scrollBy(0, pixelsToScroll);
        }
    }
}
