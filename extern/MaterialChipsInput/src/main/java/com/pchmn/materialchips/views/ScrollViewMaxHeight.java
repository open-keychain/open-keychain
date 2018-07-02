package com.pchmn.materialchips.views;


import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;

import com.pchmn.materialchips.R;
import com.pchmn.materialchips.util.ViewUtil;

public class ScrollViewMaxHeight extends NestedScrollView {

    private int mMaxHeight;
    private int mWidthMeasureSpec;

    public ScrollViewMaxHeight(Context context) {
        super(context);
    }

    public ScrollViewMaxHeight(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ScrollViewMaxHeight,
                0, 0);

        try {
            mMaxHeight = a.getDimensionPixelSize(R.styleable.ScrollViewMaxHeight_maxHeight, ViewUtil.dpToPx(300));
        }
        finally {
            a.recycle();
        }
    }

    public void setMaxHeight(int height) {
        mMaxHeight = height;
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        measure(mWidthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidthMeasureSpec = widthMeasureSpec;
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
