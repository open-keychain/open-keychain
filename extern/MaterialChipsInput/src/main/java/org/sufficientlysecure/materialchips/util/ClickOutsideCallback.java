package org.sufficientlysecure.materialchips.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import org.sufficientlysecure.materialchips.views.ChipsInputEditText;
import org.sufficientlysecure.materialchips.views.DetailedChipView;

public class ClickOutsideCallback extends DelegateWindowCallback {
    private Activity activity;

    public ClickOutsideCallback(Window.Callback delegateCallback, Activity activity) {
        super(delegateCallback);
        this.activity = activity;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if(v instanceof DetailedChipView) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                    ((DetailedChipView) v).fadeOut();
                }
            }
            if (v instanceof ChipsInputEditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())
                        && !((ChipsInputEditText) v).isFilterableListVisible()) {
                    hideKeyboard(v);
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
