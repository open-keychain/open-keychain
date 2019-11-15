package org.sufficientlysecure.materialchips;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.materialchips.model.ChipInterface;
import org.sufficientlysecure.materialchips.util.LetterTileProvider;
import org.sufficientlysecure.materialchips.util.ViewUtil;

public class ChipView extends RelativeLayout {

    private static final String TAG = ChipView.class.toString();
    // context
    private Context mContext;
    // xml elements
    private LinearLayout mContentLayout;
    private TextView mLabelTextView;
    private ImageButton mDeleteButton;
    // attributes
    private static final int NONE = -1;
    private String mLabel;
    private ColorStateList mLabelColor;
    private boolean mDeletable = false;
    private Drawable mDeleteIcon;
    private ColorStateList mDeleteIconColor;
    private ColorStateList mBackgroundColor;
    // letter tile provider
    private LetterTileProvider mLetterTileProvider;
    // chip
    private ChipInterface mChip;

    public ChipView(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public ChipView(Context context, AttributeSet attrs) {
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
        View rootView = inflate(getContext(), R.layout.chip_view, this);

        mContentLayout = (LinearLayout) rootView.findViewById(R.id.content);
        mLabelTextView = (TextView) rootView.findViewById(R.id.label);
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_button);

        // letter tile provider
        mLetterTileProvider = new LetterTileProvider(mContext);

        // attributes
        if (attrs != null) {
            TypedArray a = mContext.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ChipView,
                    0, 0);

            try {
                // label
                mLabel = a.getString(R.styleable.ChipView_label);
                mLabelColor = a.getColorStateList(R.styleable.ChipView_labelColor);
                mDeletable = a.getBoolean(R.styleable.ChipView_deletable, false);
                mDeleteIconColor = a.getColorStateList(R.styleable.ChipView_deleteIconColor);
                int deleteIconId = a.getResourceId(R.styleable.ChipView_deleteIcon, NONE);
                if (deleteIconId != NONE)
                    mDeleteIcon = ContextCompat.getDrawable(mContext, deleteIconId);
                // background color
                mBackgroundColor = a.getColorStateList(R.styleable.ChipView_backgroundColor);
            } finally {
                a.recycle();
            }
        }

        // inflate
        inflateWithAttributes();
    }

    /**
     * Inflate the view
     */
    private void inflateWithAttributes() {
        // label
        setLabel(mLabel);
        if (mLabelColor != null)
            setLabelColor(mLabelColor);

        // delete button
        setDeletable(mDeletable);

        // background color
        if (mBackgroundColor != null)
            setChipBackgroundColor(mBackgroundColor);
    }

    public void inflate(ChipInterface chip) {
        mChip = chip;
        // label
        mLabel = mChip.getLabel();
        // inflate
        inflateWithAttributes();
    }

    /**
     * Get label
     *
     * @return the label
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * Set label
     *
     * @param label the label to set
     */
    public void setLabel(String label) {
        mLabel = label;
        mLabelTextView.setText(label);
    }

    /**
     * Set label color
     *
     * @param color the color to set
     */
    public void setLabelColor(ColorStateList color) {
        mLabelColor = color;
        mLabelTextView.setTextColor(color);
    }

    /**
     * Set label color
     *
     * @param color the color to set
     */
    public void setLabelColor(@ColorInt int color) {
        mLabelColor = ColorStateList.valueOf(color);
        mLabelTextView.setTextColor(color);
    }

//    /**
//     * Show or hide avatar icon
//     *
//     * @param hasAvatarIcon true to show, false to hide
//     */
//    public void setHasAvatarIcon(boolean hasAvatarIcon) {
//        mHasAvatarIcon = hasAvatarIcon;
//
//        if(!mHasAvatarIcon) {
//            // hide icon
//            mAvatarIconImageView.setVisibility(GONE);
//            // adjust padding
//            if(mDeleteButton.getVisibility() == VISIBLE)
//                mLabelTextView.setPadding(ViewUtil.dpToPx(12), 0, 0, 0);
//            else
//                mLabelTextView.setPadding(ViewUtil.dpToPx(12), 0, ViewUtil.dpToPx(12), 0);
//
//        }
//        else {
//            // show icon
//            mAvatarIconImageView.setVisibility(VISIBLE);
//            // adjust padding
//            if(mDeleteButton.getVisibility() == VISIBLE)
//                mLabelTextView.setPadding(ViewUtil.dpToPx(8), 0, 0, 0);
//            else
//                mLabelTextView.setPadding(ViewUtil.dpToPx(8), 0, ViewUtil.dpToPx(12), 0);
//
//            // set icon
//            if(mAvatarIconUri != null)
//                mAvatarIconImageView.setImageURI(mAvatarIconUri);
//            else if(mAvatarIconDrawable != null)
//                mAvatarIconImageView.setImageDrawable(mAvatarIconDrawable);
//            else
//                mAvatarIconImageView.setImageBitmap(mLetterTileProvider.getLetterTile(getLabel()));
//        }
//    }

