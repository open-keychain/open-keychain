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


import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.TestHelpers;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.sufficientlysecure.keychain.TestHelpers.getImageNames;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.TestHelpers.pickRandom;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.isRecyclerItemView;
import static org.sufficientlysecure.keychain.matcher.DrawableMatcher.withDrawable;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class MiscFileOperationTests {

    @Rule
    public final IntentsTestRule<MainActivity> mActivityRule
            = new IntentsTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            intent.putExtra(MainActivity.EXTRA_INIT_FRAG, MainActivity.ID_ENCRYPT_DECRYPT);
            return intent;
        }
    };
    private Activity mActivity;

    @Before
    public void setUp() throws Exception {
        mActivity = mActivityRule.getActivity();

        TestHelpers.copyFiles();

        // import these two, make sure they're there
        importKeysFromResource(mActivity, "x.sec.asc");

        // make sure no passphrases are cached
        PassphraseCacheService.clearCachedPassphrases(mActivity);
    }

    @Test
    public void testDecryptNonPgpFile() throws Exception {

        // decrypt any non-pgp file
        File file = pickRandom(getImageNames());
        handleOpenFileIntentKitKat(file);
        onView(withId(R.id.decrypt_files)).perform(click());

        onView(withId(R.id.decrypt_files_action_decrypt)).perform(click());

        { // decrypt

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                        hasDescendant(allOf(
                            hasDescendant(withDrawable(R.drawable.status_signature_invalid_cutout_24dp, true)),
                            hasDescendant(withText(R.string.msg_dc_error_invalid_data)))))),
                withId(R.id.result_error_log))).perform(click());

        }

    }

    @Test
    public void testDecryptNonPgpClipboard() throws Exception {

        // decrypt any non-pgp file
        ClipboardReflection.copyToClipboard(mActivity, randomString(0, 50));

        onView(withId(R.id.decrypt_from_clipboard)).perform(click());

        { // decrypt

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                            hasDescendant(allOf(
                                    hasDescendant(withDrawable(R.drawable.status_signature_invalid_cutout_24dp, true)),
                                    hasDescendant(withText(R.string.msg_dc_error_invalid_data)))))),
                    withId(R.id.result_error_log))).perform(click());

        }

    }


    @TargetApi(VERSION_CODES.KITKAT)
    private void handleOpenFileIntentKitKat(File file) {
        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        Intents.intending(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
                // hasExtraWithKey(Intent.EXTRA_ALLOW_MULTIPLE)
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );
    }

}
