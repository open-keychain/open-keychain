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


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.widget.AdapterView;

import org.junit.Before;
import org.junit.Rule;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.TestHelpers;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.File;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.dismissSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.getImageNames;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.TestHelpers.pickRandom;
import static org.sufficientlysecure.keychain.TestHelpers.randomString;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.isRecyclerItemView;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withDisplayedChild;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;
import static org.sufficientlysecure.keychain.matcher.DrawableMatcher.withDrawable;

//TODO This test is disabled because it needs to be fixed to work with updated code
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class MiscCryptOperationTests {

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
        // clear dis shit
        Preferences.getPreferences(getInstrumentation().getTargetContext()).clear();

        mActivity = mActivityRule.getActivity();

        TestHelpers.copyFiles();

        // import these two, make sure they're there
        importKeysFromResource(mActivity, "x.sec.asc");

        // make sure no passphrases are cached
        PassphraseCacheService.clearAllCachedPassphrases(mActivity);
    }

    //@Test
    public void testDecryptNonPgpFile() throws Exception {

        // decrypt any non-pgp file
        File file = pickRandom(getImageNames());
        handleOpenFileIntentKitKat(file);
        onView(withId(R.id.decrypt_files)).perform(click());

        { // decrypt

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(allOf(
                            hasDescendant(withDrawable(R.drawable.status_signature_invalid_cutout_24dp, true)),
                            hasDescendant(withText(R.string.msg_dc_error_invalid_data)))))),
                    withId(R.id.result_error_log))).perform(click());

        }

    }

    //@Test
    public void testDecryptEmptySelection() throws Exception {

        // decrypt any non-pgp file
        handleOpenFileEmptyKitKat();
        onView(withId(R.id.decrypt_files)).perform(click());

        checkSnackbar(Style.ERROR, R.string.no_file_selected);

    }

    //@Test
    public void testDecryptEmptyClipboard() throws Exception {

        // decrypt any non-pgp file
        ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""));

        onView(withId(R.id.decrypt_from_clipboard)).perform(click());
        checkSnackbar(Style.ERROR, R.string.error_clipboard_empty);

    }

    //@Test
    public void testDecryptNonPgpClipboard() throws Exception {

        // decrypt any non-pgp file
        ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, randomString(0, 50));
        clipboard.setPrimaryClip(clip);
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
    private void handleOpenFileEmptyKitKat() {
        Intent data = new Intent();
        data.setData(null);

        Intents.intending(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
                // hasExtraWithKey(Intent.EXTRA_ALLOW_MULTIPLE)
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );
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

    //@Test
    public void testEncryptTokenFromKeyView() throws Exception {

        // navigate to edit key dialog
        onData(withKeyItemId(0x9D604D2F310716A3L))
                .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
                        isDescendantOfA(withId(R.id.key_list_list))))
                .perform(click());
        onView(withId(R.id.view_key_action_encrypt_text)).perform(click());

        // make sure the encrypt is correctly set
        onView(withId(R.id.result_encryption_icon)).check(matches(withDisplayedChild(1)));
        // TODO check token id

    }

    //@Test
    public void testMenuSaveDefault() throws Exception {

        onView(withId(R.id.encrypt_files)).perform(click());

        { // save checked options

            openActionBarOverflowOrOptionsMenu(mActivity);

            // check initial button states
            onView(allOf(withId(R.id.checkbox),
                    hasSibling(withChild(withText(R.string.label_delete_after_encryption)))))
                    .check(matches(isNotChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_enable_compression)))))
                    .check(matches(isChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_encrypt_filenames)))))
                    .check(matches(isChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_file_ascii_armor)))))
                    .check(matches(isNotChecked()));

            // press some buttons

            onView(withText(R.string.label_enable_compression)).perform(click());
            checkSnackbar(Style.OK, R.string.snack_compression_off);
            onView(withText(R.string.btn_save_default)).perform(click());
            checkSnackbar(Style.OK, R.string.btn_saved);
            dismissSnackbar();

            openActionBarOverflowOrOptionsMenu(mActivity);
            onView(withText(R.string.label_encrypt_filenames)).perform(click());
            checkSnackbar(Style.OK, R.string.snack_encrypt_filenames_off);
            onView(withText(R.string.btn_save_default)).perform(click());
            checkSnackbar(Style.OK, R.string.btn_saved);
            dismissSnackbar();

            openActionBarOverflowOrOptionsMenu(mActivity);
            onView(withText(R.string.label_file_ascii_armor)).perform(click());
            checkSnackbar(Style.OK, R.string.snack_armor_on);
            onView(withText(R.string.btn_save_default)).perform(click());
            checkSnackbar(Style.OK, R.string.btn_saved);
            dismissSnackbar();

        }

        pressBack();
        onView(withId(R.id.encrypt_files)).perform(click());

        { // save checked options

            openActionBarOverflowOrOptionsMenu(mActivity);

            // check initial button states (as saved from before!)
            onView(allOf(withId(R.id.checkbox),
                    hasSibling(withChild(withText(R.string.label_delete_after_encryption)))))
                    .check(matches(isNotChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_enable_compression)))))
                    .check(matches(isNotChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_encrypt_filenames)))))
                    .check(matches(isNotChecked()));
            onView(allOf(withId(R.id.checkbox), hasSibling(withChild(withText(R.string.label_file_ascii_armor)))))
                    .check(matches(isChecked()));

        }

    }

}
