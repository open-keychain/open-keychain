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


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;


public class ConnectionStatusView extends View {
    private static final int ARC_COUNT = 3;
    public static final int COLOR_CONNECTED = 0xff394baf;
    public static final int COLOR_DISCONNECTED = 0xffcccccc;


    private Arc[] arcs;
    private ValueAnimator[] animators;
    private boolean isConnected = false;


    public ConnectionStatusView(Context context) {
        super(context);
    }

    public ConnectionStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ConnectionStatusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        initializeObjects();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measuredWidth = resolveSize(150, widthMeasureSpec);
        final int measuredHeight = resolveSize(150, heightMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < ARC_COUNT; i++) {
            Arc arc = arcs[i];
            canvas.drawArc(arc.oval, 225, 90, false, arc.paint);
        }

        if (isConnected != isAnimationInitiated()) {
            resetAnimations();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        cancelAnimations();
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;

        if (arcs != null) {
            for (int i = 0; i < ARC_COUNT; i++) {
                arcs[i].paint.setColor(isConnected ? COLOR_CONNECTED : COLOR_DISCONNECTED);
            }
        }

        invalidate();
    }

    private void resetAnimations() {
        if (isConnected != isAnimationInitiated()) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (isConnected) {
                        setupAnimations();
                    } else {
                        cancelAnimations();
                    }
                }
            });
        }
    }

    private boolean isAnimationInitiated() {
        return animators != null;
    }

    private void setupAnimations() {
        if (isAnimationInitiated()) {
            return;
        }

        animators = new ValueAnimator[ARC_COUNT];
        for (int i = 0; i < ARC_COUNT; i++) {
            final int index = i;
            ValueAnimator animator = ValueAnimator.ofInt(100, 255, 100);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setDuration(2000);
            animator.setStartDelay(i * 300);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    arcs[index].paint.setAlpha((int) animation.getAnimatedValue());
                    invalidate();
                }
            });
            animator.start();

            animators[i] = animator;
        }
    }

    private void cancelAnimations() {
        if (!isAnimationInitiated()) {
            return;
        }

        for (int i = 0; i < ARC_COUNT; i++) {
            animators[i].cancel();
        }
        animators = null;
    }

    private void initializeObjects() {
        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        float r = Math.min(width, height) / 2f;

        arcs = new Arc[ARC_COUNT];
        for (int i = 0; i < ARC_COUNT; i++) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(r / 10f);
            paint.setColor(isConnected ? COLOR_CONNECTED : COLOR_DISCONNECTED);

            float d = r / 4 + i * r / 4;
            RectF oval = new RectF(centerX - d, centerY - d + r / 3, centerX + d, centerY + d + r / 3);

            arcs[i] = new Arc(paint, oval);
        }
    }

    private static class Arc {
        private final Paint paint;
        private final RectF oval;

        Arc(Paint paint, RectF oval) {
            this.paint = paint;
            this.oval = oval;
        }
    }
}