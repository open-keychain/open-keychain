/*
 * Copyright (C) 2015 Vincent Breitmoser <look@my.amazin.horse>
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
 *
 */

package org.sufficientlysecure.keychain.matcher;


import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewAnimator;

import com.nispok.snackbar.Snackbar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.sufficientlysecure.keychain.R;

import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.sufficientlysecure.keychain.matcher.DrawableMatcher.withDrawable;


public abstract class CustomMatchers {

    public static Matcher<View> withDisplayedChild(final int child) {
        return new BoundedMatcher<View, ViewAnimator>(ViewAnimator.class) {
            public void describeTo(Description description) {
                description.appendText("with displayed child: " + child);
            }

            @Override
            public boolean matchesSafely(ViewAnimator viewAnimator) {
                return viewAnimator.getDisplayedChild() == child;
            }
        };
    }

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

    public static Matcher<RecyclerView.ViewHolder> withKeyHolderId(final long keyId) {
        return new BoundedMatcher<RecyclerView.ViewHolder, RecyclerView.ViewHolder>(RecyclerView.ViewHolder.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with ViewHolder id: " + keyId);
            }

            @Override
            protected boolean matchesSafely(View item) {
                return item.getItemId() == keyId;
            }
        };
    }

    public static Matcher<View> withKeyToken(@ColorRes final long keyId) {
        return new BoundedMatcher<View, EncryptKeyCompletionView>(EncryptKeyCompletionView.class) {
            public void describeTo(Description description) {
                description.appendText("with key id token: " + keyId);
            }

            @Override
            public boolean matchesSafely(EncryptKeyCompletionView tokenView) {
                for (Object object : tokenView.getObjects()) {
                    if (object instanceof KeyItem && ((KeyItem) object).mKeyId == keyId) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static Matcher<View> withRecyclerView(@IdRes int viewId) {
        return allOf(isAssignableFrom(RecyclerView.class), withId(viewId));
    }

    public static Matcher<View> isRecyclerItemView(@IdRes int recyclerId, Matcher<View> specificChildMatcher) {
        return allOf(withParent(withRecyclerView(recyclerId)), specificChildMatcher);
    }

    public static Matcher<View> withEncryptionStatus(boolean encrypted) {

        if (encrypted) {
            return allOf(
                hasDescendant(allOf(
                        withId(R.id.result_encryption_text), withText(R.string.decrypt_result_encrypted))),
                hasDescendant(allOf(
                        withId(R.id.result_encryption_icon), withDrawable(R.drawable.status_lock_closed_24dp, true)))
            );
        } else {
            return allOf(
                hasDescendant(allOf(
                        withId(R.id.result_encryption_text), withText(R.string.decrypt_result_not_encrypted))),
                hasDescendant(allOf(
                        withId(R.id.result_encryption_icon), withDrawable(R.drawable.status_lock_open_24dp, true)))
            );
        }
    }

    public static Matcher<View> withSignatureNone() {

        return allOf(
            hasDescendant(allOf(
                    withId(R.id.result_signature_text), withText(R.string.decrypt_result_no_signature))),
            hasDescendant(allOf(
                    withId(R.id.result_signature_icon), withDrawable(R.drawable.status_signature_invalid_cutout_24dp, true))),
             hasDescendant(allOf(
                    withId(R.id.result_signature_layout), not(isDisplayed())))
        );

    }

    public static Matcher<View> withSignatureMyKey() {

        return allOf(
            hasDescendant(allOf(
                    withId(R.id.result_signature_text), withText(R.string.decrypt_result_signature_certified))),
            hasDescendant(allOf(
                    withId(R.id.result_signature_icon), withDrawable(R.drawable.status_signature_verified_cutout_24dp, true))),
            hasDescendant(allOf(
                    withId(R.id.result_signature_layout), isDisplayed()))
        );

    }

}
