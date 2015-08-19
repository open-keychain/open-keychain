package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.eftimoff.patternview.PatternView;
import com.eftimoff.patternview.cells.Cell;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.TestHelpers;
import org.sufficientlysecure.keychain.actions.CustomActions;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.resources.DialogFragmentIdlingResource;
import org.sufficientlysecure.keychain.resources.NFCWizardIdlingResource;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.wizard.PatternUnlockWizardFragment;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.Espresso.unregisterIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.action.ViewActions.scrollTo;
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
    public static final String PATTERN = "000-000&000-001&000-002&000-003";
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
        TestHelpers.cleanupDatabase(mActivity.getActivity());
        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(5 * 1000 * 60, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(5 * 1000 * 60, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() throws Exception {
        TestHelpers.cleanupDatabase(mActivity.getActivity());
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
     * Test containsKeys with a null fingerprint.
     */
    @Test
    public void testContainsKeysNullParam() {
        CreateKeyWizardActivity activity = mActivity.getActivity();
        assertFalse(activity.containsKeys(null));
    }

    /**
     * Test the activity for first time usage.
     */
    @Test
    public void testPinCreation() {
        CreateKeyWizardActivity activity = mActivity.getActivity();

        IdlingResource idlingResource = new DialogFragmentIdlingResource(activity.getSupportFragmentManager(),
                ServiceProgressHandler.TAG_PROGRESS_DIALOG);
        registerIdlingResources(idlingResource);

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
        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PIN);
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

        // Closes the keyboard
        onView(withId(R.id.create_key_email)).perform(CustomActions.closeSoftKeyboard());

        // Adds same email as additional email and dismisses the snackbar
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .perform(swipeLeft());

        // Adds additional email
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_ADDITIONAL_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        //click dialog OK
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

        unregisterIdlingResources(idlingResource);
    }

    @Test
    public void testPatternCreation() {
        CreateKeyWizardActivity activity = mActivity.getActivity();

        IdlingResource idlingResource = new DialogFragmentIdlingResource(activity.getSupportFragmentManager(),
                ServiceProgressHandler.TAG_PROGRESS_DIALOG);
        registerIdlingResources(idlingResource);

        ViewInteraction nextButton = onView(withId(R.id.nextButton));

        // Clicks create my key
        onView(withId(R.id.create_key_create_key_button)).perform(click());

        // Selects Pattern option
        onView(withId(R.id.radioPatternUnlock)).perform(scrollTo(), click());

        // Clicks next
        nextButton.perform(click());

        //checks if the secret key type is of type PATTERN
        assertEquals(activity.getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PATTERN);

        //"input" the pattern
        final PatternUnlockWizardFragment patternUnlockWizardFragment = (PatternUnlockWizardFragment)
                activity.getCurrentVisibleFragment();

        assertNotNull(patternUnlockWizardFragment);

        final List<Cell> cells = new ArrayList<>();
        cells.add(new Cell(0, 0));
        cells.add(new Cell(0, 1));
        cells.add(new Cell(0, 2));
        cells.add(new Cell(0, 3));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                patternUnlockWizardFragment.getPatternView().setPattern(PatternView.DisplayMode.Correct, cells);
                patternUnlockWizardFragment.appendPattern(patternUnlockWizardFragment.getPatternView().patternToString());
            }
        });

        nextButton.perform(click());

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                patternUnlockWizardFragment.getPatternView().setPattern(PatternView.DisplayMode.Correct, cells);
                patternUnlockWizardFragment.appendPattern(patternUnlockWizardFragment.getPatternView().patternToString());
            }
        });

        nextButton.perform(click());
        nextButton.perform(click());

        //check passphrase
        assertNotNull(activity.getPassphrase());
        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PATTERN);

        try {
            Passphrase patternPassphrase = patternUnlockWizardFragment.encodePassphrase(PATTERN);
            assertTrue(Arrays.equals(activity.getPassphrase().getCharArray(), patternPassphrase.getCharArray()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

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

        // Closes the keyboard
        onView(withId(R.id.create_key_email)).perform(CustomActions.closeSoftKeyboard());

        // Adds same email as additional email and dismisses the snackbar
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .perform(swipeLeft());

        // Adds additional email
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_ADDITIONAL_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        //click dialog OK
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
        try {
            Passphrase patternPassphrase = patternUnlockWizardFragment.encodePassphrase(PATTERN);
            assertTrue(Arrays.equals(activity.getPassphrase().getCharArray(), patternPassphrase.getCharArray()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.PATTERN);
        assertTrue(activity.getName().equals(SAMPLE_NAME));
        assertTrue(activity.getEmail().equals(SAMPLE_EMAIL));
        assertTrue(activity.getAdditionalEmails() == null || activity.getAdditionalEmails().size() == 0);
        assertTrue(!activity.isFirstTime());

        // Clicks next
        nextButton.perform(click());

        unregisterIdlingResources(idlingResource);
    }

    @Test
    public void testNFCCreation() {
        CreateKeyWizardActivity activity = mActivity.getActivity();

        IdlingResource idlingResource = new DialogFragmentIdlingResource(activity.getSupportFragmentManager(),
                ServiceProgressHandler.TAG_PROGRESS_DIALOG);
        registerIdlingResources(idlingResource);

        IdlingResource nfcIdlingResource = new NFCWizardIdlingResource(activity);
        registerIdlingResources(nfcIdlingResource);

        ViewInteraction nextButton = onView(withId(R.id.nextButton));

        // Clicks create my key
        onView(withId(R.id.create_key_create_key_button)).perform(click());

        // Selects NFC option
        onView(withId(R.id.radioNFCUnlock)).perform(scrollTo(), click());

        // Clicks next
        nextButton.perform(click());

        //checks if the secret key type is of type NFC
        assertEquals(activity.getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.NFC_TAG);
        nextButton.perform(click());

        //check passphrase
        assertNotNull(activity.getPassphrase());
        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.NFC_TAG);

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

        // Closes the keyboard
        onView(withId(R.id.create_key_email)).perform(CustomActions.closeSoftKeyboard());

        // Adds same email as additional email and dismisses the snackbar
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .perform(swipeLeft());

        // Adds additional email
        onView(withId(R.id.create_key_add_email)).perform(click());
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());
        onView(withId(R.id.add_email_address)).perform(typeText(SAMPLE_ADDITIONAL_EMAIL));

        // Closes the keyboard
        onView(withId(R.id.add_email_address)).perform(CustomActions.closeSoftKeyboard());

        //click dialog OK
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
        assertTrue(activity.getPassphrase().getCharArray().length == 16);
        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.NFC_TAG);
        assertTrue(activity.getName().equals(SAMPLE_NAME));
        assertTrue(activity.getEmail().equals(SAMPLE_EMAIL));
        assertTrue(activity.getAdditionalEmails() == null || activity.getAdditionalEmails().size() == 0);
        assertTrue(!activity.isFirstTime());

        // Clicks next
        nextButton.perform(click());

        unregisterIdlingResources(idlingResource);
        unregisterIdlingResources(nfcIdlingResource);
    }
}