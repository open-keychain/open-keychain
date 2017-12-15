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