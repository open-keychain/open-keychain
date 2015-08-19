package org.sufficientlysecure.keychain.resources;

import android.support.test.espresso.IdlingResource;

import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.wizard.NFCUnlockWizardFragment;

/**
 * Espresso idling resource that waits for the user to put the nfc tag on the back of the device.
 */
public class NFCWizardIdlingResource implements IdlingResource {
    private CreateKeyWizardActivity mCreateKeyWizardActivity;
    private ResourceCallback mResourceCallback;

    public NFCWizardIdlingResource(CreateKeyWizardActivity createKeyWizardActivity) {
        mCreateKeyWizardActivity = createKeyWizardActivity;
    }

    @Override
    public String getName() {
        return NFCWizardIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = true;
        if (mCreateKeyWizardActivity.getCurrentVisibleFragment() instanceof NFCUnlockWizardFragment) {
            NFCUnlockWizardFragment nfcUnlockWizardFragment = (NFCUnlockWizardFragment)
                    mCreateKeyWizardActivity.getCurrentVisibleFragment();

            //Not in idle state if it has a nfc connection or the nfc tech is null (this means the user
            //did not put the card in the back of the phone.
            idle = nfcUnlockWizardFragment.getNfcTechnology() != null && !nfcUnlockWizardFragment.
                    getNfcTechnology().isConnected();
        }

        if (idle) {
            mResourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}
