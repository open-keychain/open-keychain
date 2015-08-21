package org.sufficientlysecure.keychain.ui.wizard;

import android.content.Intent;
import android.support.test.espresso.ViewInteraction;
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
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.sufficientlysecure.keychain.matcher.EditTextMatchers.withError;

@RunWith(AndroidJUnit4.class)
public class NameWizardFragmentTest {
    public static final String SAMPLE_NAME = "Sample Name";
    private NameWizardFragment mFragment;

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
    public void setUp() {
        //force the name fragment to load.
        mActivity.getActivity().onNameState();
        try {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.getActivity().getSupportFragmentManager().executePendingTransactions();
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        mFragment = (NameWizardFragment) mActivity.getActivity().getCurrentVisibleFragment();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test the method isTextEmpty for non empty text.
     */
    @Test
    public void testIsTextEmptyNotEmpty() {
        assertThat(mFragment.isTextEmpty(SAMPLE_NAME), is(false));
    }

    /**
     * Test the method isTextEmpty for empty text.
     */
    @Test
    public void testIsTextEmptyEmpty() {
        assertThat(mFragment.isTextEmpty(""), is(true));
    }

    /**
     * General interface test.
     */
    @Test
    public void testUI() {
        ViewInteraction nextButton = onView(withId(R.id.nextButton));
        nextButton.perform(click());

        onView(withId(R.id.create_key_name)).check(matches(withError(R.string.create_key_empty)));

        // Types name
        onView(withId(R.id.create_key_name)).perform(typeText(SAMPLE_NAME));

        // Closes the keyboard
        onView(withId(R.id.create_key_name)).perform(CustomActions.closeSoftKeyboard());

        // Clicks next
        nextButton.perform(click());

        // checks if the name has been saved.
        assertTrue(mActivity.getActivity().getName().equals(SAMPLE_NAME));
    }
}
