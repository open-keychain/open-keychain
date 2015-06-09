package org.sufficientlysecure.keychain.ui.keyunlock.activities;

import org.sufficientlysecure.keychain.ui.keyunlock.Model.WizardModel;

/**
 * Wizard common interface communication
 */
public interface WizardCommonListener {

    /**
     * Hides the back and next buttons from the screen
     * @param hide
     */
    void onHideNavigationButtons(boolean hide);

    /**
     * Forces the wizard to advance to the next wizard screen
     */
    void onAdvanceToNextWizardStep();


    /**
     * Obtains the Wizard Data Model.
     * @return The Model itself
     */
    WizardModel getModel();
}
