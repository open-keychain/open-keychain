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
import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.sufficientlysecure.keychain.actions.CustomActions.actionOpenDrawer;
import static org.sufficientlysecure.keychain.actions.CustomActions.tokenEncryptViewAddToken;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EncryptDecryptTests {

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
    public void testTextEncryptDecrypt() throws Exception {

        // navigate to encrypt/decrypt
        onView(withId(R.id.drawer_layout)).perform(actionOpenDrawer());
        onView(ViewMatchers.withText(R.string.nav_encrypt_decrypt)).perform(click());
        onView(withId(R.id.encrypt_text)).perform(click());

        {
            // TODO instrument this (difficult because of TokenCompleteView's async implementation)
            onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));

            String text = "how much wood";
            onView(withId(R.id.encrypt_text_text)).perform(typeText(text));

            onView(withId(R.id.encrypt_copy)).perform(click());
        }

        // go to decrypt from clipboard view
        pressBack();
        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        // synchronization with passphrase caching thing doesn't work
        onView(withId(R.id.passphrase_passphrase)).inRoot(isPlatformPopup()).perform(typeText("x"));

    }

    static void importKeysFromResource(Context context, String name) throws Exception {
        IteratorWithIOThrow<UncachedKeyRing> stream = UncachedKeyRing.fromStream(
                getInstrumentation().getContext().getAssets().open(name));

        ProviderHelper helper = new ProviderHelper(context);
        while(stream.hasNext()) {
            UncachedKeyRing ring = stream.next();
            if (ring.isSecret()) {
                helper.saveSecretKeyRing(ring, new ProgressScaler());
            } else {
                helper.saveSecretKeyRing(ring, new ProgressScaler());
            }
        }

    }

}
