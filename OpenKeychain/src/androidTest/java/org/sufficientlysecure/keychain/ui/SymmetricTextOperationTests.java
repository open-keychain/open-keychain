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

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasFlags;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasHost;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasScheme;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.isRecyclerItemView;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withEncryptionStatus;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withSignatureNone;

//TODO This test is disabled because it needs to be fixed to work with updated code
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class SymmetricTextOperationTests {

    public static final String PASSPHRASE = randomString(5, 20);

    @Rule
    public final IntentsTestRule<MainActivity> mActivity
            = new IntentsTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            intent.putExtra(MainActivity.EXTRA_INIT_FRAG, MainActivity.ID_ENCRYPT_DECRYPT);
            return intent;
        }
    };

    //@Test
    public void testSymmetricCryptClipboard() throws Exception {

        mActivity.getActivity();

        String text = randomString(10, 30);

        // navigate to encrypt/decrypt
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

            onView(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(R.string.filename_unknown_text))))
                    .check(matches(allOf(withEncryptionStatus(true), withSignatureNone())));

            intending(allOf(
                    hasAction("android.intent.action.CHOOSER"),
                    hasExtra(equalTo(Intent.EXTRA_INTENT), allOf(
                            hasAction(Intent.ACTION_VIEW),
                            hasFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            hasData(allOf(hasScheme("content"), hasHost(TemporaryFileProvider.AUTHORITY))),
                            hasType("text/plain")
                    ))
            )).respondWith(new ActivityResult(Activity.RESULT_OK, null));

            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(R.string.filename_unknown_text)))),
                    withId(R.id.file))).perform(click());

        }

    }

    //@Test
    public void testSymmetricCryptShare() throws Exception {

        mActivity.getActivity();

        String text = randomString(10, 30);

        // navigate to encrypt/decrypt
        onView(withId(R.id.encrypt_text)).perform(click());

        {
            onView(withId(R.id.encrypt_text_text)).perform(typeText(text));

            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            onView(withText(R.string.label_symmetric)).perform(click());

            onView(withId(R.id.passphrase)).perform(typeText(PASSPHRASE));

            onView(withId(R.id.passphraseAgain)).perform(typeText(PASSPHRASE));

            onView(withId(R.id.encrypt_text_text)).check(matches(withText(text)));

            intending(allOf(
                    hasAction("android.intent.action.CHOOSER"),
                    hasExtra(equalTo(Intent.EXTRA_INTENT), allOf(
                            hasAction(Intent.ACTION_SEND),
                            hasFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            hasExtraWithKey(Intent.EXTRA_TEXT),
                            hasType("text/plain")
                    ))
            )).respondWith(new ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.encrypt_share)).perform(click());

        }

    }


}
