package org.sufficientlysecure.keychain.ui.keyunlock.activities;


/**
 * Communication between the Activity and its fragments
 */
public interface CreateKeyWizardListener {
    /**
     * Method that is triggered when the user clicks on the next button
     * @return
     */
    boolean onNextClicked();

    /**
     * Method that is triggered when the user clicks on the back button
     */
    void onBackClicked();
}
