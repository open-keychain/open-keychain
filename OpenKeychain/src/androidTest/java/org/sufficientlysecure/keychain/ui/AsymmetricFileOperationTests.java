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
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.widget.AdapterView;

import org.junit.Before;
import org.junit.Rule;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.TestHelpers;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import java.io.File;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.getImageNames;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.TestHelpers.pickRandom;
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
public class AsymmetricFileOperationTests {

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

    @Before
    public void setUp() throws Exception {
        Activity activity = mActivity.getActivity();

        TestHelpers.copyFiles();

        // import these two, make sure they're there
        importKeysFromResource(activity, "x.sec.asc");

        // make sure no passphrases are cached
        PassphraseCacheService.clearAllCachedPassphrases(activity);
    }

    //@Test
    public void testFileSaveEncryptDecrypt() throws Exception {

        // navigate to 'encrypt text'
        onView(withId(R.id.encrypt_files)).perform(click());

        File file = pickRandom(getImageNames());
        File outputFile = new File(getInstrumentation().getTargetContext().getFilesDir(), "output-token.gpg");

        { // encrypt

            // the EncryptKeyCompletionView is tested individually
            onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));

            handleAddFileIntent(file);
            onView(withId(R.id.file_list_entry_add)).perform(click());

            handleSaveEncryptedFileIntent(outputFile);
            onView(withId(R.id.encrypt_save)).perform(click());

            assertThat("output file has been written", true, is(outputFile.exists()));

        }

        // go to decrypt from clipboard view
        pressBack();

        handleOpenFileIntentKitKat(outputFile);
        onView(withId(R.id.decrypt_files)).perform(click());

        { // decrypt
            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));
            onView(withText(R.string.btn_unlock)).perform(click());

            onView(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(file.getName()))))
                    .check(matches(allOf(withEncryptionStatus(true), withSignatureNone())));
        }

        { // delete original file

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(file.getName())))),
                    withId(R.id.context_menu))).perform(click());

            // delete file
            onView(withText(R.string.btn_delete_original)).perform(click());

            checkSnackbar(Style.OK, R.string.file_delete_ok);
            assertThat("output file has been deleted", false, is(outputFile.exists()));

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(file.getName())))),
                    withId(R.id.context_menu))).perform(click());

            // delete file
            onView(withText(R.string.btn_delete_original)).perform(click());

            checkSnackbar(Style.WARN, R.string.file_delete_none);

        }

        { // save file (*after* deletion~)

            // open context menu
            onView(allOf(isDescendantOfA(isRecyclerItemView(R.id.decrypted_files_list,
                    hasDescendant(withText(file.getName())))),
                    withId(R.id.context_menu))).perform(click());

            File savedFile =
                    new File(getInstrumentation().getTargetContext().getFilesDir(), "vo.png");
            handleSaveDecryptedFileIntent(savedFile, file.getName());

            // save decrypted content
            onView(withText(R.string.btn_save_file)).perform(click());

            checkSnackbar(Style.OK, R.string.file_saved);
            assertThat("decrypted file has been saved", true, is(savedFile.exists()));

            // cleanup
            // noinspection ResultOfMethodCallIgnored
            file.delete();

        }

    }

    private void handleAddFileIntent(File file) {
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            handleAddFileIntentKitKat(file);
        } else {
            handleAddFileIntentOlder(file);
        }
    }

    @TargetApi(VERSION_CODES.KITKAT)
    private void handleAddFileIntentKitKat(File file) {
        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        Intents.intending(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE)),
                hasExtraWithKey(Intent.EXTRA_ALLOW_MULTIPLE)
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );
    }

    private void handleAddFileIntentOlder(File file) {
        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        Intents.intending(allOf(
                hasAction(Intent.ACTION_GET_CONTENT),
                hasType("*/*"),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );
    }

    @TargetApi(VERSION_CODES.KITKAT)
    private void handleSaveDecryptedFileIntent(File file, String expectedTitle) {
        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        Intents.intending(allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasExtra("android.content.extra.SHOW_ADVANCED", true),
                hasExtra(Intent.EXTRA_TITLE, expectedTitle),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );
    }

    @TargetApi(VERSION_CODES.KITKAT)
    private void handleSaveEncryptedFileIntent(File file) {

        try {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        } catch (Exception e) {
            // nvm
        }

        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        Intents.intending(allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("*/*"),
                hasExtra("android.content.extra.SHOW_ADVANCED", true),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
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

    //@Test
    public void testGeneralErrorHandling() throws Exception {

        // navigate to encrypt files fragment
        onView(withId(R.id.encrypt_files)).perform(click());

        File[] files = getImageNames();

        { // encrypt screen

            onView(withId(R.id.encrypt_share)).perform(click());
            checkSnackbar(Style.ERROR, R.string.error_no_file_selected);

            handleAddFileIntent(files[0]);
            onView(withId(R.id.file_list_entry_add)).perform(click());

            handleAddFileIntent(files[1]);
            onView(withId(R.id.file_list_entry_add)).perform(click());

            onView(withId(R.id.encrypt_share)).perform(click());
            checkSnackbar(Style.ERROR, R.string.select_encryption_key);

            onView(withId(R.id.sign)).perform(click());
            onData(withKeyItemId(0x9D604D2F310716A3L))
                    .inAdapterView(isAssignableFrom(AdapterView.class))
                    .perform(click());

            onView(withId(R.id.encrypt_share)).perform(click());
            checkSnackbar(Style.ERROR, R.string.error_detached_signature);

            // the EncryptKeyCompletionView is tested individually
            onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));

            onView(withId(R.id.encrypt_save)).perform(click());
            checkSnackbar(Style.ERROR, R.string.error_multi_files);

            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            onView(withText(R.string.btn_copy_encrypted_signed)).perform(click());
            checkSnackbar(Style.ERROR, R.string.error_multi_clipboard);

        }

    }

}
