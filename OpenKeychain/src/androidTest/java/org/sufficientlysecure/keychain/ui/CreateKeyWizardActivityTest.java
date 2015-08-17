package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.actions.CustomActions;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

import java.util.Arrays;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.sufficientlysecure.keychain.matcher.EditTextMatchers.withError;


/**
 * Wizard tests
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateKeyWizardActivityTest {
    public static final String SAMPLE_NAME = "Sample Name";
    public static final String SAMPLE_EMAIL = "sample_email@gmail.com";
    public static final String SAMPLE_ADDITIONAL_EMAIL = "sample_additional_email@gmail.com";
    public static final String SAMPLE_PASSWORD = "sample_password";
    public static final char[] PIN = {'2', '5', '8', '0'};

    @Rule
    public final ActivityTestRule<CreateKeyWizardActivity> mActivity
            = new ActivityTestRule<CreateKeyWizardActivity>(CreateKeyWizardActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            return intent;
        }
    };

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * UNIT TESTING
     */

    /**
     * Test if it does not contain keys
     */
    @Test
    public void testHasContainsKeys() {
        CreateKeyWizardActivity activity = mActivity.getActivity();
        byte[] fingerPrints = {0x00, 0x00, 0x01};

        assertTrue(activity.containsKeys(fingerPrints));
    }

    /**
     * Test if it does not contain keys
     */
    @Test
    public void testNotContainsKeys() {
        CreateKeyWizardActivity activity = mActivity.getActivity();
        byte[] fingerPrints = {0x00, 0x00, 0x00};

        assertFalse(activity.containsKeys(fingerPrints));
    }

    /**
     * Test the activity for first time usage.
     */
    @Test
    public void testPinCreation() {
        CreateKeyWizardActivity activity = mActivity.getActivity();
        ViewInteraction nextButton = onView(withId(R.id.nextButton));

        // Clicks create my key
        onView(withId(R.id.create_key_create_key_button)).perform(click());

        // Selects Pin option
        onView(withId(R.id.radioPinUnlock)).perform(click());

        // Clicks next
        nextButton.perform(click());

        //checks if the secret key type is of type PIN
        assertEquals(activity.getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PIN);

        //input the pin
        onView(withId(R.id.unlockKey2)).perform(click());
        onView(withId(R.id.unlockKey5)).perform(click());
        onView(withId(R.id.unlockKey8)).perform(click());
        onView(withId(R.id.unlockKey0)).perform(click());

        // Clicks next
        nextButton.perform(click());

        //re-input the pin
        onView(withId(R.id.unlockKey2)).perform(click());
        onView(withId(R.id.unlockKey5)).perform(click());
        onView(withId(R.id.unlockKey8)).perform(click());
        onView(withId(R.id.unlockKey0)).perform(click());

        // Clicks next
        nextButton.perform(click());

        // Clicks next
        nextButton.perform(click());

        //check passphrase
        assertNotNull(activity.getPassphrase());
        assertTrue(Arrays.equals(activity.getPassphrase().getCharArray(), PIN));

        // Clicks next with empty name
        nextButton.perform(click());
        onView(withId(R.id.create_key_name)).check(matches(withError(R.string.create_key_empty)));

        // Types name
        onView(withId(R.id.create_key_name)).perform(typeText(SAMPLE_NAME));

        // Closes the keyboard
        onView(withId(R.id.create_key_name)).perform(CustomActions.closeSoftKeyboard());

        // Clicks next
        nextButton.perform(click());

        // checks if the name has been saved.
        assertTrue(activity.getName().equals(SAMPLE_NAME));

        // Types email
        onView(withId(R.id.create_key_email)).perform(typeText(SAMPLE_EMAIL));

        // Adds same email as additional email and dismisses the snackbar
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_EMAIL));
        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .perform(swipeLeft());

        // Adds additional email
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_ADDITIONAL_EMAIL));
        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
        onView(withId(R.id.create_key_emails))
                .check(matches(hasDescendant(allOf(withId(R.id.create_key_email_item_email),
                        withText(SAMPLE_ADDITIONAL_EMAIL)))));

        // Removes additional email and clicks next
        onView(allOf(withId(R.id.create_key_email_item_delete_button),
                hasSibling(allOf(withId(R.id.create_key_email_item_email),
                        withText(SAMPLE_ADDITIONAL_EMAIL)))))
                .perform(click())
                .check(doesNotExist());

        // Closes the keyboard
        onView(withId(R.id.create_key_add_email)).perform(CustomActions.closeSoftKeyboard());

        // Clicks next
        nextButton.perform(click());

        // checks if the email has been saved.
        assertTrue(activity.getEmail().equals(SAMPLE_EMAIL));

        //checks final name and email
        onView(withId(R.id.name)).check(matches(withText(SAMPLE_NAME)));
        onView(withId(R.id.email)).check(matches(withText(SAMPLE_EMAIL)));

        //opens edit key
        onView(withId(R.id.create_key_edit_button)).perform(click());
        onView(ViewMatchers.isRoot()).perform(pressBack());

        //check wizard view model data consistency
        assertNotNull(activity.getPassphrase());
        assertTrue(Arrays.equals(activity.getPassphrase().getCharArray(), PIN));
        assertEquals(activity.getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PIN);
        assertTrue(activity.getName().equals(SAMPLE_NAME));
        assertTrue(activity.getEmail().equals(SAMPLE_EMAIL));
        assertTrue(activity.getAdditionalEmails() == null || activity.getAdditionalEmails().size() == 0);
        assertTrue(!activity.isFirstTime());

        // Clicks next
        nextButton.perform(click());
    }
}