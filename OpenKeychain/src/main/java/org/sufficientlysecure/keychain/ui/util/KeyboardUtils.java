package org.sufficientlysecure.keychain.ui.util;


import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    /**
     * Hides the keyboard
     *
     * @param context
     */
    public static void hideKeyboard(Context context) {
        if (context != null) {
            InputMethodManager inputManager = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /**
     * Hides the keyboard if it is focused on a view.
     *
     * @param context
     * @param currentFocus
     */
    public static void hideKeyboard(Context context, View currentFocus) {
        if (context == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        if (currentFocus == null)
            return;

        inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
    }
}
