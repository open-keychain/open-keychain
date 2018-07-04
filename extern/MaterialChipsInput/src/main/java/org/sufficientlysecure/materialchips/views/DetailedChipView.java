package org.sufficientlysecure.materialchips.views;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.materialchips.R;
import org.sufficientlysecure.materialchips.model.ChipInterface;
import org.sufficientlysecure.materialchips.util.ColorUtil;
import org.sufficientlysecure.materialchips.util.LetterTileProvider;


public class DetailedChipView extends LinearLayout {

    private static final String TAG = DetailedChipView.class.toString();
    // context
    private Context mContext;
    // xml elements
    private LinearLayout mContentLayout;
    private TextView mNameTextView;
    private TextView mInfoTextView;
    private ImageButton mDeleteButton;
    // letter tile provider
    private static LetterTileProvider mLetterTileProvider;
    // attributes
    private ColorStateList mBackgroundColor;

    public DetailedChipView(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public DetailedChipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    /**
     * Inflate the view according to attributes
     *
     * @param attrs the attributes
     */
    private void init(AttributeSet attrs) {
        // inflate layout
        View rootView = inflate(getContext(), R.layout.detailed_chip_view, this);

        mContentLayout = (LinearLayout) rootView.findViewById(R.id.content);
        mNameTextView = (TextView) rootView.findViewById(R.id.name);
        mInfoTextView = (TextView) rootView.findViewById(R.id.info);
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_button);

        // letter tile provider
        mLetterTileProvider = new LetterTileProvider(mContext);

        // hide on first
        setVisibility(GONE);
        // hide on touch outside
        hideOnTouchOutside();
    }

    /**
     * Hide the view on touch outside of it
     */
    private void hideOnTouchOutside() {
        // set focusable
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    /**
     * Fade in
     */
    public void fadeIn() {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(200);
        startAnimation(anim);
        setVisibility(VISIBLE);
        // focus on the view
        requestFocus();
    }

    /**
     * Fade out
     */
    public void fadeOut() {
        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(200);
        startAnimation(anim);
        setVisibility(GONE);
        // fix onclick issue
        clearFocus();
        setClickable(false);
    }

    public void setName(String name) {
        mNameTextView.setText(name);
    }

    public void setInfo(String info) {
        if(info != null) {
            mInfoTextView.setVisibility(VISIBLE);
            mInfoTextView.setText(info);
        }
        else {
            mInfoTextView.setVisibility(GONE);
        }
    }

    public void setTextColor(ColorStateList color) {
        mNameTextView.setTextColor(color);
        mInfoTextView.setTextColor(ColorUtil.alpha(color.getDefaultColor(), 150));
    }

    public void setBackGroundcolor(ColorStateList color) {
        mBackgroundColor = color;
        mContentLayout.getBackground().setColorFilter(color.getDefaultColor(), PorterDuff.Mode.SRC_ATOP);
    }

    public int getBackgroundColor() {
        return mBackgroundColor == null ? ContextCompat.getColor(mContext, R.color.chips_opened_bg) : mBackgroundColor.getDefaultColor();
    }

    public void setDeleteIconColor(ColorStateList color) {
        mDeleteButton.getDrawable().mutate().setColorFilter(color.getDefaultColor(), PorterDuff.Mode.SRC_ATOP);
    }

    public void setOnDeleteClicked(OnClickListener onClickListener) {
        mDeleteButton.setOnClickListener(onClickListener);
    }

    public void alignLeft() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentLayout.getLayoutParams();
        params.leftMargin = 0;
        mContentLayout.setLayoutParams(params);
    }

    public void alignRight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentLayout.getLayoutParams();
        params.rightMargin = 0;
        mContentLayout.setLayoutParams(params);
    }

    public static class Builder {
        private Context context;
        private String name;
        private String info;
        private ColorStateList textColor;
        private ColorStateList backgroundColor;
        private ColorStateList deleteIconColor;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder info(String info) {
            this.info = info;
            return this;
        }

        public Builder chip(ChipInterface chip) {
            this.name = chip.getLabel();
            this.info = chip.getInfo();
            return this;
        }

        public Builder textColor(ColorStateList textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder backgroundColor(ColorStateList backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder deleteIconColor(ColorStateList deleteIconColor) {
            this.deleteIconColor = deleteIconColor;
            return this;
        }

        public DetailedChipView build() {
            return DetailedChipView.newInstance(this);
        }
    }

    private static DetailedChipView newInstance(Builder builder) {
        DetailedChipView detailedChipView = new DetailedChipView(builder.context);
        // avatar
        // background color
        if(builder.backgroundColor != null)
            detailedChipView.setBackGroundcolor(builder.backgroundColor);

        // text color
        if(builder.textColor != null)
            detailedChipView.setTextColor(builder.textColor);
        else if(ColorUtil.isColorDark(detailedChipView.getBackgroundColor()))
            detailedChipView.setTextColor(ColorStateList.valueOf(Color.WHITE));
        else
            detailedChipView.setTextColor(ColorStateList.valueOf(Color.BLACK));

        // delete icon color
        if(builder.deleteIconColor != null)
            detailedChipView.setDeleteIconColor(builder.deleteIconColor);
        else if(ColorUtil.isColorDark(detailedChipView.getBackgroundColor()))
            detailedChipView.setDeleteIconColor(ColorStateList.valueOf(Color.WHITE));
        else
            detailedChipView.setDeleteIconColor(ColorStateList.valueOf(Color.BLACK));

        detailedChipView.setName(builder.name);
        detailedChipView.setInfo(builder.info);
        return detailedChipView;
    }
}
