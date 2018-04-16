/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;

import org.sufficientlysecure.keychain.R;

public class PrefixedEditText extends AppCompatEditText {

    private CharSequence mPrefix;
    private int mPrefixColor;
    private int desiredWidth;

    public PrefixedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
        TypedArray style = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.PrefixedEditText, 0, 0);
        mPrefix = style.getString(R.styleable.PrefixedEditText_prefix);
        mPrefixColor = style.getColor(R.styleable.PrefixedEditText_prefixColor, getCurrentTextColor());
        if (mPrefix == null) {
            mPrefix = "";
        }
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        desiredWidth = (int) Math.ceil(Layout.getDesiredWidth(mPrefix, getPaint()));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        TextPaint paint = getPaint();
        // reset to the actual text color - it might be the hint color currently
        paint.setColor(mPrefixColor);
        canvas.drawText(mPrefix, 0, mPrefix.length(), super.getCompoundPaddingLeft(), getBaseline(), paint);
    }

    @Override
    public int getCompoundPaddingLeft() {
        return super.getCompoundPaddingLeft() + desiredWidth;
    }

    public void setPrefix(CharSequence prefix) {
	    mPrefix = prefix;

	    invalidate();
	    requestLayout();
    }

}