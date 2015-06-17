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

package org.sufficientlysecure.keychain;


import android.content.Intent;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.matcher.DrawableMatcher.withDrawable;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EncryptDecryptSymmetricTests {

    public static final String PASSPHRASE = randomString(5, 20);

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

    @Test
    public void testSymmetricTextEncryptDecrypt() throws Exception {

        MainActivity activity = mActivity.getActivity();

        String text = randomString(10, 30);

        // navigate to encrypt/decrypt
        openDrawer(R.id.drawer_layout);
        onView(ViewMatchers.withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.encrypt_text)).perform(click());

        {
            onView(withId(R.id.encrypt_text_text)).perform(typeText(text));

            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            onView(withText(R.string.label_symmetric)).perform(click());

            onView(withId(R.id.passphrase)).perform(typeText(PASSPHRASE));

            onView(withId(R.id.encrypt_copy)).perform(click());

            checkSnackbar(Style.ERROR, R.string.passphrases_do_not_match);

            onView(withId(R.id.passphraseAgain)).perform(typeText(PASSPHRASE));

            onView(withId(R.id.encrypt_text_text)).check(matches(withText(text)));

            onView(withId(R.id.encrypt_copy)).perform(click());

            checkSnackbar(Style.OK, R.string.msg_se_success);
        }

        // go to decrypt from clipboard view
        pressBack();
        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        {
            onView(withId(R.id.passphrase_passphrase)).perform(typeText(PASSPHRASE));
            onView(withText(R.string.btn_unlock)).perform(click());

            onView(withId(R.id.decrypt_text_plaintext)).check(matches(
                    withText(text)));

            // TODO write generic status verifier

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

}
