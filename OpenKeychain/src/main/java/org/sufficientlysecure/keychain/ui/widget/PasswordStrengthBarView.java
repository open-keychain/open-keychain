/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Matt Allen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

/**
 * Created by matt on 04/07/2014.
 * https://github.com/matt-allen/android-password-strength-indicator
 */
public class PasswordStrengthBarView extends PasswordStrengthView {

    public PasswordStrengthBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMinHeight = 80;
        mMinWidth = 300;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        generateIndicatorColor();
        // Default to full width
        int indWidth = mIndicatorWidth;
        // If score, leave it as full - can cause it to become
        // less than full width in this calculation
        if (mCurrentScore < 20) indWidth = (mIndicatorWidth / 20) * mCurrentScore;
        // Draw indicator
        canvas.drawRect(
                getPaddingLeft(),
                getPaddingTop(),
                indWidth,
                mIndicatorHeight,
                mIndicatorPaint
        );
        // Draw guides if true
        if (mShowGuides) {
            // TODO: Try and do this with a loop, for efficiency
            // Draw bottom guide border
            float positionY = getHeight() - getPaddingBottom() - getPaddingTop();
            float notchHeight = (float) (positionY * 0.8);
            canvas.drawLine(
                    getPaddingLeft(),
                    positionY,
                    getWidth() - getPaddingRight(),
                    positionY,
                    mGuidePaint);
            // Show left-most notch
            canvas.drawLine(
                    getPaddingLeft(),
                    positionY,
                    getPaddingLeft(),
                    notchHeight,
                    mGuidePaint
            );
            // Show middle-left notch
            canvas.drawLine(
                    (float) (mIndicatorWidth * 0.25) + getPaddingLeft(),
                    positionY,
                    (float) (mIndicatorWidth * 0.25) + getPaddingLeft(),
                    notchHeight,
                    mGuidePaint
            );
            // Show the middle notch
            canvas.drawLine(
                    (float) (mIndicatorWidth * 0.5) + getPaddingLeft(),
                    positionY,
                    (float) (mIndicatorWidth * 0.5) + getPaddingLeft(),
                    notchHeight,
                    mGuidePaint
            );
            // Show the middle-right notch
            canvas.drawLine(
                    (float) (mIndicatorWidth * 0.75) + getPaddingLeft(),
                    positionY,
                    (float) (mIndicatorWidth * 0.75) + getPaddingLeft(),
                    notchHeight,
                    mGuidePaint
            );
            // Show the right-most notch
            canvas.drawLine(
                    mIndicatorWidth + getPaddingLeft(),
                    positionY,
                    mIndicatorWidth + getPaddingLeft(),
                    notchHeight,
                    mGuidePaint
            );
        }
    }
}
