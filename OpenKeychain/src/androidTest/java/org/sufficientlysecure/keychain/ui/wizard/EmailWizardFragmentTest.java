package org.sufficientlysecure.keychain.ui.wizard;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.actions.CustomActions;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EmailWizardFragmentTest {
    public static final String SAMPLE_EMAIL = "openkeychain@openkeychain.org";
    public static final String SAMPLE_WRONG_EMAIL = "openkeychain.@org";
    public static final String SAMPLE_ADDITIONAL_EMAIL = "additionalopenkeychain@openkeychain.org";
    private EmailWizardFragment mFragment;

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
    public void setUp() throws Throwable {
        //force the email fragment to load.
        mActivity.getActivity().onEmailState();
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getActivity().getSupportFragmentManager().executePendingTransactions();
            }
        });

        mFragment = (EmailWizardFragment) mActivity.getActivity().getCurrentVisibleFragment();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests if a valid email is actually valid.
     */
    @Test
    public void testIsEmailFormatValid() {
        assertTrue(mFragment.isEmailFormatValid(SAMPLE_EMAIL));
    }

    /**
     * Tests if a invalid email is not actually valid.
     */
    @Test
    public void testIsEmailFormatValidWrongFormat() {
        assertFalse(mFragment.isEmailFormatValid(SAMPLE_WRONG_EMAIL));
    }

    /**
     * Test additional emails length if no additional email is present.
     */
    @Test
    public void testGetAdditionalEmailsEmpty() {
        assertTrue(mFragment.getAdditionalEmails().size() == 0);
    }

    /**
     * Test additional emails length and item content for exactly one item.
     */
    @Test
    public void testGetAdditionalEmailsNotEmpty() {
        mFragment.onRequestAddEmail(SAMPLE_ADDITIONAL_EMAIL);
        assertTrue(mFragment.getAdditionalEmails().size() == 1);
        assertThat(mFragment.getAdditionalEmails(), hasItem(SAMPLE_ADDITIONAL_EMAIL));
    }

    /**
     * Test checkEmailValid method with a valid main email and no additional email being added.
     */
    @Test
    public void testCheckEmailValidMainEmail() {
        assertTrue(mFragment.checkEmail(SAMPLE_EMAIL, false));
    }

    /**
     * Test checkEmailValid method with a valid additional email and no main email being added.
     */
    @Test
    public void testCheckEmailValidAdditionalEmail() {
        assertTrue(mFragment.checkEmail(SAMPLE_ADDITIONAL_EMAIL, true));
    }

    /**
     * Test checkEmailValid method for main email duplication (The user added an additional email
     * before adding the main email).
     */
    @Test
    public void testCheckEmailValidMainDuplicated() {
        mFragment.onRequestAddEmail(SAMPLE_EMAIL);
        assertFalse(mFragment.checkEmail(SAMPLE_EMAIL, false));
        assertTrue(mFragment.isEmailDuplicatedInsideAdapter(SAMPLE_EMAIL));
    }

    /**
     * Test checkEmailValid method for additional email duplication (The user added a main email
     * before adding the additional email).
     */
    @Test
    public void testCheckEmailValidAdditionalEmailDuplicated() {
        onView(withId(R.id.create_key_email)).perform(typeText(SAMPLE_EMAIL));
        assertFalse(mFragment.checkEmail(SAMPLE_EMAIL, true));
        assertFalse(mFragment.isEmailDuplicatedInsideAdapter(SAMPLE_EMAIL));
    }

    /**
     * General interface test.
     */
    @Test
    public void testUI() {
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
    }
}
