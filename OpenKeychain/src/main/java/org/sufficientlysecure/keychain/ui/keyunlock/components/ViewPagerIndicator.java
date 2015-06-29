package org.sufficientlysecure.keychain.ui.keyunlock.components;

import android.content.Context;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sufficientlysecure.keychain.R;

import java.util.ArrayList;

/**
 * Custom page indicator
 */
public class ViewPagerIndicator extends LinearLayout implements ViewPager.OnPageChangeListener {
    public static final String TAG = "ViewPagerIndicator";
    public static final int defaultPageColor = Color.parseColor("#d8d8d8");

    private ViewPager mViewPager;
    private ArrayList<View> mViewPages;
    private Context mContext;
    private int mCurrentPage = 0;

    public ViewPagerIndicator(Context context, ViewPager viewPager) {
        super(context);
        mContext = context;
        initViewPagerIndicator(viewPager);
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Initializes the view pager indicator,
     * useful if this component is added directly to the xml.
     *
     * @param viewPager
     */
    public void initViewPagerIndicator(ViewPager viewPager) {
        this.setOrientation(HORIZONTAL);
        if (getLayoutParams() == null) {
            android.widget.LinearLayout.LayoutParams params = new
                    android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            setLayoutParams(params);
        }

        mViewPager = viewPager;
        mViewPages = new ArrayList<>(viewPager.getAdapter().getCount());
        mViewPager.setOnPageChangeListener(this);
        initPages();
    }

    /**
     * Initialize the indicator views.
     */
    private void initPages() {
        int count = mViewPager.getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            View view = new View(mContext);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPixels(20),
                    dpToPixels(20));
            params.setMargins(dpToPixels(5), 0, dpToPixels(5), 0);
            view.setLayoutParams(params);
            view.setDrawingCacheEnabled(true);
            view.setBackgroundResource(R.drawable.circle_shape);
            mViewPages.add(view);
            addView(view);
        }

        //initial page
        mCurrentPage = mViewPager.getCurrentItem();
        changeToPage(mCurrentPage);
    }

    /**
     * Converts dp metrics to pixel metrics.
     *
     * @param dp
     * @return
     */
    private int dpToPixels(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Updates the current page.
     *
     * @param newPosition
     */
    private void changeToPage(int newPosition) {
        //reset
        int size = mViewPages.size();
        for (int i = 0; i < size; i++) {
            if (i == newPosition) {
                GradientDrawable shapeDrawable = (GradientDrawable) mViewPages.get(newPosition).
                        getBackground();
                shapeDrawable.setColor(mContext.getResources().getColor(R.color.primary));
            } else {
                GradientDrawable oldShapeDrawable = (GradientDrawable) mViewPages.get(i).
                        getBackground();
                oldShapeDrawable.setColor(defaultPageColor);
            }
        }

        mCurrentPage = newPosition;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    /**
     * Updates the view with the current selected page.
     *
     * @param position
     */
    @Override
    public void onPageSelected(int position) {
        changeToPage(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
