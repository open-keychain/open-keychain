package org.sufficientlysecure.keychain.ui;


import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class LockableViewPager extends ViewPager {
    private boolean locked;

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (locked) {
            return false;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (locked) {
            return false;
        }

        return super.onInterceptTouchEvent(event);
    }

    public void setPagingLocked(boolean locked) {
        this.locked = locked;
    }
}