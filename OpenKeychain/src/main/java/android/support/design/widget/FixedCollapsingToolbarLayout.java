package android.support.design.widget;

import android.content.Context;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;

public class FixedCollapsingToolbarLayout extends CollapsingToolbarLayout {

    public FixedCollapsingToolbarLayout(Context context) {
        this(context, null);
    }

    public FixedCollapsingToolbarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // During the super call to onMeasure(), we'll save a copy of mLastInsets,
        // consume the insets of mLastInsets so the super call has no insets to work with,
        // then re-assign mLastInsets to what it was before the super call.
        WindowInsetsCompat oldInsets = mLastInsets;
        if (mLastInsets != null) {
            mLastInsets = mLastInsets.consumeSystemWindowInsets();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLastInsets = oldInsets;
    }

}