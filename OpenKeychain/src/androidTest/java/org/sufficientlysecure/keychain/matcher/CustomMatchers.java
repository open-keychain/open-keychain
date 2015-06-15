package org.sufficientlysecure.keychain.matcher;


import android.support.annotation.ColorRes;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.internal.util.Checks;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.sufficientlysecure.keychain.ui.KeyListFragment.KeyListAdapter;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;

import static android.support.test.internal.util.Checks.checkNotNull;


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

    public static Matcher<Object> withKeyItemId(final long keyId) {
        return new BoundedMatcher<Object, KeyItem>(KeyItem.class) {
            @Override
            public boolean matchesSafely(KeyItem item) {
                return item.mKeyId == keyId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with key id: " + keyId);
            }
        };
    }

}
