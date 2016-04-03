package org.sufficientlysecure.keychain.ui.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.github.pinball83.maskededittext.MaskedEditText;

import java.util.ListIterator;

public class NoSelectionMaskedEditText extends MaskedEditText {
    private long mLastTouchTime = SystemClock.uptimeMillis();

    public NoSelectionMaskedEditText(Context context) {
        super(context);
    }

    public NoSelectionMaskedEditText(Context context, String mask, String notMaskedSymbol) {
        super(context, mask, notMaskedSymbol);
    }

    public NoSelectionMaskedEditText(Context context, String mask, String notMaskedSymbol, Drawable maskIcon) {
        super(context, mask, notMaskedSymbol, maskIcon);
    }

    public NoSelectionMaskedEditText(Context context, String mask, String notMaskedSymbol, Drawable maskIcon, MaskIconCallback maskIconCallback) {
        super(context, mask, notMaskedSymbol, maskIcon, maskIconCallback);
    }

    public NoSelectionMaskedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSelectionMaskedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int positionBeforeSelection = this.getSelectionStart();

        boolean result = super.onTouchEvent(event);
        if(isDoubleTap(event)) {
            this.setSelection(positionBeforeSelection);
        }

        mLastTouchTime = SystemClock.uptimeMillis();
        return result;
    }

    private boolean isDoubleTap(MotionEvent event) {
        long currentTime = SystemClock.uptimeMillis();

        return event.getActionMasked() == MotionEvent.ACTION_DOWN
                && currentTime - mLastTouchTime <= ViewConfiguration.getDoubleTapTimeout();
    }

}
