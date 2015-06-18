package org.sufficientlysecure.keychain.ui.keyunlock.base;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.PassphraseWizardActivity;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.ui.keyunlock.Model.WizardModel;

import java.util.ArrayList;

/**
 * Wizard common interface communication
 */
public interface WizardFragmentListener {

    /**
     * Hides the back and next buttons from the screen
     *
     * @param hide
     */
    void onHideNavigationButtons(boolean hide);

    /**
     * Forces the wizard to advance to the next wizard screen
     *
     * @deprecated
     */
    void onAdvanceToNextWizardStep();

    /**
     * Sets the chosen unlock method.
     */

    void setUnlockMethod(CanonicalizedSecretKey.SecretKeyType secretKeyType);

    void setPassphrase(Passphrase passphrase);

    void setUserName(CharSequence userName);

    void setAdditionalEmails(ArrayList<String> additionalEmails);

    void setEmail(CharSequence email);

    CharSequence getName();

    CharSequence getEmail();

    ArrayList<String> getAdditionalEmails();

    Passphrase getPassphrase();


    /**
     * Notifies the activity of the current fragment state
     */

    void onWizardFragmentVisible(WizardFragment fragment);

    /**
     * Cancels the key creation process.
     * @return
     */
    void cancelRequest();


}
