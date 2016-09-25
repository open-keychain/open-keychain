package org.sufficientlysecure.keychain.ui.util.recyclerview.fastscroll;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;

public class FastScroller {
    private static final int DEFAULT_AUTO_HIDE_DELAY = 1500;
    private static final boolean DEFAULT_AUTO_HIDE_ENABLED = true;

    private Paint mTrack;
    private Paint mThumb;

    private int mPosX;
    private int mPosY;
    private int mWidth;
    private int mHeight;

    private Rect mContainer;
    private boolean mIsDragging;

    private int mAutoHideDelay;
    private boolean mAutoHideEnabled;

    private final int mTrackColorNormal;
    private final int mTrackColorDragging;

    private final int mThumbColorNormal;
    private final int mThumbColorDragging;

    private ValueAnimator mBarAnimator;
    private final FastScrollRecyclerView mRecyclerView;

    private final int mTouchSlop;
    private final int mTouchInset;
    private final int[] mKeyFrames;

    //private final Runnable mHideRunnable;

    public FastScroller(final FastScrollRecyclerView recyclerView, AttributeSet attributeSet) {
        Context context = recyclerView.getContext();
        mRecyclerView = recyclerView;

        mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThumb = new Paint(Paint.ANTI_ALIAS_FLAG);

        mKeyFrames = new int[2];
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchInset = FormattingUtils.dpToPx(context, 8);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attributeSet, R.styleable.FastScroller, 0, 0);

        try {
            mWidth = typedArray.getDimensionPixelSize(
                    R.styleable.FastScroller_fastScrollScrollBarWidth, FormattingUtils.dpToPx(context, 6));

            mAutoHideDelay = typedArray.getInteger(
                    R.styleable.FastScroller_fastScrollAutoHideDelay, DEFAULT_AUTO_HIDE_DELAY);
            mAutoHideEnabled = typedArray.getBoolean(
                    R.styleable.FastScroller_fastScrollAutoHideEnabled, DEFAULT_AUTO_HIDE_ENABLED);

            mTrackColorNormal = typedArray.getColor(
                    R.styleable.FastScroller_fastScrollTrackColorNormal, Color.LTGRAY);
            mTrackColorDragging = typedArray.getColor(
                    R.styleable.FastScroller_fastScrollTrackColorDragging, Color.GRAY);

            mThumbColorNormal = typedArray.getColor(
                    R.styleable.FastScroller_fastScrollThumbColorNormal, Color.GRAY);
            mThumbColorDragging = typedArray.getColor(
                    R.styleable.FastScroller_fastScrollThumbColorDragging, Color.BLUE);

            mTrack.setColor(mTrackColorNormal);
            mThumb.setColor(mThumbColorNormal);
        } finally {
            typedArray.recycle();
        }
    }

    public void hideBar() {
        if(mPosX >= mWidth || !mAutoHideEnabled) {
            return;
        }

        mKeyFrames[0] = mPosX;
        mKeyFrames[1] = mWidth;

        prepareAnimator();
        mBarAnimator.setIntValues(mKeyFrames);
        mBarAnimator.setStartDelay(mAutoHideDelay);
        mBarAnimator.setInterpolator(new FastOutLinearInInterpolator());
        mBarAnimator.start();
    }

    public void showBar() {
        if(mPosX < 1) {
            return;
        }

        mKeyFrames[0] = mPosX;
        mKeyFrames[1] = 0;

        prepareAnimator();
        mBarAnimator.setStartDelay(0);
        mBarAnimator.setIntValues(mKeyFrames);
        mBarAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mBarAnimator.start();
    }

    public boolean isAnimatingShow() {
        return mKeyFrames[1] == 0 && mBarAnimator.isStarted();
    }

    public boolean isAnimatingHide() {
        return mKeyFrames[1] == mWidth && mBarAnimator.isStarted();
    }

    public boolean isBarFullyShown() {
        return mPosX < 1;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mContainer != null
                && (event.getX() < mContainer.right)
                && (event.getX() > mContainer.left - mTouchInset)
                && (event.getY() < mContainer.bottom)
                && (event.getY() > mContainer.top);
    }

    private boolean isInThumb(int x, int y) {
        return x > (mContainer.left + mPosX - mTouchInset)
                && x < (mContainer.right + mTouchInset)
                && y > (mContainer.top + mPosY - mTouchInset)
                && y < (mContainer.top + mPosY + mHeight + mTouchInset);
    }

    public void draw(Canvas canvas) {
        if(mPosX < 0 || mPosX >= mWidth) {
            return;
        }

        if(!mRecyclerView.canScrollVertically(-1)
                && ! mRecyclerView.canScrollVertically(1)) {
            return;
        }

        int topBound = mContainer.top + mPosY;
        int leftBound = mContainer.left + mPosX;

        canvas.drawRect(leftBound, mContainer.top, mContainer.right, mContainer.bottom, mTrack);
        canvas.drawRect(leftBound, topBound, mContainer.right, topBound + mHeight, mThumb);
    }

    public void updateThumb(int offset, int extent) {
        mPosY = offset;
        mHeight = extent;
    }

    public void updateContainer(int top, int right, int bottom) {
        mPosX = 0;
        mContainer = new Rect(right - mWidth, top, right, bottom);

        invalidate();
    }

    private void invalidate() {
        mRecyclerView.invalidate(mContainer);
    }

    private void prepareAnimator() {
        if (mBarAnimator != null) {
            mBarAnimator.cancel();
        } else {
            mBarAnimator = new ValueAnimator();
            mBarAnimator.setDuration(150);
            mBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mPosX = (Integer) animation.getAnimatedValue();

                    invalidate();
                }
            });
        }
    }

    public void handleTouchEvent(int action, int x, int y, int lastX, int lastY) {
        if(!mRecyclerView.canScrollVertically(-1)
                && ! mRecyclerView.canScrollVertically(1)) {
            return;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if(isBarFullyShown()) { //
                    prepareAnimator(); // cancel any pending animations

                    mTrack.setColor(mTrackColorDragging);
                    mThumb.setColor(mThumbColorDragging);

                    if(!isInThumb(x, y)) {
                        // jump to point
                        mPosY = Math.min(
                                Math.max(mContainer.top, y - (mHeight / 2)),
                                mContainer.bottom - mHeight
                        );

                        float range = (mContainer.bottom - mContainer.top) - mHeight;
                        mRecyclerView.scrollToFraction(mPosY / range);
                    } else {
                        invalidate();
                    }
                } else {
                    if(!isAnimatingShow()) {
                        showBar();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if((!mIsDragging
                        && isInThumb(x, y)
                        && Math.abs(y - lastY) > mTouchSlop)
                        || mIsDragging) {

                    if(!mIsDragging) {
                        mIsDragging = true;
                    }

                    float dist = y - lastY;
                    float range = (mContainer.bottom - mContainer.top) - mHeight;

                    if(mRecyclerView.canScrollVertically(dist < 0 ? -1 : 1)) {
                        mRecyclerView.scrollByFraction(dist / range);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(mIsDragging) {
                    mIsDragging = false;
                }

                mTrack.setColor(mTrackColorNormal);
                mThumb.setColor(mThumbColorNormal);

                if(!mBarAnimator.isRunning()
                        && mRecyclerView.getScrollState()
                        == RecyclerView.SCROLL_STATE_IDLE) {
                    hideBar();
                    invalidate();
                }
                break;
        }
    }
}
