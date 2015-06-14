package org.sufficientlysecure.keychain.actions;


import android.support.annotation.ColorRes;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public abstract class CustomMatchers {

    public static Matcher<View> withSnackbarLineColor(@ColorRes final int colorRes) {
        return new BoundedMatcher<View, Snackbar>(Snackbar.class) {
            public void describeTo(Description description) {
                description.appendText("with color resource id: " + colorRes);
            }

            @Override
            public boolean matchesSafely(Snackbar snackbar) {
                return snackbar.getResources().getColor(colorRes) == snackbar.getLineColor();
            }
        };
    }

}
