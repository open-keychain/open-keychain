/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain;


import android.app.Activity;
import android.content.Intent;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.AdapterView;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.actions.CustomActions.actionOpenDrawer;
import static org.sufficientlysecure.keychain.actions.CustomActions.tokenEncryptViewAddToken;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;
import static org.sufficientlysecure.keychain.matcher.DrawableMatcher.withDrawable;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AsymmetricOperationTests {

    @Rule
    public final ActivityTestRule<MainActivity> mActivity
            = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            return intent;
        }
    };

    @Before
    public void setUp() throws Exception {
        Activity activity = mActivity.getActivity();

        // import these two, make sure they're there
        importKeysFromResource(activity, "x.sec.asc");
    }

    @Test
    public void testTextEncryptDecryptFromToken() throws Exception {

        // navigate to 'encrypt text'
        onView(withId(R.id.drawer_layout)).perform(actionOpenDrawer());
        onView(withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.encrypt_text)).perform(click());

        String cleartext = randomString(10, 30);

        { // encrypt
            // TODO instrument this (difficult because of TokenCompleteView's async implementation)
            onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));

            onView(withId(R.id.encrypt_text_text)).perform(typeText(cleartext));

            onView(withId(R.id.encrypt_copy)).perform(click());
        }

        // go to decrypt from clipboard view
        pressBack();
        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        { // decrypt
            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));
            onView(withText(R.string.btn_unlock)).perform(click());

            onView(withId(R.id.decrypt_text_plaintext)).check(matches(
                    withText(cleartext)));

            onView(withId(R.id.result_encryption_text)).check(matches(
                    withText(R.string.decrypt_result_encrypted)));
            onView(withId(R.id.result_signature_text)).check(matches(
                    withText(R.string.decrypt_result_no_signature)));
            onView(withId(R.id.result_signature_layout)).check(matches(
                    not(isDisplayed())));

            onView(withId(R.id.result_encryption_icon)).check(matches(
                    withDrawable(R.drawable.status_lock_closed_24dp)));
            onView(withId(R.id.result_signature_icon)).check(matches(
                    withDrawable(R.drawable.status_signature_unknown_cutout_24dp)));
        }

    }

    @Test
    public void testTextEncryptDecryptFromKeyView() throws Exception {

        String cleartext = randomString(10, 30);

        { // encrypt

            // navigate to edit key dialog
            onData(withKeyItemId(0x9D604D2F310716A3L))
                    .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
                            isDescendantOfA(withId(R.id.key_list_list))))
                    .perform(click());
            onView(withId(R.id.view_key_action_encrypt_text)).perform(click());

            onView(withId(R.id.encrypt_text_text)).perform(typeText(cleartext));

            onView(withId(R.id.encrypt_copy)).perform(click());
        }

        // go to decrypt from clipboard view
        pressBack();
        pressBack();

        onView(withId(R.id.drawer_layout)).perform(actionOpenDrawer());
        onView(withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        { // decrypt

            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));
            onView(withText(R.string.btn_unlock)).perform(click());

            onView(withId(R.id.decrypt_text_plaintext)).check(matches(
                    withText(cleartext)));

            onView(withId(R.id.result_encryption_text)).check(matches(
                    withText(R.string.decrypt_result_encrypted)));
            onView(withId(R.id.result_signature_text)).check(matches(
                    withText(R.string.decrypt_result_no_signature)));
            onView(withId(R.id.result_signature_layout)).check(matches(
                    not(isDisplayed())));

            onView(withId(R.id.result_encryption_icon)).check(matches(
                    withDrawable(R.drawable.status_lock_closed_24dp)));
            onView(withId(R.id.result_signature_icon)).check(matches(
                    withDrawable(R.drawable.status_signature_unknown_cutout_24dp)));

        }

    }

    @Test
    public void testSignVerify() throws Exception {

        String cleartext = randomString(10, 30);

        // navigate to 'encrypt text'
        onView(withId(R.id.drawer_layout)).perform(actionOpenDrawer());
        onView(withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.encrypt_text)).perform(click());

        { // sign

            // navigate to edit key dialog
            onView(withId(R.id.sign)).perform(click());
            onData(withKeyItemId(0x9D604D2F310716A3L))
                    .inAdapterView(isAssignableFrom(AdapterView.class))
                    .perform(click());

            onView(withId(R.id.encrypt_text_text)).perform(typeText(cleartext));

            onView(withId(R.id.encrypt_copy)).perform(click());

            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));
            onView(withText(R.string.btn_unlock)).perform(click());

            checkSnackbar(Style.OK, R.string.msg_se_success);

        }

        // go to decrypt from clipboard view
        pressBack();

        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        { // decrypt

            onView(withId(R.id.decrypt_text_plaintext)).check(matches(
                    // startsWith because there may be extra newlines
                    withText(CoreMatchers.startsWith(cleartext))));

            onView(withId(R.id.result_encryption_text)).check(matches(
                    withText(R.string.decrypt_result_not_encrypted)));
            onView(withId(R.id.result_signature_text)).check(matches(
                    withText(R.string.decrypt_result_signature_secret)));
            onView(withId(R.id.result_signature_layout)).check(matches(
                    isDisplayed()));

            onView(withId(R.id.result_encryption_icon)).check(matches(
                    withDrawable(R.drawable.status_lock_open_24dp)));
            onView(withId(R.id.result_signature_icon)).check(matches(
                    withDrawable(R.drawable.status_signature_verified_cutout_24dp)));

        }

    }

}
