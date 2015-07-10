package org.sufficientlysecure.keychain.ui.base;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Wizard common interface communication
 */
public interface WizardFragmentListener {

	/**
	 * Hides the back and next buttons from the screen
	 *
	 * @param hideBack
	 * @param hideNext
	 */
	void onHideNavigationButtons(boolean hideBack, boolean hideNext);

	/**
	 * Forces the wizard to advance to the next wizard screen
	 */
	void onAdvanceToNextWizardStep();

	/**
	 * Sets the chosen unlock method.
	 */

	void setUnlockMethod(CanonicalizedSecretKey.SecretKeyType secretKeyType);

	/**
	 * Sets the passphrase.
	 *
	 * @param passphrase
	 */
	void setPassphrase(Passphrase passphrase);

	/**
	 * Sets the user name.
	 *
	 * @param userName
	 */
	void setUserName(CharSequence userName);

	/**
	 * Sets the additional user email's.
	 *
	 * @param additionalEmails
	 */
	void setAdditionalEmails(ArrayList<String> additionalEmails);

	/**
	 * Sets the user email address.
	 *
	 * @param email
	 */
	void setEmail(CharSequence email);

	/**
	 * Returns the user name.
	 *
	 * @return
	 */
	CharSequence getName();

	/**
	 * Returns the user email.
	 *
	 * @return
	 */
	CharSequence getEmail();

	/**
	 * Returns the additional emails.
	 *
	 * @return
	 */
	ArrayList<String> getAdditionalEmails();

	/**
	 * Returns the passphrase.
	 *
	 * @return
	 */
	Passphrase getPassphrase();

	/**
	 * Returns the secret key type.
	 *
	 * @return
	 */
	CanonicalizedSecretKey.SecretKeyType getSecretKeyType();


	/**
	 * Notifies the activity of the current fragment state.
	 */

	void onWizardFragmentVisible(WizardFragment fragment);

	/**
	 * Cancels the key creation process.
	 *
	 * @return
	 */
	void cancelRequest();

	/**
	 * Method that returns true if it's the first time the user is creating a key.
	 */
	boolean isFirstTime();

	/**
	 * Uses a Yubi Key wizard instead of the normal wizard process.
	 */
	void setUseYubiKey();

	boolean createYubiKey();

	Passphrase getYubiKeyAdminPin();

	Passphrase getYubiKeyPin();

	//Yubi Key callbacks
	byte[] nfcGetFingerprints() throws IOException;

	byte[] nfcGetAid() throws IOException;

	String nfcGetUserId() throws IOException;
}
