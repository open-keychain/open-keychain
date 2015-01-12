package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.EditText;

/** */
public class HttpsPrefixedText extends EditText {

    private String mPrefix; // can be hardcoded for demo purposes
    private Rect mPrefixRect = new Rect();

	public HttpsPrefixedText(Context context, AttributeSet attrs) {
		super(context, attrs);
        mPrefix = "https://";
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