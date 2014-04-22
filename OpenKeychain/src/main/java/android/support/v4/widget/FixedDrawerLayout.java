package android.support.v4.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

/**
 * Fix for NullPointerException at android.support.v4.widget.DrawerLayout.isContentView(DrawerLayout.java:840)
 * <p/>
 * http://stackoverflow.com/a/18107942
 */
public class FixedDrawerLayout extends DrawerLayout {
    public FixedDrawerLayout(Context context) {
        super(context);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    boolean isContentView(View child) {
        if (child == null) {
            return false;
        }
        return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
    }
}
