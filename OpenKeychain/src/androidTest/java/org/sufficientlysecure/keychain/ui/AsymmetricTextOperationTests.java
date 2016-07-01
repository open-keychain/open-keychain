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
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.widget.AdapterView;

import org.junit.Before;
import org.junit.Rule;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.actions.CustomActions.tokenEncryptViewAddToken;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.isRecyclerItemView;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withDisplayedChild;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withEncryptionStatus;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withSignatureMyKey;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withSignatureNone;

//TODO This test is disabled because it needs to be fixed to work with updated code
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class AsymmetricTextOperationTests {

    @Rule
    public final ActivityTestRule<MainActivity> mActivity
            = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            intent.putExtra(MainActivity.EXTRA_INIT_FRAG, MainActivity.ID_ENCRYPT_DECRYPT);
            return intent;
        }
    };

    @Before
    public void setUp() throws Exception {
        Activity activity = mActivity.getActivity();

        // import these two, make sure they're there
        importKeysFromResource(activity, "x.sec.asc");

        // make sure no passphrases are cached
        PassphraseCacheService.clearAllCachedPassphrases(activity);
    }

    //@Test
    public void testTextEncryptDecryptFromToken() throws Exception {

        // navigate to 'encrypt text'
        onView(withId(R.id.encrypt_text)).perform(click());

        String cleartext = randomString(10, 30);

        { // encrypt

            // the EncryptKeyCompletionView is tested individually
            onView(withId(R.id.result_encryption_icon)).check(matches(withDisplayedChild(0)));
            onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));
            onView(withId(R.id.result_encryption_icon)).check(matches(withDisplayedChild(1)));

            onView(withId(R.id.encrypt_text_text)).perform(typeText(cleartext));

            onView(withId(R.id.encrypt_copy)).perform(click());
        }

        // go to decrypt from clipboard view
        pressBack();
        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        { // decrypt
            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));
            onView(withText(R.string.btn_unlock)).perform(click());

            onView(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(R.string.filename_unknown_text))))
                    .check(matches(allOf(
                            hasDescendant(withText(FileHelper.readableFileSize(cleartext.length()))),
                            withEncryptionStatus(true),
                            withSignatureNone()
                    )));

        }

    }

    //@Test
    public void testSignVerify() throws Exception {

        String cleartext = randomString(10, 30);

        // navigate to 'encrypt text'
        onView(withId(R.id.encrypt_text)).perform(click());

        { // sign

            onView(withId(R.id.encrypt_copy)).perform(click());
            checkSnackbar(Style.ERROR, R.string.error_empty_text);

            onView(withId(R.id.result_signature_icon)).check(matches(withDisplayedChild(0)));
            onView(withId(R.id.sign)).perform(click());
            onData(withKeyItemId(0x9D604D2F310716A3L))
                    .inAdapterView(isAssignableFrom(AdapterView.class))
                    .perform(click());
            onView(withId(R.id.result_signature_icon)).check(matches(withDisplayedChild(1)));

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

            onView(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(R.string.filename_unknown))))
                    .check(matches(allOf(withEncryptionStatus(false), withSignatureMyKey())));

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(R.string.filename_unknown)))),
                    withId(R.id.context_menu))).perform(click());

            // check if log looks ok
            onView(withText(R.string.snackbar_details)).perform(click());
            onView(withText(R.string.msg_dc_clear_signature_ok)).check(matches(isDisplayed()));
            pressBack();

        }

    }

}
