package org.sufficientlysecure.materialchips.util;


import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.Window.Callback;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;


public abstract class DelegateWindowCallback implements Window.Callback {
    private Window.Callback delegateCallback;

    public DelegateWindowCallback(Callback delegateCallback) {
        this.delegateCallback = delegateCallback;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return delegateCallback.dispatchKeyEvent(keyEvent);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return delegateCallback.dispatchKeyShortcutEvent(keyEvent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return delegateCallback.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return delegateCallback.dispatchTrackballEvent(motionEvent);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return delegateCallback.dispatchGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        return delegateCallback.dispatchPopulateAccessibilityEvent(accessibilityEvent);
    }

    @Nullable
    @Override
    public View onCreatePanelView(int i) {
        return delegateCallback.onCreatePanelView(i);
    }

    @Override
    public boolean onCreatePanelMenu(int i, Menu menu) {
        return delegateCallback.onCreatePanelMenu(i, menu);
    }

    @Override
    public boolean onPreparePanel(int i, View view, Menu menu) {
        return delegateCallback.onPreparePanel(i, view, menu);
    }

    @Override
    public boolean onMenuOpened(int i, Menu menu) {
        return delegateCallback.onMenuOpened(i, menu);
    }

    @Override
    public boolean onMenuItemSelected(int i, MenuItem menuItem) {
        return delegateCallback.onMenuItemSelected(i, menuItem);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
        delegateCallback.onWindowAttributesChanged(layoutParams);
    }

    @Override
    public void onContentChanged() {
        delegateCallback.onContentChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean b) {
        delegateCallback.onWindowFocusChanged(b);
    }

    @Override
    public void onAttachedToWindow() {
        delegateCallback.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        delegateCallback.onDetachedFromWindow();
    }

    @Override
    public void onPanelClosed(int i, Menu menu) {
        delegateCallback.onPanelClosed(i, menu);
    }

    @Override
    public boolean onSearchRequested() {
        return delegateCallback.onSearchRequested();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        return delegateCallback.onSearchRequested(searchEvent);
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return delegateCallback.onWindowStartingActionMode(callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int i) {
        return delegateCallback.onWindowStartingActionMode(callback, i);
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        delegateCallback.onActionModeStarted(actionMode);
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        delegateCallback.onActionModeFinished(actionMode);
    }

}
