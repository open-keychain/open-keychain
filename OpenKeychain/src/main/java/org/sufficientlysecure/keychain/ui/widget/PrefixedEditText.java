package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;

public class PrefixedEditText extends EditText {

    private String mPrefix;
    private Rect mPrefixRect = new Rect();

	public PrefixedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
        TypedArray style = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.PrefixedEditText, 0, 0);
        mPrefix = style.getString(R.styleable.PrefixedEditText_prefix);
        if (mPrefix == null) {
            mPrefix = "";
        }
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        getPaint().getTextBounds(mPrefix, 0, mPrefix.length(), mPrefixRect);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(mPrefix, super.getCompoundPaddingLeft(), getBaseline(), getPaint());
    }

    @Override
    public int getCompoundPaddingLeft() {
        return super.getCompoundPaddingLeft() + mPrefixRect.width();
    }

}