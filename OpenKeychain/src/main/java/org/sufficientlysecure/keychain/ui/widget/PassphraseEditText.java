/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;

public class PassphraseEditText extends AppCompatEditText {

    PasswordStrengthBarView mPasswordStrengthBarView;
    int mPasswordBarWidth;
    int mPasswordBarHeight;
    float barGap;

    public PassphraseEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPasswordBarHeight = (int) (8 * getResources().getDisplayMetrics().density);
        mPasswordBarWidth = (int) (50 * getResources().getDisplayMetrics().density);

        barGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getContext().getResources().getDisplayMetrics());

        this.setPadding(getPaddingLeft(), getPaddingTop(),
                getPaddingRight() + (int) barGap + mPasswordBarWidth, getPaddingBottom());

        mPasswordStrengthBarView = new PasswordStrengthBarView(context, attrs);
        mPasswordStrengthBarView.setShowGuides(false);

        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPasswordStrengthBarView.setPassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mPasswordStrengthBarView.layout(0, 0, mPasswordBarWidth, mPasswordBarHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float translateX = getScrollX() + canvas.getWidth() - mPasswordBarWidth;
        float translateY = (canvas.getHeight() - mPasswordBarHeight) / 2;
        canvas.translate(translateX, translateY);
        mPasswordStrengthBarView.draw(canvas);
        canvas.translate(-translateX, -translateY);
    }
}