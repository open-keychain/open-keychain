package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;

import com.eftimoff.patternview.PatternView;
import com.eftimoff.patternview.cells.Cell;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;

public class PatternUnlockWizardFragmentTest {
    public static final String PATTERN = "000-000&000-001&000-002&000-003";
    private PatternUnlockWizardFragment mFragment;

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

        //force the nfc fragment to load.
        mActivity.getActivity().onInstantiatePatternUnlockMethod();
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

        mFragment = (PatternUnlockWizardFragment) mActivity.getActivity().getCurrentVisibleFragment();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests the method clearInputKeyword for empty pattern.
     */
    @Test
    public void testClearInputKeyword() {
        mFragment.appendPattern(PATTERN);
        assertThat(mFragment.getCurrentInputKeyWord().toString(), equalTo(PATTERN));
        mFragment.clearInputKeyword();
        assertThat(mFragment.getCurrentInputKeyWord().toString(), equalTo(""));
        assertThat(mFragment.getLastInputKeyWord().toString(), equalTo(""));
    }

    /**
     * Test the method EncodePassphrase for proper encoded array size.
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testEncodePassphrase() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Passphrase passphrase = mFragment.encodePassphrase(PATTERN);
        assertTrue(passphrase.getCharArray().length == 32);
    }

    /**
     * General interface test.
     */
    @Test
    public void testUI() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        ViewInteraction nextButton = onView(withId(R.id.nextButton));
        final CreateKeyWizardActivity activity = mActivity.getActivity();

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

        Passphrase patternPassphrase = patternUnlockWizardFragment.encodePassphrase(PATTERN);
        assertTrue(Arrays.equals(activity.getPassphrase().getCharArray(), patternPassphrase.getCharArray()));
    }
}
