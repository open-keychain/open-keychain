package org.sufficientlysecure.keychain.actions;


import android.support.annotation.StringRes;

import org.hamcrest.CoreMatchers;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.sufficientlysecure.keychain.actions.CustomMatchers.withSnackbarLineColor;


abstract public class CheckHelpers {

    public static void checkSnackbar(Style style, @StringRes Integer text) {

        onView(withClassName(CoreMatchers.endsWith("Snackbar")))
                .check(matches(withSnackbarLineColor(style.mLineColor)));

        if (text != null) {
            onView(withClassName(CoreMatchers.endsWith("Snackbar")))
                    .check(matches(hasDescendant(withText(text))));
        }

    }

}