//    /**
//     * Set avatar icon
//     *
//     * @param avatarIcon the icon to set
//     */
//    public void setAvatarIcon(Drawable avatarIcon) {
//        mAvatarIconDrawable = avatarIcon;
//        mHasAvatarIcon = true;
//        inflateWithAttributes();
//    }

//    /**
//     * Set avatar icon
//     *
//     * @param avatarUri the uri of the icon to set
//     */
//    public void setAvatarIcon(Uri avatarUri) {
//        mAvatarIconUri = avatarUri;
//        mHasAvatarIcon = true;
//        inflateWithAttributes();
//    }

    /**
     * Show or hide delte button
     *
     * @param deletable true to show, false to hide
     */
    public void setDeletable(boolean deletable) {
        mDeletable = deletable;
        if (!mDeletable) {
            // hide delete icon
            mDeleteButton.setVisibility(GONE);
            // adjust padding
            mLabelTextView.setPadding(ViewUtil.dpToPx(12), 0, ViewUtil.dpToPx(12), 0);
        } else {
            // show icon
            mDeleteButton.setVisibility(VISIBLE);
            // adjust padding
            mLabelTextView.setPadding(ViewUtil.dpToPx(12), 0, 0, 0);

            // set icon
            if (mDeleteIcon != null)
                mDeleteButton.setImageDrawable(mDeleteIcon);
            if (mDeleteIconColor != null)
                mDeleteButton.getDrawable().mutate().setColorFilter(mDeleteIconColor.getDefaultColor(), PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * Set delete icon color
     *
     * @param color the color to set
     */
    public void setDeleteIconColor(ColorStateList color) {
        mDeleteIconColor = color;
        mDeletable = true;
        inflateWithAttributes();
    }

    /**
     * Set delete icon color
     *
     * @param color the color to set
     */
    public void setDeleteIconColor(@ColorInt int color) {
        mDeleteIconColor = ColorStateList.valueOf(color);
        mDeletable = true;
        inflateWithAttributes();
    }

    /**
     * Set delete icon
     *
     * @param deleteIcon the icon to set
     */
    public void setDeleteIcon(Drawable deleteIcon) {
        mDeleteIcon = deleteIcon;
        mDeletable = true;
        inflateWithAttributes();
    }

    /**
     * Set background color
     *
     * @param color the color to set
     */
    public void setChipBackgroundColor(ColorStateList color) {
        mBackgroundColor = color;
        setChipBackgroundColor(color.getDefaultColor());
    }

    /**
     * Set background color
     *
     * @param color the color to set
     */
    public void setChipBackgroundColor(@ColorInt int color) {
        mBackgroundColor = ColorStateList.valueOf(color);
        mContentLayout.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * Set the chip object
     *
     * @param chip the chip
     */
    public void setChip(ChipInterface chip) {
        mChip = chip;
    }

    /**
     * Set OnClickListener on the delete button
     *
     * @param onClickListener the OnClickListener
     */
    public void setOnDeleteClicked(OnClickListener onClickListener) {
        mDeleteButton.setOnClickListener(onClickListener);
    }

    /**
     * Set OnclickListener on the entire chip
     *
     * @param onClickListener the OnClickListener
     */
    public void setOnChipClicked(OnClickListener onClickListener) {
        mContentLayout.setOnClickListener(onClickListener);
    }

    /**
     * Builder class
     */
    public static class Builder {
        private Context context;
        private String label;
        private ColorStateList labelColor;
        private boolean deletable = false;
        private Drawable deleteIcon;
        private ColorStateList deleteIconColor;
        private ColorStateList backgroundColor;
        private ChipInterface chip;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder labelColor(ColorStateList labelColor) {
            this.labelColor = labelColor;
            return this;
        }

        public Builder deletable(boolean deletable) {
            this.deletable = deletable;
            return this;
        }

        public Builder deleteIcon(Drawable deleteIcon) {
            this.deleteIcon = deleteIcon;
            return this;
        }

        public Builder deleteIconColor(ColorStateList deleteIconColor) {
            this.deleteIconColor = deleteIconColor;
            return this;
        }

        public Builder backgroundColor(ColorStateList backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder chip(ChipInterface chip) {
            this.chip = chip;
            this.label = chip.getLabel();
            return this;
        }

        public ChipView build() {
            return newInstance(this);
        }
    }

    private static ChipView newInstance(Builder builder) {
        ChipView chipView = new ChipView(builder.context);
        chipView.mLabel = builder.label;
        chipView.mLabelColor = builder.labelColor;
        chipView.mDeletable = builder.deletable;
        chipView.mDeleteIcon = builder.deleteIcon;
        chipView.mDeleteIconColor = builder.deleteIconColor;
        chipView.mBackgroundColor = builder.backgroundColor;
        chipView.mChip = builder.chip;
        chipView.inflateWithAttributes();

        return chipView;
    }
}
