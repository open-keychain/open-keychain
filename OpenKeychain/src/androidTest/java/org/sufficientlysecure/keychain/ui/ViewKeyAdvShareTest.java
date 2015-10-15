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
 */

package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasHost;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasScheme;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sufficientlysecure.keychain.TestHelpers.checkAndDismissSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.cleanupForTests;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ViewKeyAdvShareTest {

    @Rule
    public final IntentsTestRule<ViewKeyAdvActivity> mActivityRule
            = new IntentsTestRule<ViewKeyAdvActivity>(ViewKeyAdvActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.setData(KeyRings.buildGenericKeyRingUri(0x9D604D2F310716A3L));
            intent.putExtra(ViewKeyAdvActivity.EXTRA_SELECTED_TAB, ViewKeyAdvActivity.TAB_SHARE);
            return intent;
        }
    };
    private Activity mActivity;

    @Before
    public void setUp() throws Exception {
        mActivity = mActivityRule.getActivity();

        cleanupForTests(mActivity);
    }

    @Test
    public void testShareOperations() throws Exception {

        // no-op should yield snackbar
        onView(withId(R.id.view_key_action_fingerprint_clipboard)).perform(click());
        checkAndDismissSnackbar(Style.OK, R.string.fingerprint_copied_to_clipboard);
        assertThat("clipboard data is fingerprint", ClipboardReflection.getClipboardText(mActivity),
                is("c619d53f7a5f96f391a84ca79d604d2f310716a3"));

        intending(allOf(
                hasAction("android.intent.action.CHOOSER"),
                hasExtra(equalTo(Intent.EXTRA_INTENT), allOf(
                        hasAction(Intent.ACTION_SEND),
                        hasType("text/plain"),
                        hasExtra(is(Intent.EXTRA_TEXT), is("openpgp4fpr:c619d53f7a5f96f391a84ca79d604d2f310716a3")),
                        hasExtra(is(Intent.EXTRA_STREAM),
                                allOf(hasScheme("content"), hasHost(TemporaryFileProvider.AUTHORITY)))
                ))
        )).respondWith(new ActivityResult(Activity.RESULT_OK, null));
        onView(withId(R.id.view_key_action_fingerprint_share)).perform(click());

        onView(withId(R.id.view_key_action_key_clipboard)).perform(click());
        checkAndDismissSnackbar(Style.OK, R.string.key_copied_to_clipboard);
        assertThat("clipboard data is key",
                ClipboardReflection.getClipboardText(mActivity), startsWith("----"));

        intending(allOf(
                hasAction("android.intent.action.CHOOSER"),
                hasExtra(equalTo(Intent.EXTRA_INTENT), allOf(
                        hasAction(Intent.ACTION_SEND),
                        hasType("text/plain"),
                        hasExtra(is(Intent.EXTRA_TEXT), startsWith("----")),
                        hasExtra(is(Intent.EXTRA_STREAM),
                                allOf(hasScheme("content"), hasHost(TemporaryFileProvider.AUTHORITY)))
                ))
        )).respondWith(new ActivityResult(Activity.RESULT_OK, null));
        onView(withId(R.id.view_key_action_key_share)).perform(click());

    }


}
