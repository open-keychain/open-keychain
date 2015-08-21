package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.resources.NFCWizardIdlingResource;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.Espresso.unregisterIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NFCUnlockWizardFragmentTest {
    private NFCUnlockWizardFragment mFragment;

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
        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(5 * 1000 * 60, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(5 * 1000 * 60, TimeUnit.MILLISECONDS);

        //force the nfc fragment to load.
        mActivity.getActivity().onInstantiateNFCUnlockMethod();
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

        mFragment = (NFCUnlockWizardFragment) mActivity.getActivity().getCurrentVisibleFragment();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test the method generateSecureRoomPin for proper 128 bit hash generation.
     */
    @Test
    public void testGenerateSecureRoomPin() throws NoSuchAlgorithmException {
        assertTrue(mFragment.generateSecureRoomPin().length == 16);
    }

    /**
     * General interface test.
     */
    @Test
    public void testUI() {
        final CreateKeyWizardActivity activity = mActivity.getActivity();
        IdlingResource nfcIdlingResource = new NFCWizardIdlingResource(activity);
        registerIdlingResources(nfcIdlingResource);

        ViewInteraction nextButton = onView(withId(R.id.nextButton));

        nextButton.perform(click());

        //check passphrase
        assertNotNull(activity.getPassphrase());
        assertEquals(activity.getPassphrase().getSecretKeyType(), CanonicalizedSecretKey.SecretKeyType.NFC_TAG);

        unregisterIdlingResources(nfcIdlingResource);
    }
}
