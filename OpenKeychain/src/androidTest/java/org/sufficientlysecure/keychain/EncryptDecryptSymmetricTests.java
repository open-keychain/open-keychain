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
import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.test.suitebuilder.annotation.LargeTest;

import com.nispok.snackbar.Snackbar;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.actions.CheckHelpers;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.sufficientlysecure.keychain.actions.CheckHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.actions.CustomActions.actionOpenDrawer;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EncryptDecryptSymmetricTests {

    public static final String PASSPHRASE = "fn9nf8wnaf";

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

        // navigate to encrypt/decrypt
        onView(withId(R.id.drawer_layout)).perform(actionOpenDrawer());
        onView(ViewMatchers.withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.encrypt_text)).perform(click());

        {
            String text = "how much wood";
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

        // TODO fix thing, finish test

    }

}
